package cn.charlotte.pit.commands.admin;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UuidDuplicateHistoryTest {
    @TempDir Path root;
    private static final Gson GSON = new Gson();
    private static final String UUID = "bb7fbf1d-c915-493c-abc7-297e478b47ed";

    @Test
    void ordinaryTransferIsNotSimultaneousPossessionAndMissingOwnerHistoryIsExplicit() throws Exception {
        var source = index(2, List.of(event(UUID, "甲", "id-a", 1000, item("inventory[0]", 1)),
                event(UUID, "乙", "id-b", 100_000, item("warehouse[1][4]", 1))));
        var results = UuidDuplicateHistory.analyze(source, targets(UUID, "absent"), 60_000, ignored -> {}).results();
        var found = result(results, UUID);
        assertEquals("NO_SIMULTANEOUS_EVIDENCE", found.status());
        assertNull(found.firstCrossPlayerCandidate());
        assertNull(found.firstSameBackupDuplicate());
        assertEquals("乙", found.firstHolderChange().later().playerName());
        assertEquals("NO_LOCAL_HISTORY", result(results, "absent").status());
    }

    @Test
    void findsEarliestDuplicateAcrossInventoryAndWarehouseInOneSnapshot() throws Exception {
        var source = index(2, List.of(event(UUID, "甲", "id-first", 1000, item("inventory[0]", 1)),
                event(UUID, "甲", "id-duplicate", 2000, item("inventory[4]", 1), item("warehouse[3][8]", 1)),
                event(UUID, "甲", "id-later", 3000, item("inventory[5]", 1), item("inventory[6]", 1))));
        var result = analyze(source, 60_000);
        assertEquals("SAME_BACKUP_DUPLICATE", result.status());
        assertEquals("id-duplicate", result.firstSameBackupDuplicate().backupDatabaseId());
        assertEquals(2000, result.firstSameBackupDuplicate().timestamp());
        assertEquals(2, result.firstSameBackupDuplicate().copies());
        assertEquals("warehouse[3][8]", result.firstSameBackupDuplicate().items().get(1).location());
        assertNull(result.firstCrossPlayerCandidate());
    }

    @Test
    void movingSlotsAndRepeatedSerializationOfSameSlotAreNotExtraCopies() throws Exception {
        var source = index(2, List.of(event(UUID, "甲", "id-a", 1000, item("inventory[1]", 1)),
                event(UUID, "甲", "id-b", 2000, item("warehouse[1][2]", 1)),
                event(UUID, "甲", "id-b", 2000, item("warehouse[1][2]", 1), item("warehouse[1][2]", 1))));
        var result = analyze(source, 60_000);
        assertEquals("NO_SIMULTANEOUS_EVIDENCE", result.status());
        assertNull(result.firstSameBackupDuplicate());
        assertNull(result.firstHolderChange());
    }

    @Test
    void stackedUuidInOneSlotIsDuplicateEvenWithoutTwoSlots() throws Exception {
        var result = analyze(index(2, List.of(event(UUID, "甲", "id-stack", 1000, item("inventory[2]", 3)))), 0);
        assertEquals(3, result.firstSameBackupDuplicate().copies());
        assertEquals(1, result.firstSameBackupDuplicate().items().size());
    }

    @Test
    void firstCloseCrossPlayerObservationIsCandidateNotAConfirmedDuplication() throws Exception {
        var source = index(1, List.of(event(UUID, "甲", "id-a", 1000, item("inventory[0]", 1)),
                event(UUID, "乙", "id-b", 61_000, item("warehouse[1][2]", 1)),
                event(UUID, "丙", "id-c", 61_500, item("inventory[0]", 1))));
        var result = analyze(source, 60_000);
        assertEquals("CROSS_PLAYER_CANDIDATE_ONLY", result.status());
        assertEquals("甲", result.firstCrossPlayerCandidate().earlier().playerName());
        assertEquals("乙", result.firstCrossPlayerCandidate().later().playerName());
        assertEquals(60_000, result.firstCrossPlayerCandidate().differenceMillis());
        assertNull(result.firstSameBackupDuplicate());
    }

    @Test
    void zeroWindowOnlyIncludesExactlyEqualMillisecond() throws Exception {
        var source = index(2, List.of(event(UUID, "甲", "id-a", 1000, item("inventory[0]", 1)),
                event(UUID, "乙", "id-b", 1001, item("inventory[0]", 1)),
                event(UUID, "丙", "id-c", 1001, item("inventory[1]", 1))));
        var result = analyze(source, 0);
        assertEquals("乙", result.firstCrossPlayerCandidate().earlier().playerName());
        assertEquals("丙", result.firstCrossPlayerCandidate().later().playerName());
        assertEquals(0, result.firstCrossPlayerCandidate().differenceMillis());
    }

    @Test
    void sameDatabaseBackupAssignedToTwoNamesIsNotCrossPlayerEvidence() throws Exception {
        var source = index(2, List.of(event(UUID, "甲", "same-id", 1000, item("inventory[0]", 1)),
                event(UUID, "乙", "same-id", 1000, item("inventory[0]", 1))));
        assertNull(analyze(source, 60_000).firstCrossPlayerCandidate());
    }

    @Test
    void renamedPlayerWithSameUuidIsNotAnotherHolder() throws Exception {
        var a = event(UUID, "旧名", "id-a", 1000, item("inventory[0]", 1));
        var b = event(UUID, "新名", "id-b", 1001, item("inventory[4]", 1));
        b.put("playerUuid", a.get("playerUuid"));
        var result = analyze(index(2, List.of(a, b)), 60_000);
        assertNull(result.firstCrossPlayerCandidate());
        assertNull(result.firstHolderChange());
    }

    @Test
    void unrelatedLargeNbtIsNotRetainedAndOnlyRelevantShardsAreRead() throws Exception {
        var unrelated = event("not-target", "其他人", "id-other", 500, item("inventory[0]", 1));
        unrelated.put("items", List.of(Map.of("location", "inventory[0]", "amount", 1,
                "lore", List.of("很长的 lore"), "nbt", "x".repeat(100_000))));
        var source = index(2, List.of(event(UUID, "甲", "id-a", 1000, item("inventory[0]", 1))));
        Path relevant = source.directory().resolve("shard-" + UuidDuplicateHistory.shardOf(UUID) + ".ndjson");
        Files.writeString(relevant, GSON.toJson(unrelated) + "\n" + Files.readString(relevant));
        Files.writeString(source.directory().resolve("shard-unrelated.ndjson"), "broken data must not be read");
        var analysis = UuidDuplicateHistory.analyze(source, targets(UUID), 60_000, ignored -> {});
        assertEquals(2, analysis.progress().readEvents());
        assertEquals(1, analysis.progress().matchedEvents());
        assertEquals(1, analysis.progress().totalShards());
    }

    @Test
    void outOfOrderAndMalformedMatchedHistoryFailInsteadOfReturningWrongEarliest() throws Exception {
        var source = index(2, List.of(event(UUID, "甲", "id-a", 2000, item("inventory[0]", 1)),
                event(UUID, "乙", "id-b", 1000, item("inventory[1]", 1))));
        assertThrows(IOException.class, () -> analyze(source, 60_000));
        Path shard = source.directory().resolve("shard-" + UuidDuplicateHistory.shardOf(UUID) + ".ndjson");
        Files.writeString(shard, "{broken json\n");
        assertThrows(IOException.class, () -> analyze(source, 60_000));
    }

    @Test
    void missingShardFailsAndMissingTimestampIsCountedAsInvalid() throws Exception {
        var source = index(2, List.of(event(UUID, "甲", "id-a", 0, item("inventory[0]", 2))));
        var analysis = UuidDuplicateHistory.analyze(source, targets(UUID), 0, ignored -> {});
        assertEquals(1, analysis.progress().invalidEvents());
        assertEquals("NO_LOCAL_HISTORY", analysis.results().getFirst().status());
        Files.delete(source.directory().resolve("shard-" + UuidDuplicateHistory.shardOf(UUID) + ".ndjson"));
        assertThrows(IOException.class, () -> analyze(source, 0));
    }

    @Test
    void duplicateReportDiscoveryIgnoresTemporaryAndOtherReportsAndSupportsRestart() throws Exception {
        Path directory = Files.createDirectory(root.resolve("uuid"));
        Path complete = directory.resolve("duplicate-mythic-item-uuid-1.json");
        Files.writeString(complete, """
                {"target":{"count":2,"locations":[
                  {"playerUuid":"a","playerName":"玩家甲","container":"inventory","slot":1,"lore":["ignored"]},
                  {"playerUuid":"b","playerName":"玩家乙","container":"ender_chest","warehouseIndex":2,"slot":3}]},
                 "single":{"count":1,"locations":[{}]}}
                """);
        Files.writeString(directory.resolve("duplicate-mythic-item-uuid-2.json.tmp"), "partial");
        Files.writeString(directory.resolve("duplicate-uuid-history-3.json"), "unrelated");
        assertEquals(complete, UuidDuplicateHistory.latestDuplicateReport(directory));
        var read = UuidDuplicateHistory.readDuplicates(complete);
        assertEquals(1, read.size());
        assertEquals("玩家甲", read.get("target").locations().getFirst().playerName());
        assertEquals(2, read.get("target").locations().get(1).warehouseIndex());
    }

    @Test
    void onlyCompletedLatestIndexIsUsedAndPathTraversalInManifestIsRejected() throws Exception {
        var source = index(2, List.of());
        assertEquals(source.directory(), UuidDuplicateHistory.latestIndex(root).directory());
        Files.createDirectory(root.resolve("uuid-history-index-unfinished"));
        assertEquals(source.directory(), UuidDuplicateHistory.latestIndex(root).directory());
        Path bad = Files.createDirectory(root.resolve("uuid-history-index-bad"));
        Path manifest = bad.resolve("manifest.json");
        Files.writeString(manifest, "{\"version\":2,\"shards\":[\"../escape\"]}");
        Files.setLastModifiedTime(manifest, FileTime.fromMillis(System.currentTimeMillis() + 100_000));
        assertThrows(IOException.class, () -> UuidDuplicateHistory.latestIndex(root));
    }

    @Test
    void resultsAreChronologicalAndJsonIncludesSourcesCurrentOwnersAndEvidenceDetails() throws Exception {
        String other = "other-uuid";
        var source = index(2, List.of(event(UUID, "甲", "id-late", 3000, item("inventory[1]", 2)),
                event(other, "乙", "id-early", 1000, item("inventory[3]", 1), item("warehouse[1][0]", 1))));
        var analysis = UuidDuplicateHistory.analyze(source, targets(UUID, other, "absent"), 60_000, ignored -> {});
        assertEquals(List.of(other, UUID, "absent"), analysis.results().stream().map(UuidDuplicateHistory.Result::itemUuid).toList());
        Path original = root.resolve("duplicate-report.json");
        Files.writeString(original, "{}");
        Path output = root.resolve("result.json");
        UuidDuplicateHistory.writeReport(output, original, analysis, 60);
        var json = JsonParser.parseString(Files.readString(output)).getAsJsonObject();
        assertEquals(60, json.get("crossPlayerWindowSeconds").getAsInt());
        assertEquals(2, json.getAsJsonObject("historyMetadata").get("version").getAsInt());
        var result = json.getAsJsonArray("results").get(0).getAsJsonObject();
        assertEquals("id-early", result.getAsJsonObject("firstSameBackupDuplicate").get("backupDatabaseId").getAsString());
        assertTrue(result.get("firstCrossPlayerCandidate").isJsonNull());
        assertEquals("当前玩家", result.getAsJsonObject("current").getAsJsonArray("locations").get(0)
                .getAsJsonObject().get("playerName").getAsString());
        assertFalse(Files.exists(output.resolveSibling("result.json.tmp")));
    }

    @Test
    void longHistoryIsStreamedWithProgressAndRepeatedSightingsAreNotAssumedToOverlapOtherOwners() throws Exception {
        List<Map<String, Object>> events = new ArrayList<>();
        for (int i = 1; i <= 10_001; i++)
            events.add(event(UUID, "甲", "id-" + i, i, item("inventory[" + (i % 36) + "]", 1)));
        events.add(event(UUID, "乙", "id-trade", 100_000, item("inventory[0]", 1)));
        List<UuidDuplicateHistory.Progress> updates = new ArrayList<>();
        var analysis = UuidDuplicateHistory.analyze(index(2, events), targets(UUID), 1000, updates::add);
        assertEquals(10_002, analysis.results().getFirst().recordedEvents());
        assertTrue(updates.stream().anyMatch(value -> value.readEvents() == 10_000));
        assertNull(analysis.results().getFirst().firstCrossPlayerCandidate());
        assertNull(analysis.results().getFirst().firstSameBackupDuplicate());
    }

    @Test
    void publishingNeverOverwritesAnExistingCompletedReport() throws Exception {
        Path output = root.resolve("already-completed.json");
        Path temporary = root.resolve("new.json.tmp");
        Files.writeString(output, "existing report");
        Files.writeString(temporary, "new report");
        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> UuidDuplicateHistory.publish(temporary, output));
        assertEquals("existing report", Files.readString(output));
        assertTrue(Files.exists(temporary));
    }

    private UuidDuplicateHistory.Result analyze(UuidDuplicateHistory.Source source, long window) throws IOException {
        return UuidDuplicateHistory.analyze(source, targets(UUID), window, ignored -> {}).results().getFirst();
    }
    private static UuidDuplicateHistory.Result result(List<UuidDuplicateHistory.Result> results, String uuid) {
        return results.stream().filter(result -> uuid.equals(result.itemUuid())).findFirst().orElseThrow();
    }
    private static Map<String, UuidDuplicateHistory.CurrentDuplicate> targets(String... uuids) {
        Map<String, UuidDuplicateHistory.CurrentDuplicate> result = new HashMap<>();
        for (String uuid : uuids) result.put(uuid, new UuidDuplicateHistory.CurrentDuplicate(2, List.of(
                new UuidDuplicateHistory.CurrentLocation("now-a", "当前玩家", "inventory", null, 1, null, null, null, "神话之剑"),
                new UuidDuplicateHistory.CurrentLocation("now-b", "其他玩家", "ender_chest", 1, 2, null, null, null, "神话之剑"))));
        return result;
    }
    private static Map<String, Object> item(String location, int amount) {
        return Map.of("location", location, "amount", amount, "internalName", "mythic_sword",
                "material", "golden_sword", "displayName", "神话之剑");
    }
    @SafeVarargs
    private static Map<String, Object> event(String uuid, String player, String id, long time, Map<String, Object>... items) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("itemUuid", uuid); value.put("playerUuid", "uuid-" + player); value.put("playerName", player);
        value.put("backupDatabaseId", id); value.put("backupUuid", "backup-" + id); value.put("timestamp", time);
        value.put("time", "readable-" + time); value.put("prestige", 30); value.put("items", List.of(items));
        return value;
    }
    private UuidDuplicateHistory.Source index(int version, List<Map<String, Object>> events) throws IOException {
        Path directory = Files.createTempDirectory(root, "uuid-history-index-");
        Map<String, List<Map<String, Object>>> shards = new LinkedHashMap<>();
        for (var event : events)
            shards.computeIfAbsent(UuidDuplicateHistory.shardOf(event.get("itemUuid").toString()), ignored -> new ArrayList<>()).add(event);
        for (var shard : shards.entrySet()) {
            StringBuilder text = new StringBuilder();
            shard.getValue().forEach(event -> text.append(GSON.toJson(event)).append('\n'));
            Files.writeString(directory.resolve("shard-" + shard.getKey() + ".ndjson"), text);
        }
        var metadata = new UuidDuplicateHistory.Metadata(version, 10_000, "material-history.json",
                events.size(), events.size(), 0, List.copyOf(shards.keySet()));
        Files.writeString(directory.resolve("manifest.json"), GSON.toJson(metadata));
        return new UuidDuplicateHistory.Source(directory, metadata);
    }
}
