package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerMailData;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.mail.Mail;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemUtil;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanMythicBundle {

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
    private volatile int lastBundleCount;

    public void start(CommandSender sender) {
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage("已有神话装备收纳箱扫描任务正在运行，请使用 /scan mythicbundle status 查看进度。");
            return;
        }

        startedAt = System.currentTimeMillis();
        scannedPlayers.set(0);
        scannedMailboxes.set(0);
        scanErrors.set(0);
        lastError = null;

        sender.sendMessage("已开始后台扫描数据库中的神话装备收纳箱，完成后会在插件目录 logs/scan/mythicbundle 下输出 JSON 报告。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> executeScan(sender));
    }

    public void status(CommandSender sender) {
        if (running.get()) {
            Duration duration = Duration.ofMillis(Math.max(0L, System.currentTimeMillis() - startedAt));
            sender.sendMessage("神话装备收纳箱扫描任务运行中。");
            sender.sendMessage("已扫描玩家: " + scannedPlayers.get());
            sender.sendMessage("已扫描邮箱: " + scannedMailboxes.get());
            sender.sendMessage("异常数据数: " + scanErrors.get());
            sender.sendMessage("运行时长: " + formatDuration(duration));
            return;
        }

        sender.sendMessage("当前没有运行中的神话装备收纳箱扫描任务。");
        if (lastReportPath != null) {
            sender.sendMessage("最近一次报告: " + lastReportPath);
            sender.sendMessage("持有玩家数: " + lastOwnerCount + "，收纳箱总数: " + lastBundleCount);
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
            lastBundleCount = sortedOwners.stream().mapToInt(OwnerCount::getCount).sum();
            Path reportPath = writeReport(sortedOwners);
            lastReportPath = reportPath.toAbsolutePath().toString();

            ThePit.getInstance().info("scanmythicbundle finished: scannedPlayers=" + scannedPlayers.get()
                    + ", scannedMailboxes=" + scannedMailboxes.get()
                    + ", scanErrors=" + scanErrors.get()
                    + ", ownerCount=" + lastOwnerCount
                    + ", bundleCount=" + lastBundleCount
                    + ", report=" + lastReportPath);

            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(
                    "扫描完成。持有玩家数: " + lastOwnerCount
                            + "，收纳箱总数: " + lastBundleCount
                            + "，异常数据数: " + scanErrors.get()
                            + "，日志: " + lastReportPath));
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error("scanmythicbundle failed", exception);
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
                    OwnerCount found = new OwnerCount(profile.getUuid(), profile.getPlayerName());
                    PlayerInv inventory = profile.getInventory();
                    if (inventory != null) {
                        BundleCounts inventoryCounts = countItems(inventory.getContents());
                        inventoryCounts.add(countItems(inventory.getArmorContents()));
                        found.addInventory(inventoryCounts);
                    }

                    PlayerEnderChest[] enderChests = profile.getEnderChests();
                    if (enderChests != null) {
                        for (PlayerEnderChest enderChest : enderChests) {
                            if (enderChest != null && enderChest.getInventory() != null) {
                                found.addWarehouses(countItems(enderChest.getInventory().getContents()));
                            }
                        }
                    }

                    if (profile.getAttachedEnchant() != null && !profile.getAttachedEnchant().isBlank()) {
                        found.addAttachedEnchant(countItem(InventoryUtil.deserializeItemStack(profile.getAttachedEnchant())));
                    }

                    if (found.getCount() > 0) {
                        owners.put(profile.getUuid(), found);
                    }
                } catch (Exception exception) {
                    scanErrors.incrementAndGet();
                    ThePit.getInstance().getSLF4JLogger().warn("scanmythicbundle failed to scan player {}", profile.getUuid(), exception);
                }

                if (scannedPlayers.get() % 1000 == 0) {
                    ThePit.getInstance().info("scanmythicbundle progress: scannedPlayers=" + scannedPlayers.get());
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
                    BundleCounts mailCounts = new BundleCounts();
                    for (Mail mail : mailData.getMails()) {
                        if (mail == null || mail.isClaimed() || mail.getExpireTime() <= now || mail.getItem() == null) {
                            continue;
                        }
                        mailCounts.add(countItems(mail.getItem().getContents()));
                        mailCounts.add(countItems(mail.getItem().getArmorContents()));
                    }

                    if (mailCounts.getCount() > 0) {
                        OwnerCount owner = owners.computeIfAbsent(mailData.getUuid(),
                                ignored -> new OwnerCount(mailData.getUuid(), mailData.getName()));
                        if (owner.playerName == null) {
                            owner.playerName = mailData.getName();
                        }
                        owner.addMail(mailCounts);
                    }
                } catch (Exception exception) {
                    scanErrors.incrementAndGet();
                    ThePit.getInstance().getSLF4JLogger().warn("scanmythicbundle failed to scan mailbox {}", mailData.getUuid(), exception);
                }
            }
        }
    }

    private BundleCounts countItems(ItemStack[] items) {
        BundleCounts counts = new BundleCounts();
        if (items == null) {
            return counts;
        }

        for (ItemStack item : items) {
            counts.add(countItem(item));
        }
        return counts;
    }

    private BundleCounts countItem(ItemStack item) {
        BundleCounts counts = new BundleCounts();
        if (item == null) {
            return counts;
        }

        int amount = Math.max(0, item.getAmount());
        String internalName = ItemUtil.getInternalName(item);
        if (internalName == null) {
            return counts;
        }

        switch (internalName.toLowerCase(Locale.ROOT)) {
            case "pants_bundle_full" -> counts.pantsFull += amount;
            case "pants_bundle_empty" -> counts.pantsEmpty += amount;
            case "sword_bundle_full" -> counts.swordFull += amount;
            case "sword_bundle_empty" -> counts.swordEmpty += amount;
            case "bow_bundle_full" -> counts.bowFull += amount;
            case "bow_bundle_empty" -> counts.bowEmpty += amount;
            default -> {
            }
        }
        return counts;
    }

    private Path writeReport(List<OwnerCount> owners) throws IOException {
        Path logsDirectory = ScanReportPath.directory("mythicbundle");

        String suffix = LocalDateTime.now().format(FILE_NAME_TIME);
        Path reportPath = logsDirectory.resolve("mythic-bundle-owners-" + suffix + ".json");
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
        private final int bundleCount;
        private final List<OwnerCount> players;

        private Report(int scannedPlayers,
                       int scannedMailboxes,
                       int scanErrors,
                       int ownerCount,
                       int bundleCount,
                       List<OwnerCount> players) {
            this.scannedPlayers = scannedPlayers;
            this.scannedMailboxes = scannedMailboxes;
            this.scanErrors = scanErrors;
            this.ownerCount = ownerCount;
            this.bundleCount = bundleCount;
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
        private int pantsFull;
        private int pantsEmpty;
        private int swordFull;
        private int swordEmpty;
        private int bowFull;
        private int bowEmpty;
        private int count;

        private OwnerCount(String playerUuid, String playerName) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
        }

        private int getCount() {
            return inventory + warehouses + attachedEnchant + mail;
        }

        private void addInventory(BundleCounts counts) {
            inventory += counts.getCount();
            addTypes(counts);
        }

        private void addWarehouses(BundleCounts counts) {
            warehouses += counts.getCount();
            addTypes(counts);
        }

        private void addAttachedEnchant(BundleCounts counts) {
            attachedEnchant += counts.getCount();
            addTypes(counts);
        }

        private void addMail(BundleCounts counts) {
            mail += counts.getCount();
            addTypes(counts);
        }

        private void addTypes(BundleCounts counts) {
            pantsFull += counts.pantsFull;
            pantsEmpty += counts.pantsEmpty;
            swordFull += counts.swordFull;
            swordEmpty += counts.swordEmpty;
            bowFull += counts.bowFull;
            bowEmpty += counts.bowEmpty;
        }

        private void calculateCount() {
            count = getCount();
        }
    }

    private static final class BundleCounts {
        private int pantsFull;
        private int pantsEmpty;
        private int swordFull;
        private int swordEmpty;
        private int bowFull;
        private int bowEmpty;

        private int getCount() {
            return pantsFull + pantsEmpty + swordFull + swordEmpty + bowFull + bowEmpty;
        }

        private void add(BundleCounts other) {
            pantsFull += other.pantsFull;
            pantsEmpty += other.pantsEmpty;
            swordFull += other.swordFull;
            swordEmpty += other.swordEmpty;
            bowFull += other.bowFull;
            bowEmpty += other.bowEmpty;
        }
    }
}
