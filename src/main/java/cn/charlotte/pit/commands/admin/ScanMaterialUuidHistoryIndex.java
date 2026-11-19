package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerInvBackup;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.util.item.ItemUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import de.tr7zw.nbtapi.NBT;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/** Builds a reusable, sharded UUID-item history index from a materialhistory report. */
final class ScanMaterialUuidHistoryIndex {

    private static final int DATABASE_BATCH_SIZE = 500;
    private static final int EVENT_BUFFER_SIZE = 16;
    private static final int SHARD_MASK = 0x3ff;
    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private static final Gson LINE_GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final ThreadLocal<MessageDigest> SHA_256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    });

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger scannedBackups = new AtomicInteger();
    private final AtomicInteger loadedBackups = new AtomicInteger();
    private final AtomicInteger scannedItemStacks = new AtomicInteger();
    private final AtomicInteger historyEvents = new AtomicInteger();
    private final AtomicInteger scanErrors = new AtomicInteger();

    private volatile long startedAt = -1L;
    private volatile String lastIndexPath;
    private volatile String lastError;
    private volatile int lastUniqueUuids;
    private volatile IndexManifest cachedManifest;
    private volatile Path cachedIndexDirectory;

    void start(CommandSender sender, ScanRareMaterialHistory materialHistory) {
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage("已有 materialhistory UUID 索引任务正在运行，请使用 /scan materialhistory uuidindex status。");
            return;
        }
        startedAt = System.currentTimeMillis();
        scannedBackups.set(0);
        loadedBackups.set(0);
        scannedItemStacks.set(0);
        historyEvents.set(0);
        scanErrors.set(0);
        lastUniqueUuids = 0;
        lastError = null;
        sender.sendMessage("已开始异步整理最近完成的 materialhistory / all 报告中的 UUID 物品历史；生成索引会读取数据库备份。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> execute(sender, materialHistory));
    }

    void status(CommandSender sender) {
        if (running.get()) {
            sender.sendMessage("materialhistory UUID 索引运行中。");
            sender.sendMessage("已扫描备份: " + scannedBackups.get()
                    + "，成功读取: " + loadedBackups.get()
                    + "，物品堆: " + scannedItemStacks.get()
                    + "，历史节点: " + historyEvents.get()
                    + "，异常: " + scanErrors.get());
            sender.sendMessage("运行时长: " + formatDuration(System.currentTimeMillis() - startedAt));
            return;
        }
        sender.sendMessage("当前没有运行中的 materialhistory UUID 索引任务。");
        if (lastIndexPath != null) {
            sender.sendMessage("最近索引: " + lastIndexPath);
            sender.sendMessage("唯一 UUID: " + lastUniqueUuids + "，历史节点: " + historyEvents.get());
        }
        if (lastError != null) {
            sender.sendMessage("最近一次错误: " + lastError);
        }
    }

    private void execute(CommandSender sender, ScanRareMaterialHistory materialHistory) {
        Path indexDirectory = null;
        try {
            List<MaterialHistoryBackupReference> references = materialHistory.getMaterialHistoryBackupReferences();
            if (references.isEmpty()) {
                throw new IllegalStateException("没有可用的 materialhistory 数据，请先运行 /scan materialhistory 或 /scan materialhistory all");
            }

            indexDirectory = ScanReportPath.directory("materialhistory").resolve(
                    "uuid-history-index-" + LocalDateTime.now().format(FILE_NAME_TIME));
            Files.createDirectories(indexDirectory);

            Map<String, LastState> lastStates = new HashMap<>();
            Map<String, List<UuidHistoryEvent>> eventBuffers = new HashMap<>();
            Set<String> writtenShards = new HashSet<>();

            for (int start = 0; start < references.size(); start += DATABASE_BATCH_SIZE) {
                List<MaterialHistoryBackupReference> batch = references.subList(
                        start,
                        Math.min(start + DATABASE_BATCH_SIZE, references.size())
                );
                Map<String, PlayerInvBackup> backups = loadBackups(batch);
                for (MaterialHistoryBackupReference reference : batch) {
                    int scanned = scannedBackups.incrementAndGet();
                    PlayerInvBackup backup = findBackup(backups, reference);
                    if (backup == null) {
                        scanErrors.incrementAndGet();
                        continue;
                    }
                    loadedBackups.incrementAndGet();
                    try {
                        Map<String, UuidBackupState> states = extractStates(backup);
                        List<String> itemUuids = new ArrayList<>(states.keySet());
                        itemUuids.sort(String::compareTo);
                        for (String itemUuid : itemUuids) {
                            UuidBackupState state = states.get(itemUuid);
                            String stateHash = state.stateHash();
                            LastState previous = lastStates.get(itemUuid);
                            if (previous != null
                                    && Objects.equals(previous.playerUuid, reference.playerUuid())
                                    && Objects.equals(previous.stateHash, stateHash)) {
                                continue;
                            }
                            UuidHistoryEvent event = new UuidHistoryEvent(reference, itemUuid, stateHash, state.items);
                            bufferEvent(indexDirectory, eventBuffers, writtenShards, event);
                            lastStates.put(itemUuid, new LastState(reference.playerUuid(), stateHash));
                            historyEvents.incrementAndGet();
                        }
                    } catch (Exception exception) {
                        scanErrors.incrementAndGet();
                        ThePit.getInstance().getSLF4JLogger().warn(
                                "scan materialhistory uuidindex failed to scan backup {}",
                                reference.databaseId(), exception);
                    }
                    if (scanned % 1_000 == 0) {
                        ThePit.getInstance().info("scan materialhistory uuidindex progress: scannedBackups=" + scanned
                                + ", loadedBackups=" + loadedBackups.get()
                                + ", uniqueUuids=" + lastStates.size()
                                + ", historyEvents=" + historyEvents.get()
                                + ", scanErrors=" + scanErrors.get());
                    }
                }
            }
            flushAll(indexDirectory, eventBuffers, writtenShards);

            lastUniqueUuids = lastStates.size();
            IndexManifest manifest = new IndexManifest(
                    2,
                    System.currentTimeMillis(),
                    materialHistory.getMaterialHistoryReportPath(),
                    references.size(),
                    loadedBackups.get(),
                    scannedItemStacks.get(),
                    lastUniqueUuids,
                    historyEvents.get(),
                    scanErrors.get(),
                    writtenShards.stream().sorted().toList()
            );
            Path manifestPath = indexDirectory.resolve("manifest.json");
            try (Writer writer = Files.newBufferedWriter(
                    manifestPath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW)) {
                GSON.toJson(manifest, writer);
            }
            cachedManifest = manifest;
            cachedIndexDirectory = indexDirectory;
            lastIndexPath = indexDirectory.toAbsolutePath().toString();

            ThePit.getInstance().info("scan materialhistory uuidindex finished: candidateBackups=" + references.size()
                    + ", loadedBackups=" + loadedBackups.get()
                    + ", scannedItems=" + scannedItemStacks.get()
                    + ", uniqueUuids=" + lastUniqueUuids
                    + ", historyEvents=" + historyEvents.get()
                    + ", scanErrors=" + scanErrors.get()
                    + ", index=" + lastIndexPath);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(
                    "UUID 历史索引完成。读取备份: " + loadedBackups.get()
                            + "，唯一 UUID: " + lastUniqueUuids
                            + "，历史节点: " + historyEvents.get()
                            + "，异常: " + scanErrors.get()
                            + "，索引: " + lastIndexPath));
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error("scan materialhistory uuidindex failed", exception);
            Bukkit.getScheduler().runTask(ThePit.getInstance(),
                    () -> sender.sendMessage("UUID 历史索引失败: " + lastError));
        } finally {
            running.set(false);
        }
    }

    UuidHistoryLookup lookup(Set<String> itemUuids) throws IOException {
        LoadedIndex loadedIndex = getLatestIndex();
        if (loadedIndex == null) {
            return null;
        }
        if (itemUuids == null || itemUuids.isEmpty()) {
            return new UuidHistoryLookup(Collections.emptyList(), loadedIndex.manifest);
        }
        Map<String, Set<String>> uuidsByShard = new HashMap<>();
        for (String itemUuid : itemUuids) {
            uuidsByShard.computeIfAbsent(shardOf(itemUuid), ignored -> new HashSet<>()).add(itemUuid);
        }

        List<UuidHistoryEvent> events = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : uuidsByShard.entrySet()) {
            Path shardPath = loadedIndex.directory.resolve("shard-" + entry.getKey() + ".ndjson");
            if (!Files.isRegularFile(shardPath)) {
                continue;
            }
            try (BufferedReader reader = Files.newBufferedReader(shardPath, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    UuidHistoryEvent event = LINE_GSON.fromJson(line, UuidHistoryEvent.class);
                    if (event != null && entry.getValue().contains(event.itemUuid)) {
                        events.add(event);
                    }
                }
            }
        }
        events.sort(Comparator
                .comparingLong((UuidHistoryEvent event) -> event.timestamp)
                .thenComparing(event -> event.itemUuid)
                .thenComparing(event -> event.playerUuid, Comparator.nullsFirst(String::compareTo)));
        return new UuidHistoryLookup(events, loadedIndex.manifest);
    }

    TransferredUuidLookup findTransferredFrom(String playerUuid) throws IOException {
        LoadedIndex loadedIndex = getLatestIndex();
        if (loadedIndex == null || loadedIndex.manifest.shards == null) {
            return null;
        }
        List<TransferredUuid> transferred = new ArrayList<>();
        int scannedShards = 0;
        for (String shard : loadedIndex.manifest.shards) {
            Path shardPath = loadedIndex.directory.resolve("shard-" + shard + ".ndjson");
            if (!Files.isRegularFile(shardPath)) {
                continue;
            }
            Set<String> seenUuids = new HashSet<>();
            Map<String, TransferredUuid> candidates = new HashMap<>();
            try (BufferedReader reader = Files.newBufferedReader(shardPath, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    UuidHistoryEvent event = LINE_GSON.fromJson(line, UuidHistoryEvent.class);
                    if (event == null || event.itemUuid == null) {
                        continue;
                    }
                    if (seenUuids.add(event.itemUuid)) {
                        if (Objects.equals(playerUuid, event.playerUuid)) {
                            candidates.put(event.itemUuid, new TransferredUuid(event.itemUuid, event, event));
                        }
                        continue;
                    }
                    TransferredUuid candidate = candidates.get(event.itemUuid);
                    if (candidate != null) {
                        candidate.latest = event;
                    }
                }
            }
            for (TransferredUuid candidate : candidates.values()) {
                if (!Objects.equals(playerUuid, candidate.latest.playerUuid)) {
                    transferred.add(candidate);
                }
            }
            scannedShards++;
            if (scannedShards % 128 == 0) {
                ThePit.getInstance().info("scan materialhistory uuidtransfer progress: scannedShards="
                        + scannedShards + ", matches=" + transferred.size());
            }
        }
        transferred.sort(Comparator
                .comparingLong((TransferredUuid item) -> item.latest.timestamp).reversed()
                .thenComparing(item -> item.itemUuid));
        return new TransferredUuidLookup(transferred, loadedIndex.manifest);
    }

    RecordedUuidLookup findWithEnchantmentRecord(String recordDescription) throws IOException {
        LoadedIndex loadedIndex = getLatestIndex();
        if (loadedIndex == null || loadedIndex.manifest.shards == null) {
            return null;
        }
        String marker = "|" + recordDescription + "|";
        List<RecordedUuid> matches = new ArrayList<>();
        int scannedShards = 0;
        for (String shard : loadedIndex.manifest.shards) {
            Path shardPath = loadedIndex.directory.resolve("shard-" + shard + ".ndjson");
            if (!Files.isRegularFile(shardPath)) {
                continue;
            }
            Map<String, UuidHistoryEvent> latestByUuid = new HashMap<>();
            Map<String, UuidHistoryEvent> latestMatchByUuid = new HashMap<>();
            try (BufferedReader reader = Files.newBufferedReader(shardPath, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    UuidHistoryEvent event = LINE_GSON.fromJson(line, UuidHistoryEvent.class);
                    if (event == null || event.itemUuid == null) {
                        continue;
                    }
                    latestByUuid.put(event.itemUuid, event);
                    if (containsRecord(event, marker)) {
                        latestMatchByUuid.put(event.itemUuid, event);
                    }
                }
            }
            for (Map.Entry<String, UuidHistoryEvent> entry : latestMatchByUuid.entrySet()) {
                matches.add(new RecordedUuid(
                        entry.getKey(),
                        entry.getValue(),
                        latestByUuid.getOrDefault(entry.getKey(), entry.getValue())
                ));
            }
            scannedShards++;
            if (scannedShards % 128 == 0) {
                ThePit.getInstance().info("scan materialhistory legitgem progress: scannedShards="
                        + scannedShards + ", matches=" + matches.size());
            }
        }
        matches.sort(Comparator
                .comparingLong((RecordedUuid item) -> item.matched.timestamp).reversed()
                .thenComparing(item -> item.itemUuid));
        return new RecordedUuidLookup(matches, loadedIndex.manifest);
    }

    RecordedUsageLookup countEnchantmentRecordUsage(String recordDescription) throws IOException {
        LoadedIndex loadedIndex = getLatestIndex();
        if (loadedIndex == null || loadedIndex.manifest.shards == null) {
            return null;
        }
        String marker = "|" + recordDescription + "|";
        Map<String, RecordedUsage> usageByPlayer = new HashMap<>();
        int scannedShards = 0;
        for (String shard : loadedIndex.manifest.shards) {
            Path shardPath = loadedIndex.directory.resolve("shard-" + shard + ".ndjson");
            if (!Files.isRegularFile(shardPath)) {
                continue;
            }
            Set<RecordOperationKey> seenOperations = new HashSet<>();
            try (BufferedReader reader = Files.newBufferedReader(shardPath, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    UuidHistoryEvent event = LINE_GSON.fromJson(line, UuidHistoryEvent.class);
                    if (event == null || event.itemUuid == null || event.items == null) {
                        continue;
                    }
                    for (IndexedItem item : event.items) {
                        if (item == null || item.nbt == null || !item.nbt.contains(marker)) {
                            continue;
                        }
                        for (IndexedRecord record : readRecords(item.nbt, recordDescription)) {
                            String playerKey = record.playerName.toLowerCase(Locale.ROOT);
                            RecordOperationKey operation = new RecordOperationKey(
                                    event.itemUuid, playerKey, record.timestamp);
                            if (!seenOperations.add(operation)) {
                                continue;
                            }
                            RecordedUsage usage = usageByPlayer.computeIfAbsent(
                                    playerKey, ignored -> new RecordedUsage(record.playerName));
                            usage.playerName = record.playerName;
                            usage.uses++;
                            usage.itemUuids.add(event.itemUuid);
                            usage.firstUseTimestamp = usage.firstUseTimestamp == 0L
                                    ? record.timestamp
                                    : Math.min(usage.firstUseTimestamp, record.timestamp);
                            usage.lastUseTimestamp = Math.max(usage.lastUseTimestamp, record.timestamp);
                        }
                    }
                }
            }
            scannedShards++;
            if (scannedShards % 128 == 0) {
                int totalUses = usageByPlayer.values().stream().mapToInt(usage -> usage.uses).sum();
                ThePit.getInstance().info("scan materialhistory legitgem usage progress: scannedShards="
                        + scannedShards + ", players=" + usageByPlayer.size() + ", uses=" + totalUses);
            }
        }
        List<RecordedUsage> usages = new ArrayList<>(usageByPlayer.values());
        usages.sort(Comparator
                .comparingInt((RecordedUsage usage) -> usage.uses).reversed()
                .thenComparing(usage -> usage.playerName, String.CASE_INSENSITIVE_ORDER));
        return new RecordedUsageLookup(usages, loadedIndex.manifest);
    }

    private List<IndexedRecord> readRecords(String snbt, String recordDescription) {
        try {
            CompoundTag root = TagParser.parseTag(snbt);
            String serialized = root.getCompound("extra").getString("records");
            if (serialized.isBlank()) {
                return Collections.emptyList();
            }
            List<IndexedRecord> result = new ArrayList<>();
            for (String entry : serialized.split(";")) {
                String[] fields = entry.split("\\|", -1);
                if (fields.length < 3 || !recordDescription.equals(fields[1]) || fields[0].isBlank()) {
                    continue;
                }
                try {
                    result.add(new IndexedRecord(fields[0], Long.parseLong(fields[2])));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed historical records.
                }
            }
            return result;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private boolean containsRecord(UuidHistoryEvent event, String marker) {
        if (event.items == null) {
            return false;
        }
        for (IndexedItem item : event.items) {
            if (item != null && item.nbt != null && item.nbt.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private LoadedIndex getLatestIndex() throws IOException {
        if (cachedManifest != null
                && cachedIndexDirectory != null
                && Files.isRegularFile(cachedIndexDirectory.resolve("manifest.json"))) {
            return new LoadedIndex(cachedIndexDirectory, cachedManifest);
        }
        Path directory = ScanReportPath.directory("materialhistory");
        List<Path> candidates;
        try (Stream<Path> paths = Files.list(directory)) {
            candidates = paths
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("uuid-history-index-"))
                    .filter(path -> Files.isRegularFile(path.resolve("manifest.json")))
                    .sorted(Comparator.comparingLong(this::lastModified).reversed())
                    .toList();
        }
        for (Path candidate : candidates) {
            try (Reader reader = Files.newBufferedReader(candidate.resolve("manifest.json"), StandardCharsets.UTF_8)) {
                IndexManifest manifest = GSON.fromJson(reader, IndexManifest.class);
                if (manifest != null) {
                    cachedManifest = manifest;
                    cachedIndexDirectory = candidate;
                    lastIndexPath = candidate.toAbsolutePath().toString();
                    return new LoadedIndex(candidate, manifest);
                }
            }
        }
        return null;
    }

    private Map<String, PlayerInvBackup> loadBackups(List<MaterialHistoryBackupReference> references) {
        Map<String, PlayerInvBackup> loaded = new HashMap<>();
        List<ObjectId> ids = references.stream()
                .map(MaterialHistoryBackupReference::databaseId)
                .filter(id -> id != null && ObjectId.isValid(id))
                .distinct()
                .map(ObjectId::new)
                .toList();
        if (!ids.isEmpty()) {
            try (MongoCursor<PlayerInvBackup> cursor = ThePit.getInstance().getMongoDB().getInvCollection()
                    .find(Filters.in("_id", ids)).batchSize(100).iterator()) {
                while (cursor.hasNext()) {
                    registerBackup(loaded, cursor.next());
                }
            }
        }
        List<String> fallback = references.stream()
                .filter(reference -> findBackup(loaded, reference) == null)
                .map(MaterialHistoryBackupReference::backupUuid)
                .filter(uuid -> uuid != null && !uuid.isBlank())
                .distinct()
                .toList();
        if (!fallback.isEmpty()) {
            try (MongoCursor<PlayerInvBackup> cursor = ThePit.getInstance().getMongoDB().getInvCollection()
                    .find(Filters.in("backupUuid", fallback)).batchSize(100).iterator()) {
                while (cursor.hasNext()) {
                    registerBackup(loaded, cursor.next());
                }
            }
        }
        return loaded;
    }

    private void registerBackup(Map<String, PlayerInvBackup> loaded, PlayerInvBackup backup) {
        if (backup == null) {
            return;
        }
        if (backup.getId() != null) {
            loaded.put("id:" + backup.getId(), backup);
        }
        if (backup.getBackupUuid() != null) {
            loaded.put("uuid:" + backup.getBackupUuid(), backup);
        }
    }

    private PlayerInvBackup findBackup(Map<String, PlayerInvBackup> loaded,
                                       MaterialHistoryBackupReference reference) {
        PlayerInvBackup backup = reference.databaseId() == null
                ? null
                : loaded.get("id:" + reference.databaseId());
        return backup == null && reference.backupUuid() != null
                ? loaded.get("uuid:" + reference.backupUuid())
                : backup;
    }

    private Map<String, UuidBackupState> extractStates(PlayerInvBackup backup) {
        Map<String, UuidBackupState> states = new HashMap<>();
        PlayerInv inventory = backup.getInv();
        if (inventory != null) {
            ItemStack[] contents = inventory.getContents();
            addItems(states, contents, "inventory", 36);
            if (contents != null && contents.length > 40) {
                addItems(states, new ItemStack[]{contents[40]}, "offhand", 1);
            }
            addItems(states, inventory.getArmorContents(), "armor", -1);
        }
        PlayerEnderChest[] warehouses = backup.getChest();
        if (warehouses != null) {
            for (int index = 0; index < warehouses.length; index++) {
                PlayerEnderChest warehouse = warehouses[index];
                if (warehouse != null && warehouse.getInventory() != null) {
                    addItems(states, warehouse.getInventory().getContents(),
                            "warehouse[" + (index + 1) + "]", -1);
                }
            }
        }
        states.values().forEach(state -> state.items.sort(Comparator
                .comparing((IndexedItem item) -> item.itemStateHash)
                .thenComparing(item -> item.location)));
        return states;
    }

    private void addItems(Map<String, UuidBackupState> states,
                          ItemStack[] items,
                          String locationPrefix,
                          int maximumSlots) {
        if (items == null) {
            return;
        }
        int length = maximumSlots < 0 ? items.length : Math.min(items.length, maximumSlots);
        for (int slot = 0; slot < length; slot++) {
            ItemStack item = items[slot];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            scannedItemStacks.incrementAndGet();
            try {
                String itemUuid = ItemUtil.getUUID(item);
                if (itemUuid == null || itemUuid.isBlank()) {
                    continue;
                }
                IndexedItem indexedItem = IndexedItem.from(item, locationPrefix + "[" + slot + "]");
                states.computeIfAbsent(itemUuid, ignored -> new UuidBackupState()).items.add(indexedItem);
            } catch (Exception exception) {
                scanErrors.incrementAndGet();
            }
        }
    }

    private void bufferEvent(Path directory,
                             Map<String, List<UuidHistoryEvent>> buffers,
                             Set<String> writtenShards,
                             UuidHistoryEvent event) throws IOException {
        String shard = shardOf(event.itemUuid);
        List<UuidHistoryEvent> buffer = buffers.computeIfAbsent(shard, ignored -> new ArrayList<>());
        buffer.add(event);
        if (buffer.size() >= EVENT_BUFFER_SIZE) {
            flushShard(directory, shard, buffer);
            writtenShards.add(shard);
            buffer.clear();
        }
    }

    private void flushAll(Path directory,
                          Map<String, List<UuidHistoryEvent>> buffers,
                          Set<String> writtenShards) throws IOException {
        for (Map.Entry<String, List<UuidHistoryEvent>> entry : buffers.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                flushShard(directory, entry.getKey(), entry.getValue());
                writtenShards.add(entry.getKey());
                entry.getValue().clear();
            }
        }
    }

    private void flushShard(Path directory, String shard, List<UuidHistoryEvent> events) throws IOException {
        Path path = directory.resolve("shard-" + shard + ".ndjson");
        try (BufferedWriter writer = Files.newBufferedWriter(
                path,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            for (UuidHistoryEvent event : events) {
                writer.write(LINE_GSON.toJson(event));
                writer.newLine();
            }
        }
    }

    private static String shardOf(String itemUuid) {
        return String.format("%03x", itemUuid.hashCode() & SHARD_MASK);
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path.resolve("manifest.json")).toMillis();
        } catch (IOException ignored) {
            return Long.MIN_VALUE;
        }
    }

    private static String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(Math.max(0L, millis));
        long seconds = duration.getSeconds();
        return String.format("%02d:%02d:%02d", seconds / 3600, seconds % 3600 / 60, seconds % 60);
    }

    private static String hash(String value) {
        MessageDigest digest = SHA_256.get();
        digest.reset();
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private record LastState(String playerUuid, String stateHash) {
    }

    private static final class UuidBackupState {
        private final List<IndexedItem> items = new ArrayList<>();

        private String stateHash() {
            return backupStateHash(items);
        }
    }

    // Preserve the multiplicity of identical copies. A single item becoming two identical items
    // is evidence worth retaining even when the owner and all other attributes stay unchanged.
    static String backupStateHash(List<IndexedItem> items) {
        List<String> hashes = items.stream().filter(Objects::nonNull)
                .map(IndexedItem::comparisonStateHash).sorted().toList();
        return hash(String.join("|", hashes));
    }

    static final class IndexedItem {
        String location;
        String internalName;
        String material;
        String displayName;
        int amount;
        List<String> lore;
        String nbt;
        String itemStateHash;

        private static IndexedItem from(ItemStack item, String location) {
            IndexedItem result = new IndexedItem();
            result.location = location;
            result.internalName = ItemUtil.getInternalName(item);
            result.material = item.getType().name().toLowerCase(Locale.ROOT);
            ItemMeta meta = item.getItemMeta();
            result.displayName = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : null;
            result.amount = item.getAmount();
            result.lore = meta != null && meta.hasLore() && meta.getLore() != null
                    ? new ArrayList<>(meta.getLore())
                    : Collections.emptyList();
            result.nbt = NBT.get(item, nbt -> {
                return nbt.toString();
            });
            result.itemStateHash = hash(result.material + "\u0000"
                    + result.internalName + "\u0000"
                    + result.displayName + "\u0000"
                    + result.amount + "\u0000"
                    + result.lore + "\u0000"
                    + result.nbt);
            return result;
        }

        private String comparisonStateHash() {
            return ScanMaterialUuidHistoryIndex.comparisonStateHash(
                    material, internalName, displayName, amount, lore, nbt);
        }

    }

    static final class UuidHistoryEvent {
        String itemUuid;
        String playerUuid;
        String playerName;
        int prestige;
        String backupDatabaseId;
        String backupUuid;
        long timestamp;
        String time;
        String stateHash;
        List<IndexedItem> items;

        private UuidHistoryEvent(MaterialHistoryBackupReference reference,
                                 String itemUuid,
                                 String stateHash,
                                 List<IndexedItem> items) {
            this.itemUuid = itemUuid;
            this.playerUuid = reference.playerUuid();
            this.playerName = reference.playerName();
            this.prestige = reference.prestige();
            this.backupDatabaseId = reference.databaseId();
            this.backupUuid = reference.backupUuid();
            this.timestamp = reference.timestamp();
            this.time = reference.time();
            this.stateHash = stateHash;
            this.items = new ArrayList<>(items);
        }
    }

    static String comparisonStateHash(UuidHistoryEvent event) {
        if (event == null || event.items == null || event.items.isEmpty()) {
            return null;
        }
        List<String> hashes = event.items.stream()
                .filter(Objects::nonNull)
                .map(IndexedItem::comparisonStateHash)
                .distinct()
                .sorted()
                .toList();
        return hash(String.join("|", hashes));
    }

    static String comparisonStateHash(IndexedItem item) {
        return item == null ? null : item.comparisonStateHash();
    }

    static String comparisonStateHash(String material,
                                      String internalName,
                                      String displayName,
                                      int amount,
                                      List<String> lore,
                                      String nbt) {
        List<String> comparisonLore = lore == null
                ? Collections.emptyList()
                : lore.stream().map(ScanMaterialUuidHistoryIndex::normalizeLoreForComparison).toList();
        return hash(material + "\u0000"
                + internalName + "\u0000"
                + displayName + "\u0000"
                + amount + "\u0000"
                + comparisonLore + "\u0000"
                + normalizeNbtForComparison(nbt));
    }

    private static String normalizeLoreForComparison(String value) {
        if (value == null) {
            return null;
        }
        String normalized = replaceFirstNumberBeforeSlash(value, "生命");
        return replaceFirstNumberAfterMarker(normalized, "记录的击杀");
    }

    private static String replaceFirstNumberBeforeSlash(String value, String marker) {
        int markerIndex = value.indexOf(marker);
        if (markerIndex < 0) {
            return value;
        }
        int slashIndex = value.indexOf('/', markerIndex + marker.length());
        if (slashIndex < 0) {
            return value;
        }
        int numberStart = -1;
        for (int index = markerIndex + marker.length(); index < slashIndex; index++) {
            if (Character.isDigit(value.charAt(index))) {
                numberStart = index;
                break;
            }
        }
        if (numberStart < 0) {
            return value;
        }
        int numberEnd = numberStart + 1;
        while (numberEnd < slashIndex && Character.isDigit(value.charAt(numberEnd))) {
            numberEnd++;
        }
        return value.substring(0, numberStart) + "#" + value.substring(numberEnd);
    }

    private static String replaceFirstNumberAfterMarker(String value, String marker) {
        int markerIndex = value.indexOf(marker);
        if (markerIndex < 0) {
            return value;
        }
        int numberStart = -1;
        for (int index = markerIndex + marker.length(); index < value.length(); index++) {
            if (Character.isDigit(value.charAt(index))) {
                numberStart = index;
                break;
            }
        }
        if (numberStart < 0) {
            return value;
        }
        int numberEnd = numberStart + 1;
        while (numberEnd < value.length() && Character.isDigit(value.charAt(numberEnd))) {
            numberEnd++;
        }
        return value.substring(0, numberStart) + "#" + value.substring(numberEnd);
    }

    private static String normalizeNbtForComparison(String snbt) {
        if (snbt == null || snbt.isBlank()) {
            return snbt;
        }
        try {
            StringBuilder result = new StringBuilder(snbt.length());
            appendCanonicalTag(result, TagParser.parseTag(snbt), "");
            return result.toString();
        } catch (Exception ignored) {
            return snbt;
        }
    }

    private static void appendCanonicalTag(StringBuilder result, Tag tag, String path) {
        if (tag instanceof CompoundTag compound) {
            result.append('{');
            List<String> keys = new ArrayList<>(compound.getAllKeys());
            keys.sort(String::compareTo);
            for (String key : keys) {
                Tag value = compound.get(key);
                if (value == null || ignoreComparisonField(path, key, value)) {
                    continue;
                }
                result.append(key.length()).append(':').append(key).append('=');
                if ("killCounterGem".equalsIgnoreCase(key)) {
                    result.append("counter");
                } else if (isRandomAttributeUuid(path, key, value)) {
                    result.append("attribute_uuid");
                } else {
                    appendCanonicalTag(result, value, path.isEmpty() ? key : path + "." + key);
                }
                result.append(';');
            }
            result.append('}');
            return;
        }
        if (tag instanceof ListTag list) {
            List<String> values = new ArrayList<>(list.size());
            for (Tag value : list) {
                StringBuilder child = new StringBuilder();
                appendCanonicalTag(child, value, path + "[]");
                values.add(child.toString());
            }
            if (path.endsWith(".ench")) {
                values.sort(String::compareTo);
            }
            result.append('[').append(String.join(",", values)).append(']');
            return;
        }
        if (tag instanceof ByteArrayTag bytes) {
            result.append("bytes:").append(java.util.Arrays.toString(bytes.getAsByteArray()));
            return;
        }
        if (tag instanceof IntArrayTag integers) {
            result.append("ints:").append(java.util.Arrays.toString(integers.getAsIntArray()));
            return;
        }
        if (tag instanceof LongArrayTag longs) {
            result.append("longs:").append(java.util.Arrays.toString(longs.getAsLongArray()));
            return;
        }
        if (tag instanceof NumericTag number) {
            result.append("number:").append(number.getAsNumber());
            return;
        }
        if (tag instanceof StringTag string) {
            String value = string.getAsString();
            result.append("string:").append(value.length()).append(':').append(value);
            return;
        }
        result.append(tag.getId()).append(':').append(tag.getAsString());
    }

    private static boolean ignoreComparisonField(String path, String key, Tag value) {
        if (!"extra".equals(path)) {
            return false;
        }
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        if (Set.of("live", "creator", "creatorname", "owner", "ownername", "traded", "tradetime")
                .contains(normalizedKey)) {
            return true;
        }
        return Set.of(
                        "boostedbybook",
                        "boostedbyeternal",
                        "boostedbygem",
                        "boostedbyglobalgem",
                        "boostedbyphasebook"
                ).contains(normalizedKey)
                && value instanceof NumericTag number
                && number.getAsInt() == 0;
    }

    private static boolean isRandomAttributeUuid(String path, String key, Tag value) {
        if (!path.toLowerCase(Locale.ROOT).contains("attribute")) {
            return false;
        }
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        if (Set.of("uuid", "uuidmost", "uuidleast").contains(normalizedKey)) {
            return true;
        }
        return "id".equals(normalizedKey)
                && value instanceof StringTag string
                && string.getAsString().matches("(?i)(?:minecraft:)?[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    static final class UuidHistoryLookup {
        final List<UuidHistoryEvent> events;
        final IndexManifest manifest;

        private UuidHistoryLookup(List<UuidHistoryEvent> events, IndexManifest manifest) {
            this.events = events;
            this.manifest = manifest;
        }
    }

    static final class TransferredUuid {
        final String itemUuid;
        final UuidHistoryEvent first;
        UuidHistoryEvent latest;

        private TransferredUuid(String itemUuid,
                                UuidHistoryEvent first,
                                UuidHistoryEvent latest) {
            this.itemUuid = itemUuid;
            this.first = first;
            this.latest = latest;
        }
    }

    static final class TransferredUuidLookup {
        final List<TransferredUuid> items;
        final IndexManifest manifest;

        private TransferredUuidLookup(List<TransferredUuid> items, IndexManifest manifest) {
            this.items = items;
            this.manifest = manifest;
        }
    }

    static final class RecordedUuid {
        final String itemUuid;
        final UuidHistoryEvent matched;
        final UuidHistoryEvent latest;

        private RecordedUuid(String itemUuid,
                             UuidHistoryEvent matched,
                             UuidHistoryEvent latest) {
            this.itemUuid = itemUuid;
            this.matched = matched;
            this.latest = latest;
        }
    }

    static final class RecordedUuidLookup {
        final List<RecordedUuid> items;
        final IndexManifest manifest;

        private RecordedUuidLookup(List<RecordedUuid> items, IndexManifest manifest) {
            this.items = items;
            this.manifest = manifest;
        }
    }

    static final class RecordedUsage {
        String playerName;
        int uses;
        long firstUseTimestamp;
        long lastUseTimestamp;
        final Set<String> itemUuids = new HashSet<>();

        private RecordedUsage(String playerName) {
            this.playerName = playerName;
        }
    }

    static final class RecordedUsageLookup {
        final List<RecordedUsage> usages;
        final IndexManifest manifest;

        private RecordedUsageLookup(List<RecordedUsage> usages, IndexManifest manifest) {
            this.usages = usages;
            this.manifest = manifest;
        }
    }

    private record IndexedRecord(String playerName, long timestamp) {
    }

    private record RecordOperationKey(String itemUuid, String playerName, long timestamp) {
    }

    static final class IndexManifest {
        int version;
        long generatedAt;
        String sourceMaterialHistoryReport;
        int candidateBackups;
        int loadedBackups;
        int scannedItemStacks;
        int uniqueUuids;
        int historyEvents;
        int scanErrors;
        List<String> shards;

        private IndexManifest(int version,
                              long generatedAt,
                              String sourceMaterialHistoryReport,
                              int candidateBackups,
                              int loadedBackups,
                              int scannedItemStacks,
                              int uniqueUuids,
                              int historyEvents,
                              int scanErrors,
                              List<String> shards) {
            this.version = version;
            this.generatedAt = generatedAt;
            this.sourceMaterialHistoryReport = sourceMaterialHistoryReport;
            this.candidateBackups = candidateBackups;
            this.loadedBackups = loadedBackups;
            this.scannedItemStacks = scannedItemStacks;
            this.uniqueUuids = uniqueUuids;
            this.historyEvents = historyEvents;
            this.scanErrors = scanErrors;
            this.shards = new ArrayList<>(shards);
        }
    }

    private record LoadedIndex(Path directory, IndexManifest manifest) {
    }
}
