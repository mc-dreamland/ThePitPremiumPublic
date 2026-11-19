package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerInvBackup;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanInventoryGrowth {

    private static final int DEFAULT_HOURS = 24;
    private static final int MAX_HOURS = 24 * 7;
    private static final int MAX_SNAPSHOTS_PER_PLAYER = 20;
    private static final int MIN_GROWTH_EVENTS = 2;
    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger scannedBackups = new AtomicInteger();
    private final AtomicInteger scannedPlayers = new AtomicInteger();
    private final AtomicInteger scanErrors = new AtomicInteger();

    private volatile long startedAt = -1L;
    private volatile String lastReportPath;
    private volatile String lastError;
    private volatile int lastSuspiciousPlayers;
    private volatile int lastWindowHours = DEFAULT_HOURS;

    public void start(CommandSender sender, Integer hours) {
        int requestedHours = hours == null ? DEFAULT_HOURS : hours;
        if (requestedHours < 1 || requestedHours > MAX_HOURS) {
            sender.sendMessage("扫描时间必须在 1 到 " + MAX_HOURS + " 小时之间。");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage("已有背包增长扫描任务正在运行，请使用 /scan growth status 查看进度。");
            return;
        }

        startedAt = System.currentTimeMillis();
        scannedBackups.set(0);
        scannedPlayers.set(0);
        scanErrors.set(0);
        lastError = null;
        lastWindowHours = requestedHours;

        sender.sendMessage("已开始扫描最近 " + requestedHours
                + " 小时的背包和仓库备份，完成后会在插件目录 logs/scan/growth 下输出 JSON 报告。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(),
                () -> executeScan(sender, requestedHours));
    }

    public void status(CommandSender sender) {
        if (running.get()) {
            Duration duration = Duration.ofMillis(Math.max(0L, System.currentTimeMillis() - startedAt));
            sender.sendMessage("背包增长扫描任务运行中，扫描范围: 最近 " + lastWindowHours + " 小时。");
            sender.sendMessage("已读取备份: " + scannedBackups.get());
            sender.sendMessage("已发现玩家: " + scannedPlayers.get());
            sender.sendMessage("异常数据数: " + scanErrors.get());
            sender.sendMessage("运行时长: " + formatDuration(duration));
            return;
        }

        sender.sendMessage("当前没有运行中的背包增长扫描任务。");
        if (lastReportPath != null) {
            sender.sendMessage("最近一次报告: " + lastReportPath);
            sender.sendMessage("可疑玩家数: " + lastSuspiciousPlayers);
        }
        if (lastError != null) {
            sender.sendMessage("最近一次错误: " + lastError);
        }
    }

    private void executeScan(CommandSender sender, int hours) {
        long cutoff = System.currentTimeMillis() - Duration.ofHours(hours).toMillis();
        Map<String, Deque<InventorySnapshot>> snapshotsByPlayer = new HashMap<>();

        try {
            loadSnapshots(cutoff, snapshotsByPlayer);
            scannedPlayers.set(snapshotsByPlayer.size());

            List<PlayerGrowth> suspiciousPlayers = snapshotsByPlayer.entrySet().stream()
                    .map(entry -> analyze(entry.getKey(), entry.getValue()))
                    .filter(growth -> growth != null)
                    .sorted(Comparator.comparingInt(PlayerGrowth::growthEvents).reversed()
                            .thenComparing(Comparator.comparingInt(PlayerGrowth::itemsAdded).reversed())
                            .thenComparing(PlayerGrowth::playerUuid))
                    .toList();

            resolvePlayerNames(suspiciousPlayers);
            lastSuspiciousPlayers = suspiciousPlayers.size();
            Path reportPath = writeReport(hours, cutoff, suspiciousPlayers);
            lastReportPath = reportPath.toAbsolutePath().toString();

            ThePit.getInstance().info("scaninventorygrowth finished: windowHours=" + hours
                    + ", scannedBackups=" + scannedBackups.get()
                    + ", scannedPlayers=" + scannedPlayers.get()
                    + ", scanErrors=" + scanErrors.get()
                    + ", suspiciousPlayers=" + lastSuspiciousPlayers
                    + ", report=" + lastReportPath);

            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(
                    "扫描完成。读取备份: " + scannedBackups.get()
                            + "，玩家数: " + scannedPlayers.get()
                            + "，可疑玩家数: " + lastSuspiciousPlayers
                            + "，异常数据数: " + scanErrors.get()
                            + "，日志: " + lastReportPath));
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error("scaninventorygrowth failed", exception);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage("扫描失败: " + lastError));
        } finally {
            running.set(false);
        }
    }

    private void loadSnapshots(long cutoff, Map<String, Deque<InventorySnapshot>> snapshotsByPlayer) {
        try (MongoCursor<PlayerInvBackup> cursor = ThePit.getInstance()
                .getMongoDB()
                .getInvCollection()
                .find(Filters.gte("timeStamp", cutoff))
                .sort(Sorts.ascending("uuid", "timeStamp"))
                .iterator()) {
            while (cursor.hasNext()) {
                PlayerInvBackup backup = cursor.next();
                scannedBackups.incrementAndGet();
                if (backup == null || backup.getUuid() == null || backup.getTimeStamp() < cutoff) {
                    continue;
                }

                try {
                    InventorySnapshot snapshot = createSnapshot(backup);
                    Deque<InventorySnapshot> playerSnapshots = snapshotsByPlayer.get(backup.getUuid());
                    if (playerSnapshots == null) {
                        playerSnapshots = new ArrayDeque<>();
                        snapshotsByPlayer.put(backup.getUuid(), playerSnapshots);
                        scannedPlayers.incrementAndGet();
                    }
                    playerSnapshots.addLast(snapshot);
                    while (playerSnapshots.size() > MAX_SNAPSHOTS_PER_PLAYER) {
                        playerSnapshots.removeFirst();
                    }
                } catch (Exception exception) {
                    scanErrors.incrementAndGet();
                    ThePit.getInstance().getSLF4JLogger().warn(
                            "scaninventorygrowth failed to read backup {} for player {}",
                            backup.getBackupUuid(), backup.getUuid(), exception);
                }

                if (scannedBackups.get() % 5000 == 0) {
                    ThePit.getInstance().info("scaninventorygrowth progress: scannedBackups=" + scannedBackups.get()
                            + ", players=" + snapshotsByPlayer.size());
                }
            }
        }
    }

    private InventorySnapshot createSnapshot(PlayerInvBackup backup) {
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
        return new InventorySnapshot(
                backup.getTimeStamp(),
                formatTime(backup.getTimeStamp()),
                backup.getBackupUuid(),
                inventory.total,
                warehouses.total,
                combined.total,
                combined.types
        );
    }

    private PlayerGrowth analyze(String playerUuid, Deque<InventorySnapshot> snapshotDeque) {
        List<InventorySnapshot> snapshots = new ArrayList<>(snapshotDeque);
        if (snapshots.size() < MIN_GROWTH_EVENTS + 1) {
            return null;
        }

        GrowthRun best = null;
        int runStart = 0;
        int growthEvents = 0;
        int itemsAdded = 0;

        for (int i = 1; i < snapshots.size(); i++) {
            int delta = snapshots.get(i).totalItems - snapshots.get(i - 1).totalItems;
            if (delta < 0) {
                runStart = i;
                growthEvents = 0;
                itemsAdded = 0;
                continue;
            }
            if (delta > 0) {
                growthEvents++;
                itemsAdded += delta;
            }
            if (growthEvents >= MIN_GROWTH_EVENTS) {
                GrowthRun candidate = new GrowthRun(runStart, i, growthEvents, itemsAdded);
                if (best == null || candidate.isMoreSuspiciousThan(best)) {
                    best = candidate;
                }
            }
        }

        if (best == null) {
            return null;
        }

        InventorySnapshot first = snapshots.get(best.startIndex);
        InventorySnapshot last = snapshots.get(best.endIndex);
        Map<String, Integer> itemGrowth = positiveTypeGrowth(first.itemTypes, last.itemTypes);
        List<InventorySnapshot> evidence = new ArrayList<>(snapshots.subList(best.startIndex, best.endIndex + 1));
        return new PlayerGrowth(
                playerUuid,
                null,
                snapshots.size(),
                best.growthEvents,
                best.itemsAdded,
                last.inventoryItems - first.inventoryItems,
                last.warehouseItems - first.warehouseItems,
                first.timestamp,
                first.time,
                last.timestamp,
                last.time,
                itemGrowth,
                evidence
        );
    }

    private Map<String, Integer> positiveTypeGrowth(Map<String, Integer> first, Map<String, Integer> last) {
        return last.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue() - first.getOrDefault(entry.getKey(), 0)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);
    }

    private void resolvePlayerNames(List<PlayerGrowth> players) {
        for (PlayerGrowth growth : players) {
            try {
                PlayerProfile profile = ThePit.getInstance()
                        .getMongoDB()
                        .getProfileCollection()
                        .find(Filters.eq("uuid", growth.playerUuid))
                        .first();
                if (profile != null) {
                    growth.playerName = profile.getPlayerName();
                }
            } catch (Exception exception) {
                scanErrors.incrementAndGet();
                ThePit.getInstance().getSLF4JLogger().warn(
                        "scaninventorygrowth failed to resolve player {}", growth.playerUuid, exception);
            }
        }
    }

    private Path writeReport(int hours, long cutoff, List<PlayerGrowth> players) throws IOException {
        Path logsDirectory = ScanReportPath.directory("growth");

        String suffix = LocalDateTime.now().format(FILE_NAME_TIME);
        Path reportPath = logsDirectory.resolve("inventory-growth-" + suffix + ".json");
        Report report = new Report(
                hours,
                cutoff,
                formatTime(cutoff),
                MAX_SNAPSHOTS_PER_PLAYER,
                MIN_GROWTH_EVENTS,
                scannedBackups.get(),
                scannedPlayers.get(),
                scanErrors.get(),
                players.size(),
                players
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

    private static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = seconds % 3600 / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    private static final class ItemCounts {
        private final Map<String, Integer> types = new HashMap<>();
        private int total;

        private void add(ItemStack[] items) {
            if (items == null) {
                return;
            }
            for (ItemStack item : items) {
                add(item);
            }
        }

        private void add(ItemStack item) {
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
                return;
            }
            String internalName = ItemUtil.getInternalName(item);
            String type = internalName == null || internalName.isBlank()
                    ? "minecraft:" + item.getType().name().toLowerCase(Locale.ROOT)
                    : internalName;
            total += item.getAmount();
            types.merge(type, item.getAmount(), Integer::sum);
        }

        private void add(ItemCounts other) {
            total += other.total;
            other.types.forEach((type, amount) -> types.merge(type, amount, Integer::sum));
        }
    }

    private static final class GrowthRun {
        private final int startIndex;
        private final int endIndex;
        private final int growthEvents;
        private final int itemsAdded;

        private GrowthRun(int startIndex, int endIndex, int growthEvents, int itemsAdded) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.growthEvents = growthEvents;
            this.itemsAdded = itemsAdded;
        }

        private boolean isMoreSuspiciousThan(GrowthRun other) {
            if (growthEvents != other.growthEvents) {
                return growthEvents > other.growthEvents;
            }
            if (itemsAdded != other.itemsAdded) {
                return itemsAdded > other.itemsAdded;
            }
            return endIndex - startIndex > other.endIndex - other.startIndex;
        }
    }

    private static final class InventorySnapshot {
        private final long timestamp;
        private final String time;
        private final String backupUuid;
        private final int inventoryItems;
        private final int warehouseItems;
        private final int totalItems;
        private final Map<String, Integer> itemTypes;

        private InventorySnapshot(long timestamp,
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

    private static final class PlayerGrowth {
        private final String playerUuid;
        private String playerName;
        private final int analyzedSnapshots;
        private final int growthEvents;
        private final int itemsAdded;
        private final int inventoryNetGrowth;
        private final int warehouseNetGrowth;
        private final long firstTimestamp;
        private final String firstTime;
        private final long lastTimestamp;
        private final String lastTime;
        private final Map<String, Integer> itemGrowth;
        private final List<InventorySnapshot> snapshots;

        private PlayerGrowth(String playerUuid,
                             String playerName,
                             int analyzedSnapshots,
                             int growthEvents,
                             int itemsAdded,
                             int inventoryNetGrowth,
                             int warehouseNetGrowth,
                             long firstTimestamp,
                             String firstTime,
                             long lastTimestamp,
                             String lastTime,
                             Map<String, Integer> itemGrowth,
                             List<InventorySnapshot> snapshots) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.analyzedSnapshots = analyzedSnapshots;
            this.growthEvents = growthEvents;
            this.itemsAdded = itemsAdded;
            this.inventoryNetGrowth = inventoryNetGrowth;
            this.warehouseNetGrowth = warehouseNetGrowth;
            this.firstTimestamp = firstTimestamp;
            this.firstTime = firstTime;
            this.lastTimestamp = lastTimestamp;
            this.lastTime = lastTime;
            this.itemGrowth = new LinkedHashMap<>(itemGrowth);
            this.snapshots = new ArrayList<>(snapshots);
        }

        private int growthEvents() {
            return growthEvents;
        }

        private int itemsAdded() {
            return itemsAdded;
        }

        private String playerUuid() {
            return playerUuid;
        }
    }

    private static final class Report {
        private final int windowHours;
        private final long cutoffTimestamp;
        private final String cutoffTime;
        private final int maxSnapshotsPerPlayer;
        private final int minimumGrowthEvents;
        private final int scannedBackups;
        private final int scannedPlayers;
        private final int scanErrors;
        private final int suspiciousPlayers;
        private final List<PlayerGrowth> players;

        private Report(int windowHours,
                       long cutoffTimestamp,
                       String cutoffTime,
                       int maxSnapshotsPerPlayer,
                       int minimumGrowthEvents,
                       int scannedBackups,
                       int scannedPlayers,
                       int scanErrors,
                       int suspiciousPlayers,
                       List<PlayerGrowth> players) {
            this.windowHours = windowHours;
            this.cutoffTimestamp = cutoffTimestamp;
            this.cutoffTime = cutoffTime;
            this.maxSnapshotsPerPlayer = maxSnapshotsPerPlayer;
            this.minimumGrowthEvents = minimumGrowthEvents;
            this.scannedBackups = scannedBackups;
            this.scannedPlayers = scannedPlayers;
            this.scanErrors = scanErrors;
            this.suspiciousPlayers = suspiciousPlayers;
            this.players = new ArrayList<>(players);
        }
    }
}
