package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerMailData;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.mail.Mail;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoCursor;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanPhaseBook {

    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger scannedPlayers = new AtomicInteger();
    private final AtomicInteger scannedMailboxes = new AtomicInteger();
    private final AtomicInteger scanErrors = new AtomicInteger();

    private volatile long startedAt = -1L;
    private volatile String lastReportPath;
    private volatile String lastError;
    private volatile int lastOwnerCount;
    private volatile int lastPhaseBookCount;

    public void start(CommandSender sender) {
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage("已有相位书扫描任务正在运行，请使用 /scan phasebook status 查看进度。");
            return;
        }

        startedAt = System.currentTimeMillis();
        scannedPlayers.set(0);
        scannedMailboxes.set(0);
        scanErrors.set(0);
        lastError = null;

        sender.sendMessage("已开始后台扫描数据库中的相位书，完成后会在插件目录 logs/scan/phasebook 下输出 JSON 报告。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> executeScan(sender));
    }

    public void status(CommandSender sender) {
        if (running.get()) {
            Duration duration = Duration.ofMillis(Math.max(0L, System.currentTimeMillis() - startedAt));
            sender.sendMessage("相位书扫描任务运行中。");
            sender.sendMessage("已扫描玩家: " + scannedPlayers.get());
            sender.sendMessage("已扫描邮箱: " + scannedMailboxes.get());
            sender.sendMessage("异常数据数: " + scanErrors.get());
            sender.sendMessage("运行时长: " + formatDuration(duration));
            return;
        }

        sender.sendMessage("当前没有运行中的相位书扫描任务。");
        if (lastReportPath != null) {
            sender.sendMessage("最近一次报告: " + lastReportPath);
            sender.sendMessage("持有玩家数: " + lastOwnerCount + "，相位书总数: " + lastPhaseBookCount);
        }
        if (lastError != null) {
            sender.sendMessage("最近一次错误: " + lastError);
        }
    }

    private void executeScan(CommandSender sender) {
        Map<String, OwnerCount> owners = new HashMap<>();

        try {
            scanProfiles(owners);
            scanMailboxes(owners);
            owners.values().forEach(OwnerCount::calculateCount);

            List<OwnerCount> sortedOwners = owners.values().stream()
                    .filter(owner -> owner.getCount() > 0)
                    .sorted(Comparator.comparingInt(OwnerCount::getCount).reversed()
                            .thenComparing(owner -> owner.playerName == null ? "" : owner.playerName, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(owner -> owner.playerUuid == null ? "" : owner.playerUuid))
                    .toList();

            lastOwnerCount = sortedOwners.size();
            lastPhaseBookCount = sortedOwners.stream().mapToInt(OwnerCount::getCount).sum();
            Path reportPath = writeReport(sortedOwners);
            lastReportPath = reportPath.toAbsolutePath().toString();

            ThePit.getInstance().info("scanphasebook finished: scannedPlayers=" + scannedPlayers.get()
                    + ", scannedMailboxes=" + scannedMailboxes.get()
                    + ", scanErrors=" + scanErrors.get()
                    + ", ownerCount=" + lastOwnerCount
                    + ", phaseBookCount=" + lastPhaseBookCount
                    + ", report=" + lastReportPath);

            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(
                    "扫描完成。持有玩家数: " + lastOwnerCount
                            + "，相位书总数: " + lastPhaseBookCount
                            + "，异常数据数: " + scanErrors.get()
                            + "，日志: " + lastReportPath));
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error("scanphasebook failed", exception);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage("扫描失败: " + lastError));
        } finally {
            running.set(false);
        }
    }

    private void scanProfiles(Map<String, OwnerCount> owners) {
        try (MongoCursor<PlayerProfile> cursor = ThePit.getInstance().getMongoDB().getProfileCollection().find().iterator()) {
            while (cursor.hasNext()) {
                PlayerProfile profile = cursor.next();
                scannedPlayers.incrementAndGet();
                if (profile == null || profile.getUuid() == null) {
                    continue;
                }

                try {
                    int inventoryCount = 0;
                    PlayerInv inventory = profile.getInventory();
                    if (inventory != null) {
                        inventoryCount = countItems(inventory.getContents());
                    }

                    int warehouseCount = 0;
                    PlayerEnderChest[] enderChests = profile.getEnderChests();
                    if (enderChests != null) {
                        for (PlayerEnderChest enderChest : enderChests) {
                            if (enderChest != null && enderChest.getInventory() != null) {
                                warehouseCount += countItems(enderChest.getInventory().getContents());
                            }
                        }
                    }

                    int attachedEnchantCount = 0;
                    if (profile.getAttachedEnchant() != null && !profile.getAttachedEnchant().isBlank()) {
                        attachedEnchantCount = countItem(InventoryUtil.deserializeItemStack(profile.getAttachedEnchant()));
                    }

                    if (inventoryCount + warehouseCount + attachedEnchantCount > 0) {
                        OwnerCount owner = owners.computeIfAbsent(profile.getUuid(), ignored -> new OwnerCount(profile.getUuid(), profile.getPlayerName()));
                        if (owner.playerName == null) {
                            owner.playerName = profile.getPlayerName();
                        }
                        owner.inventory += inventoryCount;
                        owner.warehouses += warehouseCount;
                        owner.attachedEnchant += attachedEnchantCount;
                    }
                } catch (Exception exception) {
                    scanErrors.incrementAndGet();
                    ThePit.getInstance().getSLF4JLogger().warn("scanphasebook failed to scan player {}", profile.getUuid(), exception);
                }

                if (scannedPlayers.get() % 1000 == 0) {
                    ThePit.getInstance().info("scanphasebook progress: scannedPlayers=" + scannedPlayers.get());
                }
            }
        }
    }

    private void scanMailboxes(Map<String, OwnerCount> owners) {
        long now = System.currentTimeMillis();
        try (MongoCursor<PlayerMailData> cursor = ThePit.getInstance().getMongoDB().getMailCollection().find().iterator()) {
            while (cursor.hasNext()) {
                PlayerMailData mailData = cursor.next();
                scannedMailboxes.incrementAndGet();
                if (mailData == null || mailData.getUuid() == null || mailData.getMails() == null) {
                    continue;
                }

                try {
                    int mailCount = 0;
                    for (Mail mail : mailData.getMails()) {
                        if (mail == null || mail.isClaimed() || mail.getExpireTime() <= now || mail.getItem() == null) {
                            continue;
                        }
                        mailCount += countItems(mail.getItem().getContents());
                    }

                    if (mailCount > 0) {
                        OwnerCount owner = owners.computeIfAbsent(mailData.getUuid(), ignored -> new OwnerCount(mailData.getUuid(), mailData.getName()));
                        if (owner.playerName == null) {
                            owner.playerName = mailData.getName();
                        }
                        owner.mail += mailCount;
                    }
                } catch (Exception exception) {
                    scanErrors.incrementAndGet();
                    ThePit.getInstance().getSLF4JLogger().warn("scanphasebook failed to scan mailbox {}", mailData.getUuid(), exception);
                }
            }
        }
    }

    private int countItems(ItemStack[] items) {
        if (items == null) {
            return 0;
        }

        int count = 0;
        for (ItemStack item : items) {
            count += countItem(item);
        }
        return count;
    }

    private int countItem(ItemStack item) {
        return PitItems.PHASE_BOOK.isInternalNameEquals(item) ? Math.max(0, item.getAmount()) : 0;
    }

    private Path writeReport(List<OwnerCount> owners) throws IOException {
        Path logsDirectory = ScanReportPath.directory("phasebook");

        String suffix = LocalDateTime.now().format(FILE_NAME_TIME);
        Path reportPath = logsDirectory.resolve("phase-book-owners-" + suffix + ".json");
        Report report = new Report(
                scannedPlayers.get(),
                scannedMailboxes.get(),
                scanErrors.get(),
                owners.size(),
                owners.stream().mapToInt(OwnerCount::getCount).sum(),
                owners
        );

        try (Writer writer = Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            GSON.toJson(report, writer);
        }
        return reportPath;
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = seconds % 3600 / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    private static final class Report {
        private final int scannedPlayers;
        private final int scannedMailboxes;
        private final int scanErrors;
        private final int ownerCount;
        private final int phaseBookCount;
        private final List<OwnerCount> players;

        private Report(int scannedPlayers,
                       int scannedMailboxes,
                       int scanErrors,
                       int ownerCount,
                       int phaseBookCount,
                       List<OwnerCount> players) {
            this.scannedPlayers = scannedPlayers;
            this.scannedMailboxes = scannedMailboxes;
            this.scanErrors = scanErrors;
            this.ownerCount = ownerCount;
            this.phaseBookCount = phaseBookCount;
            this.players = new ArrayList<>(players);
        }
    }

    private static final class OwnerCount {
        private final String playerUuid;
        private String playerName;
        private int inventory;
        private int warehouses;
        private int attachedEnchant;
        private int mail;
        private int count;

        private OwnerCount(String playerUuid, String playerName) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
        }

        private int getCount() {
            return count;
        }

        private void calculateCount() {
            count = inventory + warehouses + attachedEnchant + mail;
        }
    }
}
