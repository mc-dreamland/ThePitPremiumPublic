package cn.charlotte.pit.commands.admin;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

import static cn.charlotte.pit.commands.admin.MaterialEnchantmentHistory.*;

/** File-only export of recorded operations, including gems. Never attributes backup state changes to a player. */
final class MaterialEnchantmentExport {
    private static final Gson JSON = new Gson();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final long BUFFER_CHARACTERS = 4L * 1024 * 1024;
    record Row(long timestamp, String playerName, String itemUuid, String source) { }
    record Progress(int scannedItems, int totalItems, long matchedRecords, String stage) { }
    record Result(Path path, String playerName, long records, long matchedRecords, int scannedItems, long suspectRecords) { }
    private record InputOperation(OperationId id, String playerName, boolean timestampAfterFirstEvidence) { }
    private static final Comparator<Row> ORDER = Comparator.comparingLong(Row::timestamp).thenComparing(Row::itemUuid)
            .thenComparing(Row::source).thenComparing(row -> row.playerName().toLowerCase(Locale.ROOT)).thenComparing(Row::playerName);

    private MaterialEnchantmentExport() { }

    static Result export(Report report, String requestedPlayer, Path directory, ZoneId zone, Consumer<Progress> progress) throws IOException {
        return export(report, requestedPlayer, directory, zone, progress, 10_000, 32);
    }

    static Result export(Report report, String requestedPlayer, Path directory, ZoneId zone, Consumer<Progress> progress,
                         int chunkSize, int fanIn) throws IOException {
        if (chunkSize < 1 || fanIn < 2) throw new IllegalArgumentException("无效的导出排序参数。");
        String query = requestedPlayer == null ? "" : requestedPlayer.trim();
        if (query.isEmpty()) throw new IllegalArgumentException("请指定 records 中的玩家名字。");
        Actor actor = report.manifest().actors().stream().filter(value -> query.equalsIgnoreCase(value.playerName())
                || query.equalsIgnoreCase(value.key())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("本地 records 中未找到该操作者: " + query + "（不是按背包持有人查询）。"));
        if (UNKNOWN_ACTOR.equals(actor.key()) || actor.recordCount() <= 0)
            throw new IllegalArgumentException("该操作者没有实际 records 可导出，不能用备份持有人或候选变化代替附魔者。");
        Path index = actorPath(report.directory(), actor.key());
        if (!Files.isRegularFile(index)) throw new IOException("玩家附魔装备索引缺失: " + index);
        Files.createDirectories(directory);
        Path work = Files.createTempDirectory(directory, ".enchantrecords-export-");
        Path temporary = null;
        try {
            Sorter sorter = new Sorter(work, chunkSize);
            int scanned = 0;
            long[] matched = {0}, suspect = {0};
            progress.accept(new Progress(0, actor.itemCount(), 0, "读取操作记录"));
            try (BufferedReader reader = Files.newBufferedReader(index, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    ActorItem item;
                    try (JsonReader json = new JsonReader(new StringReader(line))) {
                        item = JSON.fromJson(json, ActorItem.class);
                        if (json.peek() != JsonToken.END_DOCUMENT || item == null || blank(item.itemUuid()))
                            throw new IOException("无效的玩家附魔装备索引行");
                    } catch (RuntimeException exception) { throw new IOException("玩家附魔装备索引格式错误: " + index, exception); }
                    readOperations(report, item.itemUuid(), actor.key(), operation -> {
                        sorter.add(new Row(operation.id().timestamp(), operation.playerName(), item.itemUuid(), operation.id().source()));
                        matched[0]++;
                        if (operation.timestampAfterFirstEvidence()) suspect[0]++;
                    });
                    scanned++;
                    if (scanned % 1000 == 0) progress.accept(new Progress(scanned, actor.itemCount(), matched[0], "读取操作记录"));
                }
            }
            sorter.flush();
            if (matched[0] == 0) throw new IOException("该玩家索引中未找到实际 records，请核对或重建 enchantrecords 本地整理结果。");
            progress.accept(new Progress(scanned, actor.itemCount(), matched[0], "分批归并排序"));
            List<Path> runs = sorter.runs;
            int round = 0;
            while (runs.size() > fanIn) {
                List<Path> merged = new ArrayList<>();
                for (int start = 0; start < runs.size(); start += fanIn) {
                    List<Path> inputs = runs.subList(start, Math.min(start + fanIn, runs.size()));
                    Path output = work.resolve("merge-" + round + "-" + merged.size() + ".ndjson");
                    try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
                        merge(inputs, row -> { writer.write(JSON.toJson(row)); writer.newLine(); });
                    }
                    merged.add(output);
                    for (Path input : inputs) Files.delete(input);
                }
                runs = merged; round++;
            }
            String prefix = "enchant-records-" + ScanReportPath.sanitize(actor.playerName()) + "-"
                    + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(zone).format(Instant.now()) + "-";
            temporary = Files.createTempFile(directory, prefix, ".txt.tmp");
            long count;
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                count = merge(runs, row -> { writer.write(format(row, zone)); writer.newLine(); });
            }
            Path output = temporary.resolveSibling(temporary.getFileName().toString().replaceFirst("\\.tmp$", ""));
            UuidDuplicateHistory.publish(temporary, output);
            progress.accept(new Progress(scanned, actor.itemCount(), matched[0], "完成"));
            return new Result(output, actor.playerName(), count, matched[0], scanned, suspect[0]);
        } finally {
            // Only remove files from this export's own freshly-created work directory.
            try (var files = Files.list(work)) {
                for (Path file : files.toList()) try { Files.deleteIfExists(file); } catch (IOException ignored) { }
            } catch (IOException ignored) { }
            try { Files.deleteIfExists(work); } catch (IOException ignored) { }
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    private interface OperationConsumer { void accept(InputOperation operation) throws IOException; }
    private static void readOperations(Report report, String uuid, String actor, OperationConsumer consumer) throws IOException {
        Path file = itemPath(report.directory(), uuid);
        String actualUuid = null;
        boolean operationsFound = false;
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(file, StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String field = reader.nextName();
                if (field.equals("itemUuid")) actualUuid = reader.nextString();
                else if (field.equals("operations")) {
                    operationsFound = true;
                    reader.beginArray();
                    while (reader.hasNext()) {
                        InputOperation operation = JSON.fromJson(reader, InputOperation.class);
                        if (operation == null || operation.id() == null) throw new IOException("附魔操作记录缺少 id: " + file);
                        if (!actor.equalsIgnoreCase(operation.id().actorKey())) continue;
                        if (operation.id().timestamp() <= 0 || blank(operation.id().source()) || blank(operation.playerName())
                                || !actor.equalsIgnoreCase(actorKey(operation.playerName())))
                            throw new IOException("附魔操作的操作者、时间或来源无效: " + file);
                        consumer.accept(operation);
                    }
                    reader.endArray();
                } else reader.skipValue();
            }
            reader.endObject();
            if (!uuid.equals(actualUuid) || !operationsFound || reader.peek() != JsonToken.END_DOCUMENT)
                throw new IOException("装备附魔历史文件不匹配或不完整: " + file);
        } catch (RuntimeException exception) { throw new IOException("装备附魔历史格式错误: " + file, exception); }
    }

    private static final class Sorter {
        final Path directory;
        final int size;
        final List<Row> buffer = new ArrayList<>();
        final List<Path> runs = new ArrayList<>();
        long characters;
        Sorter(Path directory, int size) { this.directory = directory; this.size = size; }
        void add(Row row) throws IOException {
            buffer.add(row);
            characters += (long) row.playerName().length() + row.itemUuid().length() + row.source().length();
            if (buffer.size() >= size || characters >= BUFFER_CHARACTERS) flush();
        }
        void flush() throws IOException {
            if (buffer.isEmpty()) return;
            buffer.sort(ORDER);
            Path path = directory.resolve("chunk-" + runs.size() + ".ndjson");
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
                Row previous = null;
                for (Row row : buffer) if (!same(previous, row)) {
                    writer.write(JSON.toJson(row)); writer.newLine(); previous = row;
                }
            }
            runs.add(path); buffer.clear(); characters = 0;
        }
    }

    private interface RowConsumer { void accept(Row row) throws IOException; }
    private static final class Cursor implements AutoCloseable {
        final BufferedReader reader;
        Row row;
        Cursor(Path path) throws IOException { reader = Files.newBufferedReader(path, StandardCharsets.UTF_8); }
        boolean advance() throws IOException {
            String line = reader.readLine();
            if (line == null) return false;
            try { row = JSON.fromJson(line, Row.class); }
            catch (RuntimeException exception) { throw new IOException("导出排序临时文件格式错误", exception); }
            if (row == null) throw new IOException("导出排序临时文件记录为空");
            return true;
        }
        @Override public void close() throws IOException { reader.close(); }
    }

    private static long merge(List<Path> paths, RowConsumer output) throws IOException {
        PriorityQueue<Cursor> queue = new PriorityQueue<>(Comparator.comparing(cursor -> cursor.row, ORDER));
        List<Cursor> opened = new ArrayList<>();
        try {
            for (Path path : paths) {
                Cursor cursor = new Cursor(path); opened.add(cursor);
                if (cursor.advance()) queue.add(cursor);
            }
            long count = 0;
            Row previous = null;
            while (!queue.isEmpty()) {
                Cursor cursor = queue.remove();
                Row row = cursor.row;
                if (!same(previous, row)) { output.accept(row); previous = row; count++; }
                if (cursor.advance()) queue.add(cursor);
            }
            return count;
        } finally {
            for (Cursor cursor : opened) try { cursor.close(); } catch (IOException ignored) { }
        }
    }

    private static boolean same(Row a, Row b) {
        return a != null && a.timestamp() == b.timestamp() && a.itemUuid().equals(b.itemUuid())
                && a.source().equals(b.source()) && a.playerName().equalsIgnoreCase(b.playerName());
    }
    static String format(Row row, ZoneId zone) {
        return DATE.withZone(zone).format(Instant.ofEpochMilli(row.timestamp())) + " - " + cell(row.playerName())
                + " - " + cell(row.itemUuid()) + " - " + cell(row.source());
    }
    private static String cell(String value) {
        return value.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n")
                .replace("\u0085", "\\u0085").replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
