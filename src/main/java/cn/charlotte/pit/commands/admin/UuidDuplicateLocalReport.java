package cn.charlotte.pit.commands.admin;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static cn.charlotte.pit.commands.admin.UuidDuplicateHistory.Observation;
import static cn.charlotte.pit.commands.admin.UuidDuplicateHistory.Pair;

/** Local report browser. Retains one page of summaries, not the full scan or a database cursor. */
final class UuidDuplicateLocalReport {
    static final int PAGE_SIZE = 45;
    private static final Gson GSON = new Gson();
    enum Mode {
        CURRENT("duplicate-mythic-item-uuid-"), HISTORY("duplicate-uuid-history-");
        final String prefix;
        Mode(String prefix) { this.prefix = prefix; }
    }
    record Report(Path path, Mode mode, long generatedAt, String duplicateReport, String historyIndex,
                  long windowSeconds, int indexVersion, int scanErrors, long invalidEvents) { }
    record Location(String playerUuid, String playerName, String container, Integer warehouseIndex,
                    Integer slot, Integer displaySlot, String slotName, String displayName, List<String> lore,
                    String color, String material, String holderItemInternalName, String nestedPath) { }
    record Current(int count, List<Location> locations) {
        Current { locations = locations == null ? List.of() : locations.stream().filter(Objects::nonNull).toList(); }
    }
    record History(String itemUuid, String status, Current current, long recordedEvents,
                   Observation firstAppearance, Observation firstSameBackupDuplicate,
                   Pair firstCrossPlayerCandidate, Pair firstHolderChange) { }
    record Group(String itemUuid, Current current, History history) {
        List<Location> locations() { return current == null ? List.of() : current.locations(); }
        int count() { return current == null ? 0 : current.count(); }
    }
    record Summary(String itemUuid, String displayName, String material, int count, long holders,
                   String status, long clueTimestamp) { }
    record Page(List<Summary> items, long total, int number) { }
    record HistoricalItem(String location, String internalName, String material, String displayName,
                          int amount, List<String> lore) { }
    record HistoricalDetails(List<HistoricalItem> items, String warning) { }
    record Evidence(List<String> labels, Observation observation) { }
    private record EvidenceKey(String playerUuid, String databaseId, String backupUuid, long timestamp) { }
    private record IndexEvent(String itemUuid, String playerUuid, String backupDatabaseId, String backupUuid,
                              long timestamp, List<HistoricalItem> items) { }

    private UuidDuplicateLocalReport() { }

    static Report latest(Path directory, Mode mode) throws IOException {
        if (!Files.isDirectory(directory)) return null;
        Path latest;
        try (Stream<Path> files = Files.list(directory)) {
            latest = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(mode.prefix)
                            && path.getFileName().toString().endsWith(".json"))
                    .max(Comparator.comparingLong(UuidDuplicateLocalReport::modified)
                            .thenComparing(path -> path.getFileName().toString())).orElse(null);
        }
        return latest == null ? null : report(latest, mode);
    }

    static Report report(Path path, Mode mode) throws IOException {
        long generated = modified(path), window = 0, invalid = 0;
        String duplicates = null, index = null;
        int version = 0, indexVersion = 0, errors = 0;
        if (mode == Mode.CURRENT) return new Report(path, mode, generated, null, null, 0, 0, 0, 0);
        try (JsonReader reader = reader(path)) {
            reader.beginObject();
            while (reader.hasNext()) {
                String field = reader.nextName();
                if (field.equals("results")) break;
                if (reader.peek() == JsonToken.NULL) { reader.skipValue(); continue; }
                switch (field) {
                    case "version" -> version = reader.nextInt();
                    case "generatedAtTimestamp" -> generated = reader.nextLong();
                    case "duplicateReport" -> duplicates = reader.nextString();
                    case "historyIndex" -> index = reader.nextString();
                    case "crossPlayerWindowSeconds" -> window = reader.nextLong();
                    case "historyMetadata" -> {
                        UuidDuplicateHistory.Metadata metadata = GSON.fromJson(reader, UuidDuplicateHistory.Metadata.class);
                        if (metadata != null) { indexVersion = metadata.version(); errors = metadata.scanErrors(); }
                    }
                    case "progress" -> {
                        UuidDuplicateHistory.Progress progress = GSON.fromJson(reader, UuidDuplicateHistory.Progress.class);
                        if (progress != null) invalid = progress.invalidEvents();
                    }
                    default -> reader.skipValue();
                }
            }
        } catch (RuntimeException exception) { throw new IOException("历史报告元数据格式错误: " + path, exception); }
        if (version != 1) throw new IOException("不支持的重复 UUID 历史报告版本: " + version);
        return new Report(path, mode, generated, duplicates, index, window, indexVersion, errors, invalid);
    }

    static Page page(Report report, String requestedFilter, int number) throws IOException {
        if (number < 0) throw new IllegalArgumentException("页数不能为负数。");
        String filter = normalize(requestedFilter);
        long start = (long) number * PAGE_SIZE;
        long[] total = {0};
        List<Summary> items = new ArrayList<>(PAGE_SIZE);
        visit(report, group -> {
            if (!matches(group, filter)) return;
            long position = total[0]++;
            if (position >= start && items.size() < PAGE_SIZE) items.add(summary(group));
        });
        return new Page(List.copyOf(items), total[0], number);
    }

    static Group group(Report report, String itemUuid) throws IOException {
        Group[] found = {null};
        visit(report, group -> { if (normalize(group.itemUuid()).equals(normalize(itemUuid))) found[0] = group; });
        if (found[0] == null) throw new IllegalArgumentException("这份本地报告未记录该物品 UUID: " + itemUuid);
        return found[0];
    }

    static Path sourceCurrent(Report report) {
        String name = basename(report.duplicateReport());
        if (!name.startsWith(Mode.CURRENT.prefix) || !name.endsWith(".json")) return null;
        Path path = report.path().toAbsolutePath().getParent().resolve(name);
        return Files.isRegularFile(path) ? path : null;
    }

    /** Only the source index directly below the configured materialhistory directory may supply historical Lore. */
    static HistoricalDetails historicalDetails(Report report, Observation observation, Path materialDirectory) {
        List<HistoricalItem> fallback = observation.items() == null ? List.of() : observation.items().stream()
                .filter(Objects::nonNull).map(item -> new HistoricalItem(item.location(), item.internalName(),
                        item.material(), item.displayName(), item.amount(), null)).toList();
        if (blank(observation.itemUuid())) return new HistoricalDetails(fallback, "历史记录缺少物品 UUID，无法从本地索引补读完整 Lore。");
        String name = basename(report.historyIndex());
        if (!name.matches("uuid-history-index-[\\p{L}\\p{N}._-]+"))
            return new HistoricalDetails(fallback, "历史报告未保存完整 Lore，且没有可用的对应本地索引。");
        Path root = materialDirectory.toAbsolutePath().normalize();
        Path directory = root.resolve(name).normalize();
        if (!directory.getParent().equals(root) || !Files.isRegularFile(directory.resolve("manifest.json")))
            return new HistoricalDetails(fallback, "对应本地索引不存在，仅展示历史报告中的物品概要。");
        Path shard = directory.resolve("shard-" + UuidDuplicateHistory.shardOf(observation.itemUuid()) + ".ndjson");
        try (BufferedReader reader = Files.newBufferedReader(shard, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                IndexEvent event;
                try (JsonReader json = new JsonReader(new StringReader(line))) {
                    event = GSON.fromJson(json, IndexEvent.class);
                    if (json.peek() != JsonToken.END_DOCUMENT) throw new IOException("索引行尾存在多余内容");
                }
                if (event == null || !Objects.equals(event.itemUuid(), observation.itemUuid())
                        || !Objects.equals(event.playerUuid(), observation.playerUuid()) || event.timestamp() != observation.timestamp()
                        || !sameBackup(event, observation) || event.items() == null) continue;
                List<HistoricalItem> enriched = new ArrayList<>();
                for (HistoricalItem wanted : fallback) {
                    HistoricalItem found = event.items().stream().filter(Objects::nonNull)
                            .filter(item -> Objects.equals(item.location(), wanted.location()) && item.amount() == wanted.amount()
                                    && Objects.equals(item.internalName(), wanted.internalName())
                                    && Objects.equals(item.material(), wanted.material())
                                    && Objects.equals(item.displayName(), wanted.displayName())).findFirst().orElse(wanted);
                    enriched.add(found);
                }
                return new HistoricalDetails(List.copyOf(enriched), enriched.stream().anyMatch(item -> item.lore() == null)
                        ? "部分位置缺少完整 Lore，未使用其他时间或其他玩家的物品状态替代。" : null);
            }
        } catch (IOException | RuntimeException exception) {
            return new HistoricalDetails(fallback, "对应本地索引无法读取，仅展示报告概要，不能还原当时完整 Lore。");
        }
        return new HistoricalDetails(fallback, "对应索引未匹配到同玩家、同备份、同时间记录，仅展示报告概要。");
    }

    private static boolean sameBackup(IndexEvent event, Observation observation) {
        if (!blank(observation.backupDatabaseId())) return observation.backupDatabaseId().equals(event.backupDatabaseId());
        return !blank(observation.backupUuid()) && observation.backupUuid().equals(event.backupUuid());
    }

    private static void visit(Report report, Consumer<Group> consumer) throws IOException {
        try (JsonReader reader = reader(report.path())) {
            reader.beginObject();
            boolean results = report.mode() == Mode.CURRENT;
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (report.mode() == Mode.CURRENT) {
                    Current current = GSON.fromJson(reader, Current.class);
                    if (!blank(key) && current != null && current.count() > 1 && current.locations().size() > 1)
                        consumer.accept(new Group(key, current, null));
                } else if (key.equals("results")) {
                    results = true;
                    reader.beginArray();
                    while (reader.hasNext()) {
                        History history = GSON.fromJson(reader, History.class);
                        if (history != null && !blank(history.itemUuid()))
                            consumer.accept(new Group(history.itemUuid(), history.current(), history));
                    }
                    reader.endArray();
                } else reader.skipValue();
            }
            reader.endObject();
            if (!results || reader.peek() != JsonToken.END_DOCUMENT) throw new IOException("报告缺少结果或尾部存在多余内容");
        } catch (RuntimeException exception) { throw new IOException("本地重复 UUID 报告格式错误: " + report.path(), exception); }
    }

    private static boolean matches(Group group, String query) {
        if (query.isEmpty() || normalize(group.itemUuid()).equals(query)) return true;
        if (group.locations().stream().anyMatch(location -> normalize(location.playerName()).equals(query)
                || normalize(location.playerUuid()).equals(query))) return true;
        return observations(group.history()).stream().anyMatch(observation -> normalize(observation.playerName()).equals(query)
                || normalize(observation.playerUuid()).equals(query));
    }

    static List<Observation> observations(History history) {
        if (history == null) return List.of();
        List<Observation> observations = new ArrayList<>();
        if (history.firstAppearance() != null) observations.add(history.firstAppearance());
        if (history.firstSameBackupDuplicate() != null) observations.add(history.firstSameBackupDuplicate());
        for (Pair pair : new Pair[]{history.firstCrossPlayerCandidate(), history.firstHolderChange()})
            if (pair != null) { if (pair.earlier() != null) observations.add(pair.earlier()); if (pair.later() != null) observations.add(pair.later()); }
        return observations;
    }

    static List<Evidence> evidence(History history) {
        if (history == null) return List.of();
        Map<EvidenceKey, Evidence> grouped = new LinkedHashMap<>();
        addEvidence(grouped, "本地首次出现", history.firstAppearance());
        addEvidence(grouped, "最早同一备份内重复", history.firstSameBackupDuplicate());
        Pair cross = history.firstCrossPlayerCandidate(), transfer = history.firstHolderChange();
        if (cross != null) {
            addEvidence(grouped, "跨玩家时间接近候选 · 较早一方", cross.earlier());
            addEvidence(grouped, "跨玩家时间接近候选 · 较晚一方", cross.later());
        }
        if (transfer != null) {
            addEvidence(grouped, "首次观察到持有人变化 · 之前", transfer.earlier());
            addEvidence(grouped, "首次观察到持有人变化 · 之后", transfer.later());
        }
        return grouped.values().stream().sorted(Comparator.comparingLong(value -> value.observation().timestamp())).toList();
    }

    private static void addEvidence(Map<EvidenceKey, Evidence> grouped, String label, Observation observation) {
        if (observation == null) return;
        EvidenceKey key = new EvidenceKey(observation.playerUuid(), observation.backupDatabaseId(),
                observation.backupUuid(), observation.timestamp());
        grouped.compute(key, (ignored, previous) -> {
            List<String> labels = previous == null ? new ArrayList<>() : new ArrayList<>(previous.labels());
            labels.add(label);
            return new Evidence(List.copyOf(labels), observation);
        });
    }

    private static Summary summary(Group group) {
        Location location = group.locations().isEmpty() ? null : group.locations().getFirst();
        String name = location == null ? null : location.displayName();
        String material = location == null ? null : location.material();
        History history = group.history();
        if (history != null && history.firstAppearance() != null && history.firstAppearance().items() != null
                && !history.firstAppearance().items().isEmpty()) {
            var item = history.firstAppearance().items().stream().filter(Objects::nonNull).findFirst().orElse(null);
            if (item != null) { if (blank(name)) name = item.displayName(); if (blank(material)) material = item.material(); }
        }
        long clue = Long.MAX_VALUE;
        if (history != null) {
            if (history.firstSameBackupDuplicate() != null) clue = history.firstSameBackupDuplicate().timestamp();
            if (history.firstCrossPlayerCandidate() != null && history.firstCrossPlayerCandidate().later() != null)
                clue = Math.min(clue, history.firstCrossPlayerCandidate().later().timestamp());
        }
        long holders = group.locations().stream().map(value -> blank(value.playerUuid())
                ? "name:" + normalize(value.playerName()) : normalize(value.playerUuid())).distinct().count();
        return new Summary(group.itemUuid(), name, material, group.count(), holders,
                history == null ? null : history.status(), clue == Long.MAX_VALUE ? 0 : clue);
    }

    private static JsonReader reader(Path path) throws IOException { return new JsonReader(Files.newBufferedReader(path, StandardCharsets.UTF_8)); }
    private static String basename(String path) {
        if (path == null) return "";
        String normalized = path.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private static long modified(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); } catch (IOException exception) { return Long.MIN_VALUE; }
    }
}
