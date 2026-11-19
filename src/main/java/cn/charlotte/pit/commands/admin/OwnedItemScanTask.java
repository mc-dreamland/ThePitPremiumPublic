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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class OwnedItemScanTask {

    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private final String commandName;
    private final String scanType;
    private final String displayName;
    private final String reportPrefix;
    private final Map<String, String> trackedTypes;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger scannedPlayers = new AtomicInteger();
    private final AtomicInteger scannedMailboxes = new AtomicInteger();
    private final AtomicInteger scanErrors = new AtomicInteger();

    private volatile long startedAt = -1L;
    private volatile String lastReportPath;
    private volatile String lastError;
    private volatile int lastOwnerCount;
    private volatile int lastItemCount;

    OwnedItemScanTask(String scanType,
                      String commandName,
                      String displayName,
                      String reportPrefix,
                      Map<String, String> trackedTypes) {
        this.scanType = scanType;
        this.commandName = commandName;
        this.displayName = displayName;
        this.reportPrefix = reportPrefix;
        this.trackedTypes = new LinkedHashMap<>();
        trackedTypes.forEach((internalName, reportName) ->
                this.trackedTypes.put(internalName.toLowerCase(Locale.ROOT), reportName));
    }

    void start(CommandSender sender) {
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage("已有" + displayName + "扫描任务正在运行，请使用 /" + commandName + " status 查看进度。");
            return;
        }

        startedAt = System.currentTimeMillis();
        scannedPlayers.set(0);
        scannedMailboxes.set(0);
        scanErrors.set(0);
        lastError = null;

        sender.sendMessage("已开始后台扫描数据库中的" + displayName
                + "，完成后会在插件目录 logs/scan/" + scanType + " 下输出 JSON 报告。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> executeScan(sender));
    }

    void status(CommandSender sender) {
        if (running.get()) {
            Duration duration = Duration.ofMillis(Math.max(0L, System.currentTimeMillis() - startedAt));
            sender.sendMessage(displayName + "扫描任务运行中。");
            sender.sendMessage("已扫描玩家: " + scannedPlayers.get());
            sender.sendMessage("已扫描邮箱: " + scannedMailboxes.get());
            sender.sendMessage("异常数据数: " + scanErrors.get());
            sender.sendMessage("运行时长: " + formatDuration(duration));
            return;
        }

        sender.sendMessage("当前没有运行中的" + displayName + "扫描任务。");
        if (lastReportPath != null) {
            sender.sendMessage("最近一次报告: " + lastReportPath);
            sender.sendMessage("持有玩家数: " + lastOwnerCount + "，物品总数: " + lastItemCount);
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
            lastItemCount = sortedOwners.stream().mapToInt(OwnerCount::getCount).sum();
            Path reportPath = writeReport(sortedOwners);
            lastReportPath = reportPath.toAbsolutePath().toString();

            ThePit.getInstance().info(commandName + " finished: scannedPlayers=" + scannedPlayers.get()
                    + ", scannedMailboxes=" + scannedMailboxes.get()
                    + ", scanErrors=" + scanErrors.get()
                    + ", ownerCount=" + lastOwnerCount
                    + ", itemCount=" + lastItemCount
                    + ", report=" + lastReportPath);

            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(
                    "扫描完成。持有玩家数: " + lastOwnerCount
                            + "，物品总数: " + lastItemCount
                            + "，异常数据数: " + scanErrors.get()
                            + "，日志: " + lastReportPath));
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error(commandName + " failed", exception);
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
                        ItemCounts inventoryCounts = countItems(inventory.getContents());
                        inventoryCounts.add(countItems(inventory.getArmorContents()));
                        found.addInventory(inventoryCounts);
                    }

                    PlayerEnderChest[] enderChests = profile.getEnderChests();
                    if (enderChests != null) {
                        for (PlayerEnderChest enderChest : enderChests) {
                            if (enderChest != null && enderChest.getInventory() != null) {
                                ItemCounts warehouseCounts = countItems(enderChest.getInventory().getContents());
                                found.addWarehouses(warehouseCounts);
                            }
                        }
                    }

                    found.addAttachedEnchant(countSerialized(profile.getAttachedEnchant()));
                    found.addEnchantingItem(countSerialized(profile.getEnchantingItem()));
                    found.addEnchantingScience(countSerialized(profile.getEnchantingScience()));
                    found.addForge(countSerialized(profile.getForgeItem()));
                    found.addExorcismAltar(countSerialized(profile.getExorcismAltarItem()));
                    found.addMysteryMain(countSerialized(profile.getMysteryMainItem()));
                    found.addMysteryMaterial(countSerialized(profile.getMysteryOffItem()));

                    if (found.getCount() > 0) {
                        owners.put(profile.getUuid(), found);
                    }
                } catch (Exception exception) {
                    scanErrors.incrementAndGet();
                    ThePit.getInstance().getSLF4JLogger().warn(commandName + " failed to scan player {}", profile.getUuid(), exception);
                }

                if (scannedPlayers.get() % 1000 == 0) {
                    ThePit.getInstance().info(commandName + " progress: scannedPlayers=" + scannedPlayers.get());
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
                    ItemCounts mailCounts = new ItemCounts();
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
                    ThePit.getInstance().getSLF4JLogger().warn(commandName + " failed to scan mailbox {}", mailData.getUuid(), exception);
                }
            }
        }
    }

    private ItemCounts countSerialized(String serializedItem) {
        if (serializedItem == null || serializedItem.isBlank()) {
            return new ItemCounts();
        }
        return countItem(InventoryUtil.deserializeItemStack(serializedItem));
    }

    private ItemCounts countItems(ItemStack[] items) {
        ItemCounts counts = new ItemCounts();
        if (items == null) {
            return counts;
        }
        for (ItemStack item : items) {
            counts.add(countItem(item));
        }
        return counts;
    }

    private ItemCounts countItem(ItemStack item) {
        ItemCounts counts = new ItemCounts();
        if (item == null || item.getType().isAir()) {
            return counts;
        }

        String internalName = ItemUtil.getInternalName(item);
        if (internalName == null) {
            return counts;
        }
        String reportType = trackedTypes.get(internalName.toLowerCase(Locale.ROOT));
        if (reportType != null) {
            counts.add(reportType, Math.max(0, item.getAmount()));
        }
        return counts;
    }

    private Path writeReport(List<OwnerCount> owners) throws IOException {
        Path logsDirectory = ScanReportPath.directory(scanType);

        String suffix = LocalDateTime.now().format(FILE_NAME_TIME);
        Path reportPath = logsDirectory.resolve(reportPrefix + "-" + suffix + ".json");
        Report report = new Report(
                displayName,
                new LinkedHashMap<>(trackedTypes),
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
        private final String item;
        private final Map<String, String> trackedInternalNames;
        private final int scannedPlayers;
        private final int scannedMailboxes;
        private final int scanErrors;
        private final int ownerCount;
        private final int itemCount;
        private final List<OwnerCount> players;

        private Report(String item,
                       Map<String, String> trackedInternalNames,
                       int scannedPlayers,
                       int scannedMailboxes,
                       int scanErrors,
                       int ownerCount,
                       int itemCount,
                       List<OwnerCount> players) {
            this.item = item;
            this.trackedInternalNames = trackedInternalNames;
            this.scannedPlayers = scannedPlayers;
            this.scannedMailboxes = scannedMailboxes;
            this.scanErrors = scanErrors;
            this.ownerCount = ownerCount;
            this.itemCount = itemCount;
            this.players = new ArrayList<>(players);
        }
    }

    private static final class OwnerCount {
        private final String playerUuid;
        private String playerName;
        private int inventory;
        private int warehouses;
        private int attachedEnchant;
        private int enchantingItem;
        private int enchantingScience;
        private int forge;
        private int exorcismAltar;
        private int mysteryMain;
        private int mysteryMaterial;
        private int mail;
        private final Map<String, Integer> types = new LinkedHashMap<>();
        private int count;

        private OwnerCount(String playerUuid, String playerName) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
        }

        private int getCount() {
            return inventory + warehouses + attachedEnchant + enchantingItem + enchantingScience
                    + forge + exorcismAltar + mysteryMain + mysteryMaterial + mail;
        }

        private void addInventory(ItemCounts counts) {
            inventory += counts.getCount();
            addTypes(counts);
        }

        private void addWarehouses(ItemCounts counts) {
            warehouses += counts.getCount();
            addTypes(counts);
        }

        private void addAttachedEnchant(ItemCounts counts) {
            attachedEnchant += counts.getCount();
            addTypes(counts);
        }

        private void addEnchantingItem(ItemCounts counts) {
            enchantingItem += counts.getCount();
            addTypes(counts);
        }

        private void addEnchantingScience(ItemCounts counts) {
            enchantingScience += counts.getCount();
            addTypes(counts);
        }

        private void addForge(ItemCounts counts) {
            forge += counts.getCount();
            addTypes(counts);
        }

        private void addExorcismAltar(ItemCounts counts) {
            exorcismAltar += counts.getCount();
            addTypes(counts);
        }

        private void addMysteryMain(ItemCounts counts) {
            mysteryMain += counts.getCount();
            addTypes(counts);
        }

        private void addMysteryMaterial(ItemCounts counts) {
            mysteryMaterial += counts.getCount();
            addTypes(counts);
        }

        private void addMail(ItemCounts counts) {
            mail += counts.getCount();
            addTypes(counts);
        }

        private void addTypes(ItemCounts counts) {
            counts.types.forEach((type, amount) -> types.merge(type, amount, Integer::sum));
        }

        private void calculateCount() {
            count = getCount();
        }
    }

    private static final class ItemCounts {
        private final Map<String, Integer> types = new LinkedHashMap<>();

        private int getCount() {
            return types.values().stream().mapToInt(Integer::intValue).sum();
        }

        private void add(String type, int amount) {
            if (amount > 0) {
                types.merge(type, amount, Integer::sum);
            }
        }

        private void add(ItemCounts other) {
            other.types.forEach(this::add);
        }
    }
}
