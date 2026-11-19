package cn.charlotte.pit.commands.admin;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;

import static cn.charlotte.pit.commands.admin.UuidDuplicateLocalReport.*;
import static org.junit.jupiter.api.Assertions.*;

class UuidDuplicateLocalReportTest {
    @TempDir Path root;
    private static final Gson GSON = new Gson();
    private static final String UUID = "bb7fbf1d-c915-493c-abc7-297e478b47ed";

    private Location location(String player, String name, String container, int slot, List<String> lore) {
        return new Location(player, name, container, container.equals("ender_chest") ? 2 : null,
                slot, slot + 1, null, "§d神话之剑", lore, null, "GOLDEN_SWORD", null, null);
    }
    private Current current() {
        return new Current(2, List.of(location("player-a", "玩家甲", "inventory", 3, List.of("生命: 19/19", "", "§b灵魂汲取 III")),
                location("player-b", "玩家乙", "ender_chest", 7, List.of("生命: 18/19", "第二份的 Lore"))));
    }
    private Report currentReport(String filename, Map<String, Current> values) throws Exception {
        Path path = root.resolve(filename);
        Files.writeString(path, GSON.toJson(values));
        return report(path, Mode.CURRENT);
    }
    private UuidDuplicateHistory.Observation observation(String owner, String id, long time, int amount) {
        return new UuidDuplicateHistory.Observation(UUID, "player-" + owner, "历史玩家" + owner, 30,
                id, "backup-" + id, time, "readable", amount,
                List.of(new UuidDuplicateHistory.Item("warehouse[2][7]", "mythic_sword", "golden_sword", "神话之剑", amount)));
    }
    private History history() {
        var first = observation("甲", "first", 1000, 2);
        var later = observation("乙", "later", 1500, 1);
        var pair = new UuidDuplicateHistory.Pair(first, later, 500);
        return new History(UUID, "SAME_BACKUP_DUPLICATE", current(), 123, first, first, pair, pair);
    }
    private Report historyReport(String sourceReport, String index, History... results) throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("version", 1); data.put("generatedAtTimestamp", 5000);
        data.put("duplicateReport", sourceReport); data.put("historyIndex", index);
        data.put("crossPlayerWindowSeconds", 60);
        data.put("historyMetadata", Map.of("version", 2, "scanErrors", 3));
        data.put("progress", Map.of("invalidEvents", 4));
        data.put("results", Arrays.asList(results));
        Path path = root.resolve("duplicate-uuid-history-test.json");
        Files.writeString(path, GSON.toJson(data));
        return report(path, Mode.HISTORY);
    }

    @Test void currentReportsRetainEachCopiesExactLoreOwnerLocationAndMaterial() throws Exception {
        Report report = currentReport("duplicate-mythic-item-uuid-test.json", Map.of(UUID, current()));
        Page page = page(report, null, 0);
        assertEquals(1, page.total());
        assertEquals(2, page.items().getFirst().count());
        assertEquals(2, page.items().getFirst().holders());
        assertEquals("GOLDEN_SWORD", page.items().getFirst().material());
        Group group = group(report, UUID.toUpperCase(Locale.ROOT));
        assertEquals(List.of("生命: 19/19", "", "§b灵魂汲取 III"), group.locations().getFirst().lore());
        assertEquals("第二份的 Lore", group.locations().get(1).lore().get(1));
        assertEquals(2, group.locations().get(1).warehouseIndex());
        assertEquals(7, group.locations().get(1).slot());
        assertEquals(8, group.locations().get(1).displaySlot());
        assertNull(group.history());
    }

    @Test void filtersSupportPlayerNamesPlayerUuidsAndItemUuidsWithoutDatabaseLookups() throws Exception {
        Report report = currentReport("duplicate-mythic-item-uuid-test.json", Map.of(UUID, current()));
        assertEquals(1, page(report, " 玩家甲 ", 0).total());
        assertEquals(1, page(report, "PLAYER-B", 0).total());
        assertEquals(1, page(report, UUID.toUpperCase(Locale.ROOT), 0).total());
        assertEquals(0, page(report, "不存在的玩家", 0).total());
        assertThrows(IllegalArgumentException.class, () -> group(report, "absent"));
    }

    @Test void pagesKeepOnlyFortyFiveSummariesAndNeverAddCopiesAcrossUuidGroups() throws Exception {
        Map<String, Current> values = new LinkedHashMap<>();
        for (int i = 0; i < 1000; i++) values.put("uuid-" + i, current());
        Report report = currentReport("duplicate-mythic-item-uuid-many.json", values);
        Page first = page(report, null, 0), second = page(report, null, 1), last = page(report, null, 22);
        assertEquals(1000, first.total());
        assertEquals(45, first.items().size());
        assertEquals("uuid-0", first.items().getFirst().itemUuid());
        assertEquals("uuid-45", second.items().getFirst().itemUuid());
        assertEquals(10, last.items().size());
        assertEquals("uuid-999", last.items().getLast().itemUuid());
        assertTrue(first.items().stream().allMatch(value -> value.count() == 2));
        assertThrows(IllegalArgumentException.class, () -> page(report, null, -1));
    }

    @Test void oldReportsWithoutMaterialOrLoreStillOpenAndMissingDataStaysUnknown() throws Exception {
        Path file = root.resolve("duplicate-mythic-item-uuid-old.json");
        Files.writeString(file, """
                {"old-uuid":{"count":2,"locations":[null,
                  {"playerName":"甲","container":"inventory","slot":0},
                  {"playerName":"甲","container":"ender_chest","warehouseIndex":1,"slot":2}]},
                 "single":{"count":1,"locations":[]},"empty":null}
                """);
        Report report = report(file, Mode.CURRENT);
        assertEquals(1, page(report, "甲", 0).total());
        assertEquals(1, page(report, null, 0).items().getFirst().holders());
        Location item = group(report, "old-uuid").locations().getFirst();
        assertNull(item.lore()); assertNull(item.material()); assertNull(item.displaySlot());
    }

    @Test void historyReportsExposeEvidenceMetadataAndCanFilterByPastOwnersNotOnlyCurrentHolders() throws Exception {
        Report report = historyReport(null, null, history());
        assertEquals(5000, report.generatedAt());
        assertEquals(60, report.windowSeconds());
        assertEquals(2, report.indexVersion());
        assertEquals(3, report.scanErrors());
        assertEquals(4, report.invalidEvents());
        Page page = page(report, "历史玩家甲", 0);
        assertEquals(1, page.total());
        assertEquals("SAME_BACKUP_DUPLICATE", page.items().getFirst().status());
        assertEquals(1000, page.items().getFirst().clueTimestamp());
        assertEquals(1, page(report, "玩家乙", 0).total());
        assertEquals(123, group(report, UUID).history().recordedEvents());
    }

    @Test void readsTheExistingHistoryWritersActualFormatIncludingThinCurrentLocations() throws Exception {
        Report current = currentReport("duplicate-mythic-item-uuid-original.json", Map.of(UUID, current()));
        var metadata = new UuidDuplicateHistory.Metadata(2, 5000, "materials.json", 10, 10, 0, List.of());
        var source = new UuidDuplicateHistory.Source(root.resolve("materialhistory/uuid-history-index-source"), metadata);
        var duplicate = new UuidDuplicateHistory.CurrentDuplicate(2, List.of(
                new UuidDuplicateHistory.CurrentLocation("player-a", "玩家甲", "inventory", null, 3, null, null, null, "神话之剑"),
                new UuidDuplicateHistory.CurrentLocation("player-b", "玩家乙", "ender_chest", 2, 7, null, null, null, "神话之剑")));
        var result = new UuidDuplicateHistory.Result(UUID, "SAME_BACKUP_DUPLICATE", duplicate, 123,
                history().firstAppearance(), history().firstSameBackupDuplicate(), history().firstCrossPlayerCandidate(), null);
        Path output = root.resolve("duplicate-uuid-history-actual.json");
        UuidDuplicateHistory.writeReport(output, current.path(), new UuidDuplicateHistory.Analysis(source, List.of(result),
                new UuidDuplicateHistory.Progress(1, 1, 123, 123, 0)), 60);
        Report report = report(output, Mode.HISTORY);
        assertEquals(1, page(report, "历史玩家甲", 0).total());
        assertEquals(2, group(report, UUID).locations().size());
        assertNull(group(report, UUID).locations().getFirst().lore());
        assertEquals(current.path().toAbsolutePath(), sourceCurrent(report));
        assertEquals("first", evidence(group(report, UUID).history()).getFirst().observation().backupDatabaseId());
    }

    @Test void sameBackupAppearingInSeveralEvidenceRolesIsMergedAndEvidenceIsChronological() {
        var evidence = evidence(history());
        assertEquals(2, evidence.size());
        assertEquals(4, evidence.getFirst().labels().size());
        assertTrue(evidence.getFirst().labels().contains("最早同一备份内重复"));
        assertEquals("first", evidence.getFirst().observation().backupDatabaseId());
        assertEquals("later", evidence.get(1).observation().backupDatabaseId());
        assertEquals(2, evidence.get(1).labels().size());
    }

    @Test void absentHistoryAndCandidateOnlyRemainDistinctAndDoNotInventConfirmedDuplication() throws Exception {
        var a = observation("甲", "first", 1000, 1);
        var b = observation("乙", "later", 1500, 1);
        History candidate = new History(UUID, "CROSS_PLAYER_CANDIDATE_ONLY", current(), 2, a, null,
                new UuidDuplicateHistory.Pair(a, b, 500), null);
        History absent = new History("absent", "NO_LOCAL_HISTORY", current(), 0, null, null, null, null);
        Report report = historyReport(null, null, candidate, absent);
        assertEquals(2, page(report, null, 0).total());
        assertEquals(1500, page(report, null, 0).items().getFirst().clueTimestamp());
        assertEquals(0, page(report, null, 0).items().get(1).clueTimestamp());
        assertNull(group(report, UUID).history().firstSameBackupDuplicate());
        assertTrue(evidence(group(report, "absent").history()).isEmpty());
    }

    @Test void discoversOnlyCompletedReportsOfTheRequestedKindAcrossRestarts() throws Exception {
        Report current = currentReport("duplicate-mythic-item-uuid-current.json", Map.of(UUID, current()));
        Report history = historyReport(null, null, history());
        Files.setLastModifiedTime(current.path(), FileTime.fromMillis(100));
        Files.setLastModifiedTime(history.path(), FileTime.fromMillis(200));
        Files.writeString(root.resolve("duplicate-mythic-item-uuid-unfinished.json.tmp"), "broken");
        Files.writeString(root.resolve("duplicate-item-uuid-by-player-new.json"), "broken");
        assertEquals(current.path(), latest(root, Mode.CURRENT).path());
        assertEquals(history.path(), latest(root, Mode.HISTORY).path());
        assertNull(latest(root.resolve("missing"), Mode.HISTORY));
    }

    @Test void sourceCurrentReportUsesOnlyMatchingFilenameInsideReportsDirectoryIncludingCrossPlatformPaths() throws Exception {
        Report current = currentReport("duplicate-mythic-item-uuid-source.json", Map.of(UUID, current()));
        for (String prefix : List.of("C:\\server\\plugins\\Pit\\logs\\scan\\uuid\\", "/server/plugins/Pit/logs/scan/uuid/")) {
            Report history = historyReport(prefix + current.path().getFileName(), null, history());
            assertEquals(current.path().toAbsolutePath(), sourceCurrent(history));
        }
        assertNull(sourceCurrent(historyReport("../duplicate-mythic-item-uuid-elsewhere.json", null, history())));
        assertNull(sourceCurrent(historyReport("../unrelated.json", null, history())));
    }

    private Map<String, Object> indexEvent(String owner, String id, long time, List<String> lore) {
        return Map.of("itemUuid", UUID, "playerUuid", "player-" + owner, "backupDatabaseId", id,
                "backupUuid", "backup-" + id, "timestamp", time, "items", List.of(Map.of(
                        "location", "warehouse[2][7]", "internalName", "mythic_sword", "material", "golden_sword",
                        "displayName", "神话之剑", "amount", 2, "lore", lore, "nbt", "ignored")));
    }
    private Path index(List<Map<String, Object>> events) throws Exception {
        Path directory = Files.createDirectories(root.resolve("materialhistory/uuid-history-index-source"));
        Files.writeString(directory.resolve("manifest.json"), "{}");
        try (var writer = Files.newBufferedWriter(directory.resolve("shard-" + UuidDuplicateHistory.shardOf(UUID) + ".ndjson"))) {
            for (var event : events) writer.write(GSON.toJson(event) + "\n");
        }
        return directory;
    }

    @Test void exactLocalIndexBackupCanSupplyFullHistoricalLoreWithoutBorrowingOtherOwnersOrDates() throws Exception {
        Path directory = index(List.of(indexEvent("乙", "first", 1000, List.of("其他玩家，不可使用")),
                indexEvent("甲", "first", 999, List.of("其他时间，不可使用")),
                indexEvent("甲", "wrong", 1000, List.of("其他备份，不可使用")),
                indexEvent("甲", "first", 1000, List.of("生命: 19/19", "", "§b完整历史 Lore"))));
        Report report = historyReport(null, "C:\\server\\" + directory.getFileName(), history());
        HistoricalDetails details = historicalDetails(report, history().firstAppearance(), root.resolve("materialhistory"));
        assertNull(details.warning());
        assertEquals(List.of("生命: 19/19", "", "§b完整历史 Lore"), details.items().getFirst().lore());
        assertEquals(2, details.items().getFirst().amount());
    }

    @Test void missingCorruptOrUnmatchedIndexFallsBackToReportWithoutInventingHistoricalLore() throws Exception {
        Report report = historyReport(null, "/elsewhere/uuid-history-index-source", history());
        var observation = history().firstAppearance();
        assertNotNull(historicalDetails(report, observation, root.resolve("materialhistory")).warning());
        Path directory = index(List.of(indexEvent("甲", "wrong", 1000, List.of("不能用"))));
        HistoricalDetails unmatched = historicalDetails(report, observation, root.resolve("materialhistory"));
        assertNotNull(unmatched.warning()); assertNull(unmatched.items().getFirst().lore());
        Files.writeString(directory.resolve("shard-" + UuidDuplicateHistory.shardOf(UUID) + ".ndjson"), "{broken}");
        HistoricalDetails corrupt = historicalDetails(report, observation, root.resolve("materialhistory"));
        assertNotNull(corrupt.warning()); assertEquals("warehouse[2][7]", corrupt.items().getFirst().location());
    }

    @Test void malformedReportsAndUnsupportedHistoryVersionsAreRejectedNotShownAsPartialResults() throws Exception {
        Report current = currentReport("duplicate-mythic-item-uuid-broken.json", Map.of(UUID, current()));
        Files.writeString(current.path(), Files.readString(current.path()) + "trailing junk");
        assertThrows(IOException.class, () -> page(current, null, 0));
        Report history = historyReport(null, null, history());
        String json = Files.readString(history.path());
        Files.writeString(history.path(), json.substring(0, json.length() - 1));
        assertThrows(IOException.class, () -> page(history, null, 0));
        Files.writeString(history.path(), "{\"version\":999,\"results\":[]}");
        assertThrows(IOException.class, () -> report(history.path(), Mode.HISTORY));
    }
}
