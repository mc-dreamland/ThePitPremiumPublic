package cn.charlotte.pit.commands.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static cn.charlotte.pit.commands.admin.MaterialHistoryLocalReport.*;
import static cn.charlotte.pit.commands.admin.MaterialHistoryPlayerMaximum.*;
import static org.junit.jupiter.api.Assertions.*;

class MaterialHistoryPlayerMaximumTest {
    @TempDir Path directory;

    private Header report(String... snapshots) throws Exception {
        Path path = directory.resolve("all-material-history-test.json");
        Files.writeString(path, """
                {"allAdvancedItems":true,"includesItemsWithoutUuid":true,"minimumItems":1,
                 "trackedItems":{"phase_book":"相位书","funky_feather":"时髦的羽毛","mythic_sword":"神话之剑"},
                 "snapshots":[%s]}
                """.formatted(String.join(",", snapshots)));
        return header(path);
    }

    private String snapshot(String uuid, String name, long timestamp, String inventory, String warehouse, String total) {
        return """
                {"databaseId":"id-%d","backupUuid":"backup-%d","playerUuid":"%s","playerName":"%s",
                 "prestige":30,"timestamp":%d,"time":"readable",
                 "inventoryCounts":%s,"warehouseCounts":%s,"totalCounts":%s,
                 "stackedUuidItems":[{"ignored":"No item, lore or NBT deserialization"}]}
                """.formatted(timestamp, timestamp, uuid, name, timestamp, inventory, warehouse, total);
    }

    private ItemMaximum item(Result result, String item) {
        return result.items().stream().filter(value -> value.item().equals(item)).findFirst().orElseThrow();
    }

    @Test void eachItemUsesTheSameSnapshotCombinedPeakNotSumOfCopiesOrSeparateLocationPeaks() throws Exception {
        Header header = report(
                snapshot("a", "Alice", 100, "{\"phase_book\":8,\"funky_feather\":20}", "{\"phase_book\":1}",
                        "{\"phase_book\":9,\"funky_feather\":20}"),
                snapshot("a", "Alice", 200, "{\"phase_book\":1}", "{\"phase_book\":10}", "{\"phase_book\":11}"),
                snapshot("a", "Alice", 300, "{\"phase_book\":1}", "{\"phase_book\":10}", "{\"phase_book\":11}"),
                snapshot("b", "Bob", 400, "{\"phase_book\":999}", "{}", "{\"phase_book\":999}"));
        Result result = query(header, "Alice");
        assertEquals("a", result.uuid());
        assertEquals(3, result.snapshots());
        assertEquals(List.of("funky_feather", "phase_book"), result.items().stream().map(ItemMaximum::item).toList());
        ItemMaximum books = item(result, "phase_book");
        assertEquals(11, books.maximum());
        assertEquals(1, books.peak().inventory());
        assertEquals(10, books.peak().warehouse());
        assertEquals(200, books.peak().timestamp());
        assertEquals("id-200", books.peak().databaseId());
        assertEquals("backup-200", books.peak().backupUuid());
        assertEquals(3, books.positiveSnapshots());
        assertEquals(100, books.firstTimestamp());
        assertEquals(300, books.lastTimestamp());
        assertEquals(100, item(result, "funky_feather").peak().timestamp());
        // Detail rows reuse the exact same local report; no UUID index or database is required.
        assertEquals(List.of(9L, 11L, 11L), rows(header, "phase_book", result.uuid()).stream().map(Row::total).toList());
    }

    @Test void equalMaximaChooseEarliestEvidenceAndLatestPlayerMetadataEvenForUnsortedReports() throws Exception {
        Header header = report(
                snapshot("a", "OldName", 300, "{\"phase_book\":5}", "{}", "{\"phase_book\":5}"),
                snapshot("a", "NewName", 400, "{}", "{}", "{}").replace("\"prestige\":30", "\"prestige\":35"),
                snapshot("a", "OldName", 100, "{\"phase_book\":3}", "{\"phase_book\":2}", "{\"phase_book\":5}"));
        Result result = query(header, "  oLdNaMe  ");
        assertEquals("NewName", result.name());
        assertEquals(35, result.prestige());
        assertEquals(3, result.snapshots());
        assertEquals(100, item(result, "phase_book").peak().timestamp());
        assertEquals(30, item(result, "phase_book").peak().prestige());
        assertEquals(result, query(header, "NewName"));
        assertEquals(result, query(header, "A"));
    }

    @Test void reusedNamesAreRejectedRatherThanCombiningDifferentPlayersAndUuidCanDisambiguate() throws Exception {
        Header header = report(
                snapshot("a", "玩家", 100, "{\"phase_book\":5}", "{}", "null"),
                snapshot("b", "玩家", 200, "{\"phase_book\":50}", "{}", "null"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> query(header, "玩家")).getMessage().contains("多个玩家"));
        assertEquals(5, item(query(header, "a"), "phase_book").maximum());
        assertEquals(50, item(query(header, "b"), "phase_book").maximum());
    }

    @Test void missingOrNullTotalsFallBackPerItemAndCountsAreCaseInsensitiveNonnegativeLongValues() throws Exception {
        Header header = report("null",
                snapshot("a", "玩家甲", 100, "{\"PHASE_BOOK\":3,\"phase_book\":2,\"mythic_sword\":-2}",
                        "{\"phase_book\":4,\"funky_feather\":3000000000}", "null"),
                snapshot("a", "玩家甲", 200, "{\"phase_book\":8}", "{\"funky_feather\":2}",
                        "{\"phase_book\":null,\"funky_feather\":0}"),
                snapshot("a", "玩家甲", 300, "null", "null", "null"));
        Result result = query(header, "玩家甲");
        assertEquals(3, result.snapshots());
        assertEquals(2, result.items().size());
        assertEquals(9, item(result, "phase_book").maximum());
        assertEquals(5, item(result, "phase_book").peak().inventory());
        assertEquals(2, item(result, "phase_book").positiveSnapshots());
        assertEquals(3_000_000_000L, item(result, "funky_feather").maximum());
        assertEquals(1, item(result, "funky_feather").positiveSnapshots());
        assertEquals(List.of(9L, 8L, 0L), rows(header, "phase_book", result.uuid()).stream().map(Row::total).toList());
        assertEquals(List.of(3_000_000_000L, 0L, 0L),
                rows(header, "funky_feather", result.uuid()).stream().map(Row::total).toList());
    }

    @Test void ordinaryLegacyReportsSupportReorderedFieldsAndItemTypesNotListedInTheHeader() throws Exception {
        Path path = directory.resolve("rare-material-history-legacy.json");
        Files.writeString(path, """
                {"minimumItems":10,"trackedItems":{},"snapshots":[
                  {"inventoryCounts":{"phase_book":7},"warehouseCounts":{"phase_book":9},
                   "totalCounts":{"phase_book":16},"playerName":"甲","timestamp":500,"playerUuid":"A"}]}
                """);
        Result result = query(header(path), "甲");
        assertEquals(16, item(result, "phase_book").maximum());
        assertNull(item(result, "phase_book").peak().databaseId());
        assertEquals(-1, result.prestige());
        assertEquals(16, rows(header(path), "phase_book", result.uuid()).getFirst().total());
    }

    @Test void absentPlayerIsNotReportedAsZeroAndAnExistingPlayerCanHaveNoPositiveItemTypes() throws Exception {
        Header header = report(snapshot("a", "Alice", 100, "{}", "{}", "{}"));
        assertTrue(query(header, "Alice").items().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> query(header, "Bob"));
        assertThrows(IllegalArgumentException.class, () -> query(header, " "));
        assertThrows(IllegalArgumentException.class, () -> query(header, null));
    }

    @Test void itemSortingIsStableSupportsAllFourOrdersAndDoesNotMutateTheOriginal() {
        ItemMaximum a = new ItemMaximum("a", new Row(null, null, "p", "P", 1, 300, null, 5, 0, 5), 1, 300, 300);
        ItemMaximum b = new ItemMaximum("b", new Row(null, null, "p", "P", 1, 100, null, 5, 0, 5), 1, 100, 100);
        ItemMaximum c = new ItemMaximum("c", new Row(null, null, "p", "P", 1, 200, null, 1, 0, 1), 1, 200, 200);
        List<ItemMaximum> input = List.of(c, b, a);
        assertEquals(List.of(a, b, c), sort(input, Order.QUANTITY_DESC));
        assertEquals(List.of(c, a, b), sort(input, Order.QUANTITY_ASC));
        assertEquals(List.of(b, c, a), sort(input, Order.DATE_ASC));
        assertEquals(List.of(a, c, b), sort(input, Order.DATE_DESC));
        assertEquals(List.of(c, b, a), input);
    }

    @Test void streamingManyPlayersRetainsOnlyRequestedPlayersItemAggregates() throws Exception {
        Path path = directory.resolve("all-material-history-large.json");
        try (var writer = Files.newBufferedWriter(path)) {
            writer.write("{\"trackedItems\":{\"phase_book\":\"相位书\"},\"snapshots\":[");
            for (int i = 0; i < 5000; i++) {
                if (i > 0) writer.write(",");
                writer.write(snapshot("uuid-" + i % 100, "玩家" + i % 100, i + 1,
                        "{\"phase_book\":" + i + "}", "{}", "null"));
            }
            writer.write("]}");
        }
        Result result = query(header(path), "玩家7");
        assertEquals(50, result.snapshots());
        assertEquals(1, result.items().size());
        assertEquals(4907, result.items().getFirst().maximum());
        assertEquals("id-4908", result.items().getFirst().peak().databaseId());
    }

    @Test void truncatedReportsFailRatherThanReturningAnIncompleteMaximum() throws Exception {
        Header header = report(snapshot("a", "Alice", 100, "{\"phase_book\":5}", "{}", "null"));
        String json = Files.readString(header.path());
        Files.writeString(header.path(), json.substring(0, json.lastIndexOf(']')));
        assertThrows(java.io.IOException.class, () -> query(header, "Alice"));
    }
}
