package cn.charlotte.pit.commands.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;

import static cn.charlotte.pit.commands.admin.MaterialHistoryLocalReport.*;
import static org.junit.jupiter.api.Assertions.*;

class MaterialHistoryLocalReportTest {
    @TempDir Path directory;

    @Test void allReportProvidesChronologicalDeduplicatedBackupReferencesWithoutLoadingItemData() throws Exception {
        Path path = report("all-uuid-material-history-new.json", row("b", "乙", 2000, 3, 4) + ","
                + row("a", "甲", 1000, 1, 2) + "," + row("a", "甲", 1000, 1, 2));
        var references = backupReferences(path);
        assertEquals(2, references.size());
        assertEquals("id-1000", references.getFirst().databaseId());
        assertEquals("a", references.getFirst().playerUuid());
        assertEquals(2000, references.get(1).timestamp());
        assertEquals(30, references.getFirst().prestige());
    }

    private Path report(String name, String fields) throws Exception {
        Path path = directory.resolve(name);
        Files.writeString(path, """
                {"generatedAt":"now", "minimumItems":1, "allUuidAdvanced":true,
                 "trackedItems":{"mythic_sword":"神话之剑","phase_book":"相位书"},
                 "snapshots":[%s], "matchedBackups":123}
                """.formatted(fields));
        return path;
    }
    private String row(String uuid, String name, long timestamp, int inventory, int warehouse) {
        return """
                {"databaseId":"id-%d", "backupUuid":"backup-%d", "playerUuid":"%s", "playerName":"%s",
                 "prestige":30, "timestamp":%d, "time":"readable",
                 "inventoryCounts":{"mythic_sword":%d,"phase_book":5},
                 "warehouseCounts":{"mythic_sword":%d},
                 "totalCounts":{"mythic_sword":%d,"phase_book":5},
                 "stackedUuidItems":[{"ignored":"nbt and lore are never deserialized"}]}
                """.formatted(timestamp, timestamp, uuid, name, timestamp, inventory, warehouse, inventory + warehouse);
    }

    @Test void metadataResolvesChineseNamesAndCaseInsensitiveInternalIdsButRejectsUnrecordedTypes() throws Exception {
        Header header = header(report("all-uuid-material-history-one.json", row("a", "Alice", 100, 1, 2)));
        assertTrue(header.allUuidAdvanced());
        assertFalse(header.includesItemsWithoutUuid());
        assertEquals("旧版：仅带有效 UUID 的高级道具", header.scopeLabel());
        assertEquals(1, header.minimumItems());
        assertEquals("mythic_sword", header.resolveItem("MYTHIC_SWORD"));
        assertEquals("mythic_sword", header.resolveItem("神话之剑"));
        assertEquals("phase_book", header.resolveItem(" 相位书 "));
        assertThrows(IllegalArgumentException.class, () -> header.resolveItem("shopitem"));
    }
    @Test void groupTotalsUsePerSnapshotPeaksRatherThanAddingRepeatedHistoricalCopies() throws Exception {
        Path file = report("all-uuid-material-history-one.json", String.join(",",
                row("a", "Alice", 100, 0, 0), row("a", "Alice", 200, 2, 3),
                row("a", "Alice", 300, 1, 0), row("b", "Bob", 250, 0, 0),
                row("c", "Charlie", 400, 6, 0)));
        Header header = header(file);
        List<PlayerCounts> players = sortPlayers(players(header, "mythic_sword"), Order.DATE_ASC);
        assertEquals(List.of("a", "c"), players.stream().map(PlayerCounts::uuid).toList());
        var alice = players.getFirst();
        assertEquals(3, alice.snapshots());
        assertEquals(2, alice.positiveSnapshots());
        assertEquals(5, alice.maximum());
        assertEquals(200, alice.firstTimestamp());
        assertEquals(300, alice.lastTimestamp());
        assertEquals(List.of("c", "a"), sortPlayers(players, Order.QUANTITY_DESC).stream().map(PlayerCounts::uuid).toList());
        List<Row> rows = sortRows(rows(header, "mythic_sword", "a"), Order.DATE_ASC);
        assertEquals(List.of(0L, 5L, 1L), rows.stream().map(Row::total).toList());
        assertEquals(2, rows.get(1).inventory());
        assertEquals(3, rows.get(1).warehouse());
    }
    @Test void chronologicalAndQuantitySortingAreStableAndNeverMutateTheCachedInput() {
        List<Row> source = List.of(new Row("z", "z", "a", "Alice", 1, 300, null, 1, 0, 1),
                new Row("a", "a", "a", "Alice", 1, 100, null, 3, 2, 5),
                new Row("b", "b", "a", "Alice", 1, 200, null, 1, 4, 5));
        assertEquals(List.of(100L, 200L, 300L), sortRows(source, Order.DATE_ASC).stream().map(Row::timestamp).toList());
        assertEquals(List.of(300L, 200L, 100L), sortRows(source, Order.DATE_DESC).stream().map(Row::timestamp).toList());
        assertEquals(List.of(100L, 200L, 300L), sortRows(source, Order.QUANTITY_DESC).stream().map(Row::timestamp).toList());
        assertEquals(List.of(300L, 100L, 200L), sortRows(source, Order.QUANTITY_ASC).stream().map(Row::timestamp).toList());
        assertEquals(300L, source.getFirst().timestamp());
    }
    @Test void ordinaryLegacyReportsAndNullCountMapsWorkWithoutNewSchemaFields() throws Exception {
        Path file = directory.resolve("rare-material-history-old.json");
        Files.writeString(file, """
                {"minimumItems":10,"trackedItems":{"mythic_sword":"神话之剑"},"snapshots":[null,
                 {"playerUuid":"a","playerName":"Alice","timestamp":100,"inventoryCounts":null,
                  "warehouseCounts":{"MYTHIC_SWORD":7},"totalCounts":null}]}
                """);
        Header header = header(file);
        assertFalse(header.allUuidAdvanced());
        assertEquals("旧版：指定稀有物资", header.scopeLabel());
        Row row = rows(header, "mythic_sword", "a").getFirst();
        assertEquals(0, row.inventory());
        assertEquals(7, row.warehouse());
        assertEquals(7, row.total());
    }
    @Test void reportDiscoveryIncludesBothModesButNeverSelectsTemporaryOrUnrelatedJson() throws Exception {
        Path old = report("rare-material-history-old.json", "");
        Path all = report("all-uuid-material-history-new.json", "");
        Files.setLastModifiedTime(old, FileTime.fromMillis(100));
        Files.setLastModifiedTime(all, FileTime.fromMillis(200));
        Files.writeString(directory.resolve("all-uuid-material-history-incomplete.json.tmp"), "{broken");
        Files.writeString(directory.resolve("uuidtrace-new.json"), "{}");
        assertEquals(all, latest(directory));
        Path complete = directory.resolve("all-material-history-complete.json");
        Files.writeString(complete, "{\"allAdvancedItems\":true,\"includesItemsWithoutUuid\":true,\"trackedItems\":{},\"snapshots\":[]}");
        Files.setLastModifiedTime(complete, FileTime.fromMillis(300));
        Files.writeString(directory.resolve("all-material-history-incomplete.json.tmp"), "{broken");
        assertEquals(complete, latest(directory));
        assertNull(latest(directory.resolve("missing")));
    }

    @Test void newOrdinaryAndStreamingReportsBothDescribeFullAdvancedQuantityScopeIncludingUuidlessItems() throws Exception {
        for (String prefix : List.of("rare-material-history-", "all-material-history-")) {
            Path path = directory.resolve(prefix + "new-scope.json");
            Files.writeString(path, """
                    {"reportVersion":2,"allAdvancedItems":true,"includesItemsWithoutUuid":true,
                     "minimumItems":1,"trackedItems":{"mythic_sword":"神话之剑","phase_book":"相位书","funky_feather":"时髦的羽毛"},
                     "snapshots":[{"databaseId":"backup-id","backupUuid":"backup-uuid","playerUuid":"a","playerName":"Alice",
                       "timestamp":100,"inventoryCounts":{"phase_book":7,"mythic_sword":1},
                       "warehouseCounts":{"funky_feather":9},"totalCounts":{"phase_book":7,"mythic_sword":1,"funky_feather":9}}]}
                    """);
            Header header = header(path);
            assertTrue(header.allAdvancedItems());
            assertTrue(header.includesItemsWithoutUuid());
            assertFalse(header.allUuidAdvanced());
            assertTrue(header.scopeLabel().contains("含无 UUID"));
            assertEquals(7, rows(header, "phase_book", "a").getFirst().total());
            assertEquals(9, rows(header, "funky_feather", "a").getFirst().total());
            assertEquals(1, rows(header, "mythic_sword", "a").getFirst().total());
            assertEquals("backup-id", backupReferences(path).getFirst().databaseId());
        }
    }
    @Test void headerDoesNotReadTheSnapshotBodyAndQueriesStreamThousandsOfRows() throws Exception {
        Path headerOnly = directory.resolve("header.json");
        Files.writeString(headerOnly, """
                {"minimumItems":1,"trackedItems":{"mythic_sword":"神话之剑"},"snapshots":[
                """);
        assertEquals("mythic_sword", header(headerOnly).resolveItem("神话之剑"));
        Path many = directory.resolve("all-uuid-material-history-many.json");
        try (var writer = Files.newBufferedWriter(many)) {
            writer.write("{\"trackedItems\":{\"mythic_sword\":\"神话之剑\"},\"snapshots\":[");
            for (int i = 0; i < 5000; i++) {
                if (i > 0) writer.write(",");
                writer.write(row("player-" + i % 100, "Player" + i % 100, i + 1, 1, 2));
            }
            writer.write("]}");
        }
        Header header = header(many);
        assertEquals(100, players(header, "mythic_sword").size());
        assertEquals(50, rows(header, "mythic_sword", "player-7").size());
    }
    @Test void zeroQuantityIsOnlyReturnedForSnapshotsActuallyPresentInTheLocalFile() throws Exception {
        Header header = header(report("all-uuid-material-history-small.json", row("a", "Alice", 100, 0, 0)));
        List<Row> rows = rows(header, "mythic_sword", "a");
        assertEquals(1, rows.size());
        assertEquals(0, rows.getFirst().total());
        assertTrue(rows(header, "mythic_sword", "absent").isEmpty());
        assertTrue(players(header, "mythic_sword").isEmpty());
    }
}
