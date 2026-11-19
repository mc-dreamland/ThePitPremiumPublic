package cn.charlotte.pit.commands.admin;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** File-only streaming reader. No Bukkit, database, item deserialization or full-report object graph. */
final class MaterialHistoryLocalReport {
    record Header(Path path, Map<String, String> items, boolean allUuidAdvanced, int minimumItems,
                  boolean allAdvancedItems, boolean includesItemsWithoutUuid) {
        String resolveItem(String query) {
            String normalized = normalize(query);
            for (var entry : items.entrySet())
                if (normalize(entry.getKey()).equals(normalized) || normalize(entry.getValue()).equals(normalized))
                    return normalize(entry.getKey());
            throw new IllegalArgumentException("该本地报告未统计这个物品类型: " + query + "；可先运行 materialhistory all。");
        }
        String label(String item) { return items.getOrDefault(item, item); }
        String scopeLabel() {
            if (allAdvancedItems) return "全部高级道具及原物资" + (includesItemsWithoutUuid ? "（含无 UUID）" : "");
            return allUuidAdvanced ? "旧版：仅带有效 UUID 的高级道具" : "旧版：指定稀有物资";
        }
    }
    record Row(String databaseId, String backupUuid, String playerUuid, String playerName, int prestige,
               long timestamp, String time, long inventory, long warehouse, long total) { }
    record PlayerCounts(String uuid, String name, int prestige, int snapshots, int positiveSnapshots,
                        long maximum, long firstTimestamp, long lastTimestamp) { }
    enum Order {
        DATE_ASC("日期：旧 → 新"), DATE_DESC("日期：新 → 旧"),
        QUANTITY_DESC("数量：多 → 少"), QUANTITY_ASC("数量：少 → 多");
        final String label;
        Order(String label) { this.label = label; }
        Order next() { return values()[(ordinal() + 1) % values().length]; }
    }
    private MaterialHistoryLocalReport() { }

    static Path latest(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return null;
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).filter(path -> {
                String name = path.getFileName().toString();
                return name.endsWith(".json") && (name.startsWith("rare-material-history-")
                        || name.startsWith("all-material-history-") || name.startsWith("all-uuid-material-history-"));
            }).max(Comparator.comparingLong(MaterialHistoryLocalReport::modified)
                    .thenComparing(path -> path.getFileName().toString())).orElse(null);
        }
    }
    private static long modified(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); }
        catch (IOException ignored) { return Long.MIN_VALUE; }
    }
    static Header header(Path path) throws IOException {
        Map<String, String> labels = new LinkedHashMap<>();
        boolean all = false;
        boolean advanced = false, withoutUuid = false;
        int minimum = 0;
        try (JsonReader reader = reader(path)) {
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "allUuidAdvanced" -> all = reader.nextBoolean();
                    case "allAdvancedItems" -> advanced = reader.nextBoolean();
                    case "includesItemsWithoutUuid" -> withoutUuid = reader.nextBoolean();
                    case "minimumItems" -> minimum = reader.nextInt();
                    case "trackedItems" -> {
                        reader.beginObject();
                        while (reader.hasNext()) labels.put(normalize(reader.nextName()), reader.nextString());
                        reader.endObject();
                    }
                    // Both report writers put metadata first. Do not traverse millions of snapshots just for the header.
                    case "snapshots" -> { return new Header(path, Map.copyOf(labels), all, minimum, advanced, withoutUuid); }
                    default -> reader.skipValue();
                }
            }
            reader.endObject();
        }
        return new Header(path, Map.copyOf(labels), all, minimum, advanced, withoutUuid);
    }
    static List<PlayerCounts> players(Header header, String item) throws IOException {
        class Aggregate {
            String uuid, name;
            int prestige = -1, snapshots, positive;
            long maximum, first = Long.MAX_VALUE, last, namedAt = Long.MIN_VALUE;
            void add(Row row) {
                uuid = row.playerUuid;
                snapshots++;
                if (row.timestamp >= namedAt) { name = row.playerName; prestige = row.prestige; namedAt = row.timestamp; }
                if (row.total <= 0) return;
                positive++;
                maximum = Math.max(maximum, row.total);
                first = Math.min(first, row.timestamp);
                last = Math.max(last, row.timestamp);
            }
            PlayerCounts build() {
                return new PlayerCounts(uuid, name == null || name.isBlank() ? "<unknown>" : name,
                        prestige, snapshots, positive, maximum, first, last);
            }
        }
        Map<String, Aggregate> grouped = new HashMap<>();
        visit(header.path, item, row -> {
            if (row.playerUuid != null && !row.playerUuid.isBlank())
                grouped.computeIfAbsent(row.playerUuid, ignored -> new Aggregate()).add(row);
        });
        return grouped.values().stream().filter(value -> value.positive > 0).map(Aggregate::build).toList();
    }
    static List<Row> rows(Header header, String item, String uuid) throws IOException {
        List<Row> rows = new ArrayList<>();
        visit(header.path, item, row -> { if (uuid.equalsIgnoreCase(row.playerUuid)) rows.add(row); });
        return rows;
    }

    /** Resolve names entirely from local snapshots; never use online-player or database lookups. */
    static String resolvePlayerUuid(Header header, String requestedPlayer) throws IOException {
        String query = normalize(requestedPlayer);
        if (query.isEmpty()) throw new IllegalArgumentException("请指定要查询的玩家名字或 UUID。");
        Set<String> names = new HashSet<>(), uuids = new HashSet<>();
        visit(header.path, "", row -> {
            if (row.playerUuid == null || row.playerUuid.isBlank()) return;
            String uuid = normalize(row.playerUuid);
            if (uuid.equals(query)) uuids.add(uuid);
            if (normalize(row.playerName).equals(query)) names.add(uuid);
        });
        Set<String> matches = uuids.isEmpty() ? names : uuids;
        if (matches.isEmpty()) throw new IllegalArgumentException("本地报告未找到该玩家的备份记录: " + requestedPlayer);
        if (matches.size() > 1)
            throw new IllegalArgumentException("本地报告中该名字对应多个玩家，请改用玩家 UUID 查询: " + requestedPlayer);
        return matches.iterator().next();
    }

    /** Read reference metadata only, including all-mode reports, without loading counts or stacked-item details. */
    static List<MaterialHistoryBackupReference> backupReferences(Path report) throws IOException {
        List<MaterialHistoryBackupReference> references = new ArrayList<>();
        visit(report, "", row -> {
            if (row.playerUuid != null && !row.playerUuid.isBlank() && row.timestamp > 0
                    && (row.databaseId != null || row.backupUuid != null))
                references.add(new MaterialHistoryBackupReference(row.databaseId, row.backupUuid, row.playerUuid,
                        row.playerName, row.prestige, row.timestamp, row.time));
        });
        return references.stream().distinct().sorted(Comparator.comparingLong(MaterialHistoryBackupReference::timestamp)
                .thenComparing(MaterialHistoryBackupReference::playerUuid)
                .thenComparing(MaterialHistoryBackupReference::databaseId, Comparator.nullsFirst(String::compareTo)))
                .toList();
    }
    static List<Row> sortRows(List<Row> rows, Order order) {
        Comparator<Row> dates = Comparator.comparingLong(Row::timestamp)
                .thenComparing(Row::databaseId, Comparator.nullsFirst(String::compareTo))
                .thenComparing(Row::backupUuid, Comparator.nullsFirst(String::compareTo));
        Comparator<Row> comparator = switch (order) {
            case DATE_ASC -> dates;
            case DATE_DESC -> Comparator.comparingLong(Row::timestamp).reversed().thenComparing(dates);
            case QUANTITY_DESC -> Comparator.comparingLong(Row::total).reversed().thenComparing(dates);
            case QUANTITY_ASC -> Comparator.comparingLong(Row::total).thenComparing(dates);
        };
        return rows.stream().sorted(comparator).toList();
    }
    static List<PlayerCounts> sortPlayers(List<PlayerCounts> players, Order order) {
        Comparator<PlayerCounts> comparator = switch (order) {
            case DATE_ASC -> Comparator.comparingLong(PlayerCounts::firstTimestamp);
            case DATE_DESC -> Comparator.comparingLong(PlayerCounts::lastTimestamp).reversed();
            case QUANTITY_DESC -> Comparator.comparingLong(PlayerCounts::maximum).reversed();
            case QUANTITY_ASC -> Comparator.comparingLong(PlayerCounts::maximum);
        };
        return players.stream().sorted(comparator.thenComparing(PlayerCounts::uuid)).toList();
    }
    private static JsonReader reader(Path path) throws IOException {
        return new JsonReader(Files.newBufferedReader(path, StandardCharsets.UTF_8));
    }
    private static void visit(Path path, String item, Consumer<Row> consumer) throws IOException {
        try (JsonReader reader = reader(path)) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (!reader.nextName().equals("snapshots")) { reader.skipValue(); continue; }
                reader.beginArray();
                while (reader.hasNext()) {
                    if (reader.peek() == JsonToken.NULL) { reader.skipValue(); continue; }
                    consumer.accept(row(reader, item));
                }
                reader.endArray();
            }
            reader.endObject();
        }
    }
    private static Row row(JsonReader reader, String item) throws IOException {
        String database = null, backup = null, uuid = null, name = null, time = null;
        int prestige = -1;
        long timestamp = 0, inventory = 0, warehouse = 0, total = -1;
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
                case "inventoryCounts" -> { if (item.isEmpty()) reader.skipValue(); else inventory = Math.max(0, count(reader, item)); }
                case "warehouseCounts" -> { if (item.isEmpty()) reader.skipValue(); else warehouse = Math.max(0, count(reader, item)); }
                case "totalCounts" -> { if (item.isEmpty()) reader.skipValue(); else total = count(reader, item); }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return new Row(database, backup, uuid, name, prestige, timestamp, time, inventory, warehouse,
                total < 0 ? Math.addExact(inventory, warehouse) : total);
    }
    private static long count(JsonReader reader, String item) throws IOException {
        long amount = -1;
        reader.beginObject();
        while (reader.hasNext()) {
            if (normalize(reader.nextName()).equals(item) && reader.peek() != JsonToken.NULL)
                amount = Math.addExact(Math.max(0, amount), Math.max(0, reader.nextLong()));
            else reader.skipValue();
        }
        reader.endObject();
        return amount;
    }
    static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
}
