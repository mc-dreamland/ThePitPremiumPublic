package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerInvBackup;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.util.item.AdvancedItemCatalog;
import cn.charlotte.pit.util.item.ItemUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class ScanPlayerBackupDiff {

    private static final int DEFAULT_MINIMUM_ADDED_ITEMS = 64;
    private static final int MAXIMUM_MINIMUM_ADDED_ITEMS = 1_000_000;
    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger scannedBackups = new AtomicInteger();
    private final AtomicInteger comparedPairs = new AtomicInteger();
    private final AtomicInteger scanErrors = new AtomicInteger();

    private volatile long startedAt = -1L;
    private volatile String lastReportPath;
    private volatile String lastError;
    private volatile int lastMatchedPairs;
    private volatile String lastPlayerName;

    void start(CommandSender sender, String playerName, Integer minimumItems) {
        String requestedName = playerName == null ? "" : playerName.trim();
        if (requestedName.isEmpty()) {
            sender.sendMessage("请提供玩家名。");
            return;
        }

        int minimumAddedItems = minimumItems == null ? DEFAULT_MINIMUM_ADDED_ITEMS : minimumItems;
        if (minimumAddedItems < 1 || minimumAddedItems > MAXIMUM_MINIMUM_ADDED_ITEMS) {
            sender.sendMessage("最小新增数量必须在 1 到 " + MAXIMUM_MINIMUM_ADDED_ITEMS + " 之间。");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage("已有玩家备份对比任务正在运行，请使用 /scan backupdiff status 查看进度。");
            return;
        }

        startedAt = System.currentTimeMillis();
        scannedBackups.set(0);
        comparedPairs.set(0);
        scanErrors.set(0);
        lastError = null;
        lastPlayerName = requestedName;

        sender.sendMessage("已开始后台对比 " + requestedName + " 的全历史背包和仓库备份；"
                + "相邻快照新增稀有道具数达到 " + minimumAddedItems
                + " 时写入 logs/scan/backupdiff JSON 报告。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(),
                () -> executeScan(sender, requestedName, minimumAddedItems));
    }

    void status(CommandSender sender) {
        if (running.get()) {
            sender.sendMessage("玩家备份对比任务运行中：" + lastPlayerName);
            sender.sendMessage("已读取备份: " + scannedBackups.get());
            sender.sendMessage("已对比相邻备份: " + comparedPairs.get());
            sender.sendMessage("异常数据数: " + scanErrors.get());
            sender.sendMessage("运行时长: " + formatDuration(System.currentTimeMillis() - startedAt));
            return;
        }

        sender.sendMessage("当前没有运行中的玩家备份对比任务。");
        if (lastReportPath != null) {
            sender.sendMessage("最近一次报告: " + lastReportPath);
            sender.sendMessage("命中相邻备份对: " + lastMatchedPairs);
        }
        if (lastError != null) {
            sender.sendMessage("最近一次错误: " + lastError);
        }
    }

    private void executeScan(CommandSender sender, String requestedName, int minimumAddedItems) {
        try {
            PlayerProfile profile = findProfile(requestedName);
            if (profile == null || profile.getUuid() == null) {
                lastError = "未找到玩家存档: " + requestedName;
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(lastError));
                return;
            }

            List<BackupDifference> differences = compareBackups(profile.getUuid(), minimumAddedItems);
            differences.sort(Comparator
                    .comparingLong((BackupDifference difference) -> difference.after.timestamp)
                    .thenComparing(difference -> difference.after.backupUuid,
                            Comparator.nullsFirst(String::compareTo)));

            lastMatchedPairs = differences.size();
            Path reportPath = writeReport(profile, requestedName, minimumAddedItems, differences);
            lastReportPath = reportPath.toAbsolutePath().toString();

            ThePit.getInstance().info("scan backupdiff finished: player=" + profile.getPlayerName()
                    + ", uuid=" + profile.getUuid()
                    + ", scannedBackups=" + scannedBackups.get()
                    + ", comparedPairs=" + comparedPairs.get()
                    + ", matchedPairs=" + lastMatchedPairs
                    + ", scanErrors=" + scanErrors.get()
                    + ", report=" + lastReportPath);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(
                    "扫描完成。读取备份: " + scannedBackups.get()
                            + "，相邻对比: " + comparedPairs.get()
                            + "，命中大额新增: " + lastMatchedPairs
                            + "，异常数据数: " + scanErrors.get()
                            + "，日志: " + lastReportPath));
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error("scan backupdiff failed for " + requestedName, exception);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage("扫描失败: " + lastError));
        } finally {
            running.set(false);
        }
    }

    private PlayerProfile findProfile(String requestedName) {
        String lowerName = requestedName.toLowerCase(Locale.ROOT);
        PlayerProfile profile = ThePit.getInstance()
                .getMongoDB()
                .getProfileCollection()
                .find(Filters.eq("lowerName", lowerName))
                .first();
        if (profile != null) {
            return profile;
        }
        return ThePit.getInstance()
                .getMongoDB()
                .getProfileCollection()
                .find(Filters.eq("playerName", requestedName))
                .first();
    }

    private List<BackupDifference> compareBackups(String playerUuid, int minimumAddedItems) {
        List<BackupDifference> differences = new ArrayList<>();
        BackupSnapshot previous = null;

        try (MongoCursor<PlayerInvBackup> cursor = ThePit.getInstance()
                .getMongoDB()
                .getInvCollection()
                .find(Filters.eq("uuid", playerUuid))
                .sort(Sorts.ascending("timeStamp", "backupUuid"))
                .iterator()) {
            while (cursor.hasNext()) {
                PlayerInvBackup backup = cursor.next();
                scannedBackups.incrementAndGet();
                if (backup == null || backup.getUuid() == null || backup.getTimeStamp() <= 0L) {
                    scanErrors.incrementAndGet();
                    continue;
                }

                try {
                    BackupSnapshot current = createSnapshot(backup);
                    if (previous != null) {
                        comparedPairs.incrementAndGet();
                        BackupDifference difference = compare(previous, current);
                        if (difference.addedItems >= minimumAddedItems) {
                            differences.add(difference);
                        }
                    }
                    previous = current;
                } catch (Exception exception) {
                    scanErrors.incrementAndGet();
                    ThePit.getInstance().getSLF4JLogger().warn(
                            "scan backupdiff failed to read backup {} for player {}",
                            backup.getBackupUuid(), playerUuid, exception);
                }

                if (scannedBackups.get() % 1000 == 0) {
                    ThePit.getInstance().info("scan backupdiff progress: player=" + playerUuid
                            + ", scannedBackups=" + scannedBackups.get()
                            + ", comparedPairs=" + comparedPairs.get()
                            + ", matchedPairs=" + differences.size());
                }
            }
        }
        return differences;
    }

    private BackupSnapshot createSnapshot(PlayerInvBackup backup) {
        if (backup.getInv() == null && backup.getChest() == null) {
            throw new IllegalArgumentException("备份缺少背包和仓库内容");
        }

        ItemCounts inventory = new ItemCounts();
        PlayerInv playerInv = backup.getInv();
        if (playerInv != null) {
            inventory.add(playerInv.getContents());
            inventory.add(playerInv.getArmorContents());
        }

        ItemCounts warehouses = new ItemCounts();
        PlayerEnderChest[] enderChests = backup.getChest();
        if (enderChests != null) {
            for (PlayerEnderChest enderChest : enderChests) {
                if (enderChest != null && enderChest.getInventory() != null) {
                    warehouses.add(enderChest.getInventory().getContents());
                }
            }
        }

        ItemCounts combined = new ItemCounts();
        combined.add(inventory);
        combined.add(warehouses);
        return new BackupSnapshot(
                backup.getTimeStamp(),
                formatTime(backup.getTimeStamp()),
                backup.getBackupUuid(),
                inventory.total,
                warehouses.total,
                combined.total,
                combined.types
        );
    }

    private BackupDifference compare(BackupSnapshot before, BackupSnapshot after) {
        Map<String, Integer> additions = new HashMap<>();
        Map<String, Integer> removals = new HashMap<>();
        for (Map.Entry<String, Integer> entry : after.itemTypes.entrySet()) {
            int delta = entry.getValue() - before.itemTypes.getOrDefault(entry.getKey(), 0);
            if (delta > 0) {
                additions.put(entry.getKey(), delta);
            }
        }
        for (Map.Entry<String, Integer> entry : before.itemTypes.entrySet()) {
            int delta = entry.getValue() - after.itemTypes.getOrDefault(entry.getKey(), 0);
            if (delta > 0) {
                removals.put(entry.getKey(), delta);
            }
        }

        int addedItems = additions.values().stream().mapToInt(Integer::intValue).sum();
        int removedItems = removals.values().stream().mapToInt(Integer::intValue).sum();
        return new BackupDifference(
                before,
                after,
                addedItems,
                removedItems,
                after.totalItems - before.totalItems,
                after.inventoryItems - before.inventoryItems,
                after.warehouseItems - before.warehouseItems,
                sortCounts(additions),
                sortCounts(removals)
        );
    }

    private Map<String, Integer> sortCounts(Map<String, Integer> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);
    }

    private Path writeReport(PlayerProfile profile,
                             String requestedName,
                             int minimumAddedItems,
                             List<BackupDifference> differences) throws IOException {
        Path logsDirectory = ScanReportPath.directory("backupdiff");
        Path reportPath = logsDirectory.resolve(
                "backup-diff-" + ScanReportPath.sanitize(profile.getPlayerName())
                        + "-" + LocalDateTime.now().format(FILE_NAME_TIME) + ".json");
        Report report = new Report(
                requestedName,
                profile.getUuid(),
                profile.getPlayerName(),
                minimumAddedItems,
                scannedBackups.get(),
                comparedPairs.get(),
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

    private static final class ItemCounts {
        private final Map<String, Integer> types = new HashMap<>();
        private int total;

        private void add(ItemStack[] items) {
            if (items == null) {
                return;
            }
            for (ItemStack item : items) {
                if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
                    continue;
                }
                String internalName = ItemUtil.getInternalName(item);
                if (!AdvancedItemCatalog.isAdvanced(internalName)) {
                    continue;
                }
                String type = internalName.toLowerCase(Locale.ROOT);
                total += item.getAmount();
                types.merge(type, item.getAmount(), Integer::sum);
            }
        }

        private void add(ItemCounts other) {
            total += other.total;
            other.types.forEach((type, amount) -> types.merge(type, amount, Integer::sum));
        }
    }

    private static final class Report {
        private final String requestedPlayerName;
        private final String playerUuid;
        private final String playerName;
        private final int minimumAddedItems;
        private final int scannedBackups;
        private final int comparedPairs;
        private final int matchedPairs;
        private final int scanErrors;
        private final List<BackupDifference> differences;

        private Report(String requestedPlayerName,
                       String playerUuid,
                       String playerName,
                       int minimumAddedItems,
                       int scannedBackups,
                       int comparedPairs,
                       int scanErrors,
                       List<BackupDifference> differences) {
            this.requestedPlayerName = requestedPlayerName;
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.minimumAddedItems = minimumAddedItems;
            this.scannedBackups = scannedBackups;
            this.comparedPairs = comparedPairs;
            this.matchedPairs = differences.size();
            this.scanErrors = scanErrors;
            this.differences = new ArrayList<>(differences);
        }
    }

    private static final class BackupSnapshot {
        private final long timestamp;
        private final String time;
        private final String backupUuid;
        private final int inventoryItems;
        private final int warehouseItems;
        private final int totalItems;
        private final Map<String, Integer> itemTypes;

        private BackupSnapshot(long timestamp,
                               String time,
                               String backupUuid,
                               int inventoryItems,
                               int warehouseItems,
                               int totalItems,
                               Map<String, Integer> itemTypes) {
            this.timestamp = timestamp;
            this.time = time;
            this.backupUuid = backupUuid;
            this.inventoryItems = inventoryItems;
            this.warehouseItems = warehouseItems;
            this.totalItems = totalItems;
            this.itemTypes = new HashMap<>(itemTypes);
        }
    }

    private static final class BackupDifference {
        private final BackupSnapshot before;
        private final BackupSnapshot after;
        private final int addedItems;
        private final int removedItems;
        private final int netItemChange;
        private final int inventoryNetChange;
        private final int warehouseNetChange;
        private final Map<String, Integer> additions;
        private final Map<String, Integer> removals;

        private BackupDifference(BackupSnapshot before,
                                 BackupSnapshot after,
                                 int addedItems,
                                 int removedItems,
                                 int netItemChange,
                                 int inventoryNetChange,
                                 int warehouseNetChange,
                                 Map<String, Integer> additions,
                                 Map<String, Integer> removals) {
            this.before = before;
            this.after = after;
            this.addedItems = addedItems;
            this.removedItems = removedItems;
            this.netItemChange = netItemChange;
            this.inventoryNetChange = inventoryNetChange;
            this.warehouseNetChange = warehouseNetChange;
            this.additions = additions;
            this.removals = removals;
        }
    }
}
