package cn.charlotte.pit.commands.admin;

import com.google.gson.*;
import net.minecraft.nbt.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Derived, persistent, file-only enchantment audit. Operators come from records, never from inventory ownership. */
final class MaterialEnchantmentHistory {
    static final Gson GSON = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();
    static final String UNKNOWN_ACTOR = "unknown:";
    record Evidence(String playerUuid, String playerName, int prestige, String backupDatabaseId, String backupUuid,
                    long timestamp, String time, List<String> locations, long copies) { }
    record OperationId(String actorKey, String source, long timestamp) { }
    record State(String internalName, String material, String displayName, List<String> lore,
                 Integer tier, Map<String, Integer> enchantments, JsonObject nbt) { }
    record Delta(String enchantment, int before, int after) { }
    record Change(Evidence beforeEvidence, Evidence afterEvidence, State before, State after,
                  List<Delta> differences, List<OperationId> recordedCandidates, String attribution) { }
    record Conflict(Evidence evidence, List<State> states) { }
    static final class Operation {
        OperationId id;
        String playerName;
        Evidence firstSeen, lastSeen;
        int observedBackups;
        boolean timestampAfterFirstEvidence;
        Map<String, Integer> firstObservedEnchantments;
        List<String> firstObservedLore;
        private transient String lastBackupKey;
    }
    record ItemReport(String itemUuid, Evidence firstSeen, Evidence lastSeen, List<State> latestStates,
                      List<Operation> operations, List<Change> changes, List<Conflict> conflicts, int malformedItems) { }
    record Actor(String key, String playerName, int itemCount, long recordCount, long candidateChanges,
                 long firstTimestamp, long lastTimestamp) { }
    record ActorItem(String itemUuid, String displayName, String material, int recordCount, int candidateChanges,
                     long firstTimestamp, long lastTimestamp, int conflicts) { }
    record Manifest(int version, long generatedAt, String sourceIndex, UuidDuplicateHistory.Metadata sourceMetadata,
                    long readEvents, int itemCount, long recordCount, long changeCount, long conflictCount,
                    long malformedItems, List<String> limitations, List<Actor> actors) { }
    record Report(Path directory, Manifest manifest) { }
    record Progress(int shards, int totalShards, long readEvents, int items, long records, long changes, long malformedItems) { }

    private record InputItem(String location, String internalName, String material, String displayName,
                             int amount, List<String> lore, JsonElement nbt) { }
    private record InputEvent(String itemUuid, String playerUuid, String playerName, int prestige,
                              String backupDatabaseId, String backupUuid, long timestamp, String time,
                              List<InputItem> items) { }
    private record ParsedOperation(String playerName, String source, long timestamp) { }
    private record Parsed(State state, List<ParsedOperation> operations, int malformedRecords) { }

    private MaterialEnchantmentHistory() { }

    static Report build(UuidDuplicateHistory.Source source, Path directory, Consumer<Progress> progress) throws IOException {
        Files.createDirectory(directory);
        Map<String, ActorTotals> actors = new HashMap<>();
        long read = 0, records = 0, changes = 0, conflicts = 0, malformed = 0;
        int itemCount = 0, shardCount = 0;
        for (String shard : new TreeSet<>(source.metadata().shards())) {
            Path file = source.directory().resolve("shard-" + shard + ".ndjson");
            if (!Files.isRegularFile(file)) throw new IOException("UUID 索引分片缺失: " + file);
            // Only one source shard is retained; completed equipment histories are written and released per shard.
            Map<String, Builder> equipment = new HashMap<>();
            long previousTime = Long.MIN_VALUE;
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    read++;
                    InputEvent event;
                    try { event = GSON.fromJson(line, InputEvent.class); }
                    catch (RuntimeException exception) { throw new IOException("UUID 索引格式错误: " + file + "，记录 " + read, exception); }
                    if (event == null || blank(event.itemUuid) || event.timestamp <= 0 || event.items == null
                            || (blank(event.backupDatabaseId) && blank(event.backupUuid))) {
                        malformed++; continue;
                    }
                    if (event.timestamp < previousTime) throw new IOException("UUID 索引未按时间排序，请重建: " + file);
                    previousTime = event.timestamp;
                    if (event.items.stream().noneMatch(item -> item != null && eligible(item.internalName))) continue;
                    equipment.computeIfAbsent(event.itemUuid, Builder::new).add(event);
                    if (read % 10_000 == 0) progress.accept(new Progress(shardCount, source.metadata().shards().size(),
                            read, itemCount, records, changes, malformed));
                }
            }
            Map<String, List<ActorItem>> playerItems = new HashMap<>();
            for (Builder builder : equipment.values()) {
                ItemReport report = builder.finish();
                malformed += report.malformedItems;
                if (report.operations.isEmpty() && report.changes.isEmpty() && report.conflicts.isEmpty()) continue;
                writeJson(itemPath(directory, report.itemUuid), report);
                itemCount++;
                records += report.operations.size(); changes += report.changes.size(); conflicts += report.conflicts.size();
                Map<String, List<Operation>> byActor = new HashMap<>();
                report.operations.forEach(operation -> byActor.computeIfAbsent(operation.id.actorKey, ignored -> new ArrayList<>()).add(operation));
                boolean unknown = !report.conflicts.isEmpty() || report.changes.stream()
                        .anyMatch(change -> change.recordedCandidates.isEmpty() || !"ONE_RECORDED_CANDIDATE".equals(change.attribution));
                if (unknown) byActor.putIfAbsent(UNKNOWN_ACTOR, List.of());
                for (var entry : byActor.entrySet()) {
                    String actorKey = entry.getKey();
                    List<Operation> operations = entry.getValue();
                    long first = operations.stream().mapToLong(operation -> operation.id.timestamp).min().orElse(report.firstSeen.timestamp);
                    long last = operations.stream().mapToLong(operation -> operation.id.timestamp).max().orElse(report.lastSeen.timestamp);
                    String name = actorKey.equals(UNKNOWN_ACTOR) ? "无法确认操作者" : operations.getFirst().playerName;
                    int candidateChanges = (int) report.changes.stream().filter(change -> actorKey.equals(UNKNOWN_ACTOR)
                            ? change.recordedCandidates.isEmpty() || !"ONE_RECORDED_CANDIDATE".equals(change.attribution)
                            : change.recordedCandidates.stream().anyMatch(operation -> operation.actorKey.equals(actorKey))).count();
                    State latest = report.latestStates.isEmpty() ? null : report.latestStates.getFirst();
                    ActorItem item = new ActorItem(report.itemUuid, latest == null ? null : latest.displayName,
                            latest == null ? null : latest.material, operations.size(), candidateChanges, first, last, report.conflicts.size());
                    playerItems.computeIfAbsent(actorKey, ignored -> new ArrayList<>()).add(item);
                    actors.computeIfAbsent(actorKey, ignored -> new ActorTotals(actorKey, name)).add(item);
                }
            }
            for (var entry : playerItems.entrySet()) {
                Path path = actorPath(directory, entry.getKey());
                Files.createDirectories(path.getParent());
                try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    for (ActorItem item : entry.getValue()) { writer.write(GSON.toJson(item)); writer.newLine(); }
                }
            }
            shardCount++;
            progress.accept(new Progress(shardCount, source.metadata().shards().size(), read, itemCount, records, changes, malformed));
        }
        List<Actor> summaries = actors.values().stream().map(ActorTotals::finish)
                .sorted(Comparator.comparingLong(Actor::recordCount).reversed().thenComparing(Actor::key)).toList();
        Manifest manifest = new Manifest(1, System.currentTimeMillis(), source.directory().toAbsolutePath().toString(),
                source.metadata(), read, itemCount, records, changes, conflicts, malformed, List.of(
                "操作者仅来自 extra.records 中的玩家名；备份持有人不是附魔者，玩家更名无法可靠按 UUID 合并。",
                "records 通常只保留最近 5 条；仅覆盖本地索引收录的历史，无法保证完整，也不是玩家累计附魔总次数。",
                "记录按装备 UUID、玩家名（忽略大小写）、来源和时间戳去重；完全相同的真实操作也可能无法区分。",
                "附魔差异为两次备份观察到的净变化，候选操作不等于精确归因；首次已附魔状态不是一次新增操作。",
                "同 UUID 不同副本状态、同一时间先后关系、缺失记录及多次操作会标为无法精确对应。"
        ), summaries);
        // A manifest is the completion marker. Partial runs are never selected by the viewer.
        writeJson(directory.resolve("manifest.json"), manifest);
        return new Report(directory, manifest);
    }

    private static final class Builder {
        final String uuid;
        final Map<OperationId, Operation> operations = new LinkedHashMap<>();
        final List<Change> changes = new ArrayList<>();
        final List<Conflict> conflicts = new ArrayList<>();
        Evidence first, last, previousEvidence;
        State previous;
        List<State> latest = List.of();
        int malformed;
        Builder(String uuid) { this.uuid = uuid; }
        void add(InputEvent event) {
            List<String> locations = event.items.stream().filter(Objects::nonNull).map(InputItem::location)
                    .filter(Objects::nonNull).distinct().sorted().toList();
            long copies = event.items.stream().filter(Objects::nonNull).mapToLong(item -> Math.max(0, item.amount)).sum();
            Evidence evidence = new Evidence(event.playerUuid, event.playerName, event.prestige, event.backupDatabaseId,
                    event.backupUuid, event.timestamp, event.time, locations, copies);
            if (first == null) first = evidence;
            last = evidence;
            Map<String, State> variants = new LinkedHashMap<>();
            boolean incomplete = false;
            for (InputItem item : event.items) {
                if (item == null || item.amount <= 0 || !eligible(item.internalName)) continue;
                Parsed parsed;
                try { parsed = parse(item); }
                catch (RuntimeException exception) { malformed++; incomplete = true; continue; }
                malformed += parsed.malformedRecords;
                variants.putIfAbsent(signature(parsed.state), parsed.state);
                for (ParsedOperation record : parsed.operations) {
                    OperationId id = new OperationId(actorKey(record.playerName), record.source, record.timestamp);
                    Operation operation = operations.get(id);
                    String backupKey = backupKey(evidence);
                    if (operation == null) {
                        operation = new Operation(); operation.id = id; operation.playerName = record.playerName;
                        operation.firstSeen = evidence; operation.timestampAfterFirstEvidence = record.timestamp > evidence.timestamp;
                        operation.firstObservedEnchantments = parsed.state.enchantments;
                        operation.firstObservedLore = parsed.state.lore;
                        operations.put(id, operation);
                    }
                    operation.lastSeen = evidence;
                    if (!backupKey.equals(operation.lastBackupKey)) operation.observedBackups++;
                    operation.lastBackupKey = backupKey;
                }
            }
            latest = List.copyOf(variants.values());
            if (latest.size() != 1 || incomplete) {
                if (latest.size() > 1) conflicts.add(new Conflict(evidence, latest));
                previous = null; previousEvidence = null; return;
            }
            State current = latest.getFirst();
            if (previous != null && !signature(previous).equals(signature(current))) {
                List<OperationId> candidates = operations.values().stream()
                        .filter(operation -> !operation.timestampAfterFirstEvidence && operation.id.timestamp > previousEvidence.timestamp
                                && operation.id.timestamp <= evidence.timestamp)
                        .map(operation -> operation.id).sorted(Comparator.comparingLong(OperationId::timestamp)
                                .thenComparing(OperationId::actorKey).thenComparing(OperationId::source)).toList();
                String attribution = previousEvidence.copies > 1 || evidence.copies > 1 || previousEvidence.timestamp == evidence.timestamp
                        ? "AMBIGUOUS_COPY_OR_ORDER" : candidates.isEmpty() ? "NO_RECORD_CORRELATION"
                        : candidates.size() == 1 ? "ONE_RECORDED_CANDIDATE" : "MULTIPLE_RECORDED_CANDIDATES";
                changes.add(new Change(previousEvidence, evidence, previous, current,
                        differences(previous.enchantments, current.enchantments), candidates, attribution));
            }
            previous = current; previousEvidence = evidence;
        }
        ItemReport finish() {
            List<Operation> sorted = operations.values().stream().sorted(Comparator
                    .comparingLong((Operation operation) -> operation.id.timestamp).thenComparing(operation -> operation.id.actorKey)
                    .thenComparing(operation -> operation.id.source)).toList();
            return new ItemReport(uuid, first, last, latest, sorted, List.copyOf(changes), List.copyOf(conflicts), malformed);
        }
    }

    private static Parsed parse(InputItem item) {
        JsonObject nbt = nbt(item.nbt);
        JsonObject extra = nbt.has("extra") && nbt.get("extra").isJsonObject() ? nbt.getAsJsonObject("extra") : new JsonObject();
        Map<String, Integer> enchantments = new TreeMap<>();
        if (extra.has("ench")) {
            if (!extra.get("ench").isJsonArray()) throw new IllegalArgumentException("ench 不是数组");
            for (JsonElement value : extra.getAsJsonArray("ench")) {
                String encoded = value.getAsString(); int split = encoded.lastIndexOf(':');
                if (split <= 0) throw new IllegalArgumentException("无效附魔: " + encoded);
                int level = Integer.parseInt(encoded.substring(split + 1));
                if (level > 0) enchantments.put(encoded.substring(0, split), level);
            }
        }
        Integer tier = extra.has("tier") ? extra.get("tier").getAsInt() : null;
        List<ParsedOperation> operations = new ArrayList<>();
        int malformed = 0;
        if (extra.has("records") && !extra.get("records").isJsonNull()) {
            for (String entry : extra.get("records").getAsString().split(";")) {
                if (entry.isBlank()) continue;
                String[] fields = entry.split("\\|", -1);
                try {
                    if (fields.length != 3 || fields[0].isBlank() || fields[1].isBlank()) throw new IllegalArgumentException();
                    long time = Long.parseLong(fields[2]); if (time <= 0) throw new IllegalArgumentException();
                    operations.add(new ParsedOperation(fields[0], fields[1], time));
                } catch (IllegalArgumentException exception) { malformed++; }
            }
        }
        return new Parsed(new State(item.internalName.toLowerCase(Locale.ROOT), item.material, item.displayName,
                item.lore == null ? List.of() : List.copyOf(item.lore), tier, Map.copyOf(enchantments), nbt), operations, malformed);
    }

    static JsonObject nbt(JsonElement value) {
        if (value == null || value.isJsonNull()) throw new IllegalArgumentException("缺少 NBT");
        if (value.isJsonObject()) return value.getAsJsonObject().deepCopy();
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("无效 NBT 类型");
        String text = value.getAsString();
        try { return tagJson(TagParser.parseTag(text)).getAsJsonObject(); }
        catch (Exception exception) { throw new IllegalArgumentException("无法解析物品 NBT", exception); }
    }
    private static JsonElement tagJson(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            JsonObject object = new JsonObject();
            for (String key : compound.getAllKeys()) object.add(key, tagJson(compound.get(key)));
            return object;
        }
        if (tag instanceof ListTag list) { JsonArray array = new JsonArray(); for (Tag child : list) array.add(tagJson(child)); return array; }
        if (tag instanceof NumericTag number) return new JsonPrimitive(number.getAsNumber());
        if (tag instanceof StringTag string) return new JsonPrimitive(string.getAsString());
        JsonArray array = new JsonArray();
        if (tag instanceof ByteArrayTag bytes) { for (byte number : bytes.getAsByteArray()) array.add(number); return array; }
        if (tag instanceof IntArrayTag integers) { for (int number : integers.getAsIntArray()) array.add(number); return array; }
        if (tag instanceof LongArrayTag longs) { for (long number : longs.getAsLongArray()) array.add(number); return array; }
        return JsonNull.INSTANCE;
    }
    private static List<Delta> differences(Map<String, Integer> before, Map<String, Integer> after) {
        Set<String> keys = new TreeSet<>(before.keySet()); keys.addAll(after.keySet());
        return keys.stream().filter(key -> !Objects.equals(before.get(key), after.get(key)))
                .map(key -> new Delta(key, before.getOrDefault(key, 0), after.getOrDefault(key, 0))).toList();
    }
    private static String signature(State state) { return state.internalName + "|" + state.tier + "|" + new TreeMap<>(state.enchantments); }
    private static String backupKey(Evidence evidence) {
        return evidence.playerUuid + "|" + (blank(evidence.backupDatabaseId) ? evidence.backupUuid : evidence.backupDatabaseId);
    }
    static String actorKey(String player) { return "player:" + player.toLowerCase(Locale.ROOT); }
    static boolean eligible(String internal) {
        return MaterialHistoryScanRules.mythic(internal) || "magic_fishing_rod".equalsIgnoreCase(internal)
                || "jewel_sword".equalsIgnoreCase(internal);
    }

    private static final class ActorTotals {
        final String key, name;
        int items; long records, changes, first = Long.MAX_VALUE, last;
        ActorTotals(String key, String name) { this.key = key; this.name = name; }
        void add(ActorItem item) { items++; records += item.recordCount; changes += item.candidateChanges;
            first = Math.min(first, item.firstTimestamp); last = Math.max(last, item.lastTimestamp); }
        Actor finish() { return new Actor(key, name, items, records, changes, first, last); }
    }

    static Report latest(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return null;
        Path latest;
        try (Stream<Path> paths = Files.list(directory)) {
            latest = paths.filter(Files::isDirectory).filter(path -> path.getFileName().toString().startsWith("enchant-record-history-"))
                    .filter(path -> Files.isRegularFile(path.resolve("manifest.json")))
                    .max(Comparator.comparingLong((Path path) -> modified(path.resolve("manifest.json")))
                            .thenComparing(path -> path.getFileName().toString())).orElse(null);
        }
        if (latest == null) return null;
        try (Reader reader = Files.newBufferedReader(latest.resolve("manifest.json"), StandardCharsets.UTF_8)) {
            Manifest manifest = GSON.fromJson(reader, Manifest.class);
            if (manifest == null || manifest.version != 1 || manifest.actors == null) throw new IOException("附魔整理 manifest 不完整");
            return new Report(latest, manifest);
        } catch (RuntimeException exception) { throw new IOException("附魔整理 manifest 格式错误", exception); }
    }
    static List<ActorItem> actorItems(Report report, String actorKey) throws IOException {
        List<ActorItem> items = new ArrayList<>();
        Path path = actorPath(report.directory, actorKey);
        if (!Files.isRegularFile(path)) throw new IOException("玩家附魔装备索引缺失: " + path);
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line; while ((line = reader.readLine()) != null) if (!line.isBlank()) items.add(GSON.fromJson(line, ActorItem.class));
        }
        return items.stream().sorted(Comparator.comparingLong(ActorItem::firstTimestamp).thenComparing(ActorItem::itemUuid)).toList();
    }
    static ItemReport item(Report report, String uuid) throws IOException {
        Path path = itemPath(report.directory, uuid);
        if (!Files.isRegularFile(path)) throw new IllegalArgumentException("本地整理结果中没有该装备 UUID: " + uuid);
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ItemReport item = GSON.fromJson(reader, ItemReport.class);
            if (item == null || !Objects.equals(item.itemUuid, uuid)) throw new IOException("装备历史文件不匹配");
            return item;
        }
    }
    static Path itemPath(Path directory, String uuid) { String key = fileKey(uuid); return directory.resolve("items").resolve(key.substring(0, 2)).resolve(key + ".json"); }
    static Path actorPath(Path directory, String actorKey) { return directory.resolve("players").resolve(fileKey(actorKey) + ".ndjson"); }
    private static String fileKey(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private static void writeJson(Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) { GSON.toJson(value, writer); }
        UuidDuplicateHistory.publish(temporary, path);
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static long modified(Path path) { try { return Files.getLastModifiedTime(path).toMillis(); } catch (IOException ignored) { return Long.MIN_VALUE; } }
}
