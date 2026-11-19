package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerInvBackup;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.item.ItemUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class ScanEnchantedMythicBackupDiff {

    private static final int DEFAULT_MINIMUM_CHANGES = 10;
    private static final int MAXIMUM_MINIMUM_CHANGES = 10_000;
    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private static final Set<String> MYTHIC_INTERNAL_NAMES = Set.of(
            "mythic_sword",
            "mythic_bow",
            "mythic_leggings",
            "angel_chestplate",
            "armageddon_boots",
            "kings_helmet",
            "turtle_helmet",
            "lucky_chestplate",
            "mythicshovel",
            "exorcism_armor",
            "black_stone",
            "bounty_quest",
            "gleam_stone",
            "greed_gold",
            "fly_boots"
    );

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger scannedBackups = new AtomicInteger();
    private final AtomicInteger comparedPairs = new AtomicInteger();
    private final AtomicInteger matchedPairs = new AtomicInteger();
    private final AtomicInteger scanErrors = new AtomicInteger();

    private volatile long startedAt = -1L;
    private volatile String lastReportPath;
    private volatile String lastError;

    void start(CommandSender sender, Integer minimumChanges) {
        int minimum = minimumChanges == null ? DEFAULT_MINIMUM_CHANGES : minimumChanges;
        if (minimum < 1 || minimum > MAXIMUM_MINIMUM_CHANGES) {
            sender.sendMessage("最小变动数量必须在 1 到 " + MAXIMUM_MINIMUM_CHANGES + " 之间。");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage("已有附魔神话装备历史变动扫描正在运行，请使用 /scan mythicdiff status 查看进度。");
            return;
        }

        startedAt = System.currentTimeMillis();
        scannedBackups.set(0);
        comparedPairs.set(0);
        matchedPairs.set(0);
        scanErrors.set(0);
        lastError = null;

        sender.sendMessage("已开始异步扫描全库相邻备份；附魔神话装备的新增、消失或背包/仓库迁移达到 "
                + minimum + " 件时写入本地报告。该任务不与其他 scan 共用运行锁。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> executeScan(sender, minimum));
    }

    void status(CommandSender sender) {
        if (running.get()) {
            sender.sendMessage("附魔神话装备历史变动扫描运行中。");
            sender.sendMessage("已读取备份: " + scannedBackups.get());
            sender.sendMessage("已对比相邻备份: " + comparedPairs.get());
            sender.sendMessage("已命中: " + matchedPairs.get());
            sender.sendMessage("异常数据数: " + scanErrors.get());
            sender.sendMessage("运行时长: " + formatDuration(System.currentTimeMillis() - startedAt));
            return;
        }

        sender.sendMessage("当前没有运行中的附魔神话装备历史变动扫描。");
        if (lastReportPath != null) {
            sender.sendMessage("最近一次报告: " + lastReportPath);
            sender.sendMessage("相邻对比: " + comparedPairs.get() + "，命中: " + matchedPairs.get()
                    + "，异常数据数: " + scanErrors.get());
        }
        if (lastError != null) {
            sender.sendMessage("最近一次错误: " + lastError);
        }
    }

    private void executeScan(CommandSender sender, int minimumChanges) {
        try {
            List<BackupDifference> differences = scanBackups(minimumChanges);
            differences.sort(Comparator
                    .comparingLong((BackupDifference difference) -> difference.after.timestamp)
                    .thenComparing(difference -> difference.playerUuid,
                            Comparator.nullsFirst(String::compareTo)));

            Path report = writeReport(minimumChanges, differences);
            lastReportPath = report.toAbsolutePath().toString();
            ThePit.getInstance().info("scan mythicdiff finished: scannedBackups=" + scannedBackups.get()
                    + ", comparedPairs=" + comparedPairs.get()
                    + ", matchedPairs=" + matchedPairs.get()
                    + ", scanErrors=" + scanErrors.get()
                    + ", report=" + lastReportPath);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(
                    "扫描完成。读取备份: " + scannedBackups.get()
                            + "，相邻对比: " + comparedPairs.get()
                            + "，命中: " + matchedPairs.get()
                            + "，异常数据数: " + scanErrors.get()
                            + "，日志: " + lastReportPath));
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error("scan mythicdiff failed", exception);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage("扫描失败: " + lastError));
        } finally {
            running.set(false);
        }
    }

    private List<BackupDifference> scanBackups(int minimumChanges) {
        List<BackupDifference> differences = new ArrayList<>();
        Map<String, ProfileInfo> profileCache = new HashMap<>();
        BackupSnapshot previous = null;

        try (MongoCursor<PlayerInvBackup> cursor = ThePit.getInstance()
                .getMongoDB()
                .getInvCollection()
                .find()
                .sort(Sorts.ascending("uuid", "timeStamp"))
                .batchSize(100)
                .iterator()) {
            while (cursor.hasNext()) {
                PlayerInvBackup backup = cursor.next();
                int scanned = scannedBackups.incrementAndGet();
                if (scanned % 10_000 == 0) {
                    ThePit.getInstance().info("scan mythicdiff progress: scannedBackups=" + scanned
                            + ", comparedPairs=" + comparedPairs.get()
                            + ", matchedPairs=" + matchedPairs.get()
                            + ", scanErrors=" + scanErrors.get());
                }
                if (backup == null || backup.getUuid() == null || backup.getUuid().isBlank()
                        || backup.getTimeStamp() <= 0L) {
                    scanErrors.incrementAndGet();
                    continue;
                }

                try {
                    BackupSnapshot current = createSnapshot(backup);
                    if (previous != null && previous.playerUuid.equals(current.playerUuid)) {
                        comparedPairs.incrementAndGet();
                        BackupDifference difference = compare(previous, current);
                        if (difference.affectedItems >= minimumChanges) {
                            ProfileInfo profile = profileCache.computeIfAbsent(
                                    current.playerUuid,
                                    this::loadProfileInfo
                            );
                            difference.setProfile(profile);
                            differences.add(difference);
                            matchedPairs.incrementAndGet();
                        }
                    }
                    previous = current;
                } catch (Exception exception) {
                    scanErrors.incrementAndGet();
                    ThePit.getInstance().getSLF4JLogger().warn(
                            "scan mythicdiff failed to read backup {} for player {}",
                            backup.getBackupUuid(), backup.getUuid(), exception);
                }
            }
        }
        return differences;
    }

    private BackupSnapshot createSnapshot(PlayerInvBackup backup) {
        if (backup.getInv() == null && backup.getChest() == null) {
            throw new IllegalArgumentException("备份缺少背包和仓库内容");
        }
        MythicItems inventory = new MythicItems();
        PlayerInv playerInv = backup.getInv();
        if (playerInv != null) {
            addItems(inventory, playerInv.getContents());
            addItems(inventory, playerInv.getArmorContents());
        }

        MythicItems warehouse = new MythicItems();
        PlayerEnderChest[] chests = backup.getChest();
        if (chests != null) {
            for (PlayerEnderChest chest : chests) {
                if (chest != null && chest.getInventory() != null) {
                    addItems(warehouse, chest.getInventory().getContents());
                }
            }
        }

        MythicItems combined = new MythicItems();
        combined.add(inventory);
        combined.add(warehouse);
        return new BackupSnapshot(
                backup.getUuid(),
                backup.getId(),
                backup.getBackupUuid(),
                backup.getTimeStamp(),
                formatTime(backup.getTimeStamp()),
                inventory,
                warehouse,
                combined
        );
    }

    private void addItems(MythicItems result, ItemStack[] items) {
        if (items == null) {
            return;
        }
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir() || item.getAmount() <= 0 || !item.hasItemMeta()) {
                continue;
            }
            String internalName = ItemUtil.getInternalName(item);
            if (internalName == null || !MYTHIC_INTERNAL_NAMES.contains(internalName.toLowerCase(Locale.ROOT))) {
                continue;
            }
            IMythicItem mythic = MythicUtil.getMythicItem(item);
            if (mythic == null || mythic.getEnchantments() == null || mythic.getEnchantments().isEmpty()) {
                continue;
            }

            String uuid = ItemUtil.getUUID(item);
            ItemStack identityItem = item.clone();
            identityItem.setAmount(1);
            String identity = uuid == null || uuid.isBlank()
                    ? "legacy:" + internalName.toLowerCase(Locale.ROOT) + ":" + identityItem.serialize().hashCode()
                    : "uuid:" + uuid;
            Map<String, Integer> enchantments = mythic.getEnchantments().entrySet().stream()
                    .sorted(Comparator.comparing(entry -> entry.getKey().getNbtName()))
                    .collect(LinkedHashMap::new,
                            (map, entry) -> map.put(entry.getKey().getNbtName(), entry.getValue()),
                            LinkedHashMap::putAll);
            result.add(identity, item.getAmount(), new MythicInfo(
                    identity,
                    uuid,
                    internalName,
                    item.getType().name(),
                    enchantments
            ));
        }
    }

    private BackupDifference compare(BackupSnapshot before, BackupSnapshot after) {
        Map<String, Integer> additions = positiveDifference(after.combined.counts, before.combined.counts);
        Map<String, Integer> removals = positiveDifference(before.combined.counts, after.combined.counts);
        int addedItems = sum(additions);
        int removedItems = sum(removals);

        Map<String, Integer> movedToWarehouse = new HashMap<>();
        Map<String, Integer> movedToInventory = new HashMap<>();
        for (String identity : before.combined.counts.keySet()) {
            if (!after.combined.counts.containsKey(identity)) {
                continue;
            }
            int inventoryDecrease = Math.max(0,
                    before.inventory.counts.getOrDefault(identity, 0)
                            - after.inventory.counts.getOrDefault(identity, 0));
            int warehouseIncrease = Math.max(0,
                    after.warehouse.counts.getOrDefault(identity, 0)
                            - before.warehouse.counts.getOrDefault(identity, 0));
            int toWarehouse = Math.min(inventoryDecrease, warehouseIncrease);
            if (toWarehouse > 0) {
                movedToWarehouse.put(identity, toWarehouse);
            }

            int warehouseDecrease = Math.max(0,
                    before.warehouse.counts.getOrDefault(identity, 0)
                            - after.warehouse.counts.getOrDefault(identity, 0));
            int inventoryIncrease = Math.max(0,
                    after.inventory.counts.getOrDefault(identity, 0)
                            - before.inventory.counts.getOrDefault(identity, 0));
            int toInventory = Math.min(warehouseDecrease, inventoryIncrease);
            if (toInventory > 0) {
                movedToInventory.put(identity, toInventory);
            }
        }

        int movedToWarehouseItems = sum(movedToWarehouse);
        int movedToInventoryItems = sum(movedToInventory);
        return new BackupDifference(
                before.playerUuid,
                before,
                after,
                after.timestamp - before.timestamp,
                addedItems,
                removedItems,
                movedToWarehouseItems,
                movedToInventoryItems,
                addedItems + removedItems + movedToWarehouseItems + movedToInventoryItems,
                toChanges(additions, after),
                toChanges(removals, before),
                toChanges(movedToWarehouse, after),
                toChanges(movedToInventory, after)
        );
    }

    private Map<String, Integer> positiveDifference(Map<String, Integer> minuend,
                                                    Map<String, Integer> subtrahend) {
        Map<String, Integer> result = new HashMap<>();
        minuend.forEach((identity, amount) -> {
            int difference = amount - subtrahend.getOrDefault(identity, 0);
            if (difference > 0) {
                result.put(identity, difference);
            }
        });
        return result;
    }

    private List<MythicChange> toChanges(Map<String, Integer> counts, BackupSnapshot snapshot) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new MythicChange(
                        entry.getValue(),
                        snapshot.inventory.counts.getOrDefault(entry.getKey(), 0),
                        snapshot.warehouse.counts.getOrDefault(entry.getKey(), 0),
                        snapshot.combined.details.get(entry.getKey())
                ))
                .toList();
    }

    private int sum(Map<String, Integer> counts) {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    private ProfileInfo loadProfileInfo(String playerUuid) {
        try {
            PlayerProfile profile = ThePit.getInstance()
                    .getMongoDB()
                    .getProfileCollection()
                    .find(Filters.eq("uuid", playerUuid))
                    .projection(Projections.include("playerName", "prestige"))
                    .first();
            if (profile != null) {
                return new ProfileInfo(profile.getPlayerName(), profile.getPrestige());
            }
        } catch (Exception exception) {
            ThePit.getInstance().getSLF4JLogger().warn(
                    "scan mythicdiff failed to load profile {}", playerUuid, exception);
        }
        return new ProfileInfo("<unknown>", -1);
    }

    private Path writeReport(int minimumChanges, List<BackupDifference> differences) throws IOException {
        Path reportPath = ScanReportPath.directory("mythicdiff").resolve(
                "enchanted-mythic-diff-" + LocalDateTime.now().format(FILE_NAME_TIME) + ".json");
        Report report = new Report(
                System.currentTimeMillis(),
                formatTime(System.currentTimeMillis()),
                minimumChanges,
                scannedBackups.get(),
                comparedPairs.get(),
                matchedPairs.get(),
                scanErrors.get(),
                differences
        );
        try (Writer writer = Files.newBufferedWriter(
                reportPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW)) {
            GSON.toJson(report, writer);
        }
        return reportPath;
    }

    private static String formatTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DISPLAY_TIME);
    }

    private static String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(Math.max(0L, millis));
        long seconds = duration.getSeconds();
        return String.format("%02d:%02d:%02d", seconds / 3600, seconds % 3600 / 60, seconds % 60);
    }

    private static final class MythicItems {
        private final Map<String, Integer> counts = new HashMap<>();
        private final Map<String, MythicInfo> details = new HashMap<>();
        private int total;

        private void add(String identity, int amount, MythicInfo info) {
            counts.merge(identity, amount, Integer::sum);
            details.putIfAbsent(identity, info);
            total += amount;
        }

        private void add(MythicItems other) {
            other.counts.forEach((identity, amount) -> counts.merge(identity, amount, Integer::sum));
            other.details.forEach(details::putIfAbsent);
            total += other.total;
        }
    }

    private record ProfileInfo(String playerName, int prestige) {
    }

    private static final class MythicInfo {
        private final String identity;
        private final String uuid;
        private final String internalName;
        private final String material;
        private final Map<String, Integer> enchantments;

        private MythicInfo(String identity,
                           String uuid,
                           String internalName,
                           String material,
                           Map<String, Integer> enchantments) {
            this.identity = identity;
            this.uuid = uuid;
            this.internalName = internalName;
            this.material = material;
            this.enchantments = new LinkedHashMap<>(enchantments);
        }
    }

    private static final class MythicChange {
        private final int amount;
        private final int inventoryAmount;
        private final int warehouseAmount;
        private final MythicInfo item;

        private MythicChange(int amount, int inventoryAmount, int warehouseAmount, MythicInfo item) {
            this.amount = amount;
            this.inventoryAmount = inventoryAmount;
            this.warehouseAmount = warehouseAmount;
            this.item = item;
        }
    }

    private static final class BackupSnapshot {
        private final String playerUuid;
        private final String databaseId;
        private final String backupUuid;
        private final long timestamp;
        private final String time;
        private final int inventoryItems;
        private final int warehouseItems;
        private final int totalItems;
        private final transient MythicItems inventory;
        private final transient MythicItems warehouse;
        private final transient MythicItems combined;

        private BackupSnapshot(String playerUuid,
                               String databaseId,
                               String backupUuid,
                               long timestamp,
                               String time,
                               MythicItems inventory,
                               MythicItems warehouse,
                               MythicItems combined) {
            this.playerUuid = playerUuid;
            this.databaseId = databaseId;
            this.backupUuid = backupUuid;
            this.timestamp = timestamp;
            this.time = time;
            this.inventoryItems = inventory.total;
            this.warehouseItems = warehouse.total;
            this.totalItems = combined.total;
            this.inventory = inventory;
            this.warehouse = warehouse;
            this.combined = combined;
        }
    }

    private static final class BackupDifference {
        private final String playerUuid;
        private String playerName;
        private int prestige;
        private final BackupSnapshot before;
        private final BackupSnapshot after;
        private final long intervalMillis;
        private final String interval;
        private final int addedItems;
        private final int removedItems;
        private final int movedToWarehouseItems;
        private final int movedToInventoryItems;
        private final int affectedItems;
        private final List<MythicChange> additions;
        private final List<MythicChange> removals;
        private final List<MythicChange> movedToWarehouse;
        private final List<MythicChange> movedToInventory;

        private BackupDifference(String playerUuid,
                                 BackupSnapshot before,
                                 BackupSnapshot after,
                                 long intervalMillis,
                                 int addedItems,
                                 int removedItems,
                                 int movedToWarehouseItems,
                                 int movedToInventoryItems,
                                 int affectedItems,
                                 List<MythicChange> additions,
                                 List<MythicChange> removals,
                                 List<MythicChange> movedToWarehouse,
                                 List<MythicChange> movedToInventory) {
            this.playerUuid = playerUuid;
            this.before = before;
            this.after = after;
            this.intervalMillis = intervalMillis;
            this.interval = formatDuration(intervalMillis);
            this.addedItems = addedItems;
            this.removedItems = removedItems;
            this.movedToWarehouseItems = movedToWarehouseItems;
            this.movedToInventoryItems = movedToInventoryItems;
            this.affectedItems = affectedItems;
            this.additions = new ArrayList<>(additions);
            this.removals = new ArrayList<>(removals);
            this.movedToWarehouse = new ArrayList<>(movedToWarehouse);
            this.movedToInventory = new ArrayList<>(movedToInventory);
        }

        private void setProfile(ProfileInfo profile) {
            this.playerName = profile.playerName();
            this.prestige = profile.prestige();
        }
    }

    private static final class Report {
        private final long generatedAtTimestamp;
        private final String generatedAt;
        private final int minimumChanges;
        private final int scannedBackups;
        private final int comparedPairs;
        private final int matchedPairs;
        private final int scanErrors;
        private final List<BackupDifference> differences;

        private Report(long generatedAtTimestamp,
                       String generatedAt,
                       int minimumChanges,
                       int scannedBackups,
                       int comparedPairs,
                       int matchedPairs,
                       int scanErrors,
                       List<BackupDifference> differences) {
            this.generatedAtTimestamp = generatedAtTimestamp;
            this.generatedAt = generatedAt;
            this.minimumChanges = minimumChanges;
            this.scannedBackups = scannedBackups;
            this.comparedPairs = comparedPairs;
            this.matchedPairs = matchedPairs;
            this.scanErrors = scanErrors;
            this.differences = new ArrayList<>(differences);
        }
    }
}
