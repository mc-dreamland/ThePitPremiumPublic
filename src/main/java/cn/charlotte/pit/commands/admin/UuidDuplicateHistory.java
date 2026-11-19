package cn.charlotte.pit.commands.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** File-only forensic lookup. Positive sightings never imply continuous possession. */
final class UuidDuplicateHistory {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    record CurrentLocation(String playerUuid, String playerName, String container, Integer warehouseIndex,
                           int slot, String slotName, String holderItemInternalName, String nestedPath,
                           String displayName) { }
    record CurrentDuplicate(int count, List<CurrentLocation> locations) { }
    record Item(String location, String internalName, String material, String displayName, int amount) { }
    record Observation(String itemUuid, String playerUuid, String playerName, int prestige,
                       String backupDatabaseId, String backupUuid, long timestamp, String time,
                       long copies, List<Item> items) { }
    record Pair(Observation earlier, Observation later, long differenceMillis) { }
    record Result(String itemUuid, String status, CurrentDuplicate current, long recordedEvents,
                  Observation firstAppearance, Observation firstSameBackupDuplicate,
                  Pair firstCrossPlayerCandidate, Pair firstHolderChange) {
        long sortTimestamp() {
            long same = firstSameBackupDuplicate == null ? Long.MAX_VALUE : firstSameBackupDuplicate.timestamp;
            long cross = firstCrossPlayerCandidate == null ? Long.MAX_VALUE : firstCrossPlayerCandidate.later.timestamp;
            return Math.min(same, cross);
        }
    }
    record Metadata(int version, long generatedAt, String sourceMaterialHistoryReport,
                    int candidateBackups, int loadedBackups, int scanErrors, List<String> shards) { }
    record Source(Path directory, Metadata metadata) { }
    record Progress(int scannedShards, int totalShards, long readEvents, long matchedEvents, long invalidEvents) { }
    record Analysis(Source source, List<Result> results, Progress progress) { }

    // Unlike the index's full DTO, this deliberately does not deserialize lore or NBT.
    private record Event(String itemUuid, String playerUuid, String playerName, int prestige,
                         String backupDatabaseId, String backupUuid, long timestamp, String time,
                         List<Item> items) {
        Observation observation() {
            if (playerUuid == null || playerUuid.isBlank() || timestamp <= 0 || items == null
                    || (blank(backupDatabaseId) && blank(backupUuid))) return null;
            Map<String, Item> positions = new LinkedHashMap<>();
            int unknown = 0;
            for (Item item : items) {
                if (item == null || item.amount <= 0) continue;
                String key = blank(item.location) ? "<unknown-" + unknown++ + ">" : item.location;
                // Repeated serialization of the same slot is not another physical item.
                positions.merge(key, item, (a, b) -> a.amount >= b.amount ? a : b);
            }
            if (positions.isEmpty()) return null;
            List<Item> kept = List.copyOf(positions.values());
            long copies = kept.stream().mapToLong(Item::amount).sum();
            return new Observation(itemUuid, playerUuid, playerName, prestige, backupDatabaseId,
                    backupUuid, timestamp, time, copies, kept);
        }
    }

    private UuidDuplicateHistory() { }

    static Path latestDuplicateReport(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return null;
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile).filter(path -> {
                String name = path.getFileName().toString();
                return name.startsWith("duplicate-mythic-item-uuid-") && name.endsWith(".json");
            }).max(Comparator.comparingLong(UuidDuplicateHistory::modified)
                    .thenComparing(path -> path.getFileName().toString())).orElse(null);
        }
    }

    static Map<String, CurrentDuplicate> readDuplicates(Path report) throws IOException {
        Map<String, CurrentDuplicate> result = new TreeMap<>();
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(report, StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String uuid = reader.nextName();
                CurrentDuplicate duplicate = GSON.fromJson(reader, CurrentDuplicate.class);
                if (!uuid.isBlank() && duplicate != null && duplicate.count > 1
                        && duplicate.locations != null && duplicate.locations.size() > 1)
                    result.put(uuid, duplicate);
            }
            reader.endObject();
            if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT)
                throw new IOException("重复 UUID 报告尾部存在多余内容: " + report);
        } catch (RuntimeException exception) {
            throw new IOException("重复 UUID 报告格式错误: " + report, exception);
        }
        return result;
    }

    static Source latestIndex(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return null;
        Path latest;
        try (Stream<Path> paths = Files.list(directory)) {
            latest = paths.filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("uuid-history-index-"))
                    .filter(path -> Files.isRegularFile(path.resolve("manifest.json")))
                    .max(Comparator.comparingLong((Path path) -> modified(path.resolve("manifest.json")))
                            .thenComparing(path -> path.getFileName().toString())).orElse(null);
        }
        if (latest == null) return null;
        try (Reader reader = Files.newBufferedReader(latest.resolve("manifest.json"), StandardCharsets.UTF_8)) {
            Metadata metadata = GSON.fromJson(reader, Metadata.class);
            if (metadata == null || metadata.version < 1 || metadata.version > 2 || metadata.shards == null
                    || metadata.shards.stream().anyMatch(shard -> shard == null || !shard.matches("[0-9a-f]{3}")))
                throw new IOException("本地 UUID 索引 manifest 不受支持或不完整: " + latest);
            return new Source(latest, metadata);
        } catch (RuntimeException exception) {
            throw new IOException("本地 UUID 索引 manifest 格式错误: " + latest, exception);
        }
    }

    static Analysis analyze(Source source, Map<String, CurrentDuplicate> duplicates, long windowMillis,
                            Consumer<Progress> progress) throws IOException {
        if (windowMillis < 0) throw new IllegalArgumentException("时间窗口不能为负数");
        Map<String, Map<String, Accumulator>> shards = new TreeMap<>();
        duplicates.forEach((uuid, current) -> shards.computeIfAbsent(shardOf(uuid), ignored -> new HashMap<>())
                .put(uuid, new Accumulator(uuid, current, windowMillis)));
        int scanned = 0;
        long read = 0, matched = 0, invalid = 0;
        for (var shard : shards.entrySet()) {
            Path path = source.directory.resolve("shard-" + shard.getKey() + ".ndjson");
            if (!source.metadata.shards.contains(shard.getKey())) {
                scanned++;
                progress.accept(new Progress(scanned, shards.size(), read, matched, invalid));
                continue;
            }
            if (!Files.isRegularFile(path)) throw new IOException("本地索引分片缺失: " + path);
            long previousTimestamp = Long.MIN_VALUE;
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String line;
                long lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank()) continue;
                    read++;
                    Event event;
                    try (JsonReader json = new JsonReader(new StringReader(line))) {
                        event = GSON.fromJson(json, Event.class);
                        if (json.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT)
                            throw new IOException("行尾存在多余内容");
                    } catch (RuntimeException | IOException exception) {
                        throw new IOException("本地索引分片格式错误: " + path + ":" + lineNumber, exception);
                    }
                    Accumulator accumulator = event == null ? null : shard.getValue().get(event.itemUuid);
                    if (accumulator != null) {
                        Observation observation = event.observation();
                        if (observation == null) invalid++;
                        else {
                            // The index builder writes each shard in chronological order. Never silently infer
                            // an earliest pair from a corrupt/out-of-order history.
                            if (observation.timestamp < previousTimestamp)
                                throw new IOException("索引时间顺序异常，请重新生成 uuidindex: " + path);
                            previousTimestamp = observation.timestamp;
                            accumulator.add(observation);
                            matched++;
                        }
                    }
                    if (read % 10_000 == 0)
                        progress.accept(new Progress(scanned, shards.size(), read, matched, invalid));
                }
            }
            scanned++;
            progress.accept(new Progress(scanned, shards.size(), read, matched, invalid));
        }
        List<Result> results = shards.values().stream().flatMap(map -> map.values().stream())
                .map(Accumulator::result).sorted(Comparator.comparingLong(Result::sortTimestamp)
                        .thenComparing(Result::itemUuid)).toList();
        return new Analysis(source, results, new Progress(scanned, shards.size(), read, matched, invalid));
    }

    private static final class Accumulator {
        final String uuid;
        final CurrentDuplicate current;
        final long window;
        long events;
        Observation first, sameBackup, latest;
        Pair cross, holderChange;
        Accumulator(String uuid, CurrentDuplicate current, long window) {
            this.uuid = uuid; this.current = current; this.window = window;
        }
        void add(Observation next) {
            events++;
            if (first == null) first = next;
            if (sameBackup == null && next.copies > 1) sameBackup = next;
            if (latest != null && !Objects.equals(latest.playerUuid, next.playerUuid)
                    && !sameBackup(latest, next)) {
                Pair pair = new Pair(latest, next, next.timestamp - latest.timestamp);
                if (holderChange == null) holderChange = pair;
                if (cross == null && pair.differenceMillis <= window) cross = pair;
            }
            // Keep just the immediately preceding observation, not an unbounded list of historical owners.
            latest = next;
        }
        Result result() {
            String status = sameBackup != null ? "SAME_BACKUP_DUPLICATE"
                    : cross != null ? "CROSS_PLAYER_CANDIDATE_ONLY"
                    : first == null ? "NO_LOCAL_HISTORY" : "NO_SIMULTANEOUS_EVIDENCE";
            return new Result(uuid, status, current, events, first, sameBackup, cross, holderChange);
        }
    }

    static Path writeReport(Path output, Path duplicateReport, Analysis analysis, long windowSeconds) throws IOException {
        Path temporary = output.resolveSibling(output.getFileName() + ".tmp");
        try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(temporary, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW))) {
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("version").value(1);
            writer.name("generatedAtTimestamp").value(System.currentTimeMillis());
            writer.name("duplicateReport").value(duplicateReport.toAbsolutePath().toString());
            writer.name("duplicateReportModifiedAt").value(modified(duplicateReport));
            writer.name("historyIndex").value(analysis.source.directory.toAbsolutePath().toString());
            writer.name("historyMetadata"); GSON.toJson(analysis.source.metadata, Metadata.class, writer);
            writer.name("crossPlayerWindowSeconds").value(windowSeconds);
            writer.name("limitations");
            GSON.toJson(List.of(
                    "结果仅覆盖所选本地索引已收录的备份；最早表示已收录记录中最早，不是实际复制时间。",
                    "firstSameBackupDuplicate 表示同一份玩家备份中该 UUID 数量大于 1（包含仓库/穿戴/副手）。",
                    "firstCrossPlayerCandidate 仅表示不同玩家的备份时间接近，不能证明同时持有或谁复制了物品。",
                    "firstHolderChange 仅供人工核查，先后转手本身不算重复证据；未出现的备份不视为物品消失。",
                    "旧版 v1 索引可能合并掉相同物品数量变化，且任何版本均可能省略重复状态观测；无命中不代表无复制。",
                    "本地索引不含收纳箱内部 UUID，当前重复来源若为收纳箱内部，可能无法追溯。"
            ), List.class, writer);
            writer.name("progress"); GSON.toJson(analysis.progress, Progress.class, writer);
            writer.name("results").beginArray();
            for (Result result : analysis.results) GSON.toJson(result, Result.class, writer);
            writer.endArray().endObject();
        }
        publish(temporary, output);
        return output;
    }

    static void publish(Path temporary, Path output) throws IOException {
        if (Files.exists(output)) throw new FileAlreadyExistsException(output.toString());
        try { Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, output); }
    }
    static String shardOf(String uuid) { return String.format("%03x", uuid.hashCode() & 0x3ff); }
    private static boolean sameBackup(Observation a, Observation b) {
        return (!blank(a.backupDatabaseId) && a.backupDatabaseId.equals(b.backupDatabaseId))
                || (!blank(a.backupUuid) && a.backupUuid.equals(b.backupUuid));
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static long modified(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); }
        catch (IOException ignored) { return Long.MIN_VALUE; }
    }
}
