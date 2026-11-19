package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoCursor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Scans saved player inventories for mythic equipment marked by a phase book. */
final class ScanPhaseBookMarkedMythics {

    private static final int MAIN_INVENTORY_SIZE = 36;
    private static final String[] ARMOR_SLOT_NAMES = {"boots", "leggings", "chestplate", "helmet"};
    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    /* This scanner has an independent lock and can run beside other scan commands. */
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger scannedPlayers = new AtomicInteger();
    private final AtomicInteger scannedItems = new AtomicInteger();
    private final AtomicInteger matchedEquipment = new AtomicInteger();
    private final AtomicInteger scanErrors = new AtomicInteger();

    private volatile long startedAt = -1L;
    private volatile String lastReportPath;
    private volatile String lastError;
    private volatile int lastMatchedPlayers;

    void start(CommandSender sender) {
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage("已有相位书标记神话装备扫描正在运行，请使用 /scan phasebookmarked status 查看进度。");
            return;
        }

        startedAt = System.currentTimeMillis();
        scannedPlayers.set(0);
        scannedItems.set(0);
        matchedEquipment.set(0);
        scanErrors.set(0);
        lastMatchedPlayers = 0;
        lastError = null;

        sender.sendMessage("已开始异步扫描玩家当前存档中的背包、装备栏、仓库及临时物品槽，查找带相位书标记的神话装备。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> executeScan(sender));
    }

    void status(CommandSender sender) {
        if (running.get()) {
            sender.sendMessage("相位书标记神话装备扫描运行中。");
            sender.sendMessage("已扫描玩家: " + scannedPlayers.get()
                    + "，物品堆: " + scannedItems.get()
                    + "，命中装备: " + matchedEquipment.get()
                    + "，异常数据: " + scanErrors.get());
            sender.sendMessage("运行时长: " + formatDuration(System.currentTimeMillis() - startedAt));
            return;
        }

        sender.sendMessage("当前没有运行中的相位书标记神话装备扫描。");
        if (lastReportPath != null) {
            sender.sendMessage("最近一次报告: " + lastReportPath);
            sender.sendMessage("命中玩家: " + lastMatchedPlayers + "，命中装备: " + matchedEquipment.get());
        }
        if (lastError != null) {
            sender.sendMessage("最近一次错误: " + lastError);
        }
    }

    private void executeScan(CommandSender sender) {
        List<PlayerResult> players = new ArrayList<>();
        try (MongoCursor<PlayerProfile> cursor = ThePit.getInstance()
                .getMongoDB()
                .getProfileCollection()
                .find()
                .batchSize(100)
                .iterator()) {
            while (cursor.hasNext()) {
                PlayerProfile profile = cursor.next();
                int scanned = scannedPlayers.incrementAndGet();
                if (profile != null && profile.getUuid() != null) {
                    PlayerResult result = scanProfile(profile);
                    if (!result.equipment.isEmpty()) {
                        result.equipmentCount = result.equipment.size();
                        result.equipment.sort(Comparator
                                .comparing((EquipmentResult item) -> item.container)
                                .thenComparing(item -> item.warehouse == null ? 0 : item.warehouse)
                                .thenComparingInt(item -> item.slot));
                        players.add(result);
                    }
                }
                if (scanned % 1_000 == 0) {
                    ThePit.getInstance().info("scan phasebookmarked progress: scannedPlayers=" + scanned
                            + ", scannedItems=" + scannedItems.get()
                            + ", matchedEquipment=" + matchedEquipment.get()
                            + ", scanErrors=" + scanErrors.get());
                }
            }

            players.sort(Comparator
                    .comparingInt((PlayerResult player) -> player.equipmentCount).reversed()
                    .thenComparing(player -> player.playerName,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(player -> player.playerUuid));
            lastMatchedPlayers = players.size();

            Report report = new Report(
                    System.currentTimeMillis(),
                    formatTime(System.currentTimeMillis()),
                    scannedPlayers.get(),
                    scannedItems.get(),
                    scanErrors.get(),
                    lastMatchedPlayers,
                    matchedEquipment.get(),
                    players
            );
            Path reportPath = ScanReportPath.directory("phasebookmarked").resolve(
                    "phase-book-marked-mythics-" + LocalDateTime.now().format(FILE_NAME_TIME) + ".json");
            try (Writer writer = Files.newBufferedWriter(
                    reportPath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW)) {
                GSON.toJson(report, writer);
            }
            lastReportPath = reportPath.toAbsolutePath().toString();

            ThePit.getInstance().info("scan phasebookmarked finished: scannedPlayers=" + scannedPlayers.get()
                    + ", scannedItems=" + scannedItems.get()
                    + ", matchedPlayers=" + lastMatchedPlayers
                    + ", matchedEquipment=" + matchedEquipment.get()
                    + ", scanErrors=" + scanErrors.get()
                    + ", report=" + lastReportPath);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(
                    "扫描完成。扫描玩家: " + scannedPlayers.get()
                            + "，命中玩家: " + lastMatchedPlayers
                            + "，命中装备: " + matchedEquipment.get()
                            + "，异常数据: " + scanErrors.get()
                            + "，日志: " + lastReportPath));
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error("scan phasebookmarked failed", exception);
            Bukkit.getScheduler().runTask(ThePit.getInstance(),
                    () -> sender.sendMessage("扫描失败: " + lastError));
        } finally {
            running.set(false);
        }
    }

    private PlayerResult scanProfile(PlayerProfile profile) {
        PlayerResult result = new PlayerResult(
                profile.getUuid(),
                profile.getPlayerName(),
                profile.getLevel(),
                profile.getPrestige(),
                profile.getRegisterTime(),
                profile.getRegisterTime() > 0L ? formatTime(profile.getRegisterTime()) : null,
                countTotalEnchantments(profile.getEnchantment())
        );
        try {
            PlayerInv inventory = profile.getInventory();
            if (inventory != null) {
                ItemStack[] contents = inventory.getContents();
                scanItems(result, "inventory", null, contents, MAIN_INVENTORY_SIZE, null);
                if (contents != null && contents.length > 40) {
                    scanItems(result, "offhand", null, new ItemStack[]{contents[40]}, -1, null);
                }
                scanItems(result, "inventory_armor", null,
                        inventory.getArmorContents(), -1, ARMOR_SLOT_NAMES);
            }
            PlayerEnderChest[] warehouses = profile.getEnderChests();
            if (warehouses != null) {
                for (int index = 0; index < warehouses.length; index++) {
                    PlayerEnderChest warehouse = warehouses[index];
                    if (warehouse != null && warehouse.getInventory() != null) {
                        scanItems(result, "warehouse", index + 1,
                                warehouse.getInventory().getContents(), -1, null);
                    }
                }
            }
            scanSerialized(result, "attached_enchant", profile.getAttachedEnchant());
            scanSerialized(result, "enchanting_item", profile.getEnchantingItem());
            scanSerialized(result, "enchanting_science", profile.getEnchantingScience());
            scanSerialized(result, "forge", profile.getForgeItem());
            scanSerialized(result, "exorcism_altar", profile.getExorcismAltarItem());
            scanSerialized(result, "mystery_main", profile.getMysteryMainItem());
            scanSerialized(result, "mystery_material", profile.getMysteryOffItem());
        } catch (Exception exception) {
            scanErrors.incrementAndGet();
            ThePit.getInstance().getSLF4JLogger().warn(
                    "scan phasebookmarked failed to scan player {}", profile.getUuid(), exception);
        }
        return result;
    }

    private void scanSerialized(PlayerResult player, String container, String serializedItem) {
        if (serializedItem == null || serializedItem.isBlank()) {
            return;
        }
        ItemStack item = InventoryUtil.deserializeItemStack(serializedItem);
        scanItems(player, container, null, new ItemStack[]{item}, -1, null);
    }

    private void scanItems(PlayerResult player,
                           String container,
                           Integer warehouse,
                           ItemStack[] items,
                           int maximumSlots,
                           String[] slotNames) {
        if (items == null) {
            return;
        }
        int length = maximumSlots < 0 ? items.length : Math.min(items.length, maximumSlots);
        for (int slot = 0; slot < length; slot++) {
            ItemStack item = items[slot];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            scannedItems.incrementAndGet();
            try {
                IMythicItem mythic = MythicUtil.getMythicItem(item);
                if (mythic == null || !mythic.isBoostedByPhaseBook()) {
                    continue;
                }
                player.equipment.add(createEquipmentResult(
                        item,
                        mythic,
                        container,
                        warehouse,
                        slot,
                        slotNames != null && slot < slotNames.length ? slotNames[slot] : null
                ));
                matchedEquipment.incrementAndGet();
            } catch (Exception exception) {
                scanErrors.incrementAndGet();
                ThePit.getInstance().getSLF4JLogger().debug(
                        "scan phasebookmarked failed to read item for player {} at {}:{}",
                        player.playerUuid, container, slot, exception);
            }
        }
    }

    private EquipmentResult createEquipmentResult(ItemStack item,
                                                   IMythicItem mythic,
                                                   String container,
                                                   Integer warehouse,
                                                   int slot,
                                                   String slotName) {
        Map<String, Integer> enchantments = new LinkedHashMap<>();
        if (mythic.getEnchantments() != null) {
            mythic.getEnchantments().entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0)
                    .sorted(Comparator.comparing(entry -> entry.getKey().getNbtName()))
                    .forEach(entry -> enchantments.put(entry.getKey().getNbtName(), entry.getValue()));
        }
        ItemMeta meta = item.getItemMeta();
        return new EquipmentResult(
                container,
                warehouse,
                slot,
                slot + 1,
                slotName,
                item.getType().name(),
                ItemUtil.getInternalName(item),
                meta != null && meta.hasDisplayName() ? meta.getDisplayName() : null,
                item.getAmount(),
                mythic.getUuid() == null ? ItemUtil.getUUID(item) : mythic.getUuid().toString(),
                mythic.getTier(),
                mythic.getLive(),
                mythic.getMaxLive(),
                mythic.getCreator(),
                mythic.getCreatorName(),
                mythic.getOwner(),
                mythic.getOwnerName(),
                mythic.isTraded(),
                mythic.getTradeTime(),
                enchantments
        );
    }

    private static long countTotalEnchantments(Map<String, Integer> enchantments) {
        if (enchantments == null) {
            return 0L;
        }
        return enchantments.values().stream()
                .filter(value -> value != null && value > 0)
                .mapToLong(Integer::longValue)
                .sum();
    }

    private static String formatTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DISPLAY_TIME);
    }

    private static String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(Math.max(0L, millis));
        long seconds = duration.getSeconds();
        return String.format("%02d:%02d:%02d", seconds / 3600, seconds % 3600 / 60, seconds % 60);
    }

    private static final class Report {
        private final long generatedAtTimestamp;
        private final String generatedAt;
        private final int scannedPlayers;
        private final int scannedItems;
        private final int scanErrors;
        private final int matchedPlayers;
        private final int matchedEquipment;
        private final List<PlayerResult> players;

        private Report(long generatedAtTimestamp,
                       String generatedAt,
                       int scannedPlayers,
                       int scannedItems,
                       int scanErrors,
                       int matchedPlayers,
                       int matchedEquipment,
                       List<PlayerResult> players) {
            this.generatedAtTimestamp = generatedAtTimestamp;
            this.generatedAt = generatedAt;
            this.scannedPlayers = scannedPlayers;
            this.scannedItems = scannedItems;
            this.scanErrors = scanErrors;
            this.matchedPlayers = matchedPlayers;
            this.matchedEquipment = matchedEquipment;
            this.players = new ArrayList<>(players);
        }
    }

    private static final class PlayerResult {
        private final String playerUuid;
        private final String playerName;
        private final int level;
        private final int prestige;
        private final long registerTimestamp;
        private final String registerTime;
        private final long totalEnchantments;
        private final List<EquipmentResult> equipment = new ArrayList<>();
        private int equipmentCount;

        private PlayerResult(String playerUuid,
                             String playerName,
                             int level,
                             int prestige,
                             long registerTimestamp,
                             String registerTime,
                             long totalEnchantments) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.level = level;
            this.prestige = prestige;
            this.registerTimestamp = registerTimestamp;
            this.registerTime = registerTime;
            this.totalEnchantments = totalEnchantments;
        }
    }

    private static final class EquipmentResult {
        private final String container;
        private final Integer warehouse;
        private final int slot;
        private final int displaySlot;
        private final String slotName;
        private final String material;
        private final String internalName;
        private final String displayName;
        private final int amount;
        private final String itemUuid;
        private final int tier;
        private final int lives;
        private final int maxLives;
        private final String creator;
        private final String creatorName;
        private final String owner;
        private final String ownerName;
        private final boolean traded;
        private final long tradeTime;
        private final Map<String, Integer> enchantments;

        private EquipmentResult(String container,
                                Integer warehouse,
                                int slot,
                                int displaySlot,
                                String slotName,
                                String material,
                                String internalName,
                                String displayName,
                                int amount,
                                String itemUuid,
                                int tier,
                                int lives,
                                int maxLives,
                                String creator,
                                String creatorName,
                                String owner,
                                String ownerName,
                                boolean traded,
                                long tradeTime,
                                Map<String, Integer> enchantments) {
            this.container = container;
            this.warehouse = warehouse;
            this.slot = slot;
            this.displaySlot = displaySlot;
            this.slotName = slotName;
            this.material = material;
            this.internalName = internalName;
            this.displayName = displayName;
            this.amount = amount;
            this.itemUuid = itemUuid;
            this.tier = tier;
            this.lives = lives;
            this.maxLives = maxLives;
            this.creator = creator;
            this.creatorName = creatorName;
            this.owner = owner;
            this.ownerName = ownerName;
            this.traded = traded;
            this.tradeTime = tradeTime;
            this.enchantments = new LinkedHashMap<>(enchantments);
        }
    }
}
