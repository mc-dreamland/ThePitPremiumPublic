package cn.charlotte.pit.commands.admin;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static cn.charlotte.pit.commands.admin.MaterialHistoryLocalReport.*;

/** File-only per-player peaks. Retains one snapshot's counts and one aggregate per item, not all backups. */
final class MaterialHistoryPlayerMaximum {
    record ItemMaximum(String item, Row peak, int positiveSnapshots, long firstTimestamp, long lastTimestamp) {
        long maximum() { return peak.total(); }
    }
    record Result(String uuid, String name, int prestige, int snapshots, List<ItemMaximum> items) { }
    private record Snapshot(Row metadata, Map<String, Long> inventory, Map<String, Long> warehouse,
                            Map<String, Long> totals) { }

    private static final Comparator<Row> EVIDENCE = Comparator.comparingLong(Row::timestamp)
            .thenComparing(Row::databaseId, Comparator.nullsFirst(String::compareTo))
            .thenComparing(Row::backupUuid, Comparator.nullsFirst(String::compareTo));

    private MaterialHistoryPlayerMaximum() { }

    static Result query(Header header, String requestedPlayer) throws IOException {
        // Resolve first so name changes and out-of-order rows do not omit older/newer snapshots of the same UUID.
        String uuid = resolvePlayerUuid(header, requestedPlayer);
        Map<String, Aggregate> items = new HashMap<>();
        Row latest = null;
        int snapshots = 0;
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(header.path(), StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (!reader.nextName().equals("snapshots")) { reader.skipValue(); continue; }
                reader.beginArray();
                while (reader.hasNext()) {
                    if (reader.peek() == JsonToken.NULL) { reader.skipValue(); continue; }
                    Snapshot snapshot = snapshot(reader);
                    Row row = snapshot.metadata;
                    if (!uuid.equals(normalize(row.playerUuid()))) continue;
                    snapshots++;
                    if (latest == null || EVIDENCE.compare(row, latest) > 0) latest = row;
                    Set<String> types = new HashSet<>(snapshot.inventory.keySet());
                    types.addAll(snapshot.warehouse.keySet());
                    types.addAll(snapshot.totals.keySet());
                    for (String item : types) {
                        long inventory = snapshot.inventory.getOrDefault(item, 0L);
                        long warehouse = snapshot.warehouse.getOrDefault(item, 0L);
                        Long recorded = snapshot.totals.get(item);
                        long total = recorded == null ? Math.addExact(inventory, warehouse) : recorded;
                        if (total <= 0) continue;
                        Row evidence = new Row(row.databaseId(), row.backupUuid(), row.playerUuid(), row.playerName(),
                                row.prestige(), row.timestamp(), row.time(), inventory, warehouse, total);
                        items.computeIfAbsent(item, ignored -> new Aggregate()).add(evidence);
                    }
                }
                reader.endArray();
            }
            reader.endObject();
        }
        if (latest == null) throw new IOException("玩家身份已找到，但本地报告中的备份已发生变化，请重试。");
        String name = latest.playerName() == null || latest.playerName().isBlank() ? uuid : latest.playerName();
        List<ItemMaximum> maxima = items.entrySet().stream().map(entry -> entry.getValue().build(entry.getKey())).toList();
        return new Result(uuid, name, latest.prestige(), snapshots, sort(maxima, Order.QUANTITY_DESC));
    }

    static List<ItemMaximum> sort(List<ItemMaximum> items, Order order) {
        Comparator<ItemMaximum> comparator = switch (order) {
            case DATE_ASC -> Comparator.comparingLong(item -> item.peak.timestamp());
            case DATE_DESC -> Comparator.comparingLong((ItemMaximum item) -> item.peak.timestamp()).reversed();
            case QUANTITY_DESC -> Comparator.comparingLong(ItemMaximum::maximum).reversed();
            case QUANTITY_ASC -> Comparator.comparingLong(ItemMaximum::maximum);
        };
        return items.stream().sorted(comparator.thenComparing(ItemMaximum::item)).toList();
    }

    private static final class Aggregate {
        Row peak;
        int positive;
        long first = Long.MAX_VALUE, last = Long.MIN_VALUE;
        void add(Row row) {
            positive++;
            first = Math.min(first, row.timestamp());
            last = Math.max(last, row.timestamp());
            // Equal maxima show the earliest evidence, regardless of the input report's order.
            if (peak == null || row.total() > peak.total()
                    || (row.total() == peak.total() && EVIDENCE.compare(row, peak) < 0)) peak = row;
        }
        ItemMaximum build(String item) { return new ItemMaximum(item, peak, positive, first, last); }
    }

    private static Snapshot snapshot(JsonReader reader) throws IOException {
        String database = null, backup = null, uuid = null, name = null, time = null;
        int prestige = -1;
        long timestamp = 0;
        Map<String, Long> inventory = Map.of(), warehouse = Map.of(), total = Map.of();
        reader.beginObject();
        while (reader.hasNext()) {
            String field = reader.nextName();
            if (reader.peek() == JsonToken.NULL) { reader.skipValue(); continue; }
            switch (field) {
                case "databaseId" -> database = reader.nextString();
                case "backupUuid" -> backup = reader.nextString();
                case "playerUuid" -> uuid = reader.nextString();
                case "playerName" -> name = reader.nextString();
                case "prestige" -> prestige = reader.nextInt();
                case "timestamp" -> timestamp = reader.nextLong();
                case "time" -> time = reader.nextString();
                case "inventoryCounts" -> inventory = counts(reader);
                case "warehouseCounts" -> warehouse = counts(reader);
                case "totalCounts" -> total = counts(reader);
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return new Snapshot(new Row(database, backup, uuid, name, prestige, timestamp, time, 0, 0, 0), inventory, warehouse, total);
    }

    private static Map<String, Long> counts(JsonReader reader) throws IOException {
        Map<String, Long> counts = new HashMap<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String item = normalize(reader.nextName());
            if (reader.peek() == JsonToken.NULL) { reader.skipValue(); continue; }
            long amount = Math.max(0, reader.nextLong());
            if (!item.isEmpty()) counts.merge(item, amount, Math::addExact);
        }
        reader.endObject();
        return counts;
    }
}
