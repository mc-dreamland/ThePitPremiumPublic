package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerMailData;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.mail.Mail;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.AdvancedItemCatalog;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanAdvancedItem {

    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private static final Map<String, ItemDefinition> TRACKED_ITEMS = trackedItems();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger scannedPlayers = new AtomicInteger();
    private final AtomicInteger scannedMailboxes = new AtomicInteger();
    private final AtomicInteger scannedItemStacks = new AtomicInteger();
    private final AtomicInteger scanErrors = new AtomicInteger();

    private volatile long startedAt = -1L;
    private volatile String lastReportDirectory;
    private volatile String lastError;
    private volatile int lastMatchedTypes;
    private volatile int lastOwnerCount;
    private volatile long lastItemCount;

    public void start(CommandSender sender) {
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage("已有高级道具扫描任务正在运行，请使用 /scan advanceditem status 查看进度。");
            return;
        }

        startedAt = System.currentTimeMillis();
        scannedPlayers.set(0);
        scannedMailboxes.set(0);
        scannedItemStacks.set(0);
        scanErrors.set(0);
        lastError = null;

        sender.sendMessage("已开始后台扫描数据库中的高级道具，报告将按物品类型输出到 logs/scan/advanceditem。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> executeScan(sender));
    }

    public void status(CommandSender sender) {
        if (running.get()) {
            sender.sendMessage("高级道具扫描任务运行中。");
            sender.sendMessage("已扫描玩家: " + scannedPlayers.get());
            sender.sendMessage("已扫描邮箱: " + scannedMailboxes.get());
            sender.sendMessage("已扫描物品堆: " + scannedItemStacks.get());
            sender.sendMessage("异常数据数: " + scanErrors.get());
            sender.sendMessage("运行时长: " + formatDuration(System.currentTimeMillis() - startedAt));
            return;
        }

        sender.sendMessage("当前没有运行中的高级道具扫描任务。");
        if (lastReportDirectory != null) {
            sender.sendMessage("最近一次报告目录: " + lastReportDirectory);
            sender.sendMessage("命中物品类型: " + lastMatchedTypes
                    + "，持有玩家数: " + lastOwnerCount
                    + "，高级道具总数: " + lastItemCount);
        }
        if (lastError != null) {
            sender.sendMessage("最近一次错误: " + lastError);
        }
    }

    private void executeScan(CommandSender sender) {
        Map<String, TypeAccumulator> results = new LinkedHashMap<>();
        TRACKED_ITEMS.values().forEach(definition ->
                results.put(definition.internalName, new TypeAccumulator(definition)));

        try {
            scanProfiles(results);
            scanMailboxes(results);
            ReportWriteResult writeResult = writeReports(results);

            lastReportDirectory = writeResult.directory.toAbsolutePath().toString();
            lastMatchedTypes = writeResult.matchedTypes;
            lastOwnerCount = writeResult.ownerCount;
            lastItemCount = writeResult.itemCount;

            ThePit.getInstance().info("scan advanceditem finished: scannedPlayers=" + scannedPlayers.get()
                    + ", scannedMailboxes=" + scannedMailboxes.get()
                    + ", scannedItemStacks=" + scannedItemStacks.get()
                    + ", scanErrors=" + scanErrors.get()
                    + ", matchedTypes=" + lastMatchedTypes
                    + ", ownerCount=" + lastOwnerCount
                    + ", itemCount=" + lastItemCount
                    + ", reportDirectory=" + lastReportDirectory);

            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(
                    "扫描完成。命中物品类型: " + lastMatchedTypes
                            + "，持有玩家数: " + lastOwnerCount
                            + "，高级道具总数: " + lastItemCount
                            + "，异常数据数: " + scanErrors.get()
                            + "，报告目录: " + lastReportDirectory));
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error("scan advanceditem failed", exception);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage("扫描失败: " + lastError));
        } finally {
            running.set(false);
        }
    }

    private void scanProfiles(Map<String, TypeAccumulator> results) {
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

                try {
                    String playerUuid = profile.getUuid();
                    String playerName = profile.getPlayerName();
                    PlayerInv inventory = profile.getInventory();
                    if (inventory != null) {
                        scanItems(results, playerUuid, playerName, Container.INVENTORY, inventory.getContents());
                        scanItems(results, playerUuid, playerName, Container.ARMOR, inventory.getArmorContents());
                    }

                    PlayerEnderChest[] enderChests = profile.getEnderChests();
                    if (enderChests != null) {
                        for (PlayerEnderChest enderChest : enderChests) {
                            if (enderChest != null && enderChest.getInventory() != null) {
                                scanItems(results, playerUuid, playerName, Container.WAREHOUSES,
                                        enderChest.getInventory().getContents());
                            }
                        }
                    }

                    scanSerialized(results, playerUuid, playerName, Container.ATTACHED_ENCHANT, profile.getAttachedEnchant());
                    scanSerialized(results, playerUuid, playerName, Container.ENCHANTING_ITEM, profile.getEnchantingItem());
                    scanSerialized(results, playerUuid, playerName, Container.ENCHANTING_SCIENCE, profile.getEnchantingScience());
                    scanSerialized(results, playerUuid, playerName, Container.FORGE, profile.getForgeItem());
                    scanSerialized(results, playerUuid, playerName, Container.EXORCISM_ALTAR, profile.getExorcismAltarItem());
                    scanSerialized(results, playerUuid, playerName, Container.MYSTERY_MAIN, profile.getMysteryMainItem());
                    scanSerialized(results, playerUuid, playerName, Container.MYSTERY_MATERIAL, profile.getMysteryOffItem());
                } catch (Exception exception) {
                    scanErrors.incrementAndGet();
                    ThePit.getInstance().getSLF4JLogger().warn(
                            "scan advanceditem failed to scan player {}", profile.getUuid(), exception);
                }

                if (scannedPlayers.get() % 1000 == 0) {
                    ThePit.getInstance().info("scan advanceditem progress: scannedPlayers=" + scannedPlayers.get()
                            + ", scannedItemStacks=" + scannedItemStacks.get());
                }
            }
        }
    }

    private void scanMailboxes(Map<String, TypeAccumulator> results) {
        long now = System.currentTimeMillis();
        try (MongoCursor<PlayerMailData> cursor = ThePit.getInstance()
                .getMongoDB()
                .getMailCollection()
                .find()
                .iterator()) {
            while (cursor.hasNext()) {
                PlayerMailData mailData = cursor.next();
                scannedMailboxes.incrementAndGet();
                if (mailData == null || mailData.getUuid() == null || mailData.getMails() == null) {
                    continue;
                }

                try {
                    for (Mail mail : mailData.getMails()) {
                        if (mail == null || mail.isClaimed() || mail.getExpireTime() <= now || mail.getItem() == null) {
                            continue;
                        }
                        scanItems(results, mailData.getUuid(), mailData.getName(), Container.MAIL,
                                mail.getItem().getContents());
                        scanItems(results, mailData.getUuid(), mailData.getName(), Container.MAIL,
                                mail.getItem().getArmorContents());
                    }
                } catch (Exception exception) {
                    scanErrors.incrementAndGet();
                    ThePit.getInstance().getSLF4JLogger().warn(
                            "scan advanceditem failed to scan mailbox {}", mailData.getUuid(), exception);
                }
            }
        }
    }

    private void scanSerialized(Map<String, TypeAccumulator> results,
                                String playerUuid,
                                String playerName,
                                Container container,
                                String serializedItem) {
        if (serializedItem == null || serializedItem.isBlank()) {
            return;
        }
        scanItem(results, playerUuid, playerName, container,
                InventoryUtil.deserializeItemStack(serializedItem));
    }

    private void scanItems(Map<String, TypeAccumulator> results,
                           String playerUuid,
                           String playerName,
                           Container container,
                           ItemStack[] items) {
        if (items == null) {
            return;
        }
        for (ItemStack item : items) {
            scanItem(results, playerUuid, playerName, container, item);
        }
    }

    private void scanItem(Map<String, TypeAccumulator> results,
                          String playerUuid,
                          String playerName,
                          Container container,
                          ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        scannedItemStacks.incrementAndGet();

        String internalName = ItemUtil.getInternalName(item);
        if (internalName == null || internalName.isBlank()) {
            return;
        }
        ItemDefinition definition = TRACKED_ITEMS.get(internalName.toLowerCase(Locale.ROOT));
        if (definition == null) {
            return;
        }

        TypeAccumulator accumulator = results.get(definition.internalName);
        if (accumulator != null) {
            accumulator.add(playerUuid, playerName, container, Math.max(1, item.getAmount()));
        }
    }

    private ReportWriteResult writeReports(Map<String, TypeAccumulator> results) throws IOException {
        Path directory = ScanReportPath.directory("advanceditem");
        String suffix = LocalDateTime.now().format(FILE_NAME_TIME);
        List<ItemSummary> summaries = new ArrayList<>();
        java.util.Set<String> owners = new java.util.HashSet<>();
        int matchedTypes = 0;
        long totalItems = 0L;

        for (TypeAccumulator accumulator : results.values()) {
            List<OwnerCount> sortedOwners = accumulator.sortedOwners();
            long itemCount = accumulator.itemCount();
            String fileName = null;
            if (itemCount > 0L) {
                matchedTypes++;
                totalItems += itemCount;
                sortedOwners.forEach(owner -> owners.add(owner.playerUuid));
                fileName = ScanReportPath.sanitize(accumulator.definition.internalName)
                        + "-" + suffix + ".json";
                Path reportPath = directory.resolve(fileName);
                ItemTypeReport report = new ItemTypeReport(
                        accumulator.definition.internalName,
                        accumulator.definition.displayName,
                        itemCount,
                        sortedOwners.size(),
                        sortedOwners
                );
                writeJson(reportPath, report);
            }
            summaries.add(new ItemSummary(
                    accumulator.definition.internalName,
                    accumulator.definition.displayName,
                    itemCount,
                    sortedOwners.size(),
                    fileName
            ));
        }

        summaries.sort(Comparator.comparingLong((ItemSummary summary) -> summary.itemCount).reversed()
                .thenComparing(summary -> summary.internalName, String.CASE_INSENSITIVE_ORDER));
        Path summaryPath = directory.resolve("summary-" + suffix + ".json");
        writeJson(summaryPath, new SummaryReport(
                scannedPlayers.get(),
                scannedMailboxes.get(),
                scannedItemStacks.get(),
                scanErrors.get(),
                TRACKED_ITEMS.size(),
                matchedTypes,
                owners.size(),
                totalItems,
                summaries
        ));
        return new ReportWriteResult(directory, matchedTypes, owners.size(), totalItems);
    }

    private void writeJson(Path path, Object report) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW)) {
            GSON.toJson(report, writer);
        }
    }

    private String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(Math.max(0L, millis));
        long seconds = duration.getSeconds();
        return String.format("%02d:%02d:%02d", seconds / 3600, seconds % 3600 / 60, seconds % 60);
    }

    private static Map<String, ItemDefinition> trackedItems() {
        Map<String, ItemDefinition> items = new LinkedHashMap<>();
        AdvancedItemCatalog.items().forEach((internalName, displayName) ->
                track(items, internalName, displayName));

        return Map.copyOf(items);
    }

    private static void track(Map<String, ItemDefinition> items, String internalName, String displayName) {
        if (internalName == null || internalName.isBlank()) {
            return;
        }
        items.put(internalName.toLowerCase(Locale.ROOT), new ItemDefinition(internalName, displayName));
    }

    private enum Container {
        INVENTORY,
        ARMOR,
        WAREHOUSES,
        ATTACHED_ENCHANT,
        ENCHANTING_ITEM,
        ENCHANTING_SCIENCE,
        FORGE,
        EXORCISM_ALTAR,
        MYSTERY_MAIN,
        MYSTERY_MATERIAL,
        MAIL
    }

    private record ItemDefinition(String internalName, String displayName) {
    }

    private static final class TypeAccumulator {
        private final ItemDefinition definition;
        private final Map<String, OwnerCount> owners = new LinkedHashMap<>();

        private TypeAccumulator(ItemDefinition definition) {
            this.definition = definition;
        }

        private void add(String playerUuid, String playerName, Container container, int amount) {
            OwnerCount owner = owners.computeIfAbsent(playerUuid,
                    ignored -> new OwnerCount(playerUuid, playerName));
            if ((owner.playerName == null || owner.playerName.isBlank()) && playerName != null) {
                owner.playerName = playerName;
            }
            owner.add(container, amount);
        }

        private long itemCount() {
            return owners.values().stream().mapToLong(OwnerCount::getCount).sum();
        }

        private List<OwnerCount> sortedOwners() {
            return owners.values().stream()
                    .filter(owner -> owner.getCount() > 0L)
                    .sorted(Comparator.comparingLong(OwnerCount::getCount).reversed()
                            .thenComparing(owner -> owner.playerName == null ? "" : owner.playerName,
                                    String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(owner -> owner.playerUuid))
                    .toList();
        }
    }

    private static final class OwnerCount {
        private final String playerUuid;
        private String playerName;
        private long inventory;
        private long armor;
        private long warehouses;
        private long attachedEnchant;
        private long enchantingItem;
        private long enchantingScience;
        private long forge;
        private long exorcismAltar;
        private long mysteryMain;
        private long mysteryMaterial;
        private long mail;
        private long count;

        private OwnerCount(String playerUuid, String playerName) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
        }

        private void add(Container container, int amount) {
            switch (container) {
                case INVENTORY -> inventory += amount;
                case ARMOR -> armor += amount;
                case WAREHOUSES -> warehouses += amount;
                case ATTACHED_ENCHANT -> attachedEnchant += amount;
                case ENCHANTING_ITEM -> enchantingItem += amount;
                case ENCHANTING_SCIENCE -> enchantingScience += amount;
                case FORGE -> forge += amount;
                case EXORCISM_ALTAR -> exorcismAltar += amount;
                case MYSTERY_MAIN -> mysteryMain += amount;
                case MYSTERY_MATERIAL -> mysteryMaterial += amount;
                case MAIL -> mail += amount;
            }
            count += amount;
        }

        private long getCount() {
            return count;
        }
    }

    private record ItemTypeReport(String internalName,
                                  String displayName,
                                  long itemCount,
                                  int ownerCount,
                                  List<OwnerCount> players) {
    }

    private record ItemSummary(String internalName,
                               String displayName,
                               long itemCount,
                               int ownerCount,
                               String file) {
    }

    private record SummaryReport(int scannedPlayers,
                                 int scannedMailboxes,
                                 int scannedItemStacks,
                                 int scanErrors,
                                 int trackedTypes,
                                 int matchedTypes,
                                 int ownerCount,
                                 long itemCount,
                                 List<ItemSummary> items) {
    }

    private record ReportWriteResult(Path directory, int matchedTypes, int ownerCount, long itemCount) {
    }
}
