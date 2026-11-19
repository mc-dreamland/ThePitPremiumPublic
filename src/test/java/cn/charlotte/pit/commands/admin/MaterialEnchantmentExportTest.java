package cn.charlotte.pit.commands.admin;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;

import static cn.charlotte.pit.commands.admin.MaterialEnchantmentHistory.*;
import static org.junit.jupiter.api.Assertions.*;

class MaterialEnchantmentExportTest {
    @TempDir Path root;
    private static final String A = "bb7fbf1d-c915-493c-abc7-297e478b47ed";
    private static final String B = "5804d211-9d3d-4e19-8fe0-452d5a85bb0e";

    private Operation operation(String player, String source, long timestamp) {
        Operation operation = new Operation();
        operation.id = new OperationId(actorKey(player), source, timestamp);
        operation.playerName = player;
        operation.observedBackups = 99;
        operation.firstObservedEnchantments = Map.of("shark", 3);
        operation.firstObservedLore = List.of("不能当成精确单次附魔结果");
        return operation;
    }
    private ItemReport item(String uuid, Operation... operations) {
        Evidence holder = new Evidence("holder-uuid", "背包持有人", 35, "database-id", "backup", 10000, "time", List.of("inventory[0]"), 1);
        return new ItemReport(uuid, holder, holder, List.of(), Arrays.asList(operations), List.of(), List.of(), 0);
    }
    private Report report(String player, ItemReport... items) throws IOException {
        Path directory = Files.createDirectory(root.resolve("enchant-record-history-" + UUID.randomUUID()));
        String key = actorKey(player);
        Path index = actorPath(directory, key);
        Files.createDirectories(index.getParent());
        long records = 0;
        try (var writer = Files.newBufferedWriter(index)) {
            for (ItemReport item : items) {
                Path file = itemPath(directory, item.itemUuid()); Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(item));
                int count = (int) item.operations().stream().filter(operation -> operation.id != null && key.equalsIgnoreCase(operation.id.actorKey())).count();
                records += count;
                writer.write(GSON.toJson(new ActorItem(item.itemUuid(), "装备", "golden_sword", count, 0, 1, 10000, 0)));
                writer.newLine();
            }
        }
        var source = new UuidDuplicateHistory.Metadata(2, 1, "source.json", 1, 1, 0, List.of());
        Manifest manifest = new Manifest(1, System.currentTimeMillis(), "index", source, 1, items.length, records, 0, 0, 0,
                List.of("records 仅保留最近 5 条，本地记录可能不完整"), List.of(new Actor(key, player, items.length, records, 0, 1, 10000)));
        Files.writeString(directory.resolve("manifest.json"), GSON.toJson(manifest));
        return new Report(directory, manifest);
    }
    private MaterialEnchantmentExport.Result export(Report report, String player) throws IOException {
        return MaterialEnchantmentExport.export(report, player, root.resolve("exports"), ZoneOffset.UTC, ignored -> {}, 2, 2);
    }

    @Test void exportsAllRecordedSourcesIncludingGemAndBookOperationsInGlobalTimestampOrder() throws Exception {
        Report report = report("Alice", item(A, operation("Alice", "totally_legit_gem", 1200),
                        operation("Bob", "Enchant Table", 800), operation("Alice", "Enchant Table", 900)),
                item(B, operation("Alice", "global_attention_gem", 950), operation("Alice", "phase_book", 1100)));
        var result = export(report, " aLiCe ");
        List<String> lines = Files.readAllLines(result.path());
        assertEquals(List.of("1970-01-01 00:00:00.900 - Alice - " + A + " - Enchant Table",
                "1970-01-01 00:00:00.950 - Alice - " + B + " - global_attention_gem",
                "1970-01-01 00:00:01.100 - Alice - " + B + " - phase_book",
                "1970-01-01 00:00:01.200 - Alice - " + A + " - totally_legit_gem"), lines);
        assertEquals(4, result.records());
        assertFalse(Files.readString(result.path()).contains("shark")); // A later observed state is not this operation's result.
        assertFalse(Files.readString(result.path()).contains("Bob"));
    }

    @Test void deduplicatesAcrossChunksButKeepsDifferentEquipmentAndDifferentSourcesAtTheSameTimestamp() throws Exception {
        Report report = report("Alice", item(A, operation("Alice", "Enchant Table", 900), operation("alice", "Enchant Table", 900),
                        operation("Alice", "totally_legit_gem", 900), operation("Alice", "totally_legit_gem", 900)),
                item(B, operation("Alice", "Enchant Table", 900), operation("Alice", "Enchant Table", 900)));
        var result = MaterialEnchantmentExport.export(report, "Alice", root.resolve("exports"), ZoneOffset.UTC, ignored -> {}, 1, 2);
        assertEquals(6, result.matchedRecords());
        assertEquals(3, result.records());
        assertEquals(3, Files.readAllLines(result.path()).size());
        assertEquals(1, Files.readAllLines(result.path()).stream().filter(line -> line.endsWith("totally_legit_gem")).count());
    }

    @Test void readsPersistedReportsAfterReloadUsesRecordedOperatorAndPreservesChineseInContentAndFilename() throws Exception {
        Report report = report("中文玩家", item(A, operation("中文玩家", "totally_legit_gem", 900)));
        Report reloaded = latest(root);
        assertEquals(report.directory(), reloaded.directory());
        var result = export(reloaded, actorKey("中文玩家"));
        assertTrue(result.path().getFileName().toString().contains("中文玩家"));
        assertTrue(Files.readString(result.path()).contains(" - 中文玩家 - "));
        assertThrows(IllegalArgumentException.class, () -> export(reloaded, "背包持有人"));
    }

    @Test void sourceAndPlayerNewlinesAreEscapedSoEveryOperationStaysOnePhysicalLine() {
        String line = MaterialEnchantmentExport.format(new MaterialEnchantmentExport.Row(900,
                "中文\n玩家", "uuid\r\nwith-newline", "宝石\u2028来源\\path"), ZoneId.of("Asia/Shanghai"));
        assertTrue(line.startsWith("1970-01-01 08:00:00.900 - "));
        assertFalse(line.contains("\n")); assertFalse(line.contains("\r")); assertFalse(line.contains("\u2028"));
        assertTrue(line.contains("中文\\n玩家"));
        assertTrue(line.contains("宝石\\u2028来源\\\\path"));
    }

    @Test void externalMergeHandlesManyRunsAndReportsProgressWithoutRetainingAllRecords() throws Exception {
        Operation[] operations = new Operation[101];
        for (int i = 0; i < operations.length; i++) operations[i] = operation("Alice", i % 2 == 0 ? "Enchant Table" : "totally_legit_gem", 5000 - i);
        Report report = report("Alice", item(A, operations));
        List<MaterialEnchantmentExport.Progress> progress = new ArrayList<>();
        var result = MaterialEnchantmentExport.export(report, "Alice", root.resolve("exports"), ZoneOffset.UTC, progress::add, 2, 2);
        List<String> lines = Files.readAllLines(result.path());
        assertEquals(101, lines.size()); assertEquals(101, result.records());
        assertTrue(lines.getFirst().startsWith("1970-01-01 00:00:04.900"));
        assertTrue(lines.getLast().startsWith("1970-01-01 00:00:05.000"));
        assertEquals("完成", progress.getLast().stage());
        try (var files = Files.list(root.resolve("exports"))) {
            assertEquals(List.of(result.path()), files.toList());
        }
    }

    @Test void brokenEquipmentReportDoesNotPublishPartialOutputAndOnlyItsOwnTemporaryFilesAreRemoved() throws Exception {
        Report report = report("Alice", item(A, operation("Alice", "Enchant Table", 900)), item(B, operation("Alice", "totally_legit_gem", 1000)));
        Path output = Files.createDirectories(root.resolve("exports"));
        Path sentinel = output.resolve("existing.txt"); Files.writeString(sentinel, "已有文件不能删");
        Files.writeString(itemPath(report.directory(), B), "{broken");
        assertThrows(IOException.class, () -> MaterialEnchantmentExport.export(report, "Alice", output, ZoneOffset.UTC, ignored -> {}, 1, 2));
        assertEquals("已有文件不能删", Files.readString(sentinel));
        try (var files = Files.list(output)) { assertEquals(List.of(sentinel), files.toList()); }
        assertEquals("{broken", Files.readString(itemPath(report.directory(), B)));
    }

    @Test void mismatchingEquipmentUuidIsRejectedEvenIfOperationsAppearBeforeTheUuidField() throws Exception {
        Report report = report("Alice", item(A, operation("Alice", "Enchant Table", 900)));
        JsonObject bad = new JsonObject(); bad.add("operations", GSON.toJsonTree(item(A, operation("Alice", "Enchant Table", 900)).operations()));
        bad.addProperty("itemUuid", B);
        Files.writeString(itemPath(report.directory(), A), GSON.toJson(bad));
        assertThrows(IOException.class, () -> export(report, "Alice"));
        try (var files = Files.list(root.resolve("exports"))) { assertTrue(files.findAny().isEmpty()); }
    }

    @Test void timeSortingUsesNumericTimestampEvenWhenDaylightSavingMakesLocalClockGoBackwards() throws Exception {
        long before = Instant.parse("2026-11-01T05:59:00Z").toEpochMilli(), after = Instant.parse("2026-11-01T06:01:00Z").toEpochMilli();
        Report report = report("Alice", item(A, operation("Alice", "totally_legit_gem", after), operation("Alice", "Enchant Table", before)));
        var result = MaterialEnchantmentExport.export(report, "Alice", root.resolve("exports"), ZoneId.of("America/New_York"), ignored -> {}, 1, 2);
        List<String> lines = Files.readAllLines(result.path());
        assertTrue(lines.getFirst().startsWith("2026-11-01 01:59:00.000"));
        assertTrue(lines.get(1).startsWith("2026-11-01 01:01:00.000"));
    }

    @Test void exportingAgainCreatesAnotherCompletedFileAndNeverOverwritesPreviousExports() throws Exception {
        Report report = report("Alice", item(A, operation("Alice", "Enchant Table", 900)));
        var first = export(report, "Alice");
        String original = Files.readString(first.path());
        var second = export(report, "Alice");
        assertNotEquals(first.path(), second.path());
        assertFalse(first.path().toString().endsWith(".tmp"));
        assertEquals(original, Files.readString(first.path()));
        assertEquals(original, Files.readString(second.path()));
    }

    @Test void malformedOrMismatchingOperationActorFailsInsteadOfExportingInventedData() throws Exception {
        Operation operation = operation("Alice", "Enchant Table", 900); operation.playerName = "Bob";
        Report report = report("Alice", item(A, operation));
        assertThrows(IOException.class, () -> export(report, "Alice"));
        try (var files = Files.list(root.resolve("exports"))) { assertTrue(files.findAny().isEmpty()); }
    }

    @Test void suspectRecordedTimestampsRemainInTheFileRatherThanSilentlyBeingDropped() throws Exception {
        Operation operation = operation("Alice", "totally_legit_gem", 999999); operation.timestampAfterFirstEvidence = true;
        Report report = report("Alice", item(A, operation));
        var result = export(report, "Alice");
        assertEquals(1, result.records()); assertEquals(1, result.suspectRecords());
        assertTrue(Files.readString(result.path()).endsWith("totally_legit_gem" + System.lineSeparator()));
    }

    @Test void absentOrRecordlessPlayerAndInvalidSortOptionsDoNotProduceAnEmptySuccessFile() throws Exception {
        Report report = report("Alice", item(A));
        assertThrows(IllegalArgumentException.class, () -> export(report, "Alice"));
        assertThrows(IllegalArgumentException.class, () -> export(report, "missing"));
        assertThrows(IllegalArgumentException.class, () -> export(report, " "));
        assertThrows(IllegalArgumentException.class, () -> MaterialEnchantmentExport.export(report, "Alice", root.resolve("exports"),
                ZoneOffset.UTC, ignored -> {}, 1, 1));
        assertFalse(Files.exists(root.resolve("exports")));
    }
}
