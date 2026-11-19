package cn.charlotte.pit.listener;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.event.PitProfileLoadedEvent;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.item.MythicBundleUuidUtil;
import cn.charlotte.pit.util.item.MythicOwnershipUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import de.tr7zw.nbtapi.NBT;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Confiscates UUID-blacklisted items after a profile has been loaded. MongoDB
 * access is always asynchronous; Bukkit inventory access is always performed
 * on the server thread.
 */
@AutoRegister
public class ItemUuidBlacklistListener implements Listener {

    private static final int MAIN_INVENTORY_SIZE = 36;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProfileLoaded(PitProfileLoadedEvent event) {
        PlayerProfile profile = event.getPlayerProfile();
        Set<String> candidateUuids = collectCandidateUuids(profile);
        if (candidateUuids.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            Map<String, Document> blacklistEntries;
            try {
                blacklistEntries = loadBlacklistEntries(candidateUuids);
            } catch (Exception exception) {
                ThePit.getInstance().getSLF4JLogger().error(
                        "Unable to query item UUID blacklist for {} ({})",
                        profile.getPlayerName(), profile.getPlayerUUID(), exception);
                return;
            }
            if (blacklistEntries.isEmpty()) {
                return;
            }

            Bukkit.getScheduler().runTask(ThePit.getInstance(), () ->
                    prepareConfiscation(profile, blacklistEntries));
        });
    }

    private Map<String, Document> loadBlacklistEntries(Set<String> candidateUuids) {
        MongoCollection<Document> collection = ThePit.getInstance()
                .getMongoDB()
                .getItemUuidBlacklistCollection();
        Map<String, Document> result = new HashMap<>();
        for (Document document : collection.find(Filters.and(
                Filters.in("itemUuid", candidateUuids),
                Filters.ne("active", false)))) {
            String itemUuid = normalizeUuid(document.getString("itemUuid"));
            if (itemUuid != null) {
                result.put(itemUuid, document);
            }
        }
        return result;
    }

    private void prepareConfiscation(PlayerProfile profile,
                                     Map<String, Document> blacklistEntries) {
        Player player = Bukkit.getPlayer(profile.getPlayerUUID());
        List<ConfiscationTarget> targets = new ArrayList<>();
        if (player != null && player.isOnline()) {
            collectOnlineInventoryTargets(player, blacklistEntries, targets);
        } else {
            collectStoredInventoryTargets(profile, blacklistEntries, targets);
        }
        collectWarehouseTargets(profile, blacklistEntries, targets);
        if (targets.isEmpty()) {
            return;
        }

        long capturedAt = System.currentTimeMillis();
        List<Document> records = new ArrayList<>(targets.size());
        for (ConfiscationTarget target : targets) {
            target.confiscationId = UUID.randomUUID().toString();
            target.serializedItem = InventoryUtil.serializeItemStack(target.snapshot);
            records.add(createPendingRecord(profile, target, capturedAt));
        }

        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            try {
                ThePit.getInstance()
                        .getMongoDB()
                        .getItemUuidConfiscationCollection()
                        .insertMany(records);
            } catch (Exception exception) {
                ThePit.getInstance().getSLF4JLogger().error(
                        "Unable to persist pending blacklist confiscations for {} ({}); no items were removed",
                        profile.getPlayerName(), profile.getPlayerUUID(), exception);
                return;
            }

            Bukkit.getScheduler().runTask(ThePit.getInstance(), () ->
                    removePersistedTargets(profile, targets));
        });
    }

    private Document createPendingRecord(PlayerProfile profile,
                                         ConfiscationTarget target,
                                         long capturedAt) {
        ItemStack item = target.snapshot;
        ItemMeta meta = item.getItemMeta();
        Document record = new Document("_id", target.confiscationId)
                .append("status", "PENDING")
                .append("itemUuid", target.matchedUuid)
                .append("containerItemUuid", normalizeUuid(ItemUtil.getUUID(item)))
                .append("playerUuid", profile.getPlayerUUID().toString())
                .append("playerName", profile.getPlayerName())
                .append("prestige", profile.getPrestige())
                .append("capturedAt", capturedAt)
                .append("source", target.source.databaseName)
                .append("location", target.location())
                .append("slot", target.slot)
                .append("warehouseIndex", target.warehouseIndex)
                .append("material", item.getType().getKey().getKey())
                .append("amount", item.getAmount())
                .append("internalName", ItemUtil.getInternalName(item))
                .append("displayName", meta != null && meta.hasDisplayName() ? meta.getDisplayName() : null)
                .append("lore", meta != null && meta.hasLore() ? meta.getLore() : List.of())
                .append("nbt", NBT.get(item, nbt -> {
                    return nbt.toString();
                }))
                .append("serializedItem", target.serializedItem);

        Document blacklist = target.blacklistEntry;
        if (blacklist != null) {
            record.append("blacklistedAt", blacklist.getLong("createdAt"))
                    .append("blacklistedByUuid", blacklist.getString("createdByUuid"))
                    .append("blacklistedByName", blacklist.getString("createdByName"));
        }
        return record;
    }

    private void removePersistedTargets(PlayerProfile profile,
                                        List<ConfiscationTarget> targets) {
        Player player = Bukkit.getPlayer(profile.getPlayerUUID());
        boolean online = player != null && player.isOnline();
        int removedStacks = 0;
        int removedItems = 0;
        long completedAt = System.currentTimeMillis();

        for (ConfiscationTarget target : targets) {
            ItemStack current = currentItem(profile, online ? player : null, target);
            boolean removed = current != null
                    && target.matchedUuid.equals(findBlacklistedUuid(current, Set.of(target.matchedUuid)))
                    && target.snapshot.equals(current);
            if (removed) {
                clearItem(profile, online ? player : null, target);
                removedStacks++;
                removedItems += Math.max(1, current.getAmount());
            }
            target.removed = removed;
        }

        if (removedStacks > 0 && online) {
            profile.setInventory(InventoryUtil.playerInventoryFromPlayer(player));
        }

        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            updateConfiscationResults(targets, completedAt);
        });

        if (removedStacks <= 0) {
            return;
        }

        ThePit.getInstance().getSLF4JLogger().warn(
                "Confiscated {} blacklisted item stack(s), {} item(s), from {} ({})",
                removedStacks, removedItems, profile.getPlayerName(), profile.getPlayerUUID());
        if (online) {
            player.sendMessage(CC.translate("&c系统检测到黑名单 UUID 道具，已回收 &f"
                    + removedStacks + " &c组物品。回收详情已保存在数据库中。"));
        }
    }

    private void updateConfiscationResults(List<ConfiscationTarget> targets,
                                           long completedAt) {
        List<UpdateOneModel<Document>> updates = new ArrayList<>(targets.size());
        for (ConfiscationTarget target : targets) {
            updates.add(new UpdateOneModel<>(
                    Filters.eq("_id", target.confiscationId),
                    Updates.combine(
                            Updates.set("status", target.removed ? "CONFISCATED" : "NOT_FOUND_OR_CHANGED"),
                            Updates.set("completedAt", completedAt),
                            target.removed
                                    ? Updates.set("confiscatedAt", completedAt)
                                    : Updates.unset("confiscatedAt")
                    )));
        }
        try {
            ThePit.getInstance()
                    .getMongoDB()
                    .getItemUuidConfiscationCollection()
                    .bulkWrite(updates);
        } catch (Exception exception) {
            ThePit.getInstance().getSLF4JLogger().error(
                    "Unable to update item UUID confiscation results", exception);
        }
    }

    private Set<String> collectCandidateUuids(PlayerProfile profile) {
        Set<String> result = new HashSet<>();
        PlayerInv inventory = profile.getInventory();
        if (inventory != null) {
            collectItemUuids(inventory.getContents(), result);
            collectItemUuids(inventory.getArmorContents(), result);
        }
        PlayerEnderChest[] warehouses = profile.getEnderChests();
        if (warehouses != null) {
            for (PlayerEnderChest warehouse : warehouses) {
                if (warehouse != null && warehouse.getInventory() != null) {
                    collectItemUuids(warehouse.getInventory().getContents(), result);
                }
            }
        }
        return result;
    }

    private void collectItemUuids(ItemStack[] items, Set<String> result) {
        if (items == null) {
            return;
        }
        for (ItemStack item : items) {
            collectItemUuids(item, result);
        }
    }

    private void collectItemUuids(ItemStack item, Set<String> result) {
        if (isEmpty(item)) {
            return;
        }
        String directUuid = normalizeUuid(ItemUtil.getUUID(item));
        if (directUuid != null) {
            result.add(directUuid);
        }
        if (!MythicOwnershipUtil.isFullEquipmentBundle(item)) {
            return;
        }
        for (int index = 0; index < MythicBundleUuidUtil.BUNDLE_SIZE; index++) {
            String bundledUuid = normalizeUuid(MythicBundleUuidUtil.getStoredUuid(item, index));
            if (bundledUuid != null) {
                result.add(bundledUuid);
            }
        }
    }

    private void collectOnlineInventoryTargets(Player player,
                                               Map<String, Document> blacklistEntries,
                                               List<ConfiscationTarget> result) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < MAIN_INVENTORY_SIZE; slot++) {
            addTarget(inventory.getItem(slot), Source.INVENTORY, slot, -1, blacklistEntries, result);
        }
        ItemStack[] armor = inventory.getArmorContents();
        for (int slot = 0; slot < armor.length; slot++) {
            addTarget(armor[slot], Source.ARMOR, slot, -1, blacklistEntries, result);
        }
        addTarget(inventory.getItemInOffHand(), Source.OFFHAND, 0, -1, blacklistEntries, result);
    }

    private void collectStoredInventoryTargets(PlayerProfile profile,
                                               Map<String, Document> blacklistEntries,
                                               List<ConfiscationTarget> result) {
        PlayerInv inventory = profile.getInventory();
        if (inventory == null) {
            return;
        }
        ItemStack[] contents = inventory.getContents();
        int length = Math.min(contents == null ? 0 : contents.length, MAIN_INVENTORY_SIZE);
        for (int slot = 0; slot < length; slot++) {
            addTarget(contents[slot], Source.INVENTORY, slot, -1, blacklistEntries, result);
        }
        ItemStack[] armor = inventory.getArmorContents();
        for (int slot = 0; slot < armor.length; slot++) {
            addTarget(armor[slot], Source.ARMOR, slot, -1, blacklistEntries, result);
        }
        if (contents != null && contents.length > 40) {
            addTarget(contents[40], Source.OFFHAND, 40, -1, blacklistEntries, result);
        }
    }

    private void collectWarehouseTargets(PlayerProfile profile,
                                         Map<String, Document> blacklistEntries,
                                         List<ConfiscationTarget> result) {
        PlayerEnderChest[] warehouses = profile.getEnderChests();
        if (warehouses == null) {
            return;
        }
        for (int warehouseIndex = 0; warehouseIndex < warehouses.length; warehouseIndex++) {
            PlayerEnderChest warehouse = warehouses[warehouseIndex];
            if (warehouse == null || warehouse.getInventory() == null) {
                continue;
            }
            for (int slot = 0; slot < warehouse.getInventory().getSize(); slot++) {
                addTarget(warehouse.getInventory().getItem(slot), Source.WAREHOUSE, slot,
                        warehouseIndex, blacklistEntries, result);
            }
        }
    }

    private void addTarget(ItemStack item,
                           Source source,
                           int slot,
                           int warehouseIndex,
                           Map<String, Document> blacklistEntries,
                           List<ConfiscationTarget> result) {
        String matchedUuid = findBlacklistedUuid(item, blacklistEntries.keySet());
        if (matchedUuid == null) {
            return;
        }
        result.add(new ConfiscationTarget(source, slot, warehouseIndex, item.clone(),
                matchedUuid, blacklistEntries.get(matchedUuid)));
    }

    private String findBlacklistedUuid(ItemStack item, Set<String> blacklistUuids) {
        if (isEmpty(item)) {
            return null;
        }
        String directUuid = normalizeUuid(ItemUtil.getUUID(item));
        if (directUuid != null && blacklistUuids.contains(directUuid)) {
            return directUuid;
        }
        if (!MythicOwnershipUtil.isFullEquipmentBundle(item)) {
            return null;
        }
        for (int index = 0; index < MythicBundleUuidUtil.BUNDLE_SIZE; index++) {
            String bundledUuid = normalizeUuid(MythicBundleUuidUtil.getStoredUuid(item, index));
            if (bundledUuid != null && blacklistUuids.contains(bundledUuid)) {
                return bundledUuid;
            }
        }
        return null;
    }

    private ItemStack currentItem(PlayerProfile profile,
                                  Player player,
                                  ConfiscationTarget target) {
        if (target.source == Source.WAREHOUSE) {
            PlayerEnderChest[] warehouses = profile.getEnderChests();
            if (warehouses == null || target.warehouseIndex < 0
                    || target.warehouseIndex >= warehouses.length
                    || warehouses[target.warehouseIndex] == null
                    || warehouses[target.warehouseIndex].getInventory() == null) {
                return null;
            }
            return warehouses[target.warehouseIndex].getInventory().getItem(target.slot);
        }
        if (player != null) {
            return switch (target.source) {
                case INVENTORY -> player.getInventory().getItem(target.slot);
                case ARMOR -> player.getInventory().getArmorContents()[target.slot];
                case OFFHAND -> player.getInventory().getItemInOffHand();
                case WAREHOUSE -> null;
            };
        }
        PlayerInv inventory = profile.getInventory();
        if (inventory == null) {
            return null;
        }
        return switch (target.source) {
            case INVENTORY -> itemAt(inventory.getContents(), target.slot);
            case ARMOR -> itemAt(inventory.getArmorContents(), target.slot);
            case OFFHAND -> itemAt(inventory.getContents(), target.slot);
            case WAREHOUSE -> null;
        };
    }

    private void clearItem(PlayerProfile profile,
                           Player player,
                           ConfiscationTarget target) {
        if (target.source == Source.WAREHOUSE) {
            profile.getEnderChests()[target.warehouseIndex].getInventory().setItem(target.slot, null);
            return;
        }
        if (player != null) {
            switch (target.source) {
                case INVENTORY -> player.getInventory().setItem(target.slot, null);
                case ARMOR -> {
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    armor[target.slot] = null;
                    player.getInventory().setArmorContents(armor);
                }
                case OFFHAND -> player.getInventory().setItemInOffHand(null);
                case WAREHOUSE -> {
                }
            }
            return;
        }
        PlayerInv inventory = profile.getInventory();
        switch (target.source) {
            case INVENTORY, OFFHAND -> {
                ItemStack[] contents = inventory.getContents();
                contents[target.slot] = null;
                inventory.setContents(contents);
            }
            case ARMOR -> {
                ItemStack[] armor = inventory.getArmorContents();
                armor[target.slot] = null;
                inventory.setArmorContents(armor);
            }
            case WAREHOUSE -> {
            }
        }
    }

    private ItemStack itemAt(ItemStack[] items, int slot) {
        return items == null || slot < 0 || slot >= items.length ? null : items[slot];
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }

    private String normalizeUuid(String itemUuid) {
        if (itemUuid == null || itemUuid.isBlank()) {
            return null;
        }
        return itemUuid.trim().toLowerCase(Locale.ROOT);
    }

    private enum Source {
        INVENTORY("inventory"),
        ARMOR("armor"),
        OFFHAND("offhand"),
        WAREHOUSE("warehouse");

        private final String databaseName;

        Source(String databaseName) {
            this.databaseName = databaseName;
        }
    }

    private static final class ConfiscationTarget {
        private final Source source;
        private final int slot;
        private final int warehouseIndex;
        private final ItemStack snapshot;
        private final String matchedUuid;
        private final Document blacklistEntry;
        private String confiscationId;
        private String serializedItem;
        private boolean removed;

        private ConfiscationTarget(Source source,
                                  int slot,
                                  int warehouseIndex,
                                  ItemStack snapshot,
                                  String matchedUuid,
                                  Document blacklistEntry) {
            this.source = source;
            this.slot = slot;
            this.warehouseIndex = warehouseIndex;
            this.snapshot = snapshot;
            this.matchedUuid = matchedUuid;
            this.blacklistEntry = blacklistEntry;
        }

        private String location() {
            return switch (source) {
                case INVENTORY -> "inventory[" + slot + "]";
                case ARMOR -> "armor[" + slot + "]";
                case OFFHAND -> "offhand";
                case WAREHOUSE -> "warehouse[" + warehouseIndex + "][" + slot + "]";
            };
        }
    }
}
