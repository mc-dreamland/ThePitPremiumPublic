package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.item.MythicBundleUuidUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoCursor;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTList;
import de.tr7zw.nbtapi.NBTType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanItemUuid {

    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private static final String[] ARMOR_SLOT_NAMES = {"boots", "leggings", "chestplate", "helmet"};
    private static final int MAIN_INVENTORY_SIZE = 36;
    private static final int MAX_NESTED_NBT_DEPTH = 12;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger scannedPlayers = new AtomicInteger();
    private final AtomicInteger scannedItemStacks = new AtomicInteger();
    @SuppressWarnings("unused")
    private final PitCommands pitCommands;

    private volatile long startedAt = -1L;
    private volatile String lastReportPath;
    private volatile String lastError;
    private volatile int lastDuplicateUuidCount;
    private volatile int lastDuplicateItemCount;

    public ScanItemUuid(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    boolean isRunning() {
        return running.get();
    }

    public void start(CommandSender sender) {
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage("已有扫描任务正在运行中，请使用 /scan uuid status 查看进度。");
            return;
        }

        startedAt = System.currentTimeMillis();
        scannedPlayers.set(0);
        scannedItemStacks.set(0);
        lastError = null;

        sender.sendMessage("已开始后台扫描数据库中的玩家背包、仓库与收纳内容中的神话装备 UUID，完成后会在插件目录 logs/scan/uuid 下输出 JSON 报告。");

        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> executeScan(sender));
    }

    public void status(CommandSender sender) {
        if (running.get()) {
            Duration duration = Duration.ofMillis(Math.max(0L, System.currentTimeMillis() - startedAt));
            sender.sendMessage("扫描任务运行中。");
            sender.sendMessage("已扫描玩家: " + scannedPlayers.get());
            sender.sendMessage("已扫描物品堆: " + scannedItemStacks.get());
            sender.sendMessage("运行时长: " + formatDuration(duration));
            return;
        }

        sender.sendMessage("当前没有运行中的扫描任务。");
        if (lastReportPath != null) {
            sender.sendMessage("最近一次神话装备重复 UUID 日志: " + lastReportPath);
            sender.sendMessage("最近一次重复 UUID 数: " + lastDuplicateUuidCount);
            sender.sendMessage("最近一次重复位置数: " + lastDuplicateItemCount);
        }
        if (lastError != null) {
            sender.sendMessage("最近一次错误: " + lastError);
        }
    }

    private void executeScan(CommandSender sender) {
        long startTime = System.currentTimeMillis();
        Map<String, ItemLocation> firstSeen = new HashMap<>();
        Map<String, DuplicateRecordBuilder> duplicates = new HashMap<>();

        try (MongoCursor<PlayerProfile> cursor = ThePit.getInstance().getMongoDB().getProfileCollection().find().iterator()) {
            while (cursor.hasNext()) {
                PlayerProfile profile = cursor.next();
                if (profile == null) {
                    continue;
                }

                scannedPlayers.incrementAndGet();
                scanProfile(profile, firstSeen, duplicates);

                if (scannedPlayers.get() % 1000 == 0) {
                    ThePit.getInstance().info("scanitemuuid progress: scannedPlayers=" + scannedPlayers.get()
                            + ", scannedItemStacks=" + scannedItemStacks.get()
                            + ", duplicateUuidCount=" + duplicates.size());
                }
            }

            String reportSuffix = LocalDateTime.now().format(FILE_NAME_TIME);
            Path reportPath = writeReport(duplicates, reportSuffix);
            lastReportPath = reportPath.toAbsolutePath().toString();
            lastDuplicateUuidCount = duplicates.size();
            lastDuplicateItemCount = duplicates.values().stream().mapToInt(record -> record.locations.size()).sum();

            long costMillis = System.currentTimeMillis() - startTime;
            ThePit.getInstance().info("scanitemuuid finished: scannedPlayers=" + scannedPlayers.get()
                    + ", scannedItemStacks=" + scannedItemStacks.get()
                    + ", duplicateUuidCount=" + lastDuplicateUuidCount
                    + ", report=" + lastReportPath
                    + ", cost=" + costMillis + "ms");

            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(
                    "扫描完成。神话装备重复 UUID 数: " + lastDuplicateUuidCount
                            + "，重复位置数: " + lastDuplicateItemCount
                            + "，日志: " + lastReportPath + "；使用 /scan uuid view 查看界面，或 /scan uuid history 追查本地历史。"));
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error("scanitemuuid failed", exception);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage("扫描失败: " + lastError));
        } finally {
            running.set(false);
        }
    }

    private void scanProfile(PlayerProfile profile,
                             Map<String, ItemLocation> firstSeen,
                             Map<String, DuplicateRecordBuilder> duplicates) {
        String playerUuid = profile.getUuid();
        String playerName = profile.getPlayerName();

        PlayerInv inventory = profile.getInventory();
        if (inventory != null) {
            scanItems(playerUuid, playerName, "inventory", null, inventory.getContents(), firstSeen, duplicates, false, MAIN_INVENTORY_SIZE);
            scanItems(playerUuid, playerName, "inventory_armor", null, inventory.getArmorContents(), firstSeen, duplicates, true);
        }

        PlayerEnderChest[] enderChests = profile.getEnderChests();
        if (enderChests == null) {
            return;
        }

        for (int index = 0; index < enderChests.length; index++) {
            PlayerEnderChest enderChest = enderChests[index];
            if (enderChest == null || enderChest.getInventory() == null) {
                continue;
            }

            scanItems(playerUuid, playerName, "ender_chest", index + 1, enderChest.getInventory().getContents(), firstSeen, duplicates, false);
        }
    }

    private void scanItems(String playerUuid,
                           String playerName,
                           String container,
                           Integer warehouseIndex,
                           ItemStack[] items,
                           Map<String, ItemLocation> firstSeen,
                           Map<String, DuplicateRecordBuilder> duplicates,
                           boolean armor) {
        scanItems(playerUuid, playerName, container, warehouseIndex, items, firstSeen, duplicates, armor, -1);
    }

    private void scanItems(String playerUuid,
                           String playerName,
                           String container,
                           Integer warehouseIndex,
                           ItemStack[] items,
                           Map<String, ItemLocation> firstSeen,
                           Map<String, DuplicateRecordBuilder> duplicates,
                           boolean armor,
                           int maxSlots) {
        if (items == null) {
            return;
        }

        int scanLength = maxSlots > -1 ? Math.min(items.length, maxSlots) : items.length;
        for (int slot = 0; slot < scanLength; slot++) {
            ItemStack item = items[slot];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            scannedItemStacks.incrementAndGet();

            IMythicItem mythicItem = MythicUtil.getMythicItem(item);
            String slotName = armor && slot < ARMOR_SLOT_NAMES.length ? ARMOR_SLOT_NAMES[slot] : null;
            String holderInternalName = ItemUtil.getInternalName(item);
            ItemInfo itemInfo = extractItemInfo(item);
            EnchantInfo enchantInfo = extractEnchantInfo(mythicItem);
            ItemLocation location = createItemLocation(
                    playerUuid,
                    playerName,
                    container,
                    warehouseIndex,
                    slot,
                    slot + 1,
                    slotName,
                    itemInfo,
                    enchantInfo,
                    null,
                    null
            );

            String itemUuid = ItemUtil.getUUID(item);
            if (itemUuid != null && !itemUuid.isBlank()) {
                registerScannedUuid(itemUuid, itemInfo, location, mythicItem != null, firstSeen, duplicates);
            }

            scanPackedBundleUuids(
                    item,
                    playerUuid,
                    playerName,
                    container,
                    warehouseIndex,
                    slot,
                    slotName,
                    holderInternalName,
                    firstSeen,
                    duplicates
            );

            if (shouldScanNestedStoredItems(item, holderInternalName)) {
                scanNestedStoredItemUuids(
                        item,
                        playerUuid,
                        playerName,
                        container,
                        warehouseIndex,
                        slot,
                        slotName,
                        holderInternalName,
                        firstSeen,
                        duplicates
                );
            }
        }
    }

    private void scanPackedBundleUuids(ItemStack bundle,
                                       String playerUuid,
                                       String playerName,
                                       String container,
                                       Integer warehouseIndex,
                                       int slot,
                                       String slotName,
                                       String holderInternalName,
                                       Map<String, ItemLocation> firstSeen,
                                       Map<String, DuplicateRecordBuilder> duplicates) {
        ItemInfo packedItemInfo = switch (holderInternalName == null ? "" : holderInternalName) {
            case "sword_bundle_full" -> new ItemInfo(Material.GOLDEN_SWORD.name(), "神话之剑（收纳箱内）", Collections.emptyList(), null);
            case "bow_bundle_full" -> new ItemInfo(Material.BOW.name(), "神话之弓（收纳箱内）", Collections.emptyList(), null);
            case "pants_bundle_full" -> new ItemInfo(Material.LEATHER_LEGGINGS.name(), "神话之甲（收纳箱内）", Collections.emptyList(), null);
            default -> null;
        };
        if (packedItemInfo == null) {
            return;
        }

        for (int index = 0; index < MythicBundleUuidUtil.BUNDLE_SIZE; index++) {
            String uuid = MythicBundleUuidUtil.getStoredUuid(bundle, index);
            if (uuid == null || uuid.isBlank()) {
                continue;
            }

            ItemLocation location = createItemLocation(
                    playerUuid,
                    playerName,
                    container,
                    warehouseIndex,
                    slot,
                    slot + 1,
                    slotName,
                    packedItemInfo,
                    null,
                    holderInternalName,
                    "extra." + MythicBundleUuidUtil.key(index)
            );
            registerScannedUuid(uuid, packedItemInfo, location, true, firstSeen, duplicates);
        }
    }

    private void registerScannedUuid(String itemUuid,
                                     ItemInfo itemInfo,
                                     ItemLocation location,
                                     boolean mythic,
                                     Map<String, ItemLocation> firstSeen,
                                     Map<String, DuplicateRecordBuilder> duplicates) {
        if (!mythic) {
            return;
        }

        registerLocation(itemUuid, itemInfo.material, location, firstSeen, duplicates);
    }

    private boolean shouldScanNestedStoredItems(ItemStack item, String internalName) {
        if (internalName == null || internalName.isBlank()) {
            return false;
        }
        if (internalName.contains("bundle") || internalName.contains("package") || internalName.contains("box")) {
            return true;
        }
        Material type = item.getType();
        return type == Material.MINECART
                || type == Material.CHEST_MINECART
                || type == Material.HOPPER_MINECART
                || type == Material.COMMAND_BLOCK_MINECART;
    }

    private void scanNestedStoredItemUuids(ItemStack item,
                                           String playerUuid,
                                           String playerName,
                                           String container,
                                           Integer warehouseIndex,
                                           int slot,
                                           String slotName,
                                           String holderInternalName,
                                           Map<String, ItemLocation> firstSeen,
                                           Map<String, DuplicateRecordBuilder> duplicates) {
        try {
            scanNestedStoredItemUuids(
                    new NBTItem(item),
                    "",
                    0,
                    playerUuid,
                    playerName,
                    container,
                    warehouseIndex,
                    slot,
                    slotName,
                    holderInternalName,
                    firstSeen,
                    duplicates
            );
        } catch (Exception exception) {
            ThePit.getInstance().getSLF4JLogger().debug("scanitemuuid nested scan failed for {}", holderInternalName, exception);
        }
    }

    private void scanNestedStoredItemUuids(NBTCompound compound,
                                           String path,
                                           int depth,
                                           String playerUuid,
                                           String playerName,
                                           String container,
                                           Integer warehouseIndex,
                                           int slot,
                                           String slotName,
                                           String holderInternalName,
                                           Map<String, ItemLocation> firstSeen,
                                           Map<String, DuplicateRecordBuilder> duplicates) {
        if (compound == null || depth > MAX_NESTED_NBT_DEPTH) {
            return;
        }

        NestedStoredItem nestedStoredItem = extractNestedStoredItem(compound, path);
        if (nestedStoredItem != null) {
            ItemLocation location = createItemLocation(
                    playerUuid,
                    playerName,
                    container,
                    warehouseIndex,
                    slot,
                    slot + 1,
                    slotName,
                    nestedStoredItem.itemInfo,
                    nestedStoredItem.enchantInfo,
                    holderInternalName,
                    nestedStoredItem.path
            );
            registerScannedUuid(
                    nestedStoredItem.uuid,
                    nestedStoredItem.itemInfo,
                    location,
                    nestedStoredItem.mythic,
                    firstSeen,
                    duplicates
            );
        }

        for (String key : compound.getKeys()) {
            if (!compound.hasTag(key)) {
                continue;
            }

            try {
                NBTType type = compound.getType(key);
                if (type == NBTType.NBTTagCompound) {
                    NBTCompound nested = compound.getCompound(key);
                    if (nested != null) {
                        scanNestedStoredItemUuids(
                                nested,
                                appendNbtPath(path, key),
                                depth + 1,
                                playerUuid,
                                playerName,
                                container,
                                warehouseIndex,
                                slot,
                                slotName,
                                holderInternalName,
                                firstSeen,
                                duplicates
                        );
                    }
                } else if (type == NBTType.NBTTagList) {
                    NBTList<?> list = compound.getCompoundList(key);
                    if (list == null) {
                        continue;
                    }
                    for (int index = 0; index < list.size(); index++) {
                        Object element = list.get(index);
                        if (element instanceof NBTCompound nested) {
                            scanNestedStoredItemUuids(
                                    nested,
                                    appendNbtPath(path, key) + "[" + index + "]",
                                    depth + 1,
                                    playerUuid,
                                    playerName,
                                    container,
                                    warehouseIndex,
                                    slot,
                                    slotName,
                                    holderInternalName,
                                    firstSeen,
                                    duplicates
                            );
                        }
                    }
                }
            } catch (Exception ignored) {
                // Ignore malformed nested data and keep the scan moving.
            }
        }
    }

    private NestedStoredItem extractNestedStoredItem(NBTCompound compound, String path) {
        if (path == null || path.isBlank() || path.endsWith(".tag") || path.endsWith(".extra")) {
            return null;
        }

        NBTCompound itemTag = compound.getCompound("tag");
        NBTCompound extra = null;
        if (itemTag != null) {
            extra = itemTag.getCompound("extra");
        }
        if (extra == null) {
            extra = compound.getCompound("extra");
        }
        if (extra == null || !extra.hasTag("uuid")) {
            return null;
        }

        String uuid = extra.getString("uuid");
        if (uuid == null || uuid.isBlank()) {
            return null;
        }

        String internalName = extra.hasTag("internal") ? extra.getString("internal") : null;
        String material = extractNestedMaterial(compound, internalName);
        String displayName = extractNestedDisplayName(compound, itemTag);
        List<String> lore = extractNestedLore(compound, itemTag);
        String color = extractNestedColor(extra);
        EnchantInfo enchantInfo = extractEnchantInfo(extra);
        boolean trackedMythicType = isTrackedMythicInternalName(internalName)
                || isTrackedMythicMaterial(material);
        boolean mythic = isKnownMythicInternalName(internalName) || trackedMythicType || enchantInfo != null;

        return new NestedStoredItem(
                uuid,
                new ItemInfo(material, displayName, lore, color),
                enchantInfo,
                mythic,
                trackedMythicType,
                path
        );
    }

    private String appendNbtPath(String path, String key) {
        if (path == null || path.isBlank()) {
            return key;
        }
        return path + "." + key;
    }

    private String extractNestedMaterial(NBTCompound compound, String internalName) {
        if (compound.hasTag("id")) {
            String id = compound.getString("id");
            if (id != null && !id.isBlank()) {
                return id;
            }
        }
        if (internalName != null && !internalName.isBlank()) {
            return internalName;
        }
        return "UNKNOWN";
    }

    private String extractNestedDisplayName(NBTCompound compound, NBTCompound itemTag) {
        NBTCompound display = itemTag == null ? null : itemTag.getCompound("display");
        if (display != null && display.hasTag("Name")) {
            return display.getString("Name");
        }
        NBTCompound extra = itemTag != null ? itemTag.getCompound("extra") : compound.getCompound("extra");
        if (extra != null && extra.hasTag("customName")) {
            return extra.getString("customName");
        }
        return null;
    }

    private List<String> extractNestedLore(NBTCompound compound, NBTCompound itemTag) {
        NBTCompound display = itemTag == null ? null : itemTag.getCompound("display");
        if (display == null || !display.hasTag("Lore")) {
            return Collections.emptyList();
        }
        try {
            NBTList<String> loreList = display.getStringList("Lore");
            if (loreList == null || loreList.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> lore = new ArrayList<>(loreList.size());
            for (int index = 0; index < loreList.size(); index++) {
                lore.add(loreList.get(index));
            }
            return lore;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private String extractNestedColor(NBTCompound extra) {
        if (extra == null || !extra.hasTag("mythic_color")) {
            return null;
        }
        return extra.getString("mythic_color");
    }

    private void registerNonMythicUuidLocation(String itemUuid,
                                               ItemInfo itemInfo,
                                               ItemLocation location,
                                               Map<String, ItemLocation> firstSeenNonMythicUuid,
                                               Map<String, NonMythicUuidRecordBuilder> nonMythicUuidItems) {
        NonMythicUuidRecordBuilder existingRecord = nonMythicUuidItems.get(itemUuid);
        if (existingRecord != null) {
            existingRecord.locations.add(location);
            return;
        }

        ItemLocation firstLocation = firstSeenNonMythicUuid.putIfAbsent(itemUuid, location);
        if (firstLocation == null) {
            return;
        }

        firstSeenNonMythicUuid.remove(itemUuid);
        NonMythicUuidRecordBuilder newRecord = new NonMythicUuidRecordBuilder(itemInfo.material);
        newRecord.locations.add(firstLocation);
        newRecord.locations.add(location);
        nonMythicUuidItems.put(itemUuid, newRecord);
    }

    private void registerTrackedMythicLocation(String itemUuid,
                                               ItemInfo itemInfo,
                                               ItemLocation location,
                                               Map<String, ItemLocation> firstSeenTrackedMythicUuid,
                                               Map<String, TrackedMythicRecordBuilder> trackedMythicItems) {
        TrackedMythicRecordBuilder existingRecord = trackedMythicItems.get(itemUuid);
        if (existingRecord != null) {
            existingRecord.locations.add(location);
            return;
        }

        ItemLocation firstLocation = firstSeenTrackedMythicUuid.putIfAbsent(itemUuid, location);
        if (firstLocation == null) {
            return;
        }

        firstSeenTrackedMythicUuid.remove(itemUuid);
        TrackedMythicRecordBuilder newRecord = new TrackedMythicRecordBuilder(itemInfo.material);
        newRecord.locations.add(firstLocation);
        newRecord.locations.add(location);
        trackedMythicItems.put(itemUuid, newRecord);
    }

    private boolean isTrackedMythicType(Material type) {
        return type == Material.LEATHER_LEGGINGS || type == Material.GOLDEN_SWORD || type == Material.BOW;
    }

    private boolean hasMythicEnchants(IMythicItem mythicItem) {
        return mythicItem.getEnchantments() != null && !mythicItem.getEnchantments().isEmpty();
    }

    private void registerLocation(String itemUuid,
                                  String material,
                                  ItemLocation location,
                                  Map<String, ItemLocation> firstSeen,
                                  Map<String, DuplicateRecordBuilder> duplicates) {
        DuplicateRecordBuilder duplicateRecord = duplicates.get(itemUuid);
        if (duplicateRecord != null) {
            duplicateRecord.locations.add(location);
            return;
        }

        ItemLocation firstLocation = firstSeen.putIfAbsent(itemUuid, location);
        if (firstLocation == null) {
            return;
        }

        firstSeen.remove(itemUuid);
        DuplicateRecordBuilder newRecord = new DuplicateRecordBuilder(material);
        newRecord.locations.add(firstLocation);
        newRecord.locations.add(location);
        duplicates.put(itemUuid, newRecord);
    }

    private ItemInfo extractItemInfo(ItemStack item) {
        String displayName = null;
        List<String> lore = Collections.emptyList();
        String color = null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                displayName = meta.getDisplayName();
            }
            if (meta.hasLore() && meta.getLore() != null) {
                lore = new ArrayList<>(meta.getLore());
            }
            if (meta instanceof LeatherArmorMeta leatherArmorMeta) {
                org.bukkit.Color leatherColor = leatherArmorMeta.getColor();
                if (leatherColor != null) {
                    color = String.format("#%02X%02X%02X", leatherColor.getRed(), leatherColor.getGreen(), leatherColor.getBlue());
                }
            }
        }
        return new ItemInfo(item.getType().name(), displayName, lore, color);
    }

    private EnchantInfo extractEnchantInfo(IMythicItem mythicItem) {
        if (mythicItem == null || !hasMythicEnchants(mythicItem)) {
            return null;
        }

        return buildEnchantInfo(mythicItem.getEnchantments());
    }

    private EnchantInfo extractEnchantInfo(NBTCompound extra) {
        if (extra == null || !extra.hasTag("ench")) {
            return null;
        }

        try {
            NBTList<String> enchList = extra.getStringList("ench");
            if (enchList == null || enchList.isEmpty()) {
                return null;
            }

            Map<AbstractEnchantment, Integer> enchantments = new HashMap<>();
            Utils.readEnchantments(enchantments, enchList);
            if (enchantments.isEmpty()) {
                return null;
            }
            return buildEnchantInfo(enchantments);
        } catch (Exception ignored) {
            return null;
        }
    }

    private EnchantInfo buildEnchantInfo(Map<AbstractEnchantment, Integer> sourceEnchantments) {
        Map<String, Integer> enchantments = new TreeMap<>();
        for (Map.Entry<AbstractEnchantment, Integer> entry : sourceEnchantments.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            enchantments.put(entry.getKey().getNbtName(), entry.getValue());
        }

        if (enchantments.isEmpty()) {
            return null;
        }

        StringBuilder signatureBuilder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            if (!signatureBuilder.isEmpty()) {
                signatureBuilder.append('|');
            }
            signatureBuilder.append(entry.getKey()).append(':').append(entry.getValue());
        }
        return new EnchantInfo(signatureBuilder.toString(), enchantments);
    }

    private boolean isTrackedMythicInternalName(String internalName) {
        if (internalName == null) {
            return false;
        }
        return "mythic_leggings".equalsIgnoreCase(internalName)
                || "mythic_sword".equalsIgnoreCase(internalName)
                || "mythic_bow".equalsIgnoreCase(internalName);
    }

    private boolean isTrackedMythicMaterial(String material) {
        if (material == null) {
            return false;
        }
        return "minecraft:leather_leggings".equalsIgnoreCase(material)
                || "minecraft:golden_sword".equalsIgnoreCase(material)
                || "minecraft:bow".equalsIgnoreCase(material)
                || "LEATHER_LEGGINGS".equalsIgnoreCase(material)
                || "GOLDEN_SWORD".equalsIgnoreCase(material)
                || "BOW".equalsIgnoreCase(material);
    }

    private boolean isKnownMythicInternalName(String internalName) {
        if (internalName == null || internalName.isBlank()) {
            return false;
        }
        return switch (internalName.toLowerCase()) {
            case "mythic_sword",
                 "mythic_bow",
                 "mythic_leggings",
                 "mythic_shovel",
                 "magic_fishing_rod",
                 "angel_chestplate",
                 "armageddon_boots",
                 "golden_helmet",
                 "fly_boots",
                 "jewel_sword",
                 "lucky_chestplate",
                 "exorcism_armor",
                 "black_stone",
                 "gleam_stone",
                 "greed_gold",
                 "bounty_quest" -> true;
            default -> false;
        };
    }

    private ItemLocation createItemLocation(String playerUuid,
                                            String playerName,
                                            String container,
                                            Integer warehouseIndex,
                                            int slot,
                                            int displaySlot,
                                            String slotName,
                                            ItemInfo itemInfo,
                                            EnchantInfo enchantInfo,
                                            String holderItemInternalName,
                                            String nestedPath) {
        return new ItemLocation(
                playerUuid,
                playerName,
                container,
                warehouseIndex,
                slot,
                displaySlot,
                slotName,
                itemInfo.displayName,
                itemInfo.material,
                itemInfo.lore,
                itemInfo.color,
                holderItemInternalName,
                nestedPath,
                enchantInfo
        );
    }

    private Path writeReport(Map<String, DuplicateRecordBuilder> duplicates, String reportSuffix) throws IOException {
        Path logsDirectory = ScanReportPath.directory("uuid");

        Path reportPath = logsDirectory.resolve("duplicate-mythic-item-uuid-" + reportSuffix + ".json");
        Path temporary = reportPath.resolveSibling(reportPath.getFileName() + ".tmp");

        Map<String, DuplicateRecord> output = new TreeMap<>();
        for (Map.Entry<String, DuplicateRecordBuilder> entry : duplicates.entrySet()) {
            List<ItemLocation> locations = entry.getValue().locations;
            output.put(entry.getKey(), new DuplicateRecord(locations.size(), locations));
        }

        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            GSON.toJson(output, writer);
        }
        UuidDuplicateHistory.publish(temporary, reportPath);
        return reportPath;
    }

    private Path writeMatchedEnchantReport(Map<String, DuplicateRecordBuilder> duplicates, String reportSuffix) throws IOException {
        Path logsDirectory = ScanReportPath.directory("uuid");

        Path reportPath = logsDirectory.resolve("duplicate-item-uuid-same-enchants-" + reportSuffix + ".json");
        Map<String, MatchedEnchantRecord> output = buildMatchedEnchantOutput(duplicates);

        try (Writer writer = Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            GSON.toJson(output, writer);
        }

        return reportPath;
    }

    private Path writeNonMythicUuidReport(Map<String, NonMythicUuidRecordBuilder> nonMythicUuidItems, String reportSuffix) throws IOException {
        Path logsDirectory = ScanReportPath.directory("uuid");

        Path reportPath = logsDirectory.resolve("non-mythic-item-with-uuid-" + reportSuffix + ".json");
        Map<String, NonMythicUuidRecord> output = new TreeMap<>();
        for (Map.Entry<String, NonMythicUuidRecordBuilder> entry : nonMythicUuidItems.entrySet()) {
            if (entry.getValue().locations.size() < 2) {
                continue;
            }
            output.put(entry.getKey(), new NonMythicUuidRecord(
                    entry.getValue().material,
                    entry.getValue().locations.size(),
                    entry.getValue().locations
            ));
        }

        try (Writer writer = Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            GSON.toJson(output, writer);
        }

        return reportPath;
    }

    private Path writeTrackedMythicReport(Map<String, TrackedMythicRecordBuilder> trackedMythicItems, String reportSuffix) throws IOException {
        Path logsDirectory = ScanReportPath.directory("uuid");

        Path reportPath = logsDirectory.resolve("mythic-sword-bow-leggings-uuid-" + reportSuffix + ".json");
        Map<String, TrackedMythicRecord> output = new TreeMap<>();
        for (Map.Entry<String, TrackedMythicRecordBuilder> entry : trackedMythicItems.entrySet()) {
            if (entry.getValue().locations.size() < 2) {
                continue;
            }
            output.put(entry.getKey(), new TrackedMythicRecord(
                    entry.getValue().material,
                    entry.getValue().locations.size(),
                    entry.getValue().locations
            ));
        }

        try (Writer writer = Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            GSON.toJson(output, writer);
        }

        return reportPath;
    }

    private Path writeDuplicatePlayerReport(Map<String, PlayerDuplicateSummary> output, String reportSuffix) throws IOException {
        Path logsDirectory = ScanReportPath.directory("uuid");

        Path reportPath = logsDirectory.resolve("duplicate-item-uuid-by-player-" + reportSuffix + ".json");

        try (Writer writer = Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            GSON.toJson(output, writer);
        }

        return reportPath;
    }

    private Path writeCleanupBlacklistReport(Map<String, CleanupBlacklistRule> output, String reportSuffix) throws IOException {
        Path logsDirectory = ScanReportPath.directory("uuid");

        Path reportPath = logsDirectory.resolve("duplicate-item-cleanup-blacklist-" + reportSuffix + ".json");

        try (Writer writer = Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            GSON.toJson(output, writer);
        }

        return reportPath;
    }

    private Map<String, MatchedEnchantRecord> buildMatchedEnchantOutput(Map<String, DuplicateRecordBuilder> duplicates) {
        Map<String, MatchedEnchantRecord> output = new TreeMap<>();

        for (Map.Entry<String, DuplicateRecordBuilder> duplicateEntry : duplicates.entrySet()) {
            Map<String, MatchedEnchantGroupBuilder> groups = new TreeMap<>();

            for (ItemLocation location : duplicateEntry.getValue().locations) {
                if (location.enchantInfo == null) {
                    continue;
                }

                MatchedEnchantGroupBuilder builder = groups.computeIfAbsent(
                        location.enchantInfo.signature,
                        key -> new MatchedEnchantGroupBuilder(location.enchantInfo.enchantments)
                );
                builder.locations.add(location);
            }

            List<MatchedEnchantGroup> matchedGroups = new ArrayList<>();
            int matchedLocationCount = 0;
            for (MatchedEnchantGroupBuilder group : groups.values()) {
                if (group.locations.size() < 2) {
                    continue;
                }
                matchedGroups.add(new MatchedEnchantGroup(group.locations.size(), group.enchantments, group.locations));
                matchedLocationCount += group.locations.size();
            }

            if (!matchedGroups.isEmpty()) {
                output.put(duplicateEntry.getKey(), new MatchedEnchantRecord(matchedLocationCount, matchedGroups));
            }
        }

        return output;
    }

    private Map<String, PlayerDuplicateSummary> buildDuplicatePlayerOutput(Map<String, DuplicateRecordBuilder> duplicates) {
        Map<String, PlayerDuplicateSummaryBuilder> builders = new TreeMap<>();

        for (Map.Entry<String, DuplicateRecordBuilder> duplicateEntry : duplicates.entrySet()) {
            String itemUuid = duplicateEntry.getKey();
            DuplicateRecordBuilder record = duplicateEntry.getValue();
            Map<String, Integer> itemCountByPlayer = new HashMap<>();

            for (ItemLocation location : record.locations) {
                itemCountByPlayer.merge(location.playerUuid, 1, Integer::sum);
            }

            for (Map.Entry<String, Integer> playerEntry : itemCountByPlayer.entrySet()) {
                PlayerDuplicateSummaryBuilder builder = builders.computeIfAbsent(
                        playerEntry.getKey(),
                        key -> new PlayerDuplicateSummaryBuilder()
                );
                builder.total++;
                builder.items.put(itemUuid, new PlayerDuplicateItemSummary(record.material, playerEntry.getValue()));
            }
        }

        Map<String, PlayerDuplicateSummary> output = new TreeMap<>();
        for (Map.Entry<String, PlayerDuplicateSummaryBuilder> entry : builders.entrySet()) {
            output.put(entry.getKey(), new PlayerDuplicateSummary(entry.getValue().total, entry.getValue().items));
        }
        return output;
    }

    private Map<String, CleanupBlacklistRule> buildCleanupBlacklistOutput(Map<String, DuplicateRecordBuilder> duplicates) {
        Map<String, CleanupBlacklistRule> output = new TreeMap<>();
        duplicates.keySet().forEach(uuid -> output.put(uuid, CleanupBlacklistRule.INSTANCE));
        return output;
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private static final class DuplicateRecordBuilder {
        private final String material;
        private final List<ItemLocation> locations = new ArrayList<>();

        private DuplicateRecordBuilder(String material) {
            this.material = material;
        }
    }

    private static final class DuplicateRecord {
        private final int count;
        private final List<ItemLocation> locations;

        private DuplicateRecord(int count, List<ItemLocation> locations) {
            this.count = count;
            this.locations = locations;
        }
    }

    private static final class ItemInfo {
        private final String material;
        private final String displayName;
        private final List<String> lore;
        private final String color;

        private ItemInfo(String material, String displayName, List<String> lore, String color) {
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
            this.color = color;
        }
    }

    private static final class EnchantInfo {
        private final transient String signature;
        private final transient Map<String, Integer> enchantments;

        private EnchantInfo(String signature, Map<String, Integer> enchantments) {
            this.signature = signature;
            this.enchantments = enchantments;
        }
    }

    private static final class MatchedEnchantGroupBuilder {
        private final Map<String, Integer> enchantments;
        private final List<ItemLocation> locations = new ArrayList<>();

        private MatchedEnchantGroupBuilder(Map<String, Integer> enchantments) {
            this.enchantments = enchantments;
        }
    }

    private static final class MatchedEnchantRecord {
        private final int count;
        private final List<MatchedEnchantGroup> matchedGroups;

        private MatchedEnchantRecord(int count, List<MatchedEnchantGroup> matchedGroups) {
            this.count = count;
            this.matchedGroups = matchedGroups;
        }
    }

    private static final class MatchedEnchantGroup {
        private final int count;
        private final Map<String, Integer> mythicEnchants;
        private final List<ItemLocation> locations;

        private MatchedEnchantGroup(int count, Map<String, Integer> mythicEnchants, List<ItemLocation> locations) {
            this.count = count;
            this.mythicEnchants = mythicEnchants;
            this.locations = locations;
        }
    }

    private static final class NonMythicUuidRecordBuilder {
        private final String material;
        private final List<ItemLocation> locations = new ArrayList<>();

        private NonMythicUuidRecordBuilder(String material) {
            this.material = material;
        }
    }

    private static final class NonMythicUuidRecord {
        private final String material;
        private final int count;
        private final List<ItemLocation> locations;

        private NonMythicUuidRecord(String material, int count, List<ItemLocation> locations) {
            this.material = material;
            this.count = count;
            this.locations = locations;
        }
    }

    private static final class TrackedMythicRecordBuilder {
        private final String material;
        private final List<ItemLocation> locations = new ArrayList<>();

        private TrackedMythicRecordBuilder(String material) {
            this.material = material;
        }
    }

    private static final class TrackedMythicRecord {
        private final String material;
        private final int count;
        private final List<ItemLocation> locations;

        private TrackedMythicRecord(String material, int count, List<ItemLocation> locations) {
            this.material = material;
            this.count = count;
            this.locations = locations;
        }
    }

    private static final class PlayerDuplicateSummaryBuilder {
        private int total;
        private final Map<String, PlayerDuplicateItemSummary> items = new TreeMap<>();
    }

    private static final class PlayerDuplicateSummary {
        private final int total;
        private final Map<String, PlayerDuplicateItemSummary> items;

        private PlayerDuplicateSummary(int total, Map<String, PlayerDuplicateItemSummary> items) {
            this.total = total;
            this.items = items;
        }
    }

    private static final class PlayerDuplicateItemSummary {
        private final String material;
        private final int count;

        private PlayerDuplicateItemSummary(String material, int count) {
            this.material = material;
            this.count = count;
        }
    }

    private static final class CleanupBlacklistRule {
        private static final CleanupBlacklistRule INSTANCE = new CleanupBlacklistRule();
    }

    private static final class NestedStoredItem {
        private final String uuid;
        private final ItemInfo itemInfo;
        private final EnchantInfo enchantInfo;
        private final boolean mythic;
        private final boolean trackedMythicType;
        private final String path;

        private NestedStoredItem(String uuid,
                                 ItemInfo itemInfo,
                                 EnchantInfo enchantInfo,
                                 boolean mythic,
                                 boolean trackedMythicType,
                                 String path) {
            this.uuid = uuid;
            this.itemInfo = itemInfo;
            this.enchantInfo = enchantInfo;
            this.mythic = mythic;
            this.trackedMythicType = trackedMythicType;
            this.path = path;
        }
    }

    private static final class ItemLocation {
        private final String playerUuid;
        private final String playerName;
        private final String container;
        private final Integer warehouseIndex;
        private final int slot;
        private final int displaySlot;
        private final String slotName;
        private final String displayName;
        private final String material;
        private final List<String> lore;
        private final String color;
        private final String holderItemInternalName;
        private final String nestedPath;
        private final transient EnchantInfo enchantInfo;

        private ItemLocation(String playerUuid,
                             String playerName,
                             String container,
                             Integer warehouseIndex,
                             int slot,
                             int displaySlot,
                             String slotName,
                             String displayName,
                             String material,
                             List<String> lore,
                             String color,
                             String holderItemInternalName,
                             String nestedPath,
                             EnchantInfo enchantInfo) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.container = container;
            this.warehouseIndex = warehouseIndex;
            this.slot = slot;
            this.displaySlot = displaySlot;
            this.slotName = slotName;
            this.displayName = displayName;
            this.material = material;
            this.lore = lore;
            this.color = color;
            this.holderItemInternalName = holderItemInternalName;
            this.nestedPath = nestedPath;
            this.enchantInfo = enchantInfo;
        }
    }
}
