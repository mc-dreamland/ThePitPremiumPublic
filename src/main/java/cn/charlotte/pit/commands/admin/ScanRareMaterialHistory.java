package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerInvBackup;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.menu.admin.backpack.BackupShowMenu;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.AdvancedItemCatalog;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import cn.charlotte.pit.util.menu.buttons.BackButton;
import cn.charlotte.pit.util.menu.buttons.DisplayButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.result.UpdateResult;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import org.bson.types.ObjectId;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.function.Consumer;

final class ScanRareMaterialHistory {

    private static final int DEFAULT_MINIMUM_ITEMS = 10;
    private static final int MAXIMUM_MINIMUM_ITEMS = 1_000_000;
    private static final int ITEMS_PER_PAGE = 45;
    private static final int UUID_TRACE_BATCH_SIZE = 500;
    private static final String PHASE_BOOK_MARKED_MYTHIC = "phase_book_marked_mythic";
    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private static final Gson COMPACT_GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();
    private static final Map<String, String> TRACKED_ITEMS = createTrackedItems();

    /* This lock belongs only to this scanner. Other /scan tasks can run concurrently. */
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final MaterialHistoryScanSpeed scanSpeed = new MaterialHistoryScanSpeed();
    private volatile int activeBatchSize;
    private volatile MaterialHistoryFastScan.CacheStats scanCacheStats = new MaterialHistoryFastScan.CacheStats(0, 0, 0, 0);
    private final AtomicInteger scannedBackups = new AtomicInteger();
    private final AtomicInteger matchedBackups = new AtomicInteger();
    private final AtomicInteger matchedUuidStackBackups = new AtomicInteger();
    private final AtomicInteger stackedUuidStacks = new AtomicInteger();
    private final AtomicInteger scanErrors = new AtomicInteger();
    private final AtomicBoolean uuidTraceRunning = new AtomicBoolean(false);
    private final AtomicBoolean uuidTransferQueryRunning = new AtomicBoolean(false);
    private final AtomicBoolean legitGemQueryRunning = new AtomicBoolean(false);
    private final AtomicBoolean legitGemUsageQueryRunning = new AtomicBoolean(false);
    private final AtomicInteger uuidTraceBackups = new AtomicInteger();
    private final AtomicInteger uuidTraceMatches = new AtomicInteger();

    private volatile long startedAt = -1L;
    private volatile String lastReportPath;
    private volatile String lastAllReportPath;
    private volatile String lastIndexSourceReportPath;
    private volatile boolean lastScanAll;
    private volatile String lastError;
    private volatile int lastMatchedPlayers;
    private volatile int lastMaximumItems;
    private volatile Report latestReport;
    private volatile long uuidTraceStartedAt = -1L;
    private volatile String lastUuidTraceReportPath;
    private volatile String lastUuidTraceError;
    private volatile UuidTraceReport latestUuidTraceReport;
    private final AtomicBoolean currentRareHistoryRunning = new AtomicBoolean(false);
    private volatile UuidTraceReport latestCurrentRareHistoryReport;
    private volatile String lastCurrentRareHistoryPath;
    private volatile String lastCurrentRareHistoryError;
    private volatile String currentRareHistoryPlayer;
    private volatile long currentRareHistoryStartedAt;
    private ScanMaterialUuidHistoryIndex uuidHistoryIndex;
    private ScanMaterialEnchantmentHistory enchantmentHistory;

    void setUuidHistoryIndex(ScanMaterialUuidHistoryIndex uuidHistoryIndex) {
        this.uuidHistoryIndex = uuidHistoryIndex;
    }

    void setEnchantmentHistory(ScanMaterialEnchantmentHistory enchantmentHistory) {
        this.enchantmentHistory = enchantmentHistory;
    }

    private void openEquipmentEnchantments(Player player, UuidEquipmentInfo equipment, Menu backMenu) {
        CurrentUuidItem item = equipment.currentItem;
        if (item == null || !CurrentHistoryItemActions.canViewEnchantments(item.internalName, item.uuid)) {
            player.sendMessage("§e该物品没有 UUID，或不属于可查询附魔记录的装备类型。");
            return;
        }
        if (enchantmentHistory == null) {
            player.sendMessage("§c附魔历史查看功能未初始化，请联系管理员。");
            return;
        }
        enchantmentHistory.openItem(player, item.uuid, backMenu);
    }

    void start(CommandSender sender, Integer minimumItems) {
        start(sender, minimumItems, false);
    }

    void startAll(CommandSender sender, Integer minimumItems) {
        start(sender, minimumItems, true);
    }

    private void start(CommandSender sender, Integer minimumItems, boolean streamingAll) {
        int minimum = minimumItems == null ? (streamingAll ? 1 : DEFAULT_MINIMUM_ITEMS) : minimumItems;
        if (minimum < 1 || minimum > MAXIMUM_MINIMUM_ITEMS) {
            sender.sendMessage("最小物资数量必须在 1 到 " + MAXIMUM_MINIMUM_ITEMS + " 之间。");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage("已有历史稀有物资扫描正在运行，请使用 /scan materialhistory status 查看进度。");
            return;
        }

        startedAt = System.currentTimeMillis();
        scannedBackups.set(0);
        matchedBackups.set(0);
        matchedUuidStackBackups.set(0);
        stackedUuidStacks.set(0);
        scanErrors.set(0);
        lastError = null;
        lastMatchedPlayers = 0;
        lastMaximumItems = 0;
        lastScanAll = streamingAll;
        scanSpeed.reset();
        scanCacheStats = new MaterialHistoryFastScan.CacheStats(0, 0, 0, 0);
        activeBatchSize = scanSpeed.batchSize();

        sender.sendMessage("已开始异步扫描全库历史背包和仓库备份；"
                + "全部高级道具及原物资清单（不要求 UUID）合计达到 " + minimum
                + " 件，或存在带 UUID 且数量大于 1 的物品栈时，快照会写入本地报告。"
                + "该任务可与其他 scan 同时运行。");
        sender.sendMessage("扫描速度: " + speedLabel() + "，数据库每批 " + activeBatchSize
                + " 份。可用 /scan materialhistory speed <每秒份数> 实时调速，0 为不限速。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            if (streamingAll) executeAllScan(sender, minimum);
            else executeScan(sender, minimum);
        });
    }

    void status(CommandSender sender) {
        sender.sendMessage("扫描模式: " + (lastScanAll ? "all：流式报告" : "普通报告") + "，全部高级道具及原物资清单（含无 UUID）");
        sender.sendMessage("处理上限: " + speedLabel() + "，数据库批量: "
                + (running.get() ? activeBatchSize : scanSpeed.batchSize()) + " 份。");
        if (running.get()) {
            sender.sendMessage("历史稀有物资扫描运行中。");
            sender.sendMessage("已读取备份: " + scannedBackups.get());
            sender.sendMessage("已命中备份: " + matchedBackups.get());
            sender.sendMessage("UUID 异常堆叠备份: " + matchedUuidStackBackups.get()
                    + "，异常物品栈: " + stackedUuidStacks.get());
            sender.sendMessage("异常数据数: " + scanErrors.get());
            sender.sendMessage("运行时长: " + formatDuration(System.currentTimeMillis() - startedAt));
            sender.sendMessage(String.format(Locale.ROOT, "实际平均: %.1f 份/秒，解码缓存命中: %d，实际解码: %d。",
                    averageScanRate(), scanCacheStats.hits(), scanCacheStats.decodes()));
            return;
        }

        sender.sendMessage("当前没有运行中的历史稀有物资扫描。");
        String reportPath = lastScanAll ? lastAllReportPath : lastReportPath;
        if (reportPath != null) {
            sender.sendMessage("最近一次报告: " + reportPath);
            sender.sendMessage("命中玩家: " + lastMatchedPlayers + "，命中备份: " + matchedBackups.get()
                    + "，单个快照最高物资数: " + lastMaximumItems);
            sender.sendMessage("UUID 异常堆叠备份: " + matchedUuidStackBackups.get()
                    + "，异常物品栈: " + stackedUuidStacks.get());
        }
        if (lastError != null) {
            sender.sendMessage("最近一次错误: " + lastError);
        }
    }

    void speed(CommandSender sender, Integer perSecond) {
        try {
            if (perSecond != null) scanSpeed.perSecond(perSecond);
            sender.sendMessage("materialhistory 处理上限: " + speedLabel()
                    + "；运行中的普通/all 扫描实时生效。0 为不限速，数值为上限而非保证速度。设置仅保留至插件重启。");
        } catch (IllegalArgumentException exception) { sender.sendMessage(exception.getMessage()); }
    }

    void batch(CommandSender sender, Integer size) {
        try {
            if (size != null) scanSpeed.batchSize(size);
            sender.sendMessage("materialhistory 数据库每批 " + scanSpeed.batchSize()
                    + " 份；下次扫描生效，不是并发数量。设置仅保留至插件重启。");
        } catch (IllegalArgumentException exception) { sender.sendMessage(exception.getMessage()); }
    }

    private String speedLabel() { return scanSpeed.perSecond() == 0 ? "不限速" : scanSpeed.perSecond() + " 份/秒"; }
    private double averageScanRate() { return scannedBackups.get() * 1000.0 / Math.max(1L, System.currentTimeMillis() - startedAt); }

    void open(CommandSender sender, String playerName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行。");
            return;
        }
        String requestedName = playerName == null ? "" : playerName.trim();
        if (requestedName.isEmpty()) {
            player.sendMessage("§7正在读取最近一次扫描命中的玩家列表...");
            Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> openPlayerListAsync(player));
            return;
        }

        player.sendMessage("§7正在读取最近一次历史稀有物资扫描报告...");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> openAsync(player, requestedName));
    }

    void traceCurrentInventoryUuids(CommandSender sender, String playerName) {
        String requestedName = playerName == null ? "" : playerName.trim();
        if (requestedName.isEmpty()) {
            sender.sendMessage("请提供玩家名。");
            return;
        }
        if (!uuidTraceRunning.compareAndSet(false, true)) {
            sender.sendMessage("已有 materialhistory UUID 追踪任务正在运行，请使用"
                    + " /scan materialhistory uuidtrace status 查看进度。");
            return;
        }

        uuidTraceStartedAt = System.currentTimeMillis();
        uuidTraceBackups.set(0);
        uuidTraceMatches.set(0);
        lastUuidTraceError = null;

        Player onlineTarget = Bukkit.getPlayerExact(requestedName);
        LiveInventorySnapshot liveSnapshot = onlineTarget == null
                ? null
                : captureLiveInventory(onlineTarget);
        sender.sendMessage("已开始异步追踪 " + requestedName + " 当前背包、装备栏、副手和仓库内的 UUID 物品；"
                + "查询最近一次 materialhistory 报告命中的全部玩家备份（包含本人），结果按时间升序写入日志。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(),
                () -> executeUuidTrace(sender, requestedName, liveSnapshot));
    }

    void traceCurrentRareHistory(CommandSender sender, String playerName) {
        String requestedName = playerName == null ? "" : playerName.trim();
        if (requestedName.isEmpty()) {
            sender.sendMessage("请提供玩家名。");
            return;
        }
        if (!currentRareHistoryRunning.compareAndSet(false, true)) {
            sender.sendMessage("已有当前稀有物品历史查询正在运行，请使用 /scan currenthistory status 查看进度。");
            return;
        }
        currentRareHistoryPlayer = requestedName;
        currentRareHistoryStartedAt = System.currentTimeMillis();
        lastCurrentRareHistoryError = null;
        sender.sendMessage("开始查询 " + requestedName + " 当前背包、装备栏、副手和全部仓库的稀有物品；历史仅从本地 UUID 索引读取。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            try {
                PlayerProfile profile = PlayerProfile.getOrLoadPlayerProfileByName(requestedName);
                LiveInventorySnapshot snapshot = Bukkit.getScheduler().callSyncMethod(
                        ThePit.getInstance(), () -> captureCurrentRareInventory(profile))
                        .get(30, TimeUnit.SECONDS);
                CurrentUuidSource source = createCurrentRareSource(snapshot);
                Set<String> uuids = new HashSet<>();
                int unidentified = 0;
                for (CurrentUuidItem item : source.items) {
                    if (item.uuid == null || item.uuid.isBlank()) {
                        unidentified++;
                    } else {
                        uuids.add(item.uuid);
                    }
                }

                ScanMaterialUuidHistoryIndex.UuidHistoryLookup lookup = uuidHistoryIndex == null
                        ? null : uuidHistoryIndex.lookup(uuids);
                if (lookup == null || lookup.manifest == null) {
                    throw new IllegalStateException("没有本地 UUID 历史索引，请先运行 /scan materialhistory uuidindex");
                }
                List<UuidTraceOccurrence> history = convertIndexedEvents(lookup.events);
                Set<String> matchedUuids = new HashSet<>();
                Set<String> matchedBackupIds = new HashSet<>();
                for (UuidTraceOccurrence occurrence : history) {
                    matchedUuids.add(occurrence.itemUuid);
                }
                for (ScanMaterialUuidHistoryIndex.UuidHistoryEvent event : lookup.events) {
                    if (event.backupDatabaseId != null || event.backupUuid != null) {
                        matchedBackupIds.add(event.backupDatabaseId == null
                                ? "uuid:" + event.backupUuid : "id:" + event.backupDatabaseId);
                    }
                }
                long now = System.currentTimeMillis();
                UuidTraceReport report = new UuidTraceReport(now, formatTime(now),
                        "local_uuid_history_index", source, uuids.size(),
                        lookup.manifest.candidateBackups, 0, matchedBackupIds.size(),
                        lookup.events.size(), history.size(), history);
                report.currentRareHistory = true;
                report.unidentifiedItemStacks = unidentified;
                report.historyIndexGeneratedAt = lookup.manifest.generatedAt;
                Path path = ScanReportPath.directory("materialhistory").resolve(
                        "current-history-" + ScanReportPath.sanitize(source.playerName)
                                + "-" + LocalDateTime.now().format(FILE_NAME_TIME) + ".json");
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW)) {
                    GSON.toJson(report, writer);
                }
                latestCurrentRareHistoryReport = report;
                lastCurrentRareHistoryPath = path.toAbsolutePath().toString();
                Set<String> blacklistedUuids = loadBlacklistedUuids(uuids);
                ThePit.getInstance().info("scan currenthistory finished: player=" + source.playerName
                        + ", currentRareStacks=" + source.items.size() + ", uniqueUuids=" + uuids.size()
                        + ", matchedUuids=" + matchedUuids.size() + ", unidentifiedStacks=" + unidentified
                        + ", localHistoryStates=" + history.size() + ", report=" + path.toAbsolutePath());
                int unidentifiedCount = unidentified;
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    if (sender instanceof Player viewer && !viewer.isOnline()) {
                        return;
                    }
                    sender.sendMessage("当前稀有物品历史查询完成：" + source.items.size()
                            + " 组物品，" + uuids.size() + " 个唯一 UUID，"
                            + matchedUuids.size() + " 个找到本地历史，" + unidentifiedCount + " 组没有 UUID。");
                    sender.sendMessage("本地索引生成时间：" + formatTime(report.historyIndexGeneratedAt)
                            + "；报告：" + path.toAbsolutePath());
                    sender.sendMessage("重新查看：/scan currenthistory view " + source.playerName);
                    if (sender instanceof Player viewer) {
                        new UuidTraceEquipmentMenu(report, blacklistedUuids).openMenu(viewer);
                    }
                });
            } catch (Exception exception) {
                lastCurrentRareHistoryError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
                ThePit.getInstance().getSLF4JLogger().error(
                        "Failed to query current rare item history for " + requestedName, exception);
                sendSync(sender, "当前稀有物品历史查询失败：" + lastCurrentRareHistoryError);
            } finally {
                currentRareHistoryRunning.set(false);
            }
        });
    }

    void openCurrentRareHistory(CommandSender sender, String playerName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行。");
            return;
        }
        String requestedName = playerName == null ? "" : playerName.trim();
        player.sendMessage("§7正在读取本地当前稀有物品历史报告...");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            try {
                UuidTraceReport report = getAvailableUuidTraceReport(requestedName, true);
                if (report == null || report.target == null) {
                    sendSync(player, "§c没有对应本地报告，请先运行 /scan currenthistory <玩家>。");
                    return;
                }
                Set<String> uuids = new HashSet<>();
                for (CurrentUuidItem item : report.target.items) {
                    if (item != null && item.uuid != null && !item.uuid.isBlank()) {
                        uuids.add(item.uuid.trim().toLowerCase(Locale.ROOT));
                    }
                }
                Set<String> blacklistedUuids = loadBlacklistedUuids(uuids);
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    if (player.isOnline()) {
                        new UuidTraceEquipmentMenu(report, blacklistedUuids).openMenu(player);
                    }
                });
            } catch (Exception exception) {
                ThePit.getInstance().getSLF4JLogger().error("Unable to open current rare history", exception);
                sendSync(player, "§c读取本地报告失败，请查看服务器日志。");
            }
        });
    }

    void currentRareHistoryStatus(CommandSender sender) {
        if (currentRareHistoryRunning.get()) {
            sender.sendMessage("正在查询 " + currentRareHistoryPlayer + "，已运行 "
                    + formatDuration(System.currentTimeMillis() - currentRareHistoryStartedAt));
        } else {
            sender.sendMessage("当前没有运行中的稀有物品历史查询。");
        }
        if (lastCurrentRareHistoryPath != null) {
            sender.sendMessage("最近一次报告：" + lastCurrentRareHistoryPath);
        }
        if (lastCurrentRareHistoryError != null) {
            sender.sendMessage("最近一次错误：" + lastCurrentRareHistoryError);
        }
    }

    private LiveInventorySnapshot captureCurrentRareInventory(PlayerProfile profile) {
        Player online = Bukkit.getPlayer(profile.getPlayerUUID());
        if (online != null && online.isOnline()) {
            PlayerProfile current = PlayerProfile.getPlayerProfileByUuid(online.getUniqueId());
            if (current == null || current == PlayerProfile.NONE_PROFILE || !current.isLoaded()) {
                throw new IllegalStateException("目标玩家存档尚未加载完成，请稍后重试");
            }
            return captureLiveInventory(online);
        }
        PlayerInv inventory = profile.getInventory();
        ItemStack[] contents = inventory == null ? null : inventory.getContents();
        ItemStack[] storage = contents == null ? new ItemStack[0]
                : java.util.Arrays.copyOf(contents, Math.min(contents.length, 36));
        List<ItemStack[]> warehouses = new ArrayList<>();
        addWarehouseSnapshots(warehouses, profile.getEnderChests());
        return new LiveInventorySnapshot(profile.getUuid(), profile.getPlayerName(),
                copyItemArray(storage),
                copyItemArray(inventory == null ? null : inventory.getArmorContents()),
                copyItemArray(contents != null && contents.length > 40
                        ? new ItemStack[]{contents[40]} : null), warehouses);
    }

    private CurrentUuidSource createCurrentRareSource(LiveInventorySnapshot snapshot) {
        List<CurrentUuidItem> items = new ArrayList<>();
        addCurrentRareItems(items, snapshot.inventory, "inventory");
        addCurrentRareItems(items, snapshot.armor, "armor");
        addCurrentRareItems(items, snapshot.extra, "offhand");
        for (int index = 0; index < snapshot.warehouses.size(); index++) {
            addCurrentRareItems(items, snapshot.warehouses.get(index), "warehouse[" + (index + 1) + "]");
        }
        return new CurrentUuidSource(snapshot.playerUuid, snapshot.playerName,
                "current_inventory_and_warehouses", items);
    }

    private void addCurrentRareItems(List<CurrentUuidItem> result, ItemStack[] items, String prefix) {
        for (int slot = 0; slot < items.length; slot++) {
            ItemStack item = items[slot];
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
                continue;
            }
            String internalName = normalize(ItemUtil.getInternalName(item));
            if (!AdvancedItemCatalog.isAdvanced(internalName) && !TRACKED_ITEMS.containsKey(internalName)) {
                continue;
            }
            String uuid = ItemUtil.getUUID(item);
            result.add(new CurrentUuidItem(uuid == null || uuid.isBlank() ? null : uuid,
                    prefix + "[" + slot + "]", internalName, item.getType().name().toLowerCase(Locale.ROOT),
                    getDisplayName(item), item.getAmount(), getLore(item), getFullNbt(item)));
        }
    }

    void uuidTraceStatus(CommandSender sender) {
        if (uuidTraceRunning.get()) {
            sender.sendMessage("materialhistory UUID 追踪任务运行中。");
            sender.sendMessage("已读取候选备份: " + uuidTraceBackups.get());
            sender.sendMessage("已发现 UUID 出现记录: " + uuidTraceMatches.get());
            sender.sendMessage("运行时长: " + formatDuration(System.currentTimeMillis() - uuidTraceStartedAt));
            return;
        }
        sender.sendMessage("当前没有运行中的 materialhistory UUID 追踪任务。");
        if (lastUuidTraceReportPath != null) {
            sender.sendMessage("最近一次报告: " + lastUuidTraceReportPath);
            sender.sendMessage("读取候选备份: " + uuidTraceBackups.get()
                    + "，UUID 出现记录: " + uuidTraceMatches.get());
        }
        if (lastUuidTraceError != null) {
            sender.sendMessage("最近一次错误: " + lastUuidTraceError);
        }
    }

    void openUuidTrace(CommandSender sender, String playerName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行。");
            return;
        }
        String requestedName = playerName == null ? "" : playerName.trim();
        player.sendMessage("§7正在读取 UUID 装备历史报告...");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            try {
                UuidTraceReport report = getAvailableUuidTraceReport(requestedName);
                if (report == null || report.target == null || report.target.items == null) {
                    sendSync(player, "§c没有找到对应的 UUID 追踪报告，请先运行 /scan materialhistory uuidtrace <玩家>。");
                    return;
                }
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    if (player.isOnline()) {
                        new UuidTraceEquipmentMenu(report).openMenu(player);
                    }
                });
            } catch (Exception exception) {
                ThePit.getInstance().getSLF4JLogger().error("Failed to open uuidtrace report", exception);
                sendSync(player, "§c读取 UUID 追踪报告失败，请查看服务器日志。");
            }
        });
    }

    void openUuidTransfers(CommandSender sender, String playerName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行。");
            return;
        }
        String requestedName = playerName == null ? "" : playerName.trim();
        if (requestedName.isEmpty()) {
            player.sendMessage("§c请提供玩家名。");
            return;
        }
        if (!uuidTransferQueryRunning.compareAndSet(false, true)) {
            player.sendMessage("§c已有 UUID 转移界面查询正在运行，请稍后再试。");
            return;
        }
        player.sendMessage("§7正在读取本地 UUID 索引并筛选首次属于该玩家、最新属于他人的装备...");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            try {
                if (uuidHistoryIndex == null) {
                    throw new IllegalStateException("UUID 历史索引功能未初始化");
                }
                PlayerProfile target = PlayerProfile.getOrLoadPlayerProfileByName(requestedName);
                ScanMaterialUuidHistoryIndex.TransferredUuidLookup lookup =
                        uuidHistoryIndex.findTransferredFrom(target.getUuid());
                if (lookup == null || lookup.manifest == null) {
                    sendSync(player, "§c没有可用的 UUID 历史索引，请先运行 /scan materialhistory uuidindex。");
                    return;
                }
                if (!sameReportPath(
                        ensureMaterialHistoryReportPath(),
                        lookup.manifest.sourceMaterialHistoryReport)) {
                    sendSync(player, "§cUUID 索引与最新 materialhistory 报告不匹配，请重新运行 uuidindex。");
                    return;
                }
                Set<String> blacklistedUuids = loadBlacklistedUuids(lookup.items);
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    if (player.isOnline()) {
                        new UuidTransferMenu(target.getUuid(), target.getPlayerName(),
                                lookup.items, blacklistedUuids)
                                .openMenu(player);
                    }
                });
            } catch (Exception exception) {
                ThePit.getInstance().getSLF4JLogger().error(
                        "Failed to query transferred UUID items for " + requestedName, exception);
                sendSync(player, "§c查询 UUID 转移装备失败，请查看服务器日志。");
            } finally {
                uuidTransferQueryRunning.set(false);
            }
        });
    }

    private Set<String> loadBlacklistedUuids(
            List<ScanMaterialUuidHistoryIndex.TransferredUuid> transferred) {
        if (transferred == null || transferred.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> candidates = new HashSet<>();
        for (ScanMaterialUuidHistoryIndex.TransferredUuid item : transferred) {
            if (item == null || item.itemUuid == null || item.itemUuid.isBlank()) {
                continue;
            }
            candidates.add(item.itemUuid.trim().toLowerCase(Locale.ROOT));
        }
        if (candidates.isEmpty()) {
            return Collections.emptySet();
        }

        return loadBlacklistedUuids(candidates);
    }

    private Set<String> loadBlacklistedUuids(Set<String> candidates) {
        if (candidates.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        for (Document document : ThePit.getInstance()
                .getMongoDB()
                .getItemUuidBlacklistCollection()
                .find(Filters.and(
                        Filters.in("itemUuid", candidates),
                        Filters.ne("active", false)))
                .projection(Projections.include("itemUuid"))) {
            String itemUuid = document.getString("itemUuid");
            if (itemUuid != null && !itemUuid.isBlank()) {
                result.add(itemUuid.trim().toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }

    void openTotallyLegitGemItems(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行。");
            return;
        }
        if (!legitGemQueryRunning.compareAndSet(false, true)) {
            player.sendMessage("§c已有遵纪守法宝石记录查询正在运行，请稍后再试。");
            return;
        }
        player.sendMessage("§7正在从本地 UUID 索引筛选含 totally_legit_gem 记录的装备...");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            try {
                if (uuidHistoryIndex == null) {
                    throw new IllegalStateException("UUID 历史索引功能未初始化");
                }
                ScanMaterialUuidHistoryIndex.RecordedUuidLookup lookup =
                        uuidHistoryIndex.findWithEnchantmentRecord("totally_legit_gem");
                if (lookup == null || lookup.manifest == null) {
                    sendSync(player, "§c没有可用的 UUID 历史索引，请先运行 /scan materialhistory uuidindex。");
                    return;
                }
                if (!sameReportPath(
                        ensureMaterialHistoryReportPath(),
                        lookup.manifest.sourceMaterialHistoryReport)) {
                    sendSync(player, "§cUUID 索引与最新 materialhistory 报告不匹配，请重新运行 uuidindex。");
                    return;
                }
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    player.sendMessage("§a筛选完成，共找到 " + lookup.items.size() + " 件装备。");
                    new LegitGemRecordMenu(lookup.items).openMenu(player);
                });
            } catch (Exception exception) {
                ThePit.getInstance().getSLF4JLogger().error(
                        "Failed to query totally_legit_gem item records", exception);
                sendSync(player, "§c查询遵纪守法宝石记录失败，请查看服务器日志。");
            } finally {
                legitGemQueryRunning.set(false);
            }
        });
    }

    void openTotallyLegitGemUsage(CommandSender sender, String playerName) {
        if (!legitGemUsageQueryRunning.compareAndSet(false, true)) {
            sender.sendMessage("§c已有遵纪守法宝石使用次数查询正在运行，请稍后再试。");
            return;
        }
        String requestedName = playerName == null ? "" : playerName.trim();
        sender.sendMessage("§7正在从本地 UUID 索引统计 totally_legit_gem 使用记录...");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            try {
                if (uuidHistoryIndex == null) {
                    throw new IllegalStateException("UUID 历史索引功能未初始化");
                }
                ScanMaterialUuidHistoryIndex.RecordedUsageLookup lookup =
                        uuidHistoryIndex.countEnchantmentRecordUsage("totally_legit_gem");
                if (lookup == null || lookup.manifest == null) {
                    sendSync(sender, "§c没有可用的 UUID 历史索引，请先运行 /scan materialhistory uuidindex。");
                    return;
                }
                if (!sameReportPath(
                        ensureMaterialHistoryReportPath(),
                        lookup.manifest.sourceMaterialHistoryReport)) {
                    sendSync(sender, "§cUUID 索引与最新 materialhistory 报告不匹配，请重新运行 uuidindex。");
                    return;
                }
                List<ScanMaterialUuidHistoryIndex.RecordedUsage> usages = lookup.usages;
                if (!requestedName.isEmpty()) {
                    ScanMaterialUuidHistoryIndex.RecordedUsage usage = usages.stream()
                            .filter(item -> requestedName.equalsIgnoreCase(item.playerName))
                            .findFirst()
                            .orElse(null);
                    if (usage == null) {
                        sendSync(sender, "§e" + requestedName + " §7在现有索引中累计使用次数: §f0");
                    } else {
                        sendSync(sender, formatLegitGemUsageMessage(usage));
                    }
                    return;
                }
                if (!(sender instanceof Player player)) {
                    sendSync(sender, "§a遵纪守法宝石使用统计完成，命中玩家: " + usages.size());
                    int limit = Math.min(50, usages.size());
                    for (int index = 0; index < limit; index++) {
                        ScanMaterialUuidHistoryIndex.RecordedUsage usage = usages.get(index);
                        int rank = index + 1;
                        sendSync(sender, "§e#" + rank + " §f" + usage.playerName
                                + " §7- 使用 §a" + usage.uses + " §7次，装备 §f"
                                + usage.itemUuids.size() + " §7件");
                    }
                    return;
                }
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    if (player.isOnline()) {
                        player.sendMessage("§a统计完成，共命中 " + usages.size() + " 名玩家。");
                        new LegitGemUsageMenu(usages).openMenu(player);
                    }
                });
            } catch (Exception exception) {
                ThePit.getInstance().getSLF4JLogger().error(
                        "Failed to count totally_legit_gem usage", exception);
                sendSync(sender, "§c统计遵纪守法宝石使用次数失败，请查看服务器日志。");
            } finally {
                legitGemUsageQueryRunning.set(false);
            }
        });
    }

    private String formatLegitGemUsageMessage(ScanMaterialUuidHistoryIndex.RecordedUsage usage) {
        return "§e" + usage.playerName
                + " §7累计使用遵纪守法宝石: §a" + usage.uses
                + " §7次，涉及装备: §f" + usage.itemUuids.size()
                + " §7件，首次: §f" + formatTime(usage.firstUseTimestamp)
                + "§7，最近: §f" + formatTime(usage.lastUseTimestamp);
    }

    private UuidTraceReport getAvailableUuidTraceReport(String requestedName) throws IOException {
        return getAvailableUuidTraceReport(requestedName, false);
    }

    private UuidTraceReport getAvailableUuidTraceReport(String requestedName, boolean currentRare) throws IOException {
        UuidTraceReport cached = currentRare ? latestCurrentRareHistoryReport : latestUuidTraceReport;
        if (cached != null && matchesUuidTraceTarget(cached, requestedName)) {
            return cached;
        }

        Path directory = ScanReportPath.directory("materialhistory");
        List<Path> reports;
        try (Stream<Path> files = Files.list(directory)) {
            reports = files
                    .filter(path -> path.getFileName().toString().startsWith(
                            currentRare ? "current-history-" : "uuid-trace-"))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparingLong(this::lastModified).reversed())
                    .toList();
        }
        for (Path path : reports) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                UuidTraceReport report = GSON.fromJson(reader, UuidTraceReport.class);
                if (report != null && matchesUuidTraceTarget(report, requestedName)) {
                    if (currentRare) {
                        latestCurrentRareHistoryReport = report;
                        lastCurrentRareHistoryPath = path.toAbsolutePath().toString();
                    } else {
                        latestUuidTraceReport = report;
                        lastUuidTraceReportPath = path.toAbsolutePath().toString();
                    }
                    return report;
                }
            }
        }
        return null;
    }

    private boolean matchesUuidTraceTarget(UuidTraceReport report, String requestedName) {
        if (requestedName == null || requestedName.isBlank()) {
            return true;
        }
        return report.target != null
                && (requestedName.equalsIgnoreCase(report.target.playerName)
                || requestedName.equalsIgnoreCase(report.target.playerUuid));
    }

    private LiveInventorySnapshot captureLiveInventory(Player player) {
        List<ItemStack[]> warehouses = new ArrayList<>();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile != null && profile != PlayerProfile.NONE_PROFILE) {
            addWarehouseSnapshots(warehouses, profile.getEnderChests());
        }
        return new LiveInventorySnapshot(
                player.getUniqueId().toString(),
                player.getName(),
                copyItemArray(player.getInventory().getStorageContents()),
                copyItemArray(player.getInventory().getArmorContents()),
                copyItemArray(player.getInventory().getExtraContents()),
                warehouses
        );
    }

    private void addWarehouseSnapshots(List<ItemStack[]> result, PlayerEnderChest[] warehouses) {
        if (warehouses == null) {
            return;
        }
        for (PlayerEnderChest warehouse : warehouses) {
            if (warehouse == null || warehouse.getInventory() == null) {
                result.add(new ItemStack[0]);
            } else {
                result.add(copyItemArray(warehouse.getInventory().getContents()));
            }
        }
    }

    private ItemStack[] copyItemArray(ItemStack[] items) {
        if (items == null) {
            return new ItemStack[0];
        }
        ItemStack[] copies = new ItemStack[items.length];
        for (int slot = 0; slot < items.length; slot++) {
            copies[slot] = items[slot] == null ? null : items[slot].clone();
        }
        return copies;
    }

    private CurrentUuidSource createLiveUuidSource(LiveInventorySnapshot snapshot) {
        List<CurrentUuidItem> items = new ArrayList<>();
        addCurrentUuidItems(items, snapshot.inventory, "inventory");
        addCurrentUuidItems(items, snapshot.armor, "armor");
        addCurrentUuidItems(items, snapshot.extra, "extra");
        for (int index = 0; index < snapshot.warehouses.size(); index++) {
            addCurrentUuidItems(items, snapshot.warehouses.get(index), "warehouse[" + (index + 1) + "]");
        }
        return new CurrentUuidSource(
                snapshot.playerUuid,
                snapshot.playerName,
                "online_live_inventory_and_warehouses",
                items
        );
    }

    private CurrentUuidSource loadSavedUuidSource(String requestedName) {
        PlayerProfile profile = PlayerProfile.getOrLoadPlayerProfileByName(requestedName);
        List<CurrentUuidItem> items = new ArrayList<>();
        PlayerInv inventory = profile.getInventory();
        if (inventory != null) {
            ItemStack[] contents = inventory.getContents();
            addCurrentUuidItems(items, contents, "inventory", 36);
            if (contents != null && contents.length > 40) {
                addCurrentUuidItems(items, new ItemStack[]{contents[40]}, "offhand");
            }
            addCurrentUuidItems(items, inventory.getArmorContents(), "armor");
        }
        PlayerEnderChest[] warehouses = profile.getEnderChests();
        if (warehouses != null) {
            for (int index = 0; index < warehouses.length; index++) {
                PlayerEnderChest warehouse = warehouses[index];
                if (warehouse != null && warehouse.getInventory() != null) {
                    addCurrentUuidItems(
                            items,
                            warehouse.getInventory().getContents(),
                            "warehouse[" + (index + 1) + "]"
                    );
                }
            }
        }
        return new CurrentUuidSource(
                profile.getUuid(),
                profile.getPlayerName(),
                "database_saved_inventory_and_warehouses",
                items
        );
    }

    private void addCurrentUuidItems(List<CurrentUuidItem> result,
                                     ItemStack[] items,
                                     String locationPrefix) {
        addCurrentUuidItems(result, items, locationPrefix, items == null ? 0 : items.length);
    }

    private void addCurrentUuidItems(List<CurrentUuidItem> result,
                                     ItemStack[] items,
                                     String locationPrefix,
                                     int maximumSlots) {
        if (items == null) {
            return;
        }
        int length = Math.min(items.length, maximumSlots);
        for (int slot = 0; slot < length; slot++) {
            ItemStack item = items[slot];
            if (item == null || item.getType().isAir()) {
                continue;
            }
            try {
                String uuid = ItemUtil.getUUID(item);
                if (uuid == null || uuid.isBlank()) {
                    continue;
                }
                result.add(new CurrentUuidItem(
                        uuid,
                        locationPrefix + "[" + slot + "]",
                        normalize(ItemUtil.getInternalName(item)),
                        item.getType().name().toLowerCase(Locale.ROOT),
                        getDisplayName(item),
                        item.getAmount(),
                        getLore(item),
                        getFullNbt(item)
                ));
            } catch (Exception exception) {
                ThePit.getInstance().getSLF4JLogger().debug(
                        "Failed to read current UUID item at {}[{}]", locationPrefix, slot, exception);
            }
        }
    }

    private void executeUuidTrace(CommandSender sender,
                                  String requestedName,
                                  LiveInventorySnapshot liveSnapshot) {
        try {
            CurrentUuidSource source = liveSnapshot == null
                    ? loadSavedUuidSource(requestedName)
                    : createLiveUuidSource(liveSnapshot);
            if (source.playerUuid == null || source.playerUuid.isBlank()) {
                throw new IllegalStateException("目标玩家 UUID 为空");
            }

            Set<String> targetUuids = new HashSet<>();
            for (CurrentUuidItem item : source.items) {
                targetUuids.add(item.uuid);
            }

            UuidTraceSearchResult searchResult = searchUuidHistory(targetUuids);
            List<UuidTraceOccurrence> occurrences = searchResult.occurrences;
            int rawOccurrences = searchResult.rawOccurrences;
            int distinctHistoryEntries = occurrences.size();

            UuidTraceReport report = new UuidTraceReport(
                    System.currentTimeMillis(),
                    formatTime(System.currentTimeMillis()),
                    searchResult.historySource,
                    source,
                    targetUuids.size(),
                    searchResult.candidateBackups,
                    searchResult.loadedBackups,
                    searchResult.matchedBackups,
                    rawOccurrences,
                    distinctHistoryEntries,
                    occurrences
            );
            Path reportPath = ScanReportPath.directory("materialhistory").resolve(
                    "uuid-trace-" + ScanReportPath.sanitize(source.playerName)
                            + "-" + LocalDateTime.now().format(FILE_NAME_TIME) + ".json");
            try (Writer writer = Files.newBufferedWriter(
                    reportPath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW)) {
                GSON.toJson(report, writer);
            }
            latestUuidTraceReport = report;
            lastUuidTraceReportPath = reportPath.toAbsolutePath().toString();

            ThePit.getInstance().info("scan materialhistory uuidtrace finished: player=" + source.playerName
                    + ", currentUuidItems=" + source.items.size()
                    + ", uniqueUuids=" + targetUuids.size()
                    + ", historySource=" + searchResult.historySource
                    + ", candidateBackups=" + searchResult.candidateBackups
                    + ", loadedBackups=" + searchResult.loadedBackups
                    + ", matchedBackups=" + searchResult.matchedBackups
                    + ", rawOccurrences=" + rawOccurrences
                    + ", distinctHistoryEntries=" + distinctHistoryEntries
                    + ", report=" + lastUuidTraceReportPath);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(
                    "UUID 追踪完成。当前 UUID 物品: " + source.items.size()
                            + "，唯一 UUID: " + targetUuids.size()
                            + "，数据源: " + searchResult.historySource
                            + "，读取历史备份: " + uuidTraceBackups.get()
                            + "，命中备份: " + searchResult.matchedBackups
                            + "，原始记录: " + rawOccurrences
                            + "，去重后历史状态: " + distinctHistoryEntries
                            + "，日志: " + lastUuidTraceReportPath));
        } catch (Exception exception) {
            lastUuidTraceError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error(
                    "scan materialhistory uuidtrace failed for " + requestedName, exception);
            Bukkit.getScheduler().runTask(ThePit.getInstance(),
                    () -> sender.sendMessage("UUID 追踪失败: " + lastUuidTraceError));
        } finally {
            uuidTraceRunning.set(false);
        }
    }

    private UuidTraceSearchResult searchUuidHistory(Set<String> targetUuids) throws IOException {
        if (targetUuids.isEmpty()) {
            return new UuidTraceSearchResult("no_uuid_items", 0, 0, 0, 0, Collections.emptyList());
        }
        if (uuidHistoryIndex != null) {
            ScanMaterialUuidHistoryIndex.UuidHistoryLookup lookup = uuidHistoryIndex.lookup(targetUuids);
            String activeMaterialReport = ensureMaterialHistoryReportPath();
            if (lookup != null
                    && lookup.manifest != null
                    && sameReportPath(activeMaterialReport, lookup.manifest.sourceMaterialHistoryReport)) {
                List<UuidTraceOccurrence> occurrences = convertIndexedEvents(lookup.events);
                Set<String> matchedBackups = new HashSet<>();
                for (ScanMaterialUuidHistoryIndex.UuidHistoryEvent event : lookup.events) {
                    matchedBackups.add(event.backupDatabaseId != null
                            ? "id:" + event.backupDatabaseId
                            : "uuid:" + event.backupUuid);
                }
                uuidTraceMatches.set(occurrences.size());
                return new UuidTraceSearchResult(
                        "local_uuid_history_index",
                        lookup.manifest.candidateBackups,
                        0,
                        matchedBackups.size(),
                        occurrences.size(),
                        occurrences
                );
            }
        }
        return searchUuidHistoryFromDatabase(targetUuids);
    }

    private List<UuidTraceOccurrence> convertIndexedEvents(
            List<ScanMaterialUuidHistoryIndex.UuidHistoryEvent> events) {
        List<UuidTraceOccurrence> occurrences = new ArrayList<>();
        if (events == null) {
            return occurrences;
        }
        List<ScanMaterialUuidHistoryIndex.UuidHistoryEvent> sortedEvents = events.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparingLong((ScanMaterialUuidHistoryIndex.UuidHistoryEvent event) -> event.timestamp)
                        .thenComparing(event -> event.itemUuid, Comparator.nullsFirst(String::compareTo)))
                .toList();
        Map<String, UuidTraceOccurrence> lastOccurrenceByUuid = new HashMap<>();
        Map<String, String> lastPlayerByUuid = new HashMap<>();
        Map<String, String> lastStateByUuid = new HashMap<>();
        for (ScanMaterialUuidHistoryIndex.UuidHistoryEvent event : sortedEvents) {
            if (event == null || event.items == null) {
                continue;
            }
            ScanMaterialUuidHistoryIndex.IndexedItem item = representativeIndexedItem(event);
            if (item == null || event.itemUuid == null) {
                continue;
            }
            UuidTraceOccurrence occurrence = new UuidTraceOccurrence(
                    event.itemUuid,
                    event.backupDatabaseId,
                    event.backupUuid,
                    event.playerUuid,
                    event.playerName,
                    event.prestige,
                    event.timestamp,
                    event.time,
                    item.location,
                    normalize(item.internalName),
                    item.material,
                    item.displayName,
                    item.amount,
                    item.lore == null ? Collections.emptyList() : item.lore,
                    item.nbt
            );
            String comparisonState = ScanMaterialUuidHistoryIndex.comparisonStateHash(item);
            UuidTraceOccurrence previous = lastOccurrenceByUuid.get(event.itemUuid);
            if (previous != null
                    && Objects.equals(lastPlayerByUuid.get(event.itemUuid), event.playerUuid)
                    && Objects.equals(lastStateByUuid.get(event.itemUuid), comparisonState)) {
                previous.extendTo(occurrence);
                continue;
            }
            occurrences.add(occurrence);
            lastOccurrenceByUuid.put(event.itemUuid, occurrence);
            lastPlayerByUuid.put(event.itemUuid, event.playerUuid);
            lastStateByUuid.put(event.itemUuid, comparisonState);
        }
        occurrences.sort(uuidTraceOccurrenceComparator());
        return occurrences;
    }

    private boolean sameReportPath(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        return Path.of(first).toAbsolutePath().normalize().toString()
                .equalsIgnoreCase(Path.of(second).toAbsolutePath().normalize().toString());
    }

    private UuidTraceSearchResult searchUuidHistoryFromDatabase(Set<String> targetUuids) throws IOException {
        Report materialReport = getAvailableReport();
        if (materialReport == null || materialReport.snapshots == null) {
            throw new IllegalStateException("没有可用的 materialhistory 报告，请先运行 /scan materialhistory");
        }

        Map<String, MaterialSnapshot> uniqueCandidates = new LinkedHashMap<>();
        for (MaterialSnapshot snapshot : materialReport.snapshots) {
            if (snapshot == null) {
                continue;
            }
            String key = snapshot.databaseId != null && !snapshot.databaseId.isBlank()
                    ? "id:" + snapshot.databaseId
                    : snapshot.backupUuid != null && !snapshot.backupUuid.isBlank()
                    ? "uuid:" + snapshot.backupUuid
                    : null;
            if (key != null) {
                uniqueCandidates.putIfAbsent(key, snapshot);
            }
        }
        List<MaterialSnapshot> candidates = new ArrayList<>(uniqueCandidates.values());
        List<UuidTraceOccurrence> occurrences = new ArrayList<>();
        Set<String> matchedBackupKeys = new HashSet<>();
        int loadedBackups = 0;

        candidates.sort(Comparator
                .comparingLong((MaterialSnapshot snapshot) -> snapshot.timestamp)
                .thenComparing(snapshot -> snapshot.playerUuid, Comparator.nullsFirst(String::compareTo))
                .thenComparing(snapshot -> snapshot.databaseId, Comparator.nullsFirst(String::compareTo)));
        for (int batchStart = 0; batchStart < candidates.size(); batchStart += UUID_TRACE_BATCH_SIZE) {
            List<MaterialSnapshot> batch = candidates.subList(
                    batchStart,
                    Math.min(batchStart + UUID_TRACE_BATCH_SIZE, candidates.size())
            );
            Map<String, PlayerInvBackup> backupsById = loadUuidTraceBackups(batch);
            for (MaterialSnapshot snapshot : batch) {
                PlayerInvBackup backup = findLoadedBackup(backupsById, snapshot);
                if (backup == null) {
                    continue;
                }
                loadedBackups++;
                int scanned = uuidTraceBackups.incrementAndGet();
                int before = occurrences.size();
                addUuidTraceOccurrences(occurrences, targetUuids, snapshot, backup);
                if (occurrences.size() > before) {
                    matchedBackupKeys.add(snapshot.databaseId != null
                            ? "id:" + snapshot.databaseId
                            : "uuid:" + snapshot.backupUuid);
                    uuidTraceMatches.addAndGet(occurrences.size() - before);
                }
                if (scanned % 1_000 == 0) {
                    ThePit.getInstance().info("scan materialhistory uuidtrace progress: scannedBackups=" + scanned
                            + ", occurrences=" + uuidTraceMatches.get());
                }
            }
        }
        occurrences.sort(uuidTraceOccurrenceComparator());
        int rawOccurrences = occurrences.size();
        occurrences = compactUuidTraceOccurrences(occurrences);
        return new UuidTraceSearchResult(
                "database_materialhistory_backups",
                candidates.size(),
                loadedBackups,
                matchedBackupKeys.size(),
                rawOccurrences,
                occurrences
        );
    }

    private Comparator<UuidTraceOccurrence> uuidTraceOccurrenceComparator() {
        return Comparator
                .comparingLong((UuidTraceOccurrence occurrence) -> occurrence.timestamp)
                .thenComparing(occurrence -> occurrence.playerName,
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(occurrence -> occurrence.itemUuid)
                .thenComparing(occurrence -> occurrence.location);
    }

    private Map<String, PlayerInvBackup> loadUuidTraceBackups(List<MaterialSnapshot> candidates) {
        Map<String, PlayerInvBackup> loaded = new HashMap<>();
        List<ObjectId> objectIds = candidates.stream()
                .map(snapshot -> snapshot.databaseId)
                .filter(id -> id != null && ObjectId.isValid(id))
                .distinct()
                .map(ObjectId::new)
                .toList();
        for (int start = 0; start < objectIds.size(); start += UUID_TRACE_BATCH_SIZE) {
            List<ObjectId> batch = objectIds.subList(
                    start,
                    Math.min(start + UUID_TRACE_BATCH_SIZE, objectIds.size())
            );
            try (MongoCursor<PlayerInvBackup> cursor = ThePit.getInstance()
                    .getMongoDB()
                    .getInvCollection()
                    .find(Filters.in("_id", batch))
                    .batchSize(100)
                    .iterator()) {
                while (cursor.hasNext()) {
                    registerLoadedBackup(loaded, cursor.next());
                }
            }
        }

        List<String> fallbackUuids = candidates.stream()
                .filter(snapshot -> findLoadedBackup(loaded, snapshot) == null)
                .map(snapshot -> snapshot.backupUuid)
                .filter(uuid -> uuid != null && !uuid.isBlank())
                .distinct()
                .toList();
        for (int start = 0; start < fallbackUuids.size(); start += UUID_TRACE_BATCH_SIZE) {
            List<String> batch = fallbackUuids.subList(
                    start,
                    Math.min(start + UUID_TRACE_BATCH_SIZE, fallbackUuids.size())
            );
            try (MongoCursor<PlayerInvBackup> cursor = ThePit.getInstance()
                    .getMongoDB()
                    .getInvCollection()
                    .find(Filters.in("backupUuid", batch))
                    .batchSize(100)
                    .iterator()) {
                while (cursor.hasNext()) {
                    registerLoadedBackup(loaded, cursor.next());
                }
            }
        }
        return loaded;
    }

    private void registerLoadedBackup(Map<String, PlayerInvBackup> loaded, PlayerInvBackup backup) {
        if (backup == null) {
            return;
        }
        if (backup.getId() != null && !backup.getId().isBlank()) {
            loaded.put("id:" + backup.getId(), backup);
        }
        if (backup.getBackupUuid() != null && !backup.getBackupUuid().isBlank()) {
            loaded.put("uuid:" + backup.getBackupUuid(), backup);
        }
    }

    private PlayerInvBackup findLoadedBackup(Map<String, PlayerInvBackup> loaded,
                                             MaterialSnapshot snapshot) {
        PlayerInvBackup backup = snapshot.databaseId == null
                ? null
                : loaded.get("id:" + snapshot.databaseId);
        if (backup == null && snapshot.backupUuid != null) {
            backup = loaded.get("uuid:" + snapshot.backupUuid);
        }
        return backup;
    }

    private void addUuidTraceOccurrences(List<UuidTraceOccurrence> result,
                                         Set<String> targetUuids,
                                         MaterialSnapshot snapshot,
                                         PlayerInvBackup backup) {
        PlayerInv inventory = backup.getInv();
        if (inventory != null) {
            ItemStack[] contents = inventory.getContents();
            addUuidTraceOccurrences(result, targetUuids, snapshot, contents, "inventory", 36);
            if (contents != null && contents.length > 40) {
                addUuidTraceOccurrences(
                        result, targetUuids, snapshot, new ItemStack[]{contents[40]}, "offhand");
            }
            addUuidTraceOccurrences(result, targetUuids, snapshot, inventory.getArmorContents(), "armor");
        }
        PlayerEnderChest[] warehouses = backup.getChest();
        if (warehouses == null) {
            return;
        }
        for (int index = 0; index < warehouses.length; index++) {
            PlayerEnderChest warehouse = warehouses[index];
            if (warehouse != null && warehouse.getInventory() != null) {
                addUuidTraceOccurrences(
                        result,
                        targetUuids,
                        snapshot,
                        warehouse.getInventory().getContents(),
                        "warehouse[" + (index + 1) + "]"
                );
            }
        }
    }

    private void addUuidTraceOccurrences(List<UuidTraceOccurrence> result,
                                         Set<String> targetUuids,
                                         MaterialSnapshot snapshot,
                                         ItemStack[] items,
                                         String locationPrefix) {
        addUuidTraceOccurrences(
                result,
                targetUuids,
                snapshot,
                items,
                locationPrefix,
                items == null ? 0 : items.length
        );
    }

    private void addUuidTraceOccurrences(List<UuidTraceOccurrence> result,
                                         Set<String> targetUuids,
                                         MaterialSnapshot snapshot,
                                         ItemStack[] items,
                                         String locationPrefix,
                                         int maximumSlots) {
        if (items == null || targetUuids.isEmpty()) {
            return;
        }
        int length = Math.min(items.length, maximumSlots);
        for (int slot = 0; slot < length; slot++) {
            ItemStack item = items[slot];
            if (item == null || item.getType().isAir()) {
                continue;
            }
            try {
                String uuid = ItemUtil.getUUID(item);
                if (uuid == null || !targetUuids.contains(uuid)) {
                    continue;
                }
                result.add(new UuidTraceOccurrence(
                        uuid,
                        snapshot.databaseId,
                        snapshot.backupUuid,
                        snapshot.playerUuid,
                        snapshot.playerName,
                        snapshot.prestige,
                        snapshot.timestamp,
                        snapshot.time,
                        locationPrefix + "[" + slot + "]",
                        normalize(ItemUtil.getInternalName(item)),
                        item.getType().name().toLowerCase(Locale.ROOT),
                        getDisplayName(item),
                        item.getAmount(),
                        getLore(item),
                        getFullNbt(item)
                ));
            } catch (Exception exception) {
                ThePit.getInstance().getSLF4JLogger().debug(
                        "Failed to read UUID trace item from backup {} at {}[{}]",
                        snapshot.databaseId, locationPrefix, slot, exception);
            }
        }
    }

    private static String getDisplayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() ? meta.getDisplayName() : null;
    }

    private static List<String> getLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore() || meta.getLore() == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(meta.getLore());
    }

    private static String getFullNbt(ItemStack item) {
        try {
            return NBT.get(item, nbt -> {
                return nbt.toString();
            });
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<UuidTraceOccurrence> compactUuidTraceOccurrences(List<UuidTraceOccurrence> occurrences) {
        List<UuidTraceOccurrence> compacted = new ArrayList<>();
        Map<UuidTraceStateKey, UuidTraceOccurrence> groupedStates = new HashMap<>();
        for (UuidTraceOccurrence occurrence : occurrences) {
            UuidTraceStateKey stateKey = occurrence.stateKey();
            UuidTraceOccurrence previous = groupedStates.get(stateKey);
            if (previous != null
                    && !previous.isSameBackup(occurrence)) {
                previous.extendTo(occurrence);
                continue;
            }
            compacted.add(occurrence);
            groupedStates.put(stateKey, occurrence);
        }
        return compacted;
    }

    private void openPlayerListAsync(Player player) {
        try {
            Report report = getAvailableReport();
            if (report == null || report.snapshots == null || report.snapshots.isEmpty()) {
                sendSync(player, "§c没有可用的历史稀有物资扫描结果，请先运行 /scan materialhistory。");
                return;
            }

            Map<String, List<MaterialSnapshot>> snapshotsByPlayer = new LinkedHashMap<>();
            for (MaterialSnapshot snapshot : report.snapshots) {
                if (snapshot.playerUuid != null && !snapshot.playerUuid.isBlank()) {
                    snapshotsByPlayer.computeIfAbsent(snapshot.playerUuid, ignored -> new ArrayList<>()).add(snapshot);
                }
            }

            Map<String, PlayerProfile> profiles = loadPlayerListProfiles(new ArrayList<>(snapshotsByPlayer.keySet()));
            List<MatchedPlayerInfo> players = new ArrayList<>();
            snapshotsByPlayer.forEach((uuid, snapshots) -> {
                PlayerProfile profile = profiles.get(uuid);
                String fallbackName = snapshots.stream()
                        .map(snapshot -> snapshot.playerName)
                        .filter(name -> name != null && !name.isBlank())
                        .findFirst()
                        .orElse("<unknown>");
                players.add(createMatchedPlayerInfo(uuid, fallbackName, profile, snapshots));
            });
            players.sort(Comparator
                    .comparingInt((MatchedPlayerInfo info) -> info.maximumRareItems)
                    .reversed()
                    .thenComparing(info -> info.playerName,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(info -> info.playerUuid));

            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                if (player.isOnline()) {
                    new MaterialHistoryPlayerMenu(players).openMenu(player);
                }
            });
        } catch (Exception exception) {
            ThePit.getInstance().getSLF4JLogger().error("Failed to open materialhistory player list", exception);
            sendSync(player, "§c读取扫描玩家列表失败，请查看服务器日志。");
        }
    }

    private Map<String, PlayerProfile> loadPlayerListProfiles(List<String> playerUuids) {
        Map<String, PlayerProfile> profiles = new HashMap<>();
        for (int start = 0; start < playerUuids.size(); start += 500) {
            List<String> batch = playerUuids.subList(start, Math.min(start + 500, playerUuids.size()));
            try (MongoCursor<PlayerProfile> cursor = ThePit.getInstance()
                    .getMongoDB()
                    .getProfileCollection()
                    .find(Filters.in("uuid", batch))
                    .projection(Projections.include(
                            "uuid",
                            "playerName",
                            "registerTime",
                            "experience",
                            "prestige",
                            "enchantment",
                            "kills"
                    ))
                    .batchSize(100)
                    .iterator()) {
                while (cursor.hasNext()) {
                    PlayerProfile profile = cursor.next();
                    if (profile != null && profile.getUuid() != null) {
                        profiles.put(profile.getUuid(), profile);
                    }
                }
            }
        }
        return profiles;
    }

    private MatchedPlayerInfo createMatchedPlayerInfo(String playerUuid,
                                                       String fallbackName,
                                                       PlayerProfile profile,
                                                       List<MaterialSnapshot> snapshots) {
        String playerName = profile == null || profile.getPlayerName() == null
                ? fallbackName
                : profile.getPlayerName();
        int level = profile == null ? -1 : profile.getLevel();
        int prestige = profile == null ? -1 : profile.getPrestige();
        long registerTime = profile == null ? -1L : profile.getRegisterTime();
        long totalEnchantments = profile == null ? 0L : countTotalEnchantments(profile.getEnchantment());
        long kills = profile == null ? 0L : profile.getKills();
        int maximumRareItems = snapshots.stream().mapToInt(snapshot -> snapshot.totalItems).max().orElse(0);
        int uuidStackBackups = (int) snapshots.stream().filter(snapshot -> snapshot.uuidStackMatch).count();
        int uuidStackGroups = snapshots.stream().mapToInt(snapshot -> snapshot.stackedUuidStackCount).sum();
        return new MatchedPlayerInfo(
                playerUuid,
                playerName,
                registerTime,
                level,
                prestige,
                totalEnchantments,
                kills,
                snapshots.size(),
                maximumRareItems,
                uuidStackBackups,
                uuidStackGroups,
                snapshots
        );
    }

    private long countTotalEnchantments(Map<String, Integer> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            Integer count = entry.getValue();
            if (entry.getKey() != null && !entry.getKey().isBlank() && count != null && count > 0) {
                total += count;
            }
        }
        return total;
    }

    private void executeScan(CommandSender sender, int minimumItems) {
        try {
            Map<String, ProfileInfo> profileCache = new HashMap<>();
            List<MaterialSnapshot> snapshots = scanBackups(minimumItems, profileCache);
            snapshots.sort(Comparator
                    .comparingLong((MaterialSnapshot snapshot) -> snapshot.timestamp)
                    .thenComparing(snapshot -> snapshot.playerUuid, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(snapshot -> snapshot.databaseId, Comparator.nullsFirst(String::compareTo)));

            lastMatchedPlayers = (int) snapshots.stream()
                    .map(snapshot -> snapshot.playerUuid)
                    .distinct()
                    .count();
            lastMaximumItems = snapshots.stream()
                    .mapToInt(snapshot -> snapshot.totalItems)
                    .max()
                    .orElse(0);

            Report report = new Report(
                    System.currentTimeMillis(),
                    formatTime(System.currentTimeMillis()),
                    minimumItems,
                    TRACKED_ITEMS,
                    scannedBackups.get(),
                    scanErrors.get(),
                    lastMatchedPlayers,
                    lastMaximumItems,
                    matchedUuidStackBackups.get(),
                    stackedUuidStacks.get(),
                    snapshots
            );
            Path reportPath = writeReport(report);
            latestReport = report;
            lastReportPath = reportPath.toAbsolutePath().toString();

            ThePit.getInstance().info("scan materialhistory finished: scannedBackups=" + scannedBackups.get()
                    + ", matchedPlayers=" + lastMatchedPlayers
                    + ", matchedBackups=" + matchedBackups.get()
                    + ", matchedUuidStackBackups=" + matchedUuidStackBackups.get()
                    + ", stackedUuidStacks=" + stackedUuidStacks.get()
                    + ", maximumItems=" + lastMaximumItems
                    + ", scanErrors=" + scanErrors.get()
                    + ", report=" + lastReportPath);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(
                    "扫描完成。读取备份: " + scannedBackups.get()
                            + "，命中玩家: " + lastMatchedPlayers
                            + "，命中备份: " + matchedBackups.get()
                            + "，UUID 异常堆叠备份: " + matchedUuidStackBackups.get()
                            + "，异常物品栈: " + stackedUuidStacks.get()
                            + "，单个快照最高物资数: " + lastMaximumItems
                            + "，异常数据数: " + scanErrors.get()
                            + "，日志: " + lastReportPath
                            + "。使用 /scan materialhistory view <玩家> 查看。"));
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error("scan materialhistory failed", exception);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage("扫描失败: " + lastError));
        } finally {
            running.set(false);
        }
    }

    /** Same item scope as ordinary mode, but keep the all-mode result streaming and independent. */
    private void executeAllScan(CommandSender sender, int minimumItems) {
        try {
            Path directory = ScanReportPath.directory("materialhistory");
            String name = "all-material-history-" + LocalDateTime.now().format(FILE_NAME_TIME) + ".json";
            Path temporary = directory.resolve(name + ".tmp"), complete = directory.resolve(name);
            Map<String, ProfileInfo> profiles = new HashMap<>();
            try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(temporary, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW))) {
                writer.setIndent("  ");
                writer.beginObject();
                writer.name("reportVersion").value(2);
                writer.name("allAdvancedItems").value(true);
                writer.name("includesItemsWithoutUuid").value(true);
                writer.name("generatedAtTimestamp").value(System.currentTimeMillis());
                writer.name("generatedAt").value(formatTime(System.currentTimeMillis()));
                writer.name("minimumItems").value(minimumItems);
                writer.name("trackedItems");
                GSON.toJson(TRACKED_ITEMS, Map.class, writer);
                writer.name("snapshots").beginArray();
                scanBackups(minimumItems, profiles, true, snapshot -> {
                    GSON.toJson(snapshot, MaterialSnapshot.class, writer);
                    lastMaximumItems = Math.max(lastMaximumItems, snapshot.totalItems);
                });
                writer.endArray();
                lastMatchedPlayers = profiles.size();
                writer.name("scannedBackups").value(scannedBackups.get());
                writer.name("matchedBackups").value(matchedBackups.get());
                writer.name("matchedPlayers").value(lastMatchedPlayers);
                writer.name("maximumItems").value(lastMaximumItems);
                writer.name("scanErrors").value(scanErrors.get());
                writer.name("matchedUuidStackBackups").value(matchedUuidStackBackups.get());
                writer.name("stackedUuidStacks").value(stackedUuidStacks.get());
                writer.endObject();
            }
            try { Files.move(temporary, complete, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, complete); }
            lastAllReportPath = complete.toAbsolutePath().toString();
            ThePit.getInstance().info("scan materialhistory all finished: scannedBackups=" + scannedBackups.get()
                    + ", matchedPlayers=" + lastMatchedPlayers + ", matchedBackups=" + matchedBackups.get()
                    + ", scanErrors=" + scanErrors.get() + ", report=" + lastAllReportPath);
            sendSync(sender, "全部高级道具扫描完成（含无 UUID）。读取 " + scannedBackups.get() + " 份备份，命中 "
                    + lastMatchedPlayers + " 位玩家 / " + matchedBackups.get() + " 份备份。报告: " + lastAllReportPath
                    + "。使用 /scan materialhistory item [物品ID] [玩家] 只读本地查看数量。原有报告未覆盖。");
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error("scan materialhistory all failed", exception);
            sendSync(sender, "全部高级道具扫描失败: " + lastError + "；未完成的 .tmp 文件不会作为报告读取。");
        } finally { running.set(false); }
    }

    private List<MaterialSnapshot> scanBackups(int minimumItems, Map<String, ProfileInfo> profileCache) throws InterruptedException {
        List<MaterialSnapshot> snapshots = new ArrayList<>();
        scanBackups(minimumItems, profileCache, false, snapshots::add);
        return snapshots;
    }

    private void scanBackups(int minimumItems, Map<String, ProfileInfo> profileCache,
                             boolean streamingAll, Consumer<MaterialSnapshot> output) throws InterruptedException {
        MaterialHistoryFastScan fast = new MaterialHistoryFastScan(TRACKED_ITEMS.keySet(), ScanRareMaterialHistory::inspectScanItem);
        List<PendingMaterialSnapshot> pending = new ArrayList<>();
        // A raw document avoids PlayerInvDeserializer.refreshInv and EnderChestDeserializer's Bukkit inventory allocation.
        try (MongoCursor<Document> cursor = ThePit.getInstance().getMongoDB().getDatabase().getCollection("inv")
                .find().projection(Projections.include("_id", "uuid", "backupUuid", "timeStamp", "inv", "chest"))
                .batchSize(activeBatchSize).noCursorTimeout(true)
                .iterator()) {
            while (cursor.hasNext()) {
                scanSpeed.awaitPermit();
                Document backup = cursor.next();
                int scanned = scannedBackups.incrementAndGet();
                if (scanned % 10_000 == 0) {
                    scanCacheStats = fast.cacheStats();
                    ThePit.getInstance().info("scan materialhistory" + (streamingAll ? " all" : "") + " progress: scannedBackups=" + scanned
                            + ", matchedBackups=" + matchedBackups.get()
                            + ", matchedUuidStackBackups=" + matchedUuidStackBackups.get()
                            + ", stackedUuidStacks=" + stackedUuidStacks.get()
                            + ", scanErrors=" + scanErrors.get() + ", averagePerSecond=" + (long) averageScanRate()
                            + ", decodeCacheHits=" + scanCacheStats.hits() + ", actualDecodes=" + scanCacheStats.decodes());
                }
                try {
                    String uuid = backup.getString("uuid");
                    Object rawTime = backup.get("timeStamp");
                    long timestamp = rawTime instanceof Number number ? number.longValue() : -1L;
                    if (uuid == null || uuid.isBlank() || timestamp <= 0L) {
                        scanErrors.incrementAndGet();
                        continue;
                    }
                    MaterialHistoryFastScan.Contents contents = fast.scan(backup);
                    boolean materialMatch = contents.combined().total() >= minimumItems;
                    if (!materialMatch && contents.stacked().isEmpty()) continue;
                    Object id = backup.get("_id");
                    pending.add(new PendingMaterialSnapshot(id == null ? null : id.toString(),
                            backup.getString("backupUuid"), uuid, timestamp, contents, materialMatch));
                } catch (Exception exception) {
                    scanErrors.incrementAndGet();
                    ThePit.getInstance().getSLF4JLogger().warn(
                            "scan materialhistory failed to read raw backup {} for player {}",
                            backup.get("_id"), backup.get("uuid"), exception);
                } finally {
                    if (scanned % 100 == 0) scanCacheStats = fast.cacheStats();
                }
                // Bounded metadata batches replace one remote profile lookup per matching player.
                if (pending.size() >= 256) emitMaterialSnapshots(pending, profileCache, output);
            }
            emitMaterialSnapshots(pending, profileCache, output);
        } finally { scanCacheStats = fast.cacheStats(); }
    }

    private static MaterialHistoryFastScan.Item inspectScanItem(String encoded) {
        ItemStack item = InventoryUtil.deserializeItemStack(encoded);
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) return MaterialHistoryFastScan.EMPTY;
        ItemMeta meta = item.getItemMeta();
        String pdcInternal = meta == null ? null : meta.getPersistentDataContainer()
                .get(ItemUtil.KEY_INTERNAL_NAME, PersistentDataType.STRING);
        if (pdcInternal != null && item.getAmount() == 1 && !AdvancedItemCatalog.isAdvanced(pdcInternal)
                && !MaterialHistoryScanRules.mythic(pdcInternal))
            return new MaterialHistoryFastScan.Item(pdcInternal, item.getType().name(), item.getAmount(), null, false);
        // Read all required tags in one NBT view; never reformat an item, add a UUID or mutate PDC.
        return NBT.get(item, nbt -> {
            ReadableNBT extra = nbt.getCompound("extra");
            String internal = pdcInternal != null ? pdcInternal : extra == null ? null : extra.getString("internal");
            String uuid = extra == null ? null : extra.getString("uuid");
            boolean marked = extra != null && extra.getBoolean("boostedByPhaseBook");
            return new MaterialHistoryFastScan.Item(internal, item.getType().name(), item.getAmount(), uuid, marked);
        });
    }

    private record PendingMaterialSnapshot(String databaseId, String backupUuid, String playerUuid, long timestamp,
                                           MaterialHistoryFastScan.Contents contents, boolean materialMatch) { }

    private void emitMaterialSnapshots(List<PendingMaterialSnapshot> pending, Map<String, ProfileInfo> profiles,
                                       Consumer<MaterialSnapshot> output) {
        if (pending.isEmpty()) return;
        Set<String> missing = new HashSet<>();
        for (PendingMaterialSnapshot snapshot : pending)
            if (!profiles.containsKey(snapshot.playerUuid)) missing.add(snapshot.playerUuid);
        if (!missing.isEmpty()) {
            try (MongoCursor<Document> cursor = ThePit.getInstance().getMongoDB().getDatabase().getCollection("players")
                    .find(Filters.in("uuid", missing)).projection(Projections.include("uuid", "playerName", "prestige"))
                    .batchSize(256).iterator()) {
                while (cursor.hasNext()) {
                    Document profile = cursor.next();
                    Object prestige = profile.get("prestige");
                    profiles.put(profile.getString("uuid"), new ProfileInfo(profile.getString("playerName"),
                            prestige instanceof Number number ? number.intValue() : -1));
                }
            } catch (Exception exception) {
                scanErrors.incrementAndGet();
                ThePit.getInstance().getSLF4JLogger().warn("scan materialhistory failed to load batched player metadata", exception);
            }
            for (String uuid : missing) profiles.putIfAbsent(uuid, new ProfileInfo("<unknown>", -1));
        }
        for (PendingMaterialSnapshot snapshot : pending) {
            ProfileInfo profile = profiles.get(snapshot.playerUuid);
            MaterialHistoryFastScan.Contents contents = snapshot.contents;
            List<StackedUuidItem> stacks = contents.stacked().stream().map(item -> new StackedUuidItem(
                    item.location(), item.internalName(), item.material(), item.amount(), item.uuid())).toList();
            MaterialSnapshot result = new MaterialSnapshot(snapshot.databaseId, snapshot.backupUuid, snapshot.playerUuid,
                    profile.playerName, profile.prestige, snapshot.timestamp, formatTime(snapshot.timestamp),
                    contents.inventory().total(), contents.warehouse().total(), contents.combined().total(),
                    snapshot.materialMatch, !stacks.isEmpty(), stacks.size(),
                    stacks.stream().mapToInt(item -> item.amount).sum(), sortCounts(contents.inventory().counts()),
                    sortCounts(contents.warehouse().counts()), sortCounts(contents.combined().counts()), stacks);
            // File-write failures abort the task; they must not be swallowed as a malformed backup.
            output.accept(result);
            matchedBackups.incrementAndGet();
            if (!stacks.isEmpty()) { matchedUuidStackBackups.incrementAndGet(); stackedUuidStacks.addAndGet(stacks.size()); }
        }
        pending.clear();
    }

    private Path writeReport(Report report) throws IOException {
        Path reportPath = ScanReportPath.directory("materialhistory").resolve(
                "rare-material-history-" + LocalDateTime.now().format(FILE_NAME_TIME) + ".json");
        Path temporary = reportPath.resolveSibling(reportPath.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(
                temporary,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW)) {
            GSON.toJson(report, writer);
        }
        try { Files.move(temporary, reportPath, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, reportPath); }
        return reportPath;
    }

    private void openAsync(Player player, String requestedName) {
        try {
            Report report = getAvailableReport();
            if (report == null || report.snapshots == null) {
                sendSync(player, "§c没有可用的历史稀有物资扫描报告，请先运行 /scan materialhistory。");
                return;
            }

            List<MaterialSnapshot> playerSnapshots = report.snapshots.stream()
                    .filter(snapshot -> requestedName.equalsIgnoreCase(snapshot.playerName)
                            || requestedName.equalsIgnoreCase(snapshot.playerUuid))
                    .sorted(Comparator.comparingLong((MaterialSnapshot snapshot) -> snapshot.timestamp).reversed())
                    .toList();
            if (playerSnapshots.isEmpty()) {
                sendSync(player, "§c最近一次扫描报告中没有该玩家的命中备份。");
                return;
            }

            String playerUuid = playerSnapshots.getFirst().playerUuid;
            PlayerProfile targetProfile = ThePit.getInstance()
                    .getMongoDB()
                    .getProfileCollection()
                    .find(Filters.eq("uuid", playerUuid))
                    .first();
            if (targetProfile == null) {
                sendSync(player, "§c找到了扫描记录，但无法读取该玩家的档案。");
                return;
            }

            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                if (player.isOnline()) {
                    new MaterialHistoryMenu(targetProfile, playerSnapshots).openMenu(player);
                }
            });
        } catch (Exception exception) {
            ThePit.getInstance().getSLF4JLogger().error(
                    "Failed to open materialhistory report for " + requestedName, exception);
            sendSync(player, "§c读取扫描报告失败，请查看服务器日志。");
        }
    }

    private Report getAvailableReport() throws IOException {
        Report report = latestReport;
        if (report == null) {
            report = loadLatestReport();
            latestReport = report;
        }
        return report;
    }

    List<MaterialHistoryBackupReference> getMaterialHistoryBackupReferences() throws IOException {
        Path path = MaterialHistoryLocalReport.latest(ThePit.getInstance().getDataFolder().toPath()
                .resolve("logs/scan/materialhistory"));
        if (path == null) return Collections.emptyList();
        List<MaterialHistoryBackupReference> references = MaterialHistoryLocalReport.backupReferences(path);
        lastIndexSourceReportPath = path.toAbsolutePath().toString();
        return references;
    }

    String getMaterialHistoryReportPath() {
        return lastIndexSourceReportPath;
    }

    String ensureMaterialHistoryReportPath() throws IOException {
        Path path = MaterialHistoryLocalReport.latest(ThePit.getInstance().getDataFolder().toPath()
                .resolve("logs/scan/materialhistory"));
        return path == null ? null : path.toAbsolutePath().toString();
    }

    private Report loadLatestReport() throws IOException {
        Path directory = ScanReportPath.directory("materialhistory");
        Path latest;
        try (Stream<Path> files = Files.list(directory)) {
            latest = files
                    .filter(path -> path.getFileName().toString().startsWith("rare-material-history-"))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .max(Comparator.comparingLong(this::lastModified))
                    .orElse(null);
        }
        if (latest == null) {
            return null;
        }
        lastReportPath = latest.toAbsolutePath().toString();
        try (Reader reader = Files.newBufferedReader(latest, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, Report.class);
        }
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            return Long.MIN_VALUE;
        }
    }

    private void sendSync(Player player, String message) {
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            if (player.isOnline()) {
                player.sendMessage(message);
            }
        });
    }

    private void sendSync(CommandSender sender, String message) {
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            if (!(sender instanceof Player player) || player.isOnline()) {
                sender.sendMessage(message);
            }
        });
    }

    private Map<String, Integer> sortCounts(Map<String, Integer> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);
    }

    private static Map<String, String> createTrackedItems() {
        Map<String, String> items = new LinkedHashMap<>();
        items.put("sword_bundle_full", "满神话之剑收纳箱");
        items.put("bow_bundle_full", "满神话之弓收纳箱");
        items.put("pants_bundle_full", "满神话之甲收纳箱");
        items.put("mystery_box_one", "神秘之盒 I");
        items.put("mystery_box_two", "神秘之盒 II");
        items.put("mystery_box_three", "神秘之盒 III");
        items.put("phase_book", "相位书");
        items.put("xing_dong_lihe", "行动礼盒");
        items.put("chunk_of_vile_item", "暗聚块");
        items.put("totally_legit_gem", "遵纪守法的宝石");
        items.put("funky_feather", "时髦的羽毛");
        items.put("new_year_lucky_bag", "新春福袋");
        items.put("cactus", "哲学仙人掌");
        items.put("sacrosanct_cactus", "神圣仙人掌");
        items.put("dimensional_cactus", "次元仙人掌");
        items.put("kings_helmet", "国王头");
        items.put("angel_chestplate", "天使甲");
        items.put("exorcism_armor", "驱魔甲");
        items.put("lucky_gem", "幸运宝石");
        items.put("lucky_stone", "幸运石");
        items.put(PHASE_BOOK_MARKED_MYTHIC, "相位书标记神话装备");
        // Keep legacy tracked resources (including cactus), and add the complete advanced-item catalog.
        return MaterialHistoryScanRules.quantityItems(items);
    }

    private static String normalize(String internalName) {
        return internalName == null ? null : internalName.toLowerCase(Locale.ROOT);
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

    private static boolean isPhaseBookMarkedMythic(ItemStack item) {
        try {
            boolean marked = NBT.get(item, nbt -> {
                return nbt.getCompound("extra").getBoolean("boostedByPhaseBook");
            });
            return marked && MythicUtil.getMythicItem(item) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private record ProfileInfo(String playerName, int prestige) {
    }

    private record UuidTraceStateKey(String itemUuid,
                                     String playerUuid,
                                     int amount,
                                     String internalName,
                                     String material,
                                     String displayName,
                                     List<String> lore,
                                     String nbt) {
    }

    private record UuidTraceSearchResult(String historySource,
                                         int candidateBackups,
                                         int loadedBackups,
                                         int matchedBackups,
                                         int rawOccurrences,
                                         List<UuidTraceOccurrence> occurrences) {
    }

    private static final class LiveInventorySnapshot {
        private final String playerUuid;
        private final String playerName;
        private final ItemStack[] inventory;
        private final ItemStack[] armor;
        private final ItemStack[] extra;
        private final List<ItemStack[]> warehouses;

        private LiveInventorySnapshot(String playerUuid,
                                      String playerName,
                                      ItemStack[] inventory,
                                      ItemStack[] armor,
                                      ItemStack[] extra,
                                      List<ItemStack[]> warehouses) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.inventory = inventory;
            this.armor = armor;
            this.extra = extra;
            this.warehouses = new ArrayList<>(warehouses);
        }
    }

    private static final class CurrentUuidSource {
        private final String playerUuid;
        private final String playerName;
        private final String source;
        private final List<CurrentUuidItem> items;

        private CurrentUuidSource(String playerUuid,
                                  String playerName,
                                  String source,
                                  List<CurrentUuidItem> items) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.source = source;
            this.items = new ArrayList<>(items);
        }
    }

    private static final class CurrentUuidItem {
        private final String uuid;
        private final String location;
        private final String internalName;
        private final String material;
        private final String displayName;
        private final int amount;
        private final List<String> lore;
        private final String nbt;

        private CurrentUuidItem(String uuid,
                                String location,
                                String internalName,
                                String material,
                                String displayName,
                                int amount,
                                List<String> lore,
                                String nbt) {
            this.uuid = uuid;
            this.location = location;
            this.internalName = internalName;
            this.material = material;
            this.displayName = displayName;
            this.amount = amount;
            this.lore = new ArrayList<>(lore);
            this.nbt = nbt;
        }
    }

    private static final class UuidTraceOccurrence {
        private final String itemUuid;
        private final String backupDatabaseId;
        private final String backupUuid;
        private final String playerUuid;
        private final String playerName;
        private final int prestige;
        private final long timestamp;
        private final String time;
        private final String location;
        private final String internalName;
        private final String material;
        private final String displayName;
        private final int amount;
        private final List<String> lore;
        private final String nbt;
        private String lastBackupDatabaseId;
        private String lastBackupUuid;
        private long lastTimestamp;
        private String lastTime;
        private String lastLocation;
        private int repeatedBackups;

        private UuidTraceOccurrence(String itemUuid,
                                    String backupDatabaseId,
                                    String backupUuid,
                                    String playerUuid,
                                    String playerName,
                                    int prestige,
                                    long timestamp,
                                    String time,
                                    String location,
                                    String internalName,
                                    String material,
                                    String displayName,
                                    int amount,
                                    List<String> lore,
                                    String nbt) {
            this.itemUuid = itemUuid;
            this.backupDatabaseId = backupDatabaseId;
            this.backupUuid = backupUuid;
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.prestige = prestige;
            this.timestamp = timestamp;
            this.time = time;
            this.location = location;
            this.internalName = internalName;
            this.material = material;
            this.displayName = displayName;
            this.amount = amount;
            this.lore = new ArrayList<>(lore);
            this.nbt = nbt;
            this.lastBackupDatabaseId = backupDatabaseId;
            this.lastBackupUuid = backupUuid;
            this.lastTimestamp = timestamp;
            this.lastTime = time;
            this.lastLocation = location;
            this.repeatedBackups = 1;
        }

        private boolean isSameBackup(UuidTraceOccurrence other) {
            if (backupDatabaseId != null && other.backupDatabaseId != null) {
                return backupDatabaseId.equals(other.backupDatabaseId);
            }
            return backupUuid != null && backupUuid.equals(other.backupUuid);
        }

        private UuidTraceStateKey stateKey() {
            return new UuidTraceStateKey(
                    itemUuid,
                    playerUuid,
                    amount,
                    internalName,
                    material,
                    displayName,
                    lore,
                    nbt
            );
        }

        private void extendTo(UuidTraceOccurrence other) {
            lastBackupDatabaseId = other.backupDatabaseId;
            lastBackupUuid = other.backupUuid;
            lastTimestamp = other.timestamp;
            lastTime = other.time;
            lastLocation = other.location;
            repeatedBackups++;
        }

        private String effectiveLastDatabaseId() {
            return lastBackupDatabaseId == null ? backupDatabaseId : lastBackupDatabaseId;
        }

        private String effectiveLastBackupUuid() {
            return lastBackupUuid == null ? backupUuid : lastBackupUuid;
        }

        private String effectiveLastTime() {
            return lastTime == null ? time : lastTime;
        }

        private String effectiveLastLocation() {
            return lastLocation == null ? location : lastLocation;
        }

        private int effectiveRepeatedBackups() {
            return Math.max(1, repeatedBackups);
        }
    }

    private static final class UuidTraceReport {
        private boolean currentRareHistory;
        private int unidentifiedItemStacks;
        private long historyIndexGeneratedAt;
        private final long generatedAtTimestamp;
        private final String generatedAt;
        private final String historySource;
        private final CurrentUuidSource target;
        private final int uniqueCurrentUuids;
        private final int candidateBackups;
        private final int loadedBackups;
        private final int matchedBackups;
        private final int rawOccurrences;
        private final int occurrences;
        private final List<UuidTraceOccurrence> results;

        private UuidTraceReport(long generatedAtTimestamp,
                                String generatedAt,
                                String historySource,
                                CurrentUuidSource target,
                                int uniqueCurrentUuids,
                                int candidateBackups,
                                int loadedBackups,
                                int matchedBackups,
                                int rawOccurrences,
                                int occurrences,
                                List<UuidTraceOccurrence> results) {
            this.generatedAtTimestamp = generatedAtTimestamp;
            this.generatedAt = generatedAt;
            this.historySource = historySource;
            this.target = target;
            this.uniqueCurrentUuids = uniqueCurrentUuids;
            this.candidateBackups = candidateBackups;
            this.loadedBackups = loadedBackups;
            this.matchedBackups = matchedBackups;
            this.rawOccurrences = rawOccurrences;
            this.occurrences = occurrences;
            this.results = new ArrayList<>(results);
        }
    }

    private static final class UuidEquipmentInfo {
        private final String itemUuid;
        private CurrentUuidItem currentItem;
        private final List<String> currentLocations = new ArrayList<>();
        private final List<UuidTraceOccurrence> history = new ArrayList<>();
        private boolean firstSeenOutsideTarget;

        private UuidEquipmentInfo(String itemUuid) {
            this.itemUuid = itemUuid;
        }

        private UuidTraceOccurrence firstOccurrence() {
            return history.isEmpty() ? null : history.getFirst();
        }
    }

    private final class UuidTraceEquipmentMenu extends Menu {
        private final UuidTraceReport report;
        private final List<UuidEquipmentInfo> equipment;
        private final Set<String> blacklistedUuids;
        private final Set<String> blacklistRequests = new HashSet<>();
        private int page;
        private boolean outsideFirstOnly;

        private UuidTraceEquipmentMenu(UuidTraceReport report) {
            this(report, Collections.emptySet());
        }

        private UuidTraceEquipmentMenu(UuidTraceReport report, Set<String> blacklistedUuids) {
            this.report = report;
            this.equipment = buildEquipment(report);
            this.blacklistedUuids = new HashSet<>(blacklistedUuids);
        }

        @Override
        public String getTitle(Player player) {
            return (report.currentRareHistory ? "当前稀有物品历史 (" : "UUID装备追踪 (") + (page + 1) + ")";
        }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            List<UuidEquipmentInfo> visible = getVisibleEquipment();
            int start = page * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, visible.size());
            for (int index = start; index < end; index++) {
                buttons.put(index - start, new UuidEquipmentButton(visible.get(index)));
            }
            if (page > 0) {
                buttons.put(45, new UuidEquipmentPageButton(-1));
            }
            if (report.currentRareHistory) {
                long matched = equipment.stream().filter(info -> !info.history.isEmpty()).count();
                buttons.put(48, new DisplayButton(new ItemBuilder(Material.BOOK)
                                .name("&b" + report.target.playerName + " 的当前稀有物品")
                                .lore("&7查询时间: &f" + report.generatedAt,
                                        "&7历史来源: &a本地 UUID 索引",
                                        "&7索引生成时间: &f" + formatTime(report.historyIndexGeneratedAt),
                                        "&7当前物品组数: &f" + report.target.items.size(),
                                        "&7唯一 UUID: &f" + report.uniqueCurrentUuids,
                                        "&7查到历史的 UUID: &a" + matched,
                                        "&7未带 UUID 的物品组: &c" + report.unidentifiedItemStacks,
                                        "&7历史包括本人和其他玩家的背包、仓库",
                                        "&7历史范围截至本地索引收录的备份")
                                .build(), true));
            }
            buttons.put(49, new OutsideFirstFilterButton());
            if (end < visible.size()) {
                buttons.put(53, new UuidEquipmentPageButton(1));
            }
            return buttons;
        }

        @Override
        public int getSize() {
            return 54;
        }

        private List<UuidEquipmentInfo> buildEquipment(UuidTraceReport report) {
            Map<String, UuidEquipmentInfo> byUuid = new LinkedHashMap<>();
            if (report.target.items != null) {
                for (CurrentUuidItem item : report.target.items) {
                    if (item == null || (item.uuid == null && !report.currentRareHistory)) {
                        continue;
                    }
                    String key = item.uuid == null ? "unidentified:" + item.location : item.uuid;
                    UuidEquipmentInfo info = byUuid.computeIfAbsent(key,
                            ignored -> new UuidEquipmentInfo(item.uuid == null ? "无 UUID" : item.uuid));
                    if (info.currentItem == null) {
                        info.currentItem = item;
                    }
                    info.currentLocations.add(item.location);
                }
            }
            if (report.results != null) {
                for (UuidTraceOccurrence occurrence : report.results) {
                    if (occurrence == null || occurrence.itemUuid == null) {
                        continue;
                    }
                    byUuid.computeIfAbsent(occurrence.itemUuid, UuidEquipmentInfo::new)
                            .history.add(occurrence);
                }
            }

            List<UuidEquipmentInfo> result = new ArrayList<>(byUuid.values());
            for (UuidEquipmentInfo info : result) {
                info.history.sort(Comparator
                        .comparingLong((UuidTraceOccurrence occurrence) -> occurrence.timestamp)
                        .thenComparing(occurrence -> occurrence.playerUuid,
                                Comparator.nullsFirst(String::compareTo)));
                UuidTraceOccurrence first = info.firstOccurrence();
                info.firstSeenOutsideTarget = first != null
                        && !Objects.equals(report.target.playerUuid, first.playerUuid);
            }
            result.sort(Comparator
                    .comparing((UuidEquipmentInfo info) -> info.firstSeenOutsideTarget).reversed()
                    .thenComparingLong(info -> {
                        UuidTraceOccurrence first = info.firstOccurrence();
                        return first == null ? Long.MAX_VALUE : first.timestamp;
                    })
                    .thenComparing(info -> info.itemUuid));
            return result;
        }

        private List<UuidEquipmentInfo> getVisibleEquipment() {
            if (!outsideFirstOnly) {
                return equipment;
            }
            return equipment.stream().filter(info -> info.firstSeenOutsideTarget).toList();
        }

        private final class UuidEquipmentButton extends Button {
            private final UuidEquipmentInfo info;

            private UuidEquipmentButton(UuidEquipmentInfo info) {
                this.info = info;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                CurrentUuidItem item = info.currentItem;
                boolean blacklisted = report.currentRareHistory && item != null && item.uuid != null
                        && blacklistedUuids.contains(item.uuid.trim().toLowerCase(Locale.ROOT));
                List<String> lore = new ArrayList<>();
                if (blacklisted) {
                    lore.add("&4&l⚠ 已加入 UUID 回收黑名单");
                    lore.add("");
                }
                lore.add("&7UUID: &f" + info.itemUuid);
                lore.add("&7当前位置: &f" + String.join(", ", info.currentLocations));
                if (report.currentRareHistory && item != null) {
                    lore.add("&7当前数量: &f" + item.amount);
                    if (item.uuid == null) {
                        lore.add("&c该物品没有 UUID，无法准确匹配历史");
                    }
                }
                lore.add("&7历史状态数: &e" + info.history.size());
                if (report.currentRareHistory) {
                    Map<String, String> holders = new LinkedHashMap<>();
                    for (UuidTraceOccurrence occurrence : info.history) {
                        holders.putIfAbsent(occurrence.playerUuid, safePlayerName(occurrence));
                    }
                    lore.add("&7历史持有人数: &e" + holders.size());
                    holders.values().stream().limit(10).forEach(name -> lore.add("&7曾出现于: &f" + name));
                    if (holders.size() > 10) {
                        lore.add("&7其余持有人请点击查看完整历史");
                    }
                }
                UuidTraceOccurrence first = info.firstOccurrence();
                if (first == null) {
                    lore.add(report.currentRareHistory
                            ? "&7本地索引历史: &8未找到" : "&7报告内首次记录: &8未找到");
                } else {
                    lore.add("&7报告内首次记录: &f" + first.time);
                    lore.add("&7首次持有人: &f" + safePlayerName(first));
                    lore.add(info.firstSeenOutsideTarget
                            ? "&c⚠ 首次记录不属于目标玩家"
                            : "&a首次记录属于目标玩家");
                }
                appendOriginalLore(lore, item == null ? null : item.lore, Integer.MAX_VALUE);
                lore.add("");
                if (report.currentRareHistory && item != null
                        && CurrentHistoryItemActions.canViewEnchantments(item.internalName, item.uuid)) {
                    lore.add("&e左键查看该装备的持有人 / 备份历史");
                    lore.add("&b右键查看该装备的历史附魔 / 打宝石记录（全部操作者）");
                    lore.add("&7附魔历史读取已整理的本地 enchantrecords 结果");
                } else lore.add("&e点击查看该装备的历史状态");
                if (report.currentRareHistory && item != null && item.uuid != null) {
                    lore.add(blacklisted ? "&4该 UUID 已在回收黑名单中"
                            : "&cShift + 点击将该物品 UUID 加入回收黑名单");
                }
                String name = displayItemName(item, info.itemUuid);
                return new ItemBuilder(materialOf(item == null ? null : item.material))
                        .name(blacklisted ? "&4[黑名单] &c" + name : name)
                        .lore(lore)
                        .build();
            }

            @Override
            public void clicked(Player player,
                                int slot,
                                ClickType clickType,
                                int hotbarButton,
                                ItemStack currentItem) {
                CurrentUuidItem item = info.currentItem;
                CurrentHistoryItemActions.Action action = CurrentHistoryItemActions.choose(
                        report.currentRareHistory, clickType.isShiftClick(), clickType == ClickType.RIGHT,
                        item == null ? null : item.internalName, item == null ? null : item.uuid);
                if (action == CurrentHistoryItemActions.Action.BLACKLIST) {
                    blacklistCurrentItem(player, info);
                    return;
                }
                if (action == CurrentHistoryItemActions.Action.ENCHANTMENTS) {
                    openEquipmentEnchantments(player, info, UuidTraceEquipmentMenu.this);
                    return;
                }
                if (report.currentRareHistory && info.history.isEmpty()) {
                    player.sendMessage(info.currentItem != null && info.currentItem.uuid == null
                            ? "§c该物品没有 UUID，无法准确匹配持有人历史。"
                            : "§e本地索引中没有该物品的历史记录，可更新 uuidindex 后重新查询。");
                    return;
                }
                new UuidTraceHistoryMenu(info, UuidTraceEquipmentMenu.this).openMenu(player);
            }
        }

        private void blacklistCurrentItem(Player player, UuidEquipmentInfo info) {
            CurrentUuidItem item = info.currentItem;
            if (item == null || item.uuid == null || item.uuid.isBlank()) {
                player.sendMessage("§c该物品没有 UUID，无法加入回收黑名单。");
                return;
            }
            String itemUuid = item.uuid.trim().toLowerCase(Locale.ROOT);
            if (blacklistedUuids.contains(itemUuid)) {
                player.sendMessage("§e该物品 UUID 已在回收黑名单中。");
                return;
            }
            if (!blacklistRequests.add(itemUuid)) {
                player.sendMessage("§e该 UUID 正在写入黑名单，请稍候。");
                return;
            }
            Document entry = new Document("_id", itemUuid)
                    .append("itemUuid", itemUuid)
                    .append("createdAt", System.currentTimeMillis())
                    .append("createdByUuid", player.getUniqueId().toString())
                    .append("createdByName", player.getName())
                    .append("creationMode", "CURRENTHISTORY_ITEM")
                    .append("sourceQueryPlayerUuid", report.target.playerUuid)
                    .append("sourceQueryPlayerName", report.target.playerName)
                    .append("sourceReportGeneratedAt", report.generatedAtTimestamp)
                    .append("sourceLocations", new ArrayList<>(info.currentLocations))
                    .append("itemSnapshot", new Document("material", item.material)
                            .append("internalName", item.internalName)
                            .append("displayName", item.displayName)
                            .append("amount", item.amount)
                            .append("lore", item.lore)
                            .append("nbt", item.nbt));
            player.sendMessage("§7正在将物品 UUID §f" + itemUuid + " §7加入回收黑名单...");
            Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
                try {
                    ThePit.getInstance().getMongoDB().getItemUuidBlacklistCollection().updateOne(
                            Filters.eq("_id", itemUuid),
                            new Document("$setOnInsert", entry)
                                    .append("$set", new Document("active", true)),
                            new UpdateOptions().upsert(true));
                    Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                        blacklistRequests.remove(itemUuid);
                        blacklistedUuids.add(itemUuid);
                        if (!player.isOnline()) {
                            return;
                        }
                        player.sendMessage("§a物品 UUID §f" + itemUuid
                                + " §a已加入回收黑名单，持有者下次登录时执行回收。");
                        if (Menu.currentlyOpenedMenus.get(player.getUniqueId()) == this) {
                            openMenu(player);
                        }
                    });
                } catch (Exception exception) {
                    ThePit.getInstance().getSLF4JLogger().error(
                            "Unable to blacklist currenthistory item UUID " + itemUuid, exception);
                    Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                        blacklistRequests.remove(itemUuid);
                        if (player.isOnline()) {
                            player.sendMessage("§c物品黑名单写入失败，请查看控制台日志。");
                        }
                    });
                }
            });
        }

        private final class OutsideFirstFilterButton extends Button {
            @Override
            public ItemStack getButtonItem(Player player) {
                long suspicious = equipment.stream().filter(info -> info.firstSeenOutsideTarget).count();
                return new ItemBuilder(Material.HOPPER)
                        .name("&b首次出现筛选")
                        .lore(
                                "&7当前: " + (outsideFirstOnly ? "&c仅首次在他人处" : "&a全部装备"),
                                "&7可疑装备: &c" + suspicious,
                                "",
                                "&e点击切换"
                        )
                        .build();
            }

            @Override
            public void clicked(Player player,
                                int slot,
                                ClickType clickType,
                                int hotbarButton,
                                ItemStack currentItem) {
                outsideFirstOnly = !outsideFirstOnly;
                page = 0;
                openMenu(player);
            }
        }

        private final class UuidEquipmentPageButton extends Button {
            private final int direction;

            private UuidEquipmentPageButton(int direction) {
                this.direction = direction;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.ARROW)
                        .name(direction < 0 ? "&6上一页" : "&a下一页")
                        .build();
            }

            @Override
            public void clicked(Player player,
                                int slot,
                                ClickType clickType,
                                int hotbarButton,
                                ItemStack currentItem) {
                int next = page + direction;
                if (next >= 0 && next * ITEMS_PER_PAGE < getVisibleEquipment().size()) {
                    page = next;
                    openMenu(player);
                }
            }
        }
    }

    private final class UuidTransferMenu extends Menu {
        private final String targetPlayerUuid;
        private final String targetPlayerName;
        private final List<ScanMaterialUuidHistoryIndex.TransferredUuid> transferred;
        private final Set<String> blacklistedUuids;
        private final Set<String> blacklistRequests = new HashSet<>();
        private int page;
        private boolean loadingHistory;
        private boolean blacklistingAll;

        private UuidTransferMenu(String targetPlayerUuid,
                                 String targetPlayerName,
                                 List<ScanMaterialUuidHistoryIndex.TransferredUuid> transferred,
                                 Set<String> blacklistedUuids) {
            this.targetPlayerUuid = targetPlayerUuid;
            this.targetPlayerName = targetPlayerName;
            this.transferred = transferred == null
                    ? Collections.emptyList()
                    : new ArrayList<>(transferred);
            this.blacklistedUuids = blacklistedUuids == null
                    ? new HashSet<>()
                    : new HashSet<>(blacklistedUuids);
        }

        @Override
        public String getTitle(Player player) {
            return "UUID已转移 (" + (page + 1) + ")";
        }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            int start = page * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, transferred.size());
            for (int index = start; index < end; index++) {
                buttons.put(index - start, new TransferredUuidButton(transferred.get(index)));
            }
            if (page > 0) {
                buttons.put(45, new TransferPageButton(-1));
            }
            buttons.put(48, new BlacklistAllButton());
            buttons.put(49, new TransferSummaryButton());
            if (end < transferred.size()) {
                buttons.put(53, new TransferPageButton(1));
            }
            return buttons;
        }

        @Override
        public int getSize() {
            return 54;
        }

        private final class TransferredUuidButton extends Button {
            private final ScanMaterialUuidHistoryIndex.TransferredUuid transferredUuid;

            private TransferredUuidButton(ScanMaterialUuidHistoryIndex.TransferredUuid transferredUuid) {
                this.transferredUuid = transferredUuid;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                ScanMaterialUuidHistoryIndex.UuidHistoryEvent first = transferredUuid.first;
                ScanMaterialUuidHistoryIndex.UuidHistoryEvent latest = transferredUuid.latest;
                ScanMaterialUuidHistoryIndex.IndexedItem item = representativeIndexedItem(latest);
                if (item == null) {
                    item = representativeIndexedItem(first);
                }
                boolean blacklisted = isBlacklisted(transferredUuid.itemUuid);
                List<String> lore = new ArrayList<>();
                if (blacklisted) {
                    lore.add("&4&l⚠ 已加入 UUID 回收黑名单");
                    lore.add("");
                }
                lore.add("&7UUID: &f" + transferredUuid.itemUuid);
                lore.add("");
                lore.add("&7首次持有人: &a" + indexedPlayerName(first));
                lore.add("&7首次时间: &f" + valueOrUnknown(first == null ? null : first.time));
                lore.add("&7首次 Mongo _id: &f" + valueOrUnknown(
                        first == null ? null : first.backupDatabaseId));
                lore.add("");
                lore.add("&7最新持有人: &c" + indexedPlayerName(latest));
                lore.add("&7最新时间: &f" + valueOrUnknown(latest == null ? null : latest.time));
                lore.add("&7最新 Mongo _id: &f" + valueOrUnknown(
                        latest == null ? null : latest.backupDatabaseId));
                lore.add("&7最新位置: &f" + indexedLocations(latest));
                lore.add(Objects.equals(
                                ScanMaterialUuidHistoryIndex.comparisonStateHash(
                                        representativeIndexedItem(first)),
                                ScanMaterialUuidHistoryIndex.comparisonStateHash(
                                        representativeIndexedItem(latest)))
                        ? "&7物品数据: &a未变化"
                        : "&7物品数据: &e发生变化");
                appendOriginalLore(lore, item == null ? null : item.lore, Integer.MAX_VALUE);
                lore.add("");
                lore.add("&e点击查看该装备的完整流转历史");
                lore.add(blacklisted
                        ? "&4该 UUID 已在回收黑名单中"
                        : "&cShift + 右键将该 UUID 加入回收黑名单");
                String displayName = indexedDisplayItemName(item, transferredUuid.itemUuid);
                return new ItemBuilder(materialOf(item == null ? null : item.material))
                        .name(blacklisted ? "&4[黑名单] &c" + displayName : displayName)
                        .lore(lore)
                        .build();
            }

            @Override
            public void clicked(Player player,
                                int slot,
                                ClickType clickType,
                                int hotbarButton,
                                ItemStack currentItem) {
                if (clickType == ClickType.SHIFT_RIGHT) {
                    addToBlacklist(player, transferredUuid);
                    return;
                }
                openHistory(player, transferredUuid);
            }
        }

        private void addToBlacklist(Player player,
                                    ScanMaterialUuidHistoryIndex.TransferredUuid transferredUuid) {
            if (blacklistingAll) {
                player.sendMessage("§e正在批量写入该玩家的装备黑名单，请稍候。");
                return;
            }
            String itemUuid = transferredUuid == null || transferredUuid.itemUuid == null
                    ? null
                    : transferredUuid.itemUuid.trim().toLowerCase(Locale.ROOT);
            if (itemUuid == null || itemUuid.isBlank()) {
                player.sendMessage("§c该条目没有可用的物品 UUID。");
                return;
            }
            if (!blacklistRequests.add(itemUuid)) {
                player.sendMessage("§e该 UUID 正在写入黑名单，请稍候。");
                return;
            }

            Document entry = createBlacklistEntry(player, transferredUuid, itemUuid)
                    .append("creationMode", "UUIDTRANSFER_ITEM");
            player.sendMessage("§7正在将物品 UUID §f" + itemUuid + " §7写入回收黑名单...");
            Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
                boolean added = false;
                Exception failure = null;
                try {
                    UpdateResult result = ThePit.getInstance()
                            .getMongoDB()
                            .getItemUuidBlacklistCollection()
                            .updateOne(
                                    Filters.eq("_id", itemUuid),
                                    new Document("$setOnInsert", entry),
                                    new UpdateOptions().upsert(true));
                    added = result.getUpsertedId() != null;
                } catch (Exception exception) {
                    failure = exception;
                }

                boolean inserted = added;
                Exception writeFailure = failure;
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    blacklistRequests.remove(itemUuid);
                    if (!player.isOnline()) {
                        return;
                    }
                    if (writeFailure != null) {
                        player.sendMessage("§cUUID 黑名单写入失败，未作任何回收操作，请查看控制台。");
                        ThePit.getInstance().getSLF4JLogger().error(
                                "Unable to add item UUID {} to blacklist", itemUuid, writeFailure);
                        return;
                    }
                    blacklistedUuids.add(itemUuid);
                    if (inserted) {
                        player.sendMessage("§a已将物品 UUID §f" + itemUuid
                                + " §a加入回收黑名单；持有者下次登录时会被审计回收。");
                    } else {
                        player.sendMessage("§e物品 UUID §f" + itemUuid + " §e已存在于回收黑名单中。");
                    }
                    openMenu(player);
                });
            });
        }

        private boolean isBlacklisted(String itemUuid) {
            return itemUuid != null
                    && blacklistedUuids.contains(itemUuid.trim().toLowerCase(Locale.ROOT));
        }

        private final class BlacklistAllButton extends Button {

            @Override
            public ItemStack getButtonItem(Player player) {
                int blacklisted = 0;
                for (ScanMaterialUuidHistoryIndex.TransferredUuid item : transferred) {
                    if (item != null && isBlacklisted(item.itemUuid)) {
                        blacklisted++;
                    }
                }
                int pending = Math.max(0, transferred.size() - blacklisted);
                List<String> lore = new ArrayList<>();
                lore.add("&7作用范围: &f本次查询结果中的全部装备");
                lore.add("&7装备总数: &f" + transferred.size());
                lore.add("&7已在黑名单: &c" + blacklisted);
                lore.add("&7待加入: &e" + pending);
                lore.add("");
                lore.add(pending <= 0
                        ? "&a全部装备均已在黑名单中"
                        : "&c点击一键加入回收黑名单");
                return new ItemBuilder(pending <= 0 ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK)
                        .name(pending <= 0
                                ? "&a&l全部装备已拉黑"
                                : "&4&l一键拉黑 " + targetPlayerName + " 的全部装备")
                        .lore(lore)
                        .build();
            }

            @Override
            public void clicked(Player player,
                                int slot,
                                ClickType clickType,
                                int hotbarButton,
                                ItemStack currentItem) {
                blacklistAll(player);
            }
        }

        private void blacklistAll(Player player) {
            if (blacklistingAll) {
                player.sendMessage("§e批量黑名单写入正在进行，请稍候。");
                return;
            }
            if (!blacklistRequests.isEmpty()) {
                player.sendMessage("§e仍有单件装备正在写入黑名单，请稍候再试。");
                return;
            }

            String batchId = UUID.randomUUID().toString();
            List<UpdateOneModel<Document>> writes = new ArrayList<>();
            Set<String> requestedUuids = new HashSet<>();
            for (ScanMaterialUuidHistoryIndex.TransferredUuid item : transferred) {
                String itemUuid = item == null || item.itemUuid == null
                        ? null
                        : item.itemUuid.trim().toLowerCase(Locale.ROOT);
                if (itemUuid == null || itemUuid.isBlank()
                        || isBlacklisted(itemUuid) || !requestedUuids.add(itemUuid)) {
                    continue;
                }
                Document entry = createBlacklistEntry(player, item, itemUuid)
                        .append("creationMode", "UUIDTRANSFER_ALL")
                        .append("batchId", batchId);
                writes.add(new UpdateOneModel<>(
                        Filters.eq("_id", itemUuid),
                        new Document("$setOnInsert", entry),
                        new UpdateOptions().upsert(true)));
            }
            if (writes.isEmpty()) {
                player.sendMessage("§a该查询结果中的全部装备都已在回收黑名单中。");
                return;
            }

            blacklistingAll = true;
            int beforeCount = blacklistedUuids.size();
            player.sendMessage("§7正在异步写入 §f" + writes.size()
                    + " §7个装备 UUID；期间不会阻塞服务器主线程...");
            Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
                Exception failure = null;
                try {
                    ThePit.getInstance()
                            .getMongoDB()
                            .getItemUuidBlacklistCollection()
                            .bulkWrite(writes, new BulkWriteOptions().ordered(false));
                } catch (Exception exception) {
                    failure = exception;
                }

                Set<String> refreshed = Collections.emptySet();
                Exception refreshFailure = null;
                try {
                    refreshed = loadBlacklistedUuids(transferred);
                } catch (Exception exception) {
                    refreshFailure = exception;
                }
                Set<String> loadedBlacklist = refreshed;
                Exception writeFailure = failure;
                Exception loadedFailure = refreshFailure;
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    blacklistingAll = false;
                    if (!player.isOnline()) {
                        return;
                    }
                    if (loadedFailure == null) {
                        blacklistedUuids.clear();
                        blacklistedUuids.addAll(loadedBlacklist);
                    }
                    int added = Math.max(0, blacklistedUuids.size() - beforeCount);
                    if (writeFailure == null && loadedFailure == null) {
                        player.sendMessage("§a批量拉黑完成：本次新增 §f" + added
                                + " §a个 UUID，当前共 §f" + blacklistedUuids.size() + " §a个。 ");
                    } else {
                        player.sendMessage("§c批量写入未完全成功；已重新读取数据库状态，请查看控制台日志。");
                        if (writeFailure != null) {
                            ThePit.getInstance().getSLF4JLogger().error(
                                    "Unable to blacklist all transferred UUID items for " + targetPlayerName,
                                    writeFailure);
                        }
                        if (loadedFailure != null) {
                            ThePit.getInstance().getSLF4JLogger().error(
                                    "Unable to refresh UUID blacklist after batch " + batchId,
                                    loadedFailure);
                        }
                    }
                    openMenu(player);
                });
            });
        }

        private Document createBlacklistEntry(Player operator,
                                              ScanMaterialUuidHistoryIndex.TransferredUuid transferredUuid,
                                              String itemUuid) {
            ScanMaterialUuidHistoryIndex.UuidHistoryEvent first = transferredUuid.first;
            ScanMaterialUuidHistoryIndex.UuidHistoryEvent latest = transferredUuid.latest;
            ScanMaterialUuidHistoryIndex.IndexedItem item = representativeIndexedItem(latest);
            if (item == null) {
                item = representativeIndexedItem(first);
            }

            Document entry = new Document("_id", itemUuid)
                    .append("itemUuid", itemUuid)
                    .append("active", true)
                    .append("createdAt", System.currentTimeMillis())
                    .append("createdByUuid", operator.getUniqueId().toString())
                    .append("createdByName", operator.getName())
                    .append("sourceQueryPlayerUuid", targetPlayerUuid)
                    .append("sourceQueryPlayerName", targetPlayerName)
                    .append("firstPlayerUuid", first == null ? null : first.playerUuid)
                    .append("firstPlayerName", first == null ? null : first.playerName)
                    .append("firstTimestamp", first == null ? null : first.timestamp)
                    .append("firstBackupDatabaseId", first == null ? null : first.backupDatabaseId)
                    .append("latestPlayerUuid", latest == null ? null : latest.playerUuid)
                    .append("latestPlayerName", latest == null ? null : latest.playerName)
                    .append("latestTimestamp", latest == null ? null : latest.timestamp)
                    .append("latestBackupDatabaseId", latest == null ? null : latest.backupDatabaseId);
            if (item != null) {
                entry.append("itemSnapshot", new Document("material", item.material)
                        .append("internalName", item.internalName)
                        .append("displayName", item.displayName)
                        .append("amount", item.amount)
                        .append("lore", item.lore == null ? List.of() : item.lore)
                        .append("nbt", item.nbt));
            }
            return entry;
        }

        private void openHistory(Player player,
                                 ScanMaterialUuidHistoryIndex.TransferredUuid transferredUuid) {
            if (loadingHistory) {
                return;
            }
            loadingHistory = true;
            player.sendMessage("§7正在读取该装备的 UUID 历史...");
            Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
                List<UuidTraceOccurrence> history = Collections.emptyList();
                Exception failure = null;
                try {
                    ScanMaterialUuidHistoryIndex.UuidHistoryLookup lookup =
                            uuidHistoryIndex.lookup(Set.of(transferredUuid.itemUuid));
                    if (lookup == null || lookup.manifest == null) {
                        throw new IllegalStateException("UUID 历史索引不存在");
                    }
                    history = convertIndexedEvents(lookup.events);
                } catch (Exception exception) {
                    failure = exception;
                }
                List<UuidTraceOccurrence> loadedHistory = history;
                Exception loadedFailure = failure;
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    loadingHistory = false;
                    if (!player.isOnline()) {
                        return;
                    }
                    if (loadedFailure != null || loadedHistory.isEmpty()) {
                        ThePit.getInstance().getSLF4JLogger().error(
                                "Failed to load UUID transfer history " + transferredUuid.itemUuid,
                                loadedFailure);
                        player.sendMessage("§c读取该装备的 UUID 历史失败，请查看服务器日志。");
                        return;
                    }
                    UuidEquipmentInfo equipment = new UuidEquipmentInfo(transferredUuid.itemUuid);
                    equipment.history.addAll(loadedHistory);
                    new UuidTraceHistoryMenu(equipment, UuidTransferMenu.this).openMenu(player);
                });
            });
        }

        private final class TransferSummaryButton extends Button {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.PLAYER_HEAD)
                        .name("&e" + targetPlayerName)
                        .lore(
                                "&7UUID: &f" + targetPlayerUuid,
                                "&7符合条件的装备: &c" + transferred.size(),
                                "",
                                "&7条件：索引内首次持有人为该玩家，",
                                "&7且最新持有人已经是其他玩家。",
                                "&7结果按最新出现时间倒序排列。"
                        )
                        .build();
            }

            @Override
            public void clicked(Player player,
                                int slot,
                                ClickType clickType,
                                int hotbarButton,
                                ItemStack currentItem) {
                // Information-only button.
            }
        }

        private final class TransferPageButton extends Button {
            private final int direction;

            private TransferPageButton(int direction) {
                this.direction = direction;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.ARROW)
                        .name(direction < 0 ? "&6上一页" : "&a下一页")
                        .build();
            }

            @Override
            public void clicked(Player player,
                                int slot,
                                ClickType clickType,
                                int hotbarButton,
                                ItemStack currentItem) {
                int next = page + direction;
                if (next >= 0 && next * ITEMS_PER_PAGE < transferred.size()) {
                    page = next;
                    openMenu(player);
                }
            }
        }
    }

    private final class LegitGemRecordMenu extends Menu {
        private final List<ScanMaterialUuidHistoryIndex.RecordedUuid> items;
        private int page;
        private boolean loadingHistory;

        private LegitGemRecordMenu(List<ScanMaterialUuidHistoryIndex.RecordedUuid> items) {
            this.items = items == null ? Collections.emptyList() : new ArrayList<>(items);
        }

        @Override
        public String getTitle(Player player) {
            return "遵纪守法宝石记录 (" + (page + 1) + ")";
        }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            int start = page * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, items.size());
            for (int index = start; index < end; index++) {
                buttons.put(index - start, new LegitGemItemButton(items.get(index)));
            }
            if (page > 0) {
                buttons.put(45, new LegitGemPageButton(-1));
            }
            buttons.put(49, new LegitGemSummaryButton());
            if (end < items.size()) {
                buttons.put(53, new LegitGemPageButton(1));
            }
            return buttons;
        }

        @Override
        public int getSize() {
            return 54;
        }

        private final class LegitGemItemButton extends Button {
            private final ScanMaterialUuidHistoryIndex.RecordedUuid recorded;

            private LegitGemItemButton(ScanMaterialUuidHistoryIndex.RecordedUuid recorded) {
                this.recorded = recorded;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                ScanMaterialUuidHistoryIndex.IndexedItem item = representativeIndexedItem(recorded.matched);
                List<String> lore = new ArrayList<>();
                lore.add("&7UUID: &f" + recorded.itemUuid);
                lore.add("&7命中备份: &f" + valueOrUnknown(recorded.matched.time));
                lore.add("&7命中时持有人: &f" + indexedPlayerName(recorded.matched));
                lore.add("&7命中 Mongo _id: &f" + valueOrUnknown(recorded.matched.backupDatabaseId));
                List<LegitGemRecordInfo> records = readLegitGemRecords(item == null ? null : item.nbt);
                if (!records.isEmpty()) {
                    lore.add("");
                    lore.add("&d遵纪守法宝石操作:");
                    for (LegitGemRecordInfo record : records) {
                        lore.add("&f" + record.playerName + " &7@ &f" + formatTime(record.timestamp));
                    }
                }
                lore.add("");
                lore.add("&7最新持有人: &e" + indexedPlayerName(recorded.latest));
                lore.add("&7最新备份: &f" + valueOrUnknown(recorded.latest.time));
                lore.add("&7最新位置: &f" + indexedLocations(recorded.latest));
                appendOriginalLore(lore, item == null ? null : item.lore, Integer.MAX_VALUE);
                lore.add("");
                lore.add("&e点击查看该装备的完整 UUID 历史");
                return new ItemBuilder(materialOf(item == null ? null : item.material))
                        .name(indexedDisplayItemName(item, recorded.itemUuid))
                        .lore(lore)
                        .build();
            }

            @Override
            public void clicked(Player player,
                                int slot,
                                ClickType clickType,
                                int hotbarButton,
                                ItemStack currentItem) {
                openHistory(player, recorded.itemUuid);
            }
        }

        private void openHistory(Player player, String itemUuid) {
            if (loadingHistory) {
                return;
            }
            loadingHistory = true;
            player.sendMessage("§7正在读取该装备的 UUID 历史...");
            Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
                List<UuidTraceOccurrence> history = Collections.emptyList();
                Exception failure = null;
                try {
                    ScanMaterialUuidHistoryIndex.UuidHistoryLookup lookup =
                            uuidHistoryIndex.lookup(Set.of(itemUuid));
                    if (lookup == null || lookup.manifest == null) {
                        throw new IllegalStateException("UUID 历史索引不存在");
                    }
                    history = convertIndexedEvents(lookup.events);
                } catch (Exception exception) {
                    failure = exception;
                }
                List<UuidTraceOccurrence> loadedHistory = history;
                Exception loadedFailure = failure;
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    loadingHistory = false;
                    if (!player.isOnline()) {
                        return;
                    }
                    if (loadedFailure != null || loadedHistory.isEmpty()) {
                        ThePit.getInstance().getSLF4JLogger().error(
                                "Failed to load legit gem UUID history " + itemUuid, loadedFailure);
                        player.sendMessage("§c读取该装备的 UUID 历史失败，请查看服务器日志。");
                        return;
                    }
                    UuidEquipmentInfo equipment = new UuidEquipmentInfo(itemUuid);
                    equipment.history.addAll(loadedHistory);
                    new UuidTraceHistoryMenu(equipment, LegitGemRecordMenu.this).openMenu(player);
                });
            });
        }

        private final class LegitGemSummaryButton extends Button {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.EMERALD)
                        .name("&a遵纪守法宝石记录")
                        .lore(
                                "&7命中装备 UUID: &f" + items.size(),
                                "&7每个 UUID 仅显示一次。",
                                "&7任一历史状态包含记录即会命中。",
                                "&7按最近命中记录的备份时间倒序排列。"
                        )
                        .build();
            }

            @Override
            public void clicked(Player player,
                                int slot,
                                ClickType clickType,
                                int hotbarButton,
                                ItemStack currentItem) {
                // Information-only button.
            }
        }

        private final class LegitGemPageButton extends Button {
            private final int direction;

            private LegitGemPageButton(int direction) {
                this.direction = direction;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.ARROW)
                        .name(direction < 0 ? "&6上一页" : "&a下一页")
                        .build();
            }

            @Override
            public void clicked(Player player,
                                int slot,
                                ClickType clickType,
                                int hotbarButton,
                                ItemStack currentItem) {
                int next = page + direction;
                if (next >= 0 && next * ITEMS_PER_PAGE < items.size()) {
                    page = next;
                    openMenu(player);
                }
            }
        }
    }

    private final class LegitGemUsageMenu extends Menu {
        private final List<ScanMaterialUuidHistoryIndex.RecordedUsage> usages;
        private int page;

        private LegitGemUsageMenu(List<ScanMaterialUuidHistoryIndex.RecordedUsage> usages) {
            this.usages = usages == null ? Collections.emptyList() : new ArrayList<>(usages);
        }

        @Override
        public String getTitle(Player player) {
            return "宝石使用次数 (" + (page + 1) + ")";
        }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            int start = page * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, usages.size());
            for (int index = start; index < end; index++) {
                buttons.put(index - start, new LegitGemUsageButton(usages.get(index), index + 1));
            }
            if (page > 0) {
                buttons.put(45, new LegitGemUsagePageButton(-1));
            }
            buttons.put(49, new LegitGemUsageSummaryButton());
            if (end < usages.size()) {
                buttons.put(53, new LegitGemUsagePageButton(1));
            }
            return buttons;
        }

        @Override
        public int getSize() {
            return 54;
        }

        private final class LegitGemUsageButton extends Button {
            private final ScanMaterialUuidHistoryIndex.RecordedUsage usage;
            private final int rank;

            private LegitGemUsageButton(ScanMaterialUuidHistoryIndex.RecordedUsage usage, int rank) {
                this.usage = usage;
                this.rank = rank;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.PLAYER_HEAD)
                        .name("&e#" + rank + " &f" + usage.playerName)
                        .lore(
                                "&7累计使用次数: &a" + usage.uses,
                                "&7涉及装备 UUID: &f" + usage.itemUuids.size(),
                                "&7首次使用: &f" + formatTime(usage.firstUseTimestamp),
                                "&7最近使用: &f" + formatTime(usage.lastUseTimestamp),
                                "",
                                "&8已按重复 records 记录去重"
                        )
                        .build();
            }

            @Override
            public void clicked(Player player,
                                int slot,
                                ClickType clickType,
                                int hotbarButton,
                                ItemStack currentItem) {
                player.sendMessage(formatLegitGemUsageMessage(usage));
            }
        }

        private final class LegitGemUsageSummaryButton extends Button {
            @Override
            public ItemStack getButtonItem(Player player) {
                int totalUses = usages.stream().mapToInt(usage -> usage.uses).sum();
                int distinctItems = usages.stream()
                        .flatMap(usage -> usage.itemUuids.stream())
                        .collect(java.util.stream.Collectors.toSet())
                        .size();
                return new ItemBuilder(Material.EMERALD)
                        .name("&a遵纪守法宝石使用统计")
                        .lore(
                                "&7玩家数: &f" + usages.size(),
                                "&7累计使用次数: &a" + totalUses,
                                "&7涉及装备 UUID: &f" + distinctItems,
                                "",
                                "&7同一装备、玩家、时间戳完全相同的",
                                "&7重复 records 只计算一次。"
                        )
                        .build();
            }

            @Override
            public void clicked(Player player,
                                int slot,
                                ClickType clickType,
                                int hotbarButton,
                                ItemStack currentItem) {
                // Information-only button.
            }
        }

        private final class LegitGemUsagePageButton extends Button {
            private final int direction;

            private LegitGemUsagePageButton(int direction) {
                this.direction = direction;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.ARROW)
                        .name(direction < 0 ? "&6上一页" : "&a下一页")
                        .build();
            }

            @Override
            public void clicked(Player player,
                                int slot,
                                ClickType clickType,
                                int hotbarButton,
                                ItemStack currentItem) {
                int next = page + direction;
                if (next >= 0 && next * ITEMS_PER_PAGE < usages.size()) {
                    page = next;
                    openMenu(player);
                }
            }
        }
    }

    private final class UuidTraceHistoryMenu extends Menu {
        private final UuidEquipmentInfo equipment;
        private final Menu backMenu;
        private int page;
        private boolean loadingBackup;

        private UuidTraceHistoryMenu(UuidEquipmentInfo equipment,
                                     Menu backMenu) {
            this.equipment = equipment;
            this.backMenu = backMenu;
        }

        @Override
        public String getTitle(Player player) {
            return "UUID历史状态 (" + (page + 1) + ")";
        }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            int start = page * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, equipment.history.size());
            for (int index = start; index < end; index++) {
                buttons.put(index - start, new UuidHistoryButton(equipment.history.get(index)));
            }
            if (page > 0) {
                buttons.put(45, new UuidHistoryPageButton(-1));
            }
            buttons.put(49, new BackButton(backMenu));
            if (backMenu instanceof UuidTraceEquipmentMenu currentMenu && currentMenu.report.currentRareHistory
                    && equipment.currentItem != null && CurrentHistoryItemActions.canViewEnchantments(
                            equipment.currentItem.internalName, equipment.currentItem.uuid)) {
                buttons.put(48, new Button() {
                    @Override public ItemStack getButtonItem(Player player) {
                        return new ItemBuilder(Material.ENCHANTED_BOOK).name("&b查看该装备的历史附魔记录")
                                .lore("&7包含附魔台、打宝石等全部已记录的操作来源。",
                                        "&7按装备 UUID 查询全部操作者，不仅是当前持有人。",
                                        "&7仅读取已整理的本地 enchantrecords，未整理时会提示。",
                                        "&e点击查看，返回可回到当前历史页。").build();
                    }
                    @Override public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                        openEquipmentEnchantments(player, equipment, UuidTraceHistoryMenu.this);
                    }
                });
            }
            if (end < equipment.history.size()) {
                buttons.put(53, new UuidHistoryPageButton(1));
            }
            return buttons;
        }

        @Override
        public int getSize() {
            return 54;
        }

        private final class UuidHistoryButton extends Button {
            private final UuidTraceOccurrence occurrence;

            private UuidHistoryButton(UuidTraceOccurrence occurrence) {
                this.occurrence = occurrence;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                List<String> lore = new ArrayList<>();
                lore.add("&7持有人: &f" + safePlayerName(occurrence));
                lore.add("&7转生: &f" + occurrence.prestige);
                lore.add("&7首次备份: &f" + occurrence.time);
                lore.add("&7末次备份: &f" + occurrence.effectiveLastTime());
                lore.add("&7同玩家相同状态备份: &e" + occurrence.effectiveRepeatedBackups());
                lore.add("&7首次位置: &f" + occurrence.location);
                lore.add("&7末次位置: &f" + occurrence.effectiveLastLocation());
                lore.add("&7Mongo _id: &f" + valueOrUnknown(occurrence.backupDatabaseId));
                appendOriginalLore(lore, occurrence.lore, Integer.MAX_VALUE);
                lore.add("");
                lore.add("&bShift + 点击输出单行 JSON");
                lore.add("&e左键打开首次备份");
                lore.add("&e右键打开末次备份");
                return new ItemBuilder(materialOf(occurrence.material))
                        .name(occurrence.displayName == null
                                ? "&e" + occurrence.time + " - " + safePlayerName(occurrence)
                                : occurrence.displayName)
                        .lore(lore)
                        .build();
            }

            @Override
            public void clicked(Player player,
                                int slot,
                                ClickType clickType,
                                int hotbarButton,
                                ItemStack currentItem) {
                if (clickType.isShiftClick()) {
                    player.sendMessage(toDebugJson(occurrence));
                    return;
                }
                openBackup(player, occurrence, clickType.isRightClick());
            }
        }

        private void openBackup(Player player, UuidTraceOccurrence occurrence, boolean latest) {
            if (loadingBackup) {
                return;
            }
            loadingBackup = true;
            player.sendMessage("§7正在读取该 UUID 状态对应的背包备份...");
            String databaseId = latest
                    ? occurrence.effectiveLastDatabaseId()
                    : occurrence.backupDatabaseId;
            String backupUuid = latest
                    ? occurrence.effectiveLastBackupUuid()
                    : occurrence.backupUuid;
            String location = latest ? occurrence.effectiveLastLocation() : occurrence.location;
            Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
                PlayerProfile profile = null;
                PlayerInvBackup backup = null;
                Exception failure = null;
                try {
                    profile = ThePit.getInstance().getMongoDB().getProfileCollection()
                            .find(Filters.eq("uuid", occurrence.playerUuid))
                            .first();
                    if (databaseId != null && ObjectId.isValid(databaseId)) {
                        backup = ThePit.getInstance().getMongoDB().getInvCollection()
                                .find(Filters.eq("_id", new ObjectId(databaseId)))
                                .first();
                    }
                    if (backup == null && backupUuid != null) {
                        backup = ThePit.getInstance().getMongoDB().getInvCollection()
                                .find(Filters.eq("backupUuid", backupUuid))
                                .first();
                    }
                } catch (Exception exception) {
                    failure = exception;
                }
                PlayerProfile loadedProfile = profile;
                PlayerInvBackup loadedBackup = backup;
                Exception loadedFailure = failure;
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    loadingBackup = false;
                    if (!player.isOnline()) {
                        return;
                    }
                    if (loadedFailure != null || loadedProfile == null || loadedBackup == null) {
                        ThePit.getInstance().getSLF4JLogger().error(
                                "Failed to load uuidtrace backup " + databaseId, loadedFailure);
                        player.sendMessage("§c读取对应背包备份失败，请查看服务器日志。");
                        return;
                    }
                    new BackupShowMenu(
                            loadedProfile,
                            loadedBackup,
                            location != null && location.startsWith("warehouse"),
                            warehousePageForLocation(loadedBackup, location),
                            UuidTraceHistoryMenu.this
                    ).openMenu(player);
                });
            });
        }

        private final class UuidHistoryPageButton extends Button {
            private final int direction;

            private UuidHistoryPageButton(int direction) {
                this.direction = direction;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.ARROW)
                        .name(direction < 0 ? "&6上一页" : "&a下一页")
                        .build();
            }

            @Override
            public void clicked(Player player,
                                int slot,
                                ClickType clickType,
                                int hotbarButton,
                                ItemStack currentItem) {
                int next = page + direction;
                if (next >= 0 && next * ITEMS_PER_PAGE < equipment.history.size()) {
                    page = next;
                    openMenu(player);
                }
            }
        }
    }

    private static Material materialOf(String materialName) {
        if (materialName != null) {
            Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
            if (material != null && !material.isAir()) {
                return material;
            }
        }
        return Material.PAPER;
    }

    private static int warehousePageForLocation(PlayerInvBackup backup, String location) {
        if (backup == null || location == null || !location.startsWith("warehouse[")) {
            return 0;
        }
        try {
            int chestEnd = location.indexOf(']', "warehouse[".length());
            int slotStart = location.indexOf('[', chestEnd + 1);
            int slotEnd = location.indexOf(']', slotStart + 1);
            int chestNumber = Integer.parseInt(location.substring("warehouse[".length(), chestEnd));
            int targetSlot = Integer.parseInt(location.substring(slotStart + 1, slotEnd));
            PlayerEnderChest[] chests = backup.getChest();
            if (chests == null || chestNumber < 1 || chestNumber > chests.length) {
                return 0;
            }
            int flattenedSlot = targetSlot;
            for (int index = 0; index < chestNumber - 1; index++) {
                PlayerEnderChest chest = chests[index];
                if (chest != null && chest.getInventory() != null) {
                    flattenedSlot += chest.getInventory().getContents().length;
                }
            }
            return Math.max(0, flattenedSlot / ITEMS_PER_PAGE);
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static String displayItemName(CurrentUuidItem item, String uuid) {
        if (item != null && item.displayName != null && !item.displayName.isBlank()) {
            return item.displayName;
        }
        if (item != null && item.internalName != null) {
            return "&e" + item.internalName;
        }
        return "&eUUID物品 " + uuid;
    }

    private static ScanMaterialUuidHistoryIndex.IndexedItem representativeIndexedItem(
            ScanMaterialUuidHistoryIndex.UuidHistoryEvent event) {
        if (event == null || event.items == null || event.items.isEmpty()) {
            return null;
        }
        return event.items.get(0);
    }

    private static String indexedDisplayItemName(ScanMaterialUuidHistoryIndex.IndexedItem item,
                                                 String uuid) {
        if (item != null && item.displayName != null && !item.displayName.isBlank()) {
            return item.displayName;
        }
        if (item != null && item.internalName != null && !item.internalName.isBlank()) {
            return "&e" + item.internalName;
        }
        return "&eUUID物品 " + uuid;
    }

    private static String indexedPlayerName(ScanMaterialUuidHistoryIndex.UuidHistoryEvent event) {
        if (event == null) {
            return "<unknown>";
        }
        return event.playerName == null || event.playerName.isBlank()
                ? valueOrUnknown(event.playerUuid)
                : event.playerName;
    }

    private static String indexedLocations(ScanMaterialUuidHistoryIndex.UuidHistoryEvent event) {
        if (event == null || event.items == null || event.items.isEmpty()) {
            return "<unknown>";
        }
        return event.items.stream()
                .map(item -> item == null ? null : item.location)
                .filter(Objects::nonNull)
                .filter(location -> !location.isBlank())
                .distinct()
                .sorted()
                .reduce((first, second) -> first + ", " + second)
                .orElse("<unknown>");
    }

    private static String toDebugJson(UuidTraceOccurrence occurrence) {
        JsonObject result = COMPACT_GSON.toJsonTree(occurrence).getAsJsonObject();
        result.add("nbt", parseNbtJson(occurrence.nbt));
        result.addProperty("comparisonStateHash", ScanMaterialUuidHistoryIndex.comparisonStateHash(
                occurrence.material,
                occurrence.internalName,
                occurrence.displayName,
                occurrence.amount,
                occurrence.lore,
                occurrence.nbt
        ));
        return COMPACT_GSON.toJson(result);
    }

    private static JsonElement parseNbtJson(String snbt) {
        if (snbt == null || snbt.isBlank()) {
            return com.google.gson.JsonNull.INSTANCE;
        }
        try {
            return nbtTagToJson(TagParser.parseTag(snbt));
        } catch (Exception exception) {
            JsonObject failure = new JsonObject();
            failure.addProperty("parseError", exception.getMessage());
            failure.addProperty("rawSnbt", snbt);
            return failure;
        }
    }

    private static List<LegitGemRecordInfo> readLegitGemRecords(String snbt) {
        if (snbt == null || snbt.isBlank()) {
            return Collections.emptyList();
        }
        try {
            CompoundTag root = TagParser.parseTag(snbt);
            CompoundTag extra = root.getCompound("extra");
            String serialized = extra.getString("records");
            if (serialized.isBlank()) {
                return Collections.emptyList();
            }
            List<LegitGemRecordInfo> result = new ArrayList<>();
            for (String entry : serialized.split(";")) {
                String[] fields = entry.split("\\|", -1);
                if (fields.length < 3 || !"totally_legit_gem".equals(fields[1])) {
                    continue;
                }
                try {
                    result.add(new LegitGemRecordInfo(fields[0], Long.parseLong(fields[2])));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed historical records while keeping the item in the result list.
                }
            }
            result.sort(Comparator.comparingLong(LegitGemRecordInfo::timestamp));
            return result;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private static JsonElement nbtTagToJson(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            JsonObject object = new JsonObject();
            for (String key : compound.getAllKeys()) {
                Tag value = compound.get(key);
                object.add(key, value == null
                        ? com.google.gson.JsonNull.INSTANCE
                        : nbtTagToJson(value));
            }
            return object;
        }
        if (tag instanceof ListTag list) {
            JsonArray array = new JsonArray();
            for (Tag value : list) {
                array.add(nbtTagToJson(value));
            }
            return array;
        }
        if (tag instanceof ByteArrayTag bytes) {
            JsonArray array = new JsonArray();
            for (byte value : bytes.getAsByteArray()) {
                array.add(value);
            }
            return array;
        }
        if (tag instanceof IntArrayTag integers) {
            JsonArray array = new JsonArray();
            for (int value : integers.getAsIntArray()) {
                array.add(value);
            }
            return array;
        }
        if (tag instanceof LongArrayTag longs) {
            JsonArray array = new JsonArray();
            for (long value : longs.getAsLongArray()) {
                array.add(value);
            }
            return array;
        }
        if (tag instanceof NumericTag number) {
            return new JsonPrimitive(number.getAsNumber());
        }
        if (tag instanceof StringTag string) {
            return new JsonPrimitive(string.getAsString());
        }
        return new JsonPrimitive(tag.getAsString());
    }

    private static String safePlayerName(UuidTraceOccurrence occurrence) {
        return occurrence.playerName == null || occurrence.playerName.isBlank()
                ? valueOrUnknown(occurrence.playerUuid)
                : occurrence.playerName;
    }

    private static void appendOriginalLore(List<String> destination,
                                           List<String> originalLore,
                                           int maximumLines) {
        if (originalLore == null || originalLore.isEmpty()) {
            return;
        }
        destination.add("");
        destination.add("&8原始 Lore:");
        int limit = Math.min(maximumLines, originalLore.size());
        for (int index = 0; index < limit; index++) {
            destination.add(originalLore.get(index));
        }
        if (originalLore.size() > limit) {
            destination.add("&8... 另有 " + (originalLore.size() - limit) + " 行，完整内容见 JSON");
        }
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "<unknown>" : value;
    }

    private record LegitGemRecordInfo(String playerName, long timestamp) {
    }

    private static final class StackedUuidItem {
        private final String location;
        private final String internalName;
        private final String material;
        private final int amount;
        private final String uuid;

        private StackedUuidItem(String location,
                                String internalName,
                                String material,
                                int amount,
                                String uuid) {
            this.location = location;
            this.internalName = internalName;
            this.material = material;
            this.amount = amount;
            this.uuid = uuid;
        }
    }

    private static final class Report {
        private final int reportVersion;
        private final boolean allAdvancedItems;
        private final boolean includesItemsWithoutUuid;
        private final long generatedAtTimestamp;
        private final String generatedAt;
        private final int minimumItems;
        private final Map<String, String> trackedItems;
        private final int scannedBackups;
        private final int matchedBackups;
        private final int matchedPlayers;
        private final int scanErrors;
        private final int maximumItems;
        private final int matchedUuidStackBackups;
        private final int stackedUuidStacks;
        private final List<MaterialSnapshot> snapshots;

        private Report(long generatedAtTimestamp,
                       String generatedAt,
                       int minimumItems,
                       Map<String, String> trackedItems,
                       int scannedBackups,
                       int scanErrors,
                       int matchedPlayers,
                       int maximumItems,
                       int matchedUuidStackBackups,
                       int stackedUuidStacks,
                       List<MaterialSnapshot> snapshots) {
            this.reportVersion = 2;
            this.allAdvancedItems = true;
            this.includesItemsWithoutUuid = true;
            this.generatedAtTimestamp = generatedAtTimestamp;
            this.generatedAt = generatedAt;
            this.minimumItems = minimumItems;
            this.trackedItems = new LinkedHashMap<>(trackedItems);
            this.scannedBackups = scannedBackups;
            this.matchedBackups = snapshots.size();
            this.matchedPlayers = matchedPlayers;
            this.scanErrors = scanErrors;
            this.maximumItems = maximumItems;
            this.matchedUuidStackBackups = matchedUuidStackBackups;
            this.stackedUuidStacks = stackedUuidStacks;
            this.snapshots = new ArrayList<>(snapshots);
        }
    }

    private static final class MaterialSnapshot {
        private final String databaseId;
        private final String backupUuid;
        private final String playerUuid;
        private final String playerName;
        private final int prestige;
        private final long timestamp;
        private final String time;
        private final int inventoryItems;
        private final int warehouseItems;
        private final int totalItems;
        private final boolean rareMaterialMatch;
        private final boolean uuidStackMatch;
        private final int stackedUuidStackCount;
        private final int stackedUuidItemAmount;
        private final Map<String, Integer> inventoryCounts;
        private final Map<String, Integer> warehouseCounts;
        private final Map<String, Integer> totalCounts;
        private final List<StackedUuidItem> stackedUuidItems;

        private MaterialSnapshot(String databaseId,
                                 String backupUuid,
                                 String playerUuid,
                                 String playerName,
                                 int prestige,
                                 long timestamp,
                                 String time,
                                 int inventoryItems,
                                 int warehouseItems,
                                 int totalItems,
                                 boolean rareMaterialMatch,
                                 boolean uuidStackMatch,
                                 int stackedUuidStackCount,
                                 int stackedUuidItemAmount,
                                 Map<String, Integer> inventoryCounts,
                                 Map<String, Integer> warehouseCounts,
                                 Map<String, Integer> totalCounts,
                                 List<StackedUuidItem> stackedUuidItems) {
            this.databaseId = databaseId;
            this.backupUuid = backupUuid;
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.prestige = prestige;
            this.timestamp = timestamp;
            this.time = time;
            this.inventoryItems = inventoryItems;
            this.warehouseItems = warehouseItems;
            this.totalItems = totalItems;
            this.rareMaterialMatch = rareMaterialMatch;
            this.uuidStackMatch = uuidStackMatch;
            this.stackedUuidStackCount = stackedUuidStackCount;
            this.stackedUuidItemAmount = stackedUuidItemAmount;
            this.inventoryCounts = inventoryCounts;
            this.warehouseCounts = warehouseCounts;
            this.totalCounts = totalCounts;
            this.stackedUuidItems = new ArrayList<>(stackedUuidItems);
        }
    }

    private static final class MatchedPlayerInfo {
        private final String playerUuid;
        private final String playerName;
        private final long registerTime;
        private final int level;
        private final int prestige;
        private final long totalEnchantments;
        private final long kills;
        private final int matchedBackups;
        private final int maximumRareItems;
        private final int uuidStackBackups;
        private final int uuidStackGroups;
        private final List<MaterialSnapshot> snapshots;

        private MatchedPlayerInfo(String playerUuid,
                                  String playerName,
                                  long registerTime,
                                  int level,
                                  int prestige,
                                  long totalEnchantments,
                                  long kills,
                                  int matchedBackups,
                                  int maximumRareItems,
                                  int uuidStackBackups,
                                  int uuidStackGroups,
                                  List<MaterialSnapshot> snapshots) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.registerTime = registerTime;
            this.level = level;
            this.prestige = prestige;
            this.totalEnchantments = totalEnchantments;
            this.kills = kills;
            this.matchedBackups = matchedBackups;
            this.maximumRareItems = maximumRareItems;
            this.uuidStackBackups = uuidStackBackups;
            this.uuidStackGroups = uuidStackGroups;
            this.snapshots = new ArrayList<>(snapshots);
        }
    }

    private static final class MaterialHistoryPlayerMenu extends Menu {
        private final List<MatchedPlayerInfo> players;
        private final List<String> itemFilters = new ArrayList<>(TRACKED_ITEMS.keySet());
        private int page;
        private int filterIndex = -1;
        private boolean loadingPlayer;

        private MaterialHistoryPlayerMenu(List<MatchedPlayerInfo> players) {
            this.players = new ArrayList<>(players);
        }

        @Override
        public String getTitle(Player player) {
            String filter = getSelectedItem();
            String filterName = filter == null ? "全部" : TRACKED_ITEMS.getOrDefault(filter, filter);
            return "物资玩家-" + filterName + " (" + (page + 1) + ")";
        }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            List<MatchedPlayerInfo> visiblePlayers = getVisiblePlayers();
            int start = page * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, visiblePlayers.size());
            for (int index = start; index < end; index++) {
                buttons.put(index - start, new PlayerButton(visiblePlayers.get(index)));
            }
            if (page > 0) {
                buttons.put(45, new PlayerPageButton(-1));
            }
            if (end < visiblePlayers.size()) {
                buttons.put(53, new PlayerPageButton(1));
            }
            buttons.put(49, new ItemFilterButton());
            return buttons;
        }

        @Override
        public int getSize() {
            return 54;
        }

        private final class PlayerButton extends Button {
            private final MatchedPlayerInfo info;

            private PlayerButton(MatchedPlayerInfo info) {
                this.info = info;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                String registerTime = info.registerTime > 0L ? formatTime(info.registerTime) : "<unknown>";
                String selectedItem = getSelectedItem();
                int selectedMaximum = maximumItemCount(info, selectedItem);
                String amountLine = selectedItem == null
                        ? "&7单个快照最高指定物资: &f" + selectedMaximum
                        : "&7单个快照最高" + TRACKED_ITEMS.getOrDefault(selectedItem, selectedItem) + ": &f" + selectedMaximum;
                return new ItemBuilder(Material.PLAYER_HEAD)
                        .name("&e" + info.playerName)
                        .lore(
                                "&7UUID: &f" + info.playerUuid,
                                "",
                                "&7数据创建时间: &f" + registerTime,
                                "&7等级: &e" + displayNumber(info.level),
                                "&7转生次数: &e" + displayNumber(info.prestige),
                                "&7累计附魔次数: &d" + info.totalEnchantments,
                                "&7累计击杀次数: &c" + info.kills,
                                "",
                                "&7命中历史备份: &f" + info.matchedBackups,
                                amountLine,
                                "&7UUID 异常备份/物品栈: &f" + info.uuidStackBackups + "/" + info.uuidStackGroups,
                                "",
                                "&e点击查看该玩家的命中备份"
                        )
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                openPlayer(player, info);
            }
        }

        private void openPlayer(Player player, MatchedPlayerInfo info) {
            if (loadingPlayer) {
                return;
            }
            loadingPlayer = true;
            player.sendMessage("§7正在读取 " + info.playerName + " 的完整档案...");
            Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
                PlayerProfile profile = null;
                Exception failure = null;
                try {
                    profile = ThePit.getInstance()
                            .getMongoDB()
                            .getProfileCollection()
                            .find(Filters.eq("uuid", info.playerUuid))
                            .first();
                } catch (Exception exception) {
                    failure = exception;
                }

                PlayerProfile result = profile;
                Exception resultFailure = failure;
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    loadingPlayer = false;
                    if (!player.isOnline()) {
                        return;
                    }
                    if (resultFailure != null || result == null) {
                        ThePit.getInstance().getSLF4JLogger().error(
                                "Failed to load materialhistory player " + info.playerUuid,
                                resultFailure);
                        player.sendMessage("§c读取玩家完整档案失败，请查看服务器日志。");
                        return;
                    }
                    List<MaterialSnapshot> snapshots = getVisibleSnapshots(info);
                    new MaterialHistoryMenu(
                            result,
                            snapshots,
                            MaterialHistoryPlayerMenu.this,
                            info
                    ).openMenu(player);
                });
            });
        }

        private void openAdjacentPlayer(Player player, MatchedPlayerInfo current, int direction) {
            List<MatchedPlayerInfo> visiblePlayers = getVisiblePlayers();
            int currentIndex = visiblePlayers.indexOf(current);
            int nextIndex = currentIndex + direction;
            if (currentIndex < 0 || nextIndex < 0 || nextIndex >= visiblePlayers.size()) {
                return;
            }
            openPlayer(player, visiblePlayers.get(nextIndex));
        }

        private boolean hasAdjacentPlayer(MatchedPlayerInfo current, int direction) {
            List<MatchedPlayerInfo> visiblePlayers = getVisiblePlayers();
            int currentIndex = visiblePlayers.indexOf(current);
            int nextIndex = currentIndex + direction;
            return currentIndex >= 0 && nextIndex >= 0 && nextIndex < visiblePlayers.size();
        }

        private List<MatchedPlayerInfo> getVisiblePlayers() {
            String selectedItem = getSelectedItem();
            return players.stream()
                    .filter(info -> selectedItem == null || maximumItemCount(info, selectedItem) > 0)
                    .sorted(Comparator
                            .comparingInt((MatchedPlayerInfo info) -> maximumItemCount(info, selectedItem))
                            .reversed()
                            .thenComparing(info -> info.playerName,
                                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                            .thenComparing(info -> info.playerUuid))
                    .toList();
        }

        private List<MaterialSnapshot> getVisibleSnapshots(MatchedPlayerInfo info) {
            String selectedItem = getSelectedItem();
            return info.snapshots.stream()
                    .filter(snapshot -> selectedItem == null || itemCount(snapshot, selectedItem) > 0)
                    .sorted(Comparator.comparingLong(
                            (MaterialSnapshot snapshot) -> snapshot.timestamp).reversed())
                    .toList();
        }

        private String getSelectedItem() {
            return filterIndex < 0 ? null : itemFilters.get(filterIndex);
        }

        private static int maximumItemCount(MatchedPlayerInfo info, String internalName) {
            if (internalName == null) {
                return info.maximumRareItems;
            }
            return info.snapshots.stream()
                    .mapToInt(snapshot -> itemCount(snapshot, internalName))
                    .max()
                    .orElse(0);
        }

        private static int itemCount(MaterialSnapshot snapshot, String internalName) {
            if (snapshot.totalCounts == null) {
                return 0;
            }
            return snapshot.totalCounts.getOrDefault(internalName, 0);
        }

        private final class ItemFilterButton extends Button {
            @Override
            public ItemStack getButtonItem(Player player) {
                String selectedItem = getSelectedItem();
                String displayName = selectedItem == null
                        ? "全部物资"
                        : TRACKED_ITEMS.getOrDefault(selectedItem, selectedItem);
                return new ItemBuilder(Material.HOPPER)
                        .name("&b物品类型筛选")
                        .lore(
                                "&7当前: &e" + displayName,
                                "&7符合玩家: &f" + getVisiblePlayers().size(),
                                "",
                                "&e左键下一个，右键上一个",
                                "&7切换后按该物品最高数量排序"
                        )
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                int optionCount = itemFilters.size() + 1;
                int option = filterIndex + 1;
                option = Math.floorMod(option + (clickType.isRightClick() ? -1 : 1), optionCount);
                filterIndex = option - 1;
                page = 0;
                openMenu(player);
            }
        }

        private final class PlayerPageButton extends Button {
            private final int direction;

            private PlayerPageButton(int direction) {
                this.direction = direction;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.ARROW)
                        .name(direction < 0 ? "&6上一页" : "&a下一页")
                        .lore("&7当前页: &f" + (page + 1))
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                List<MatchedPlayerInfo> visiblePlayers = getVisiblePlayers();
                int nextPage = page + direction;
                if (nextPage < 0 || nextPage * ITEMS_PER_PAGE >= visiblePlayers.size()) {
                    return;
                }
                page = nextPage;
                openMenu(player);
            }
        }

        private static String displayNumber(int value) {
            return value < 0 ? "<unknown>" : Integer.toString(value);
        }
    }

    private static final class MaterialHistoryMenu extends Menu {
        private final PlayerProfile targetProfile;
        private final List<MaterialSnapshot> snapshots;
        private final Menu backMenu;
        private final MaterialHistoryPlayerMenu playerMenu;
        private final MatchedPlayerInfo currentPlayer;
        private int page;
        private boolean loadingBackup;

        private MaterialHistoryMenu(PlayerProfile targetProfile, List<MaterialSnapshot> snapshots) {
            this(targetProfile, snapshots, null, null);
        }

        private MaterialHistoryMenu(PlayerProfile targetProfile,
                                    List<MaterialSnapshot> snapshots,
                                    MaterialHistoryPlayerMenu playerMenu,
                                    MatchedPlayerInfo currentPlayer) {
            this.targetProfile = targetProfile;
            this.snapshots = new ArrayList<>(snapshots);
            this.backMenu = playerMenu;
            this.playerMenu = playerMenu;
            this.currentPlayer = currentPlayer;
        }

        @Override
        public String getTitle(Player player) {
            return targetProfile.getPlayerName() + " 的稀有物资备份 (" + (page + 1) + ")";
        }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            int start = page * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, snapshots.size());
            for (int index = start; index < end; index++) {
                buttons.put(index - start, new SnapshotButton(snapshots.get(index), index));
            }
            if (page > 0) {
                buttons.put(45, new PageButton(-1));
            }
            if (end < snapshots.size()) {
                buttons.put(53, new PageButton(1));
            }
            if (backMenu != null) {
                buttons.put(49, new BackButton(backMenu));
            }
            if (playerMenu != null && currentPlayer != null) {
                if (playerMenu.hasAdjacentPlayer(currentPlayer, -1)) {
                    buttons.put(46, new AdjacentPlayerButton(-1));
                }
                if (playerMenu.hasAdjacentPlayer(currentPlayer, 1)) {
                    buttons.put(52, new AdjacentPlayerButton(1));
                }
            }
            return buttons;
        }

        @Override
        public int getSize() {
            return 54;
        }

        private final class SnapshotButton extends Button {
            private final MaterialSnapshot snapshot;
            private final int snapshotIndex;

            private SnapshotButton(MaterialSnapshot snapshot, int snapshotIndex) {
                this.snapshot = snapshot;
                this.snapshotIndex = snapshotIndex;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                List<String> lore = new ArrayList<>();
                lore.add("&7Mongo _id: &f" + valueOrUnknown(snapshot.databaseId));
                lore.add("&7备份 UUID: &f" + valueOrUnknown(snapshot.backupUuid));
                lore.add("&7合计: &e" + snapshot.totalItems
                        + " &7(背包 &f" + snapshot.inventoryItems + "&7 / 仓库 &f" + snapshot.warehouseItems + "&7)");
                if (snapshot.totalCounts != null && !snapshot.totalCounts.isEmpty()) {
                    lore.add("");
                    snapshot.totalCounts.forEach((name, amount) -> lore.add(
                            "&f" + TRACKED_ITEMS.getOrDefault(name, name) + ": &e" + amount));
                }
                if (snapshot.stackedUuidItems != null && !snapshot.stackedUuidItems.isEmpty()) {
                    lore.add("");
                    lore.add("&cUUID 异常堆叠: &f" + snapshot.stackedUuidStackCount
                            + " 组 / " + snapshot.stackedUuidItemAmount + " 件");
                    int displayLimit = Math.min(8, snapshot.stackedUuidItems.size());
                    for (int index = 0; index < displayLimit; index++) {
                        StackedUuidItem stacked = snapshot.stackedUuidItems.get(index);
                        String name = stacked.internalName == null ? stacked.material : stacked.internalName;
                        lore.add("&c- &f" + name + " &ex" + stacked.amount + " &7@ " + stacked.location);
                        lore.add("  &8UUID: " + stacked.uuid);
                    }
                    if (snapshot.stackedUuidItems.size() > displayLimit) {
                        lore.add("&7另有 " + (snapshot.stackedUuidItems.size() - displayLimit) + " 组，详见本地日志");
                    }
                }
                lore.add("");
                lore.add("&7左键查看背包，右键查看仓库");
                return new ItemBuilder(Material.CHEST)
                        .name("&6" + snapshot.time + " &7- &e" + snapshot.totalItems
                                + " 物资 / &c" + snapshot.stackedUuidStackCount + " UUID堆叠")
                        .lore(lore)
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                openBackup(player, snapshotIndex, clickType.isRightClick());
            }
        }

        private void openBackup(Player player, int snapshotIndex, boolean warehouse) {
            if (snapshotIndex < 0 || snapshotIndex >= snapshots.size()) {
                return;
            }
            MaterialSnapshot snapshot = snapshots.get(snapshotIndex);
            if (loadingBackup) {
                return;
            }
            loadingBackup = true;
            player.sendMessage("§7正在按数据库 ID 读取该背包备份...");
            Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
                    PlayerInvBackup backup = null;
                    Exception failure = null;
                    try {
                        if (snapshot.databaseId != null && ObjectId.isValid(snapshot.databaseId)) {
                            backup = ThePit.getInstance()
                                    .getMongoDB()
                                    .getInvCollection()
                                    .find(Filters.eq("_id", new ObjectId(snapshot.databaseId)))
                                    .first();
                        }
                        if (backup == null && snapshot.backupUuid != null) {
                            backup = ThePit.getInstance()
                                    .getMongoDB()
                                    .getInvCollection()
                                    .find(Filters.eq("backupUuid", snapshot.backupUuid))
                                    .first();
                        }
                    } catch (Exception exception) {
                        failure = exception;
                    }

                    PlayerInvBackup result = backup;
                    Exception resultFailure = failure;
                    Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                        loadingBackup = false;
                        if (!player.isOnline()) {
                            return;
                        }
                        if (resultFailure != null || result == null) {
                            ThePit.getInstance().getSLF4JLogger().error(
                                    "Failed to load materialhistory backup " + snapshot.databaseId,
                                    resultFailure);
                            player.sendMessage("§c读取该背包备份失败，请查看服务器日志。");
                            return;
                        }
                        new BackupShowMenu(
                                targetProfile,
                                result,
                                warehouse,
                                MaterialHistoryMenu.this,
                                snapshotIndex > 0 ? new AdjacentBackupButton(snapshotIndex - 1, -1) : null,
                                snapshotIndex + 1 < snapshots.size()
                                        ? new AdjacentBackupButton(snapshotIndex + 1, 1)
                                        : null
                        ).openMenu(player);
                    });
            });
        }

        private final class AdjacentBackupButton extends Button {
            private final int targetIndex;
            private final int direction;

            private AdjacentBackupButton(int targetIndex, int direction) {
                this.targetIndex = targetIndex;
                this.direction = direction;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                MaterialSnapshot target = snapshots.get(targetIndex);
                return new ItemBuilder(Material.ARROW)
                        .name(direction < 0 ? "&6上一个备份" : "&a下一个备份")
                        .lore(
                                "&7目标时间: &f" + target.time,
                                "&7切换后保持当前背包/仓库视图"
                        )
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                Menu openedMenu = Menu.currentlyOpenedMenus.get(player.getUniqueId());
                boolean warehouse = openedMenu instanceof BackupShowMenu backupMenu
                        && backupMenu.isWarehouseView();
                openBackup(player, targetIndex, warehouse);
            }
        }

        private final class AdjacentPlayerButton extends Button {
            private final int direction;

            private AdjacentPlayerButton(int direction) {
                this.direction = direction;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.PLAYER_HEAD)
                        .name(direction < 0 ? "&6上一个玩家" : "&a下一个玩家")
                        .lore("&7按照当前物品筛选和数量排序切换")
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                playerMenu.openAdjacentPlayer(player, currentPlayer, direction);
            }
        }

        private final class PageButton extends Button {
            private final int direction;

            private PageButton(int direction) {
                this.direction = direction;
            }

            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.ARROW)
                        .name(direction < 0 ? "&6上一页" : "&a下一页")
                        .lore("&7当前页: &f" + (page + 1))
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                int nextPage = page + direction;
                if (nextPage < 0 || nextPage * ITEMS_PER_PAGE >= snapshots.size()) {
                    return;
                }
                page = nextPage;
                openMenu(player);
            }
        }

        private static String valueOrUnknown(String value) {
            return value == null || value.isBlank() ? "<unknown>" : value;
        }
    }
}
