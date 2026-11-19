package cn.charlotte.pit.util.item;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class MythicOwnershipUtil {

    private static final Set<String> EQUIPMENT_INTERNAL_NAMES = Set.of(
            "mythic_sword",
            "mythic_bow",
            "mythic_leggings",
            "jewel_sword",
            "magic_fishing_rod",
            "angel_chestplate",
            "armageddon_boots",
            "kings_helmet",
            "Turtle_helmet",
            "lucky_chestplate",
            "MythicShovel",
            "exorcism_armor",
            "fly_boots"
    );

    private static final Set<String> FULL_EQUIPMENT_BUNDLES = Set.of(
            "pants_bundle_full",
            "sword_bundle_full",
            "bow_bundle_full"
    );

    private MythicOwnershipUtil() {
    }

    public static boolean isEquipment(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        String internalName = ItemUtil.getInternalName(item);
        return internalName != null && EQUIPMENT_INTERNAL_NAMES.contains(internalName);
    }

    public static boolean isFullEquipmentBundle(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        String internalName = ItemUtil.getInternalName(item);
        return internalName != null && FULL_EQUIPMENT_BUNDLES.contains(internalName);
    }

    public static boolean isTraded(ItemStack item) {
        if (!isEquipment(item)) {
            return false;
        }
        return NBT.get(item, nbt -> {
            ReadableNBT extra = nbt.getCompound("extra");
            return extra != null && ((extra.hasTag("traded") && extra.getBoolean("traded"))
                    || (extra.hasTag("tradeTime") && extra.getLong("tradeTime") > 0L));
        });
    }

    public static long getTradeTime(ItemStack item) {
        if (!isEquipment(item)) {
            return 0L;
        }
        return NBT.get(item, nbt -> {
            ReadableNBT extra = nbt.getCompound("extra");
            return extra != null && extra.hasTag("tradeTime") ? extra.getLong("tradeTime") : 0L;
        });
    }

    public static String getCreator(ItemStack item) {
        return getOwnershipString(item, "creator");
    }

    public static String getCreatorName(ItemStack item) {
        return getOwnershipString(item, "creatorName");
    }

    public static String getOwner(ItemStack item) {
        return getOwnershipString(item, "owner");
    }

    public static String getOwnerName(ItemStack item) {
        return getOwnershipString(item, "ownerName");
    }

    public static void bindInitial(ItemStack item, Player player) {
        AdvancedItemIdentity.ensureUuid(item);
        if (!isEquipment(item) || player == null) {
            return;
        }

        String playerUuid = player.getUniqueId().toString();
        String playerName = player.getName();
        boolean[] changed = {false};
        NBT.modify(item, nbt -> {
            var extra = nbt.getOrCreateCompound("extra");
            if (!extra.hasTag("creator") || extra.getString("creator").isBlank()) {
                extra.setString("creator", playerUuid);
                extra.setString("creatorName", playerName);
                changed[0] = true;
            }
            if (!extra.hasTag("owner") || extra.getString("owner").isBlank()) {
                extra.setString("owner", playerUuid);
                extra.setString("ownerName", playerName);
                changed[0] = true;
            }
            if (!extra.hasTag("traded")) {
                extra.setBoolean("traded", extra.hasTag("tradeTime") && extra.getLong("tradeTime") > 0L);
                changed[0] = true;
            }
        });
        if (changed[0]) {
            Utils.invalidateMythicItemCache(item);
        }
    }

    public static void bindInitial(Collection<ItemStack> items, Player player) {
        if (items == null) {
            return;
        }
        items.forEach(item -> bindInitial(item, player));
    }

    public static void bindInventory(Player player) {
        if (player == null) {
            return;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            bindInitial(item, player);
        }
    }

    public static void bindProfile(PlayerProfile profile, Player player) {
        if (profile == null || player == null) {
            return;
        }

        PlayerInv inventory = profile.getInventory();
        if (inventory != null) {
            bindItems(inventory.getContents(), player);
            bindItems(inventory.getArmorContents(), player);
        }

        PlayerEnderChest[] enderChests = profile.getEnderChests();
        if (enderChests != null) {
            for (PlayerEnderChest enderChest : enderChests) {
                if (enderChest != null && enderChest.getInventory() != null) {
                    bindItems(enderChest.getInventory().getContents(), player);
                }
            }
        }

        profile.setAttachedEnchant(bindSerialized(profile.getAttachedEnchant(), player));
        profile.setEnchantingItem(bindSerialized(profile.getEnchantingItem(), player));
        profile.setEnchantingScience(bindSerialized(profile.getEnchantingScience(), player));
        profile.setForgeItem(bindSerialized(profile.getForgeItem(), player));
        profile.setExorcismAltarItem(bindSerialized(profile.getExorcismAltarItem(), player));
        profile.setMysteryMainItem(bindSerialized(profile.getMysteryMainItem(), player));
        profile.setMysteryOffItem(bindSerialized(profile.getMysteryOffItem(), player));
    }

    public static List<ItemStack> transferCopies(List<ItemStack> items,
                                                 Player currentOwner,
                                                 Player newOwner,
                                                 long tradeTime) {
        return items.stream().map(item -> transferCopy(item, currentOwner, newOwner, tradeTime)).toList();
    }

    static long ensureTradeTime(ItemStack item, long fallbackTime) {
        if (!isEquipment(item)) {
            return 0L;
        }

        long[] result = {0L};
        boolean[] changed = {false};
        NBT.modify(item, nbt -> {
            var extra = nbt.getOrCreateCompound("extra");
            long current = extra.hasTag("tradeTime") ? extra.getLong("tradeTime") : 0L;
            if (current <= 0L) {
                current = fallbackTime;
                extra.setLong("tradeTime", current);
                changed[0] = true;
            }
            result[0] = current;
        });
        if (changed[0]) {
            Utils.invalidateMythicItemCache(item);
        }
        return result[0];
    }

    private static ItemStack transferCopy(ItemStack item,
                                          Player currentOwner,
                                          Player newOwner,
                                          long tradeTime) {
        ItemStack copy = item.clone();
        if (!isEquipment(copy)) {
            return copy;
        }

        bindInitial(copy, currentOwner);
        NBT.modify(copy, nbt -> {
            var extra = nbt.getOrCreateCompound("extra");
            extra.setString("owner", newOwner.getUniqueId().toString());
            extra.setString("ownerName", newOwner.getName());
            extra.setBoolean("traded", true);
            extra.setLong("tradeTime", tradeTime);
        });
        Utils.invalidateMythicItemCache(copy);
        return copy;
    }

    private static void bindItems(ItemStack[] items, Player player) {
        if (items == null) {
            return;
        }
        for (ItemStack item : items) {
            bindInitial(item, player);
        }
    }

    private static String bindSerialized(String serializedItem, Player player) {
        if (serializedItem == null || serializedItem.isBlank()) {
            return serializedItem;
        }
        try {
            ItemStack item = InventoryUtil.deserializeItemStack(serializedItem);
            if (item == null || item.getType() == Material.AIR) {
                return serializedItem;
            }
            if (!AdvancedItemIdentity.uuidEnabled(item) && !isEquipment(item) && !isFullEquipmentBundle(item)) {
                return serializedItem;
            }
            bindInitial(item, player);
            return InventoryUtil.serializeItemStack(item);
        } catch (RuntimeException ignored) {
            return serializedItem;
        }
    }

    private static String getOwnershipString(ItemStack item, String key) {
        if (!isEquipment(item)) {
            return null;
        }
        return NBT.get(item, nbt -> {
            ReadableNBT extra = nbt.getCompound("extra");
            if (extra == null || !extra.hasTag(key)) {
                return null;
            }
            String value = extra.getString(key);
            return value == null || value.isBlank() ? null : value;
        });
    }
}
