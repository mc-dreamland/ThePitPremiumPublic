package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoCursor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

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

final class ScanEnchantCount {

    private static final int MINIMUM_ENCHANTMENT_COUNT = 100;
    private static final int TOP_RANKING_SIZE = 30;
    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger scannedPlayers = new AtomicInteger();
    private final AtomicInteger invalidEntries = new AtomicInteger();

    private volatile long startedAt = -1L;
    private volatile String lastReportPath;
    private volatile String lastError;
    private volatile int lastMatchedPlayers;

    void start(CommandSender sender) {
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage("已有累计附魔次数扫描任务正在运行，请使用 /scan enchantcount status 查看进度。");
            return;
        }

        startedAt = System.currentTimeMillis();
        scannedPlayers.set(0);
        invalidEntries.set(0);
        lastError = null;

        sender.sendMessage("已开始后台扫描玩家累计附魔次数；仅保留大于 " + MINIMUM_ENCHANTMENT_COUNT
                + " 次的玩家，完成后会在插件目录 logs/scan/enchantcount 下输出 JSON 报告。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> executeScan(sender));
    }

    void status(CommandSender sender) {
        if (running.get()) {
            sender.sendMessage("累计附魔次数扫描任务运行中。");
            sender.sendMessage("已扫描玩家: " + scannedPlayers.get());
            sender.sendMessage("无效附魔计数项: " + invalidEntries.get());
            sender.sendMessage("运行时长: " + formatDuration(System.currentTimeMillis() - startedAt));
            return;
        }

        sender.sendMessage("当前没有运行中的累计附魔次数扫描任务。");
        if (lastReportPath != null) {
            sender.sendMessage("最近一次报告: " + lastReportPath);
            sender.sendMessage("命中玩家数: " + lastMatchedPlayers
                    + "，无效附魔计数项: " + invalidEntries.get());
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
                .iterator()) {
            while (cursor.hasNext()) {
                PlayerProfile profile = cursor.next();
                scannedPlayers.incrementAndGet();
                if (profile == null || profile.getUuid() == null) {
                    continue;
                }

                PlayerResult result = createResult(profile);
                if (result.totalEnchantments > MINIMUM_ENCHANTMENT_COUNT) {
                    players.add(result);
                }

                if (scannedPlayers.get() % 1000 == 0) {
                    ThePit.getInstance().info("scan enchantcount progress: scannedPlayers=" + scannedPlayers.get()
                            + ", matchedPlayers=" + players.size()
                            + ", invalidEntries=" + invalidEntries.get());
                }
            }

            players.sort(Comparator
                    .comparingLong((PlayerResult player) -> player.totalEnchantments).reversed()
                    .thenComparing(player -> player.playerName == null ? "" : player.playerName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(player -> player.playerUuid));
            logTopRanking(players);

            lastMatchedPlayers = players.size();
            Path reportPath = writeReport(players);
            lastReportPath = reportPath.toAbsolutePath().toString();

            ThePit.getInstance().info("scan enchantcount finished: scannedPlayers=" + scannedPlayers.get()
                    + ", matchedPlayers=" + lastMatchedPlayers
                    + ", invalidEntries=" + invalidEntries.get()
                    + ", report=" + lastReportPath);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(
                    "扫描完成。累计附魔次数大于 " + MINIMUM_ENCHANTMENT_COUNT + " 的玩家数: " + lastMatchedPlayers
                            + "，无效附魔计数项: " + invalidEntries.get()
                            + "，日志: " + lastReportPath));
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error("scan enchantcount failed", exception);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage("扫描失败: " + lastError));
        } finally {
            running.set(false);
        }
    }

    private PlayerResult createResult(PlayerProfile profile) {
        Map<String, Integer> counts = profile.getEnchantment();
        Map<String, Integer> validCounts = new LinkedHashMap<>();
        long total = 0L;
        if (counts != null) {
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
            entries.sort(Comparator.comparing(
                    Map.Entry<String, Integer>::getKey,
                    Comparator.nullsFirst(Comparator.naturalOrder())));
            for (Map.Entry<String, Integer> entry : entries) {
                String enchantment = entry.getKey();
                Integer count = entry.getValue();
                if (enchantment == null || enchantment.isBlank() || count == null || count < 0) {
                    invalidEntries.incrementAndGet();
                    continue;
                }
                validCounts.put(enchantment, count);
            }
            for (int count : validCounts.values()) {
                total += count;
            }
        }
        return new PlayerResult(
                profile.getUuid(),
                profile.getPlayerName(),
                profile.getPrestige(),
                formatDateTime(profile.getRegisterTime()),
                formatDuration(profile.getTotalPlayedTime()),
                total,
                validCounts
        );
    }

    private void logTopRanking(List<PlayerResult> players) {
        int rankingSize = Math.min(TOP_RANKING_SIZE, players.size());
        ThePit.getInstance().info("scan enchantcount top " + rankingSize
                + " (only totals greater than " + MINIMUM_ENCHANTMENT_COUNT + "): ");
        for (int index = 0; index < rankingSize; index++) {
            PlayerResult player = players.get(index);
            String playerName = player.playerName == null || player.playerName.isBlank()
                    ? "<unknown>"
                    : player.playerName;
            ThePit.getInstance().info("scan enchantcount #" + (index + 1)
                    + " player=" + playerName
                    + " uuid=" + player.playerUuid
                    + " totalEnchantments=" + player.totalEnchantments);
        }
    }

    private Path writeReport(List<PlayerResult> players) throws IOException {
        Path logsDirectory = ScanReportPath.directory("enchantcount");
        Path reportPath = logsDirectory.resolve(
                "enchantment-count-" + LocalDateTime.now().format(FILE_NAME_TIME) + ".json");
        Report report = new Report(
                MINIMUM_ENCHANTMENT_COUNT,
                scannedPlayers.get(),
                invalidEntries.get(),
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

    private static String formatDateTime(long millis) {
        return millis > 0L ? DISPLAY_DATE_TIME.format(Instant.ofEpochMilli(millis)) : null;
    }

    private static String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(Math.max(0L, millis));
        long seconds = duration.getSeconds();
        long days = seconds / 86400;
        long hours = seconds % 86400 / 3600;
        long minutes = seconds % 3600 / 60;
        long remainingSeconds = seconds % 60;
        return days + "天 " + hours + "小时 " + minutes + "分 " + remainingSeconds + "秒";
    }

    private static final class Report {
        private final int minimumExclusive;
        private final int scannedPlayers;
        private final int invalidEntries;
        private final int matchedPlayers;
        private final List<PlayerResult> players;

        private Report(int minimumExclusive,
                       int scannedPlayers,
                       int invalidEntries,
                       List<PlayerResult> players) {
            this.minimumExclusive = minimumExclusive;
            this.scannedPlayers = scannedPlayers;
            this.invalidEntries = invalidEntries;
            this.matchedPlayers = players.size();
            this.players = new ArrayList<>(players);
        }
    }

    private static final class PlayerResult {
        private final String playerUuid;
        private final String playerName;
        private final int prestige;
        private final String registerTime;
        private final String totalPlayedTime;
        private final long totalEnchantments;
        private final Map<String, Integer> enchantments;

        private PlayerResult(String playerUuid,
                             String playerName,
                             int prestige,
                             String registerTime,
                             String totalPlayedTime,
                             long totalEnchantments,
                             Map<String, Integer> enchantments) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.prestige = prestige;
            this.registerTime = registerTime;
            this.totalPlayedTime = totalPlayedTime;
            this.totalEnchantments = totalEnchantments;
            this.enchantments = new LinkedHashMap<>(enchantments);
        }
    }
}
