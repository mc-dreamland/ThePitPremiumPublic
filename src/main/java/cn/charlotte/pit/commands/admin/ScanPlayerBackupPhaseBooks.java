package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerInvBackup;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.item.PitItems;
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

final class ScanPlayerBackupPhaseBooks {

    private static final int DEFAULT_MINIMUM_PHASE_BOOKS = 10;
    private static final int MAXIMUM_MINIMUM_PHASE_BOOKS = 1_000_000;
    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger scannedBackups = new AtomicInteger();
    private final AtomicInteger scanErrors = new AtomicInteger();

    private volatile long startedAt = -1L;
    private volatile String lastReportPath;
    private volatile String lastError;
    private volatile int lastMatchedBackups;
    private volatile int lastMaximumPhaseBooks;
    private volatile String lastPlayerName;

    void start(CommandSender sender, String playerName, Integer minimumBooks) {
        String requestedName = playerName == null ? "" : playerName.trim();
        if (requestedName.isEmpty()) {
            sender.sendMessage("请提供玩家名。");
            return;
        }

        int minimumPhaseBooks = minimumBooks == null ? DEFAULT_MINIMUM_PHASE_BOOKS : minimumBooks;
        if (minimumPhaseBooks < 1 || minimumPhaseBooks > MAXIMUM_MINIMUM_PHASE_BOOKS) {
            sender.sendMessage("最小相位书数量必须在 1 到 " + MAXIMUM_MINIMUM_PHASE_BOOKS + " 之间。");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage("已有历史相位书扫描任务正在运行，请使用 /scan phasebookhistory status 查看进度。");
            return;
        }

        startedAt = System.currentTimeMillis();
        scannedBackups.set(0);
        scanErrors.set(0);
        lastError = null;
        lastPlayerName = requestedName;

        sender.sendMessage("已开始后台扫描 " + requestedName + " 的全历史背包和仓库备份；"
                + "相位书总数达到 " + minimumPhaseBooks
                + " 的快照将写入 logs/scan/phasebookhistory JSON 报告。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(),
                () -> executeScan(sender, requestedName, minimumPhaseBooks));
    }

    void status(CommandSender sender) {
        if (running.get()) {
            sender.sendMessage("历史相位书扫描任务运行中：" + lastPlayerName);
            sender.sendMessage("已读取备份: " + scannedBackups.get());
            sender.sendMessage("异常数据数: " + scanErrors.get());
            sender.sendMessage("运行时长: " + formatDuration(System.currentTimeMillis() - startedAt));
            return;
        }

        sender.sendMessage("当前没有运行中的历史相位书扫描任务。");
        if (lastReportPath != null) {
            sender.sendMessage("最近一次报告: " + lastReportPath);
            sender.sendMessage("命中备份: " + lastMatchedBackups + "，历史最高相位书数: " + lastMaximumPhaseBooks);
        }
        if (lastError != null) {
            sender.sendMessage("最近一次错误: " + lastError);
        }
    }

    private void executeScan(CommandSender sender, String requestedName, int minimumPhaseBooks) {
        try {
            PlayerProfile profile = findProfile(requestedName);
            if (profile == null || profile.getUuid() == null) {
                lastError = "未找到玩家存档: " + requestedName;
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(lastError));
                return;
            }

            List<PhaseBookSnapshot> snapshots = scanBackups(profile.getUuid(), minimumPhaseBooks);
            snapshots.sort(Comparator
                    .comparingInt((PhaseBookSnapshot snapshot) -> snapshot.totalPhaseBooks).reversed()
                    .thenComparingLong(snapshot -> snapshot.timestamp));

            lastMatchedBackups = snapshots.size();
            lastMaximumPhaseBooks = snapshots.stream()
                    .mapToInt(snapshot -> snapshot.totalPhaseBooks)
                    .max()
                    .orElse(0);
            Path reportPath = writeReport(profile, requestedName, minimumPhaseBooks, snapshots);
            lastReportPath = reportPath.toAbsolutePath().toString();

            ThePit.getInstance().info("scan phasebookhistory finished: player=" + profile.getPlayerName()
                    + ", uuid=" + profile.getUuid()
                    + ", scannedBackups=" + scannedBackups.get()
                    + ", matchedBackups=" + lastMatchedBackups
                    + ", maximumPhaseBooks=" + lastMaximumPhaseBooks
                    + ", scanErrors=" + scanErrors.get()
                    + ", report=" + lastReportPath);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(
                    "扫描完成。读取备份: " + scannedBackups.get()
                            + "，命中备份: " + lastMatchedBackups
                            + "，历史最高相位书数: " + lastMaximumPhaseBooks
                            + "，异常数据数: " + scanErrors.get()
                            + "，日志: " + lastReportPath));
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error("scan phasebookhistory failed for " + requestedName, exception);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage("扫描失败: " + lastError));
        } finally {
            running.set(false);
        }
    }

    private PlayerProfile findProfile(String requestedName) {
        PlayerProfile profile = ThePit.getInstance()
                .getMongoDB()
                .getProfileCollection()
                .find(Filters.eq("lowerName", requestedName.toLowerCase(Locale.ROOT)))
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

    private List<PhaseBookSnapshot> scanBackups(String playerUuid, int minimumPhaseBooks) {
        List<PhaseBookSnapshot> snapshots = new ArrayList<>();
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
                    PhaseBookSnapshot snapshot = createSnapshot(backup);
                    if (snapshot.totalPhaseBooks >= minimumPhaseBooks) {
                        snapshots.add(snapshot);
                    }
                } catch (Exception exception) {
                    scanErrors.incrementAndGet();
                    ThePit.getInstance().getSLF4JLogger().warn(
                            "scan phasebookhistory failed to read backup {} for player {}",
                            backup.getBackupUuid(), playerUuid, exception);
                }

                if (scannedBackups.get() % 1000 == 0) {
                    ThePit.getInstance().info("scan phasebookhistory progress: player=" + playerUuid
                            + ", scannedBackups=" + scannedBackups.get()
                            + ", matchedBackups=" + snapshots.size());
                }
            }
        }
        return snapshots;
    }

    private PhaseBookSnapshot createSnapshot(PlayerInvBackup backup) {
        if (backup.getInv() == null && backup.getChest() == null) {
            throw new IllegalArgumentException("备份缺少背包和仓库内容");
        }

        PhaseBookCounts inventory = new PhaseBookCounts();
        PlayerInv playerInv = backup.getInv();
        if (playerInv != null) {
            inventory.add(playerInv.getContents());
            inventory.add(playerInv.getArmorContents());
        }

        PhaseBookCounts warehouses = new PhaseBookCounts();
        PlayerEnderChest[] enderChests = backup.getChest();
        if (enderChests != null) {
            for (PlayerEnderChest enderChest : enderChests) {
                if (enderChest != null && enderChest.getInventory() != null) {
                    warehouses.add(enderChest.getInventory().getContents());
                }
            }
        }

        Map<String, Integer> phaseBooks = new HashMap<>(inventory.byBook);
        warehouses.byBook.forEach((book, amount) -> phaseBooks.merge(book, amount, Integer::sum));
        return new PhaseBookSnapshot(
                backup.getTimeStamp(),
                formatTime(backup.getTimeStamp()),
                backup.getBackupUuid(),
                inventory.total,
                warehouses.total,
                inventory.total + warehouses.total,
                sortCounts(phaseBooks)
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
                             int minimumPhaseBooks,
                             List<PhaseBookSnapshot> snapshots) throws IOException {
        Path logsDirectory = ScanReportPath.directory("phasebookhistory");
        Path reportPath = logsDirectory.resolve(
                "phase-book-history-" + ScanReportPath.sanitize(profile.getPlayerName())
                        + "-" + LocalDateTime.now().format(FILE_NAME_TIME) + ".json");
        Report report = new Report(
                requestedName,
                profile.getUuid(),
                profile.getPlayerName(),
                minimumPhaseBooks,
                scannedBackups.get(),
                scanErrors.get(),
                lastMaximumPhaseBooks,
                snapshots
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

    private static final class PhaseBookCounts {
        private final Map<String, Integer> byBook = new HashMap<>();
        private int total;

        private void add(ItemStack[] items) {
            if (items == null) {
                return;
            }
            for (ItemStack item : items) {
                if (!PitItems.PHASE_BOOK.isInternalNameEquals(item) || item.getAmount() <= 0) {
                    continue;
                }
                int amount = item.getAmount();
                total += amount;
                byBook.merge(bookDescription(item), amount, Integer::sum);
            }
        }

        private String bookDescription(ItemStack item) {
            String targetEnchant = ItemUtil.getItemStringData(item, "phase_book");
            String probability = ItemUtil.getItemStringData(item, "probability");
            String target = targetEnchant == null || targetEnchant.isBlank() ? "<unknown>" : targetEnchant;
            String chance = probability == null || probability.isBlank() ? "<unknown>" : probability;
            return target + " (probability=" + chance + ")";
        }
    }

    private static final class Report {
        private final String requestedPlayerName;
        private final String playerUuid;
        private final String playerName;
        private final int minimumPhaseBooks;
        private final int scannedBackups;
        private final int matchedBackups;
        private final int scanErrors;
        private final int maximumPhaseBooks;
        private final List<PhaseBookSnapshot> snapshots;

        private Report(String requestedPlayerName,
                       String playerUuid,
                       String playerName,
                       int minimumPhaseBooks,
                       int scannedBackups,
                       int scanErrors,
                       int maximumPhaseBooks,
                       List<PhaseBookSnapshot> snapshots) {
            this.requestedPlayerName = requestedPlayerName;
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.minimumPhaseBooks = minimumPhaseBooks;
            this.scannedBackups = scannedBackups;
            this.matchedBackups = snapshots.size();
            this.scanErrors = scanErrors;
            this.maximumPhaseBooks = maximumPhaseBooks;
            this.snapshots = new ArrayList<>(snapshots);
        }
    }

    private static final class PhaseBookSnapshot {
        private final long timestamp;
        private final String time;
        private final String backupUuid;
        private final int inventoryPhaseBooks;
        private final int warehousePhaseBooks;
        private final int totalPhaseBooks;
        private final Map<String, Integer> phaseBooks;

        private PhaseBookSnapshot(long timestamp,
                                  String time,
                                  String backupUuid,
                                  int inventoryPhaseBooks,
                                  int warehousePhaseBooks,
                                  int totalPhaseBooks,
                                  Map<String, Integer> phaseBooks) {
            this.timestamp = timestamp;
            this.time = time;
            this.backupUuid = backupUuid;
            this.inventoryPhaseBooks = inventoryPhaseBooks;
            this.warehousePhaseBooks = warehousePhaseBooks;
            this.totalPhaseBooks = totalPhaseBooks;
            this.phaseBooks = phaseBooks;
        }
    }
}
