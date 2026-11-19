package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/** Async command adapter; the historical investigation itself is strictly file-only. */
final class ScanUuidDuplicateHistory {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile UuidDuplicateHistory.Progress progress = new UuidDuplicateHistory.Progress(0, 0, 0, 0, 0);
    private volatile long startedAt;
    private volatile String lastReport, lastError;

    void start(CommandSender sender, Integer requestedWindow, ScanItemUuid currentScan) {
        int seconds = requestedWindow == null ? 60 : requestedWindow;
        if (seconds < 0 || seconds > 86_400) {
            sender.sendMessage("§c时间窗口需为 0～86400 秒，默认 60 秒；0 表示仅匹配相同毫秒。");
            return;
        }
        if (currentScan.isRunning()) {
            sender.sendMessage("§e/scan uuid 尚未完成，请等重复 UUID 报告生成后再查询历史。");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage("§e重复 UUID 历史追查正在运行，请使用 /scan uuid history status。");
            return;
        }
        startedAt = System.currentTimeMillis();
        progress = new UuidDuplicateHistory.Progress(0, 0, 0, 0, 0);
        lastError = null;
        sender.sendMessage("§a正在后台读取最近一次 /scan uuid 报告，并从本地 UUID 历史索引追查最早重复线索，不查询数据库。");
        sender.sendMessage("§7跨玩家候选时间窗口: " + seconds + " 秒；时间接近不等于同时持有，也不能直接认定复制者。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> execute(sender, seconds));
    }

    private void execute(CommandSender sender, int seconds) {
        try {
            Path scanRoot = ThePit.getInstance().getDataFolder().toPath().resolve("logs/scan");
            Path duplicates = UuidDuplicateHistory.latestDuplicateReport(scanRoot.resolve("uuid"));
            if (duplicates == null) throw new IllegalArgumentException("没有重复 UUID 报告，请先运行 /scan uuid。");
            var targets = UuidDuplicateHistory.readDuplicates(duplicates);
            if (targets.isEmpty()) {
                message(sender, "§a最近一次 /scan uuid 报告没有重复 UUID，无需追查。来源: " + duplicates.getFileName());
                return;
            }
            UuidDuplicateHistory.Source source = UuidDuplicateHistory.latestIndex(scanRoot.resolve("materialhistory"));
            if (source == null)
                throw new IllegalArgumentException("没有本地 UUID 历史索引，请先运行 /scan materialhistory uuidindex。该命令不会自动查库生成索引。");
            message(sender, "§7重复 UUID 报告: " + duplicates.getFileName() + "；索引生成时间: "
                    + DISPLAY_TIME.format(Instant.ofEpochMilli(source.metadata().generatedAt())));
            ThePit.getInstance().info("scan uuid history sources: duplicates=" + duplicates
                    + ", index=" + source.directory() + ", targetUuids=" + targets.size());
            UuidDuplicateHistory.Analysis analysis = UuidDuplicateHistory.analyze(source, targets,
                    seconds * 1000L, update -> {
                        progress = update;
                        ThePit.getInstance().info("scan uuid history progress: shards=" + update.scannedShards()
                                + "/" + update.totalShards() + ", readEvents=" + update.readEvents()
                                + ", matchedEvents=" + update.matchedEvents() + ", invalidEvents=" + update.invalidEvents());
                    });
            Path output = ScanReportPath.directory("uuid").resolve(
                    "duplicate-uuid-history-" + LocalDateTime.now().format(FILE_TIME) + ".json");
            UuidDuplicateHistory.writeReport(output, duplicates, analysis, seconds);
            lastReport = output.toAbsolutePath().toString();
            long same = analysis.results().stream().filter(result -> result.firstSameBackupDuplicate() != null).count();
            long cross = analysis.results().stream().filter(result -> result.firstCrossPlayerCandidate() != null).count();
            long absent = analysis.results().stream().filter(result -> result.firstAppearance() == null).count();
            // The report and these summaries are ordered by the earliest evidence/candidate timestamp.
            for (var result : analysis.results()) {
                String sameText = result.firstSameBackupDuplicate() == null ? "无"
                        : describe(result.firstSameBackupDuplicate());
                var pair = result.firstCrossPlayerCandidate();
                String crossText = pair == null ? "无"
                        : describe(pair.earlier()) + " -> " + describe(pair.later()) + " (间隔=" + pair.differenceMillis() + "ms)";
                ThePit.getInstance().info("scan uuid history: uuid=" + result.itemUuid() + ", status=" + result.status()
                        + ", 最早同份备份多份=" + sameText + ", 最早跨玩家候选=" + crossText);
            }
            message(sender, "§a追查完成。UUID: " + targets.size() + "，同份备份多份: " + same
                    + "，跨玩家时间接近候选: " + cross + "，本地无记录: " + absent + "。");
            message(sender, "§7结果按最早线索时间排序；跨玩家候选不能证明复制。报告: " + lastReport);
            message(sender, "§7使用 /scan uuid history view 查看历史线索和对应物品界面。");
            if (source.metadata().version() < 2)
                message(sender, "§e当前为旧版 v1 索引，可能遗漏同玩家物品数量变化；需要更完整线索时可重新运行 uuidindex。");
            if (source.metadata().scanErrors() > 0 || analysis.progress().invalidEvents() > 0)
                message(sender, "§e源索引或记录存在异常，报告并不完整，详情见 JSON 中的 historyMetadata/progress。");
        } catch (IllegalArgumentException exception) {
            lastError = exception.getMessage();
            message(sender, "§c" + lastError);
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error("scan uuid history failed", exception);
            message(sender, "§c本地追查失败: " + lastError + "；未查询数据库。");
        } finally {
            running.set(false);
        }
    }

    void status(CommandSender sender) {
        sender.sendMessage(running.get() ? "§e重复 UUID 本地历史追查运行中。" : "§7当前没有重复 UUID 历史追查任务。");
        if (running.get()) {
            var snapshot = progress;
            sender.sendMessage("§7分片: " + snapshot.scannedShards() + "/" + snapshot.totalShards()
                    + "，读取记录: " + snapshot.readEvents() + "，匹配: " + snapshot.matchedEvents()
                    + "，异常: " + snapshot.invalidEvents() + "，已运行: "
                    + Math.max(0L, (System.currentTimeMillis() - startedAt) / 1000L) + " 秒。");
        }
        if (lastReport != null) sender.sendMessage("§7最近报告: " + lastReport);
        if (lastError != null) sender.sendMessage("§c最近错误: " + lastError);
    }

    private static String describe(UuidDuplicateHistory.Observation observation) {
        return DISPLAY_TIME.format(Instant.ofEpochMilli(observation.timestamp())) + " "
                + observation.playerName() + "[" + observation.playerUuid() + "]"
                + " copies=" + observation.copies() + " _id=" + observation.backupDatabaseId()
                + " positions=" + observation.items().stream().map(UuidDuplicateHistory.Item::location).toList();
    }
    private static void message(CommandSender sender, String text) {
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(text));
    }
}
