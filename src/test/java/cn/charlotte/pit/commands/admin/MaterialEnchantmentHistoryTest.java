package cn.charlotte.pit.commands.admin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static cn.charlotte.pit.commands.admin.MaterialEnchantmentHistory.*;
import static org.junit.jupiter.api.Assertions.*;

class MaterialEnchantmentHistoryTest {
    @TempDir Path root;
    private static final String ITEM_UUID = "bb7fbf1d-c915-493c-abc7-297e478b47ed";

    @Test
    void groupsByRecordedOperatorNotInventoryOwnerAndPersistsAcrossReload() throws Exception {
        var report = build(List.of(event(ITEM_UUID, "持有人", 1000, item(1, "附魔者|Enchant Table|900"))));
        assertEquals(1, report.manifest().actors().size());
        assertEquals("附魔者", report.manifest().actors().getFirst().playerName());
        assertEquals(1, actorItems(report, actorKey("附魔者")).size());
        ItemReport item = item(report, ITEM_UUID);
        assertEquals("附魔者", item.operations().getFirst().playerName);
        assertEquals("持有人", item.operations().getFirst().firstSeen.playerName());
        Report reloaded = latest(root);
        assertEquals(report.directory(), reloaded.directory());
        assertEquals(1, item(reloaded, ITEM_UUID).operations().size());
        assertEquals(ITEM_UUID, actorItems(reloaded, actorKey("附魔者")).getFirst().itemUuid());
    }

    @Test
    void mergesRepeatedRecordsCaseInsensitivelyAcrossBackupsAndCopiesButNotDifferentEquipment() throws Exception {
        String records = "Alice|Enchant Table|900;alice|Enchant Table|900;Alice|totally_legit_gem|950";
        var report = build(List.of(event(ITEM_UUID, "A", 1000, item(1, records), item(1, records)),
                event(ITEM_UUID, "B", 2000, item(1, records)),
                event("another-uuid", "C", 3000, item(1, "Alice|Enchant Table|900"))));
        var item = item(report, ITEM_UUID);
        assertEquals(2, item.operations().size());
        assertEquals(2, item.operations().getFirst().observedBackups);
        assertEquals(3, report.manifest().recordCount());
        assertEquals(2, actorItems(report, actorKey("ALICE")).size());
        assertEquals(1, report.manifest().actors().size());
    }

    @Test
    void rollingFiveRecordWindowsRecoverOlderRecordsFromEarlierSnapshots() throws Exception {
        String first = records(1, 5), second = records(4, 8);
        var report = build(List.of(event(ITEM_UUID, "Holder", 10_000, item(1, first)),
                event(ITEM_UUID, "Holder", 20_000, item(1, second))));
        assertEquals(8, item(report, ITEM_UUID).operations().size());
        assertEquals(List.of(100L, 200L, 300L, 400L, 500L, 600L, 700L, 800L),
                item(report, ITEM_UUID).operations().stream().map(operation -> operation.id.timestamp()).toList());
        assertTrue(report.manifest().limitations().stream().anyMatch(value -> value.contains("最近 5 条")));
    }

    @Test
    void initialEnchantedStateIsBaselineNotAnInventedEnchantingOperation() throws Exception {
        var report = build(List.of(event(ITEM_UUID, "Holder", 1000, item(3, "Alice|Enchant Table|900"))));
        assertTrue(item(report, ITEM_UUID).changes().isEmpty());
        assertEquals(1, item(report, ITEM_UUID).operations().size());
    }

    @Test
    void comparesEnchantmentLevelsAndLinksOneRecordedCandidateWithoutClaimingExactAttribution() throws Exception {
        var report = build(List.of(event(ITEM_UUID, "A", 1000, item(1, "")),
                event(ITEM_UUID, "B", 2000, item(3, "Enchanter|totally_legit_gem|1500"))));
        Change change = item(report, ITEM_UUID).changes().getFirst();
        assertEquals("ONE_RECORDED_CANDIDATE", change.attribution());
        assertEquals(List.of(new Delta("shark", 1, 3)), change.differences());
        assertEquals(actorKey("Enchanter"), change.recordedCandidates().getFirst().actorKey());
        assertEquals("A", change.beforeEvidence().playerName());
        assertEquals("B", change.afterEvidence().playerName());
        assertTrue(change.before().nbt().isJsonObject());
        assertEquals(1, change.before().nbt().getAsJsonObject("extra").getAsJsonArray("ench").size());
    }

    @Test
    void missingOperationRecordsLeaveTheOperatorUnknownRatherThanUsingHolderName() throws Exception {
        var report = build(List.of(event(ITEM_UUID, "不应背锅", 1000, item(1, "")),
                event(ITEM_UUID, "不应背锅", 2000, item(2, ""))));
        var changed = item(report, ITEM_UUID).changes().getFirst();
        assertEquals("NO_RECORD_CORRELATION", changed.attribution());
        assertTrue(changed.recordedCandidates().isEmpty());
        assertTrue(item(report, ITEM_UUID).operations().isEmpty());
        assertEquals(UNKNOWN_ACTOR, report.manifest().actors().getFirst().key());
        assertEquals(0, report.manifest().actors().getFirst().recordCount());
    }

    @Test
    void multipleOperationsInOneBackupIntervalAreMarkedAmbiguous() throws Exception {
        var report = build(List.of(event(ITEM_UUID, "Holder", 1000, item(1, "")),
                event(ITEM_UUID, "Holder", 2000, item(3, "Alice|Enchant Table|1200;Bob|totally_legit_gem|1700"))));
        Change change = item(report, ITEM_UUID).changes().getFirst();
        assertEquals("MULTIPLE_RECORDED_CANDIDATES", change.attribution());
        assertEquals(2, change.recordedCandidates().size());
        assertTrue(report.manifest().actors().stream().anyMatch(actor -> actor.key().equals(UNKNOWN_ACTOR)));
    }

    @Test
    void onlyLivesLoreLocationOrOwnerChangingDoesNotCreateAnEnchantmentChange() throws Exception {
        var before = item(2, "Alice|Enchant Table|900");
        var after = item(2, "Alice|Enchant Table|900");
        after.put("location", "warehouse[3][10]");
        after.put("lore", List.of("生命: 8/10", "完全不同的文字"));
        ((JsonObject) after.get("nbt")).getAsJsonObject("extra").addProperty("live", 8);
        var report = build(List.of(event(ITEM_UUID, "A", 1000, before), event(ITEM_UUID, "B", 2000, after)));
        assertTrue(item(report, ITEM_UUID).changes().isEmpty());
        assertEquals(1, item(report, ITEM_UUID).operations().size());
    }

    @Test
    void conflictingCopiesInOneBackupBreakTheLinearChangeChain() throws Exception {
        var report = build(List.of(event(ITEM_UUID, "Holder", 1000, item(1, "")),
                event(ITEM_UUID, "Holder", 2000, item(1, "Alice|Enchant Table|1500"), item(3, "Alice|Enchant Table|1500")),
                event(ITEM_UUID, "Holder", 3000, item(3, "Alice|Enchant Table|1500"))));
        var item = item(report, ITEM_UUID);
        assertEquals(1, item.conflicts().size());
        assertEquals(2, item.conflicts().getFirst().states().size());
        assertTrue(item.changes().isEmpty());
        assertEquals(1, item.operations().size());
    }

    @Test
    void duplicateCopiesOrIdenticalBackupTimesDoNotAllowSingleOperationAttribution() throws Exception {
        var report = build(List.of(event(ITEM_UUID, "Holder", 1000, item(1, ""), item(1, "")),
                event(ITEM_UUID, "Holder", 2000, item(2, "Alice|Enchant Table|1500"))));
        assertEquals("AMBIGUOUS_COPY_OR_ORDER", item(report, ITEM_UUID).changes().getFirst().attribution());
        var other = build(List.of(event("other", "A", 1000, item(1, "Alice|Enchant Table|900")),
                event("other", "B", 1000, item(2, "Alice|Enchant Table|900"))));
        assertEquals("AMBIGUOUS_COPY_OR_ORDER", item(other, "other").changes().getFirst().attribution());
    }

    @Test
    void parsesRealSnbtIntoStructuredJsonAndPreservesChineseAndFullLore() throws Exception {
        var input = item(1, "");
        input.put("nbt", "{PublicBukkitValues:{\"pit:internal\":\"mythic_sword\"},extra:{ench:[\"shark:1\"],tier:3,live:19,"
                + "records:\"中文玩家|Enchant Table|900\",uuid:\"" + ITEM_UUID + "\"}}");
        input.put("lore", java.util.stream.IntStream.range(0, 100).mapToObj(index -> "完整 Lore 第 " + index + " 行").toList());
        var report = build(List.of(event(ITEM_UUID, "Holder", 1000, input)));
        var result = item(report, ITEM_UUID);
        assertEquals("中文玩家", result.operations().getFirst().playerName);
        assertEquals(100, result.operations().getFirst().firstObservedLore.size());
        assertEquals(100, result.latestStates().getFirst().lore().size());
        assertEquals(19, result.latestStates().getFirst().nbt().getAsJsonObject("extra").get("live").getAsInt());
        String saved = Files.readString(itemPath(report.directory(), ITEM_UUID));
        assertTrue(saved.contains("中文玩家"));
        assertTrue(JsonParser.parseString(saved).getAsJsonObject().getAsJsonArray("latestStates").get(0)
                .getAsJsonObject().get("nbt").isJsonObject());
    }

    @Test
    void malformedOrFutureRecordsAreNotUsedAsReliableChangeAttribution() throws Exception {
        var report = build(List.of(event(ITEM_UUID, "Holder", 1000, item(1, "Alice|Enchant Table|99999;broken;Bob|x|no")),
                event(ITEM_UUID, "Holder", 2000, item(2, "Alice|Enchant Table|99999"))));
        assertEquals(2, report.manifest().malformedItems());
        var result = item(report, ITEM_UUID);
        assertTrue(result.operations().getFirst().timestampAfterFirstEvidence);
        assertEquals("NO_RECORD_CORRELATION", result.changes().getFirst().attribution());
    }

    @Test
    void corruptItemNbtIsReportedAndBreaksComparisonAcrossTheMissingState() throws Exception {
        var bad = item(2, ""); bad.put("nbt", "{invalid snbt");
        var report = build(List.of(event(ITEM_UUID, "Holder", 1000, item(1, "Alice|Enchant Table|900")),
                event(ITEM_UUID, "Holder", 2000, bad), event(ITEM_UUID, "Holder", 3000, item(3, "Alice|Enchant Table|900"))));
        assertTrue(item(report, ITEM_UUID).changes().isEmpty());
        assertEquals(1, report.manifest().malformedItems());
    }

    @Test
    void ordinaryItemsAreExcludedAndEquipmentHistoriesWithRecordsAreIncluded() throws Exception {
        var basic = item(1, "Someone|Enchant Table|900"); basic.put("internalName", "shopitem");
        var rod = item(1, "Angler|Enchant Table|900"); rod.put("internalName", "magic_fishing_rod");
        var report = build(List.of(event(ITEM_UUID, "Holder", 1000, basic), event("rod-uuid", "Holder", 2000, rod)));
        assertEquals(1, report.manifest().itemCount());
        assertEquals("Angler", item(report, "rod-uuid").operations().getFirst().playerName);
        assertThrows(IllegalArgumentException.class, () -> item(report, ITEM_UUID));
    }

    @Test
    void missingOrOutOfOrderSourceFailsWithoutPublishingACompletedManifest() throws Exception {
        var source = source(List.of(event(ITEM_UUID, "Holder", 2000, item(1, "")), event(ITEM_UUID, "Holder", 1000, item(2, ""))));
        Path output = root.resolve("enchant-record-history-failed");
        assertThrows(IOException.class, () -> MaterialEnchantmentHistory.build(source, output, ignored -> {}));
        assertFalse(Files.exists(output.resolve("manifest.json")));
        assertNull(latest(root));
        var missing = new UuidDuplicateHistory.Source(source.directory(), new UuidDuplicateHistory.Metadata(2, 1, "report", 1, 1, 0, List.of("fff")));
        assertThrows(IOException.class, () -> MaterialEnchantmentHistory.build(missing, root.resolve("enchant-record-history-missing"), ignored -> {}));
    }

    @Test
    void filePathsAreHashedAndCannotEscapeTheReportDirectory() throws Exception {
        String maliciousUuid = "../../outside";
        var report = build(List.of(event(maliciousUuid, "Holder", 1000, item(1, "玩家甲|Enchant Table|900"))));
        assertTrue(itemPath(report.directory(), maliciousUuid).normalize().startsWith(report.directory()));
        assertEquals(maliciousUuid, item(report, maliciousUuid).itemUuid());
        assertEquals(1, actorItems(report, actorKey("玩家甲")).size());
    }

    @Test
    void exportsActualBuiltRecordsIncludingRepeatedGemObservationsExactlyOnce() throws Exception {
        var report = build(List.of(event(ITEM_UUID, "Holder", 1000,
                        item(1, "Alice|Enchant Table|800;Alice|totally_legit_gem|900;Bob|global_attention_gem|950")),
                event(ITEM_UUID, "AnotherHolder", 2000,
                        item(3, "Alice|Enchant Table|800;Alice|totally_legit_gem|900;Bob|global_attention_gem|950"))));
        var exported = MaterialEnchantmentExport.export(report, "Alice", root.resolve("exports"), java.time.ZoneOffset.UTC, ignored -> {});
        assertEquals(List.of("1970-01-01 00:00:00.800 - Alice - " + ITEM_UUID + " - Enchant Table",
                "1970-01-01 00:00:00.900 - Alice - " + ITEM_UUID + " - totally_legit_gem"), Files.readAllLines(exported.path()));
        assertEquals(2, exported.records());
    }

    @Test
    void currentEquipmentLookupByUuidIncludesAllRecordedOperatorsAndGemSourcesNotJustTheCurrentHolder() throws Exception {
        var report = build(List.of(event(ITEM_UUID, "当前持有人", 1000,
                        item(1, "最初附魔者|Enchant Table|800;宝石操作者|totally_legit_gem|900")),
                event(ITEM_UUID, "当前持有人", 2000,
                        item(3, "最初附魔者|Enchant Table|800;宝石操作者|totally_legit_gem|900;第三人|global_attention_gem|1500"))));
        assertTrue(CurrentHistoryItemActions.canViewEnchantments("mythic_sword", ITEM_UUID));
        var equipment = item(report, ITEM_UUID);
        assertEquals(List.of("最初附魔者", "宝石操作者", "第三人"), equipment.operations().stream().map(operation -> operation.playerName).toList());
        assertEquals(List.of("Enchant Table", "totally_legit_gem", "global_attention_gem"),
                equipment.operations().stream().map(operation -> operation.id.source()).toList());
        assertTrue(equipment.operations().stream().noneMatch(operation -> "当前持有人".equals(operation.playerName)));
        assertEquals("当前持有人", equipment.lastSeen().playerName());
    }

    private Report build(List<Map<String, Object>> events) throws IOException {
        return MaterialEnchantmentHistory.build(source(events), root.resolve("enchant-record-history-" + UUID.randomUUID()), ignored -> {});
    }
    private static ItemReport item(Report report, String uuid) throws IOException {
        return MaterialEnchantmentHistory.item(report, uuid);
    }
    private UuidDuplicateHistory.Source source(List<Map<String, Object>> events) throws IOException {
        Path source = Files.createTempDirectory(root, "uuid-history-index-");
        Map<String, List<Map<String, Object>>> shards = new TreeMap<>();
        for (var event : events) shards.computeIfAbsent(UuidDuplicateHistory.shardOf(event.get("itemUuid").toString()), ignored -> new ArrayList<>()).add(event);
        for (var shard : shards.entrySet()) {
            StringBuilder output = new StringBuilder();
            for (var event : shard.getValue()) output.append(GSON.toJson(event)).append('\n');
            Files.writeString(source.resolve("shard-" + shard.getKey() + ".ndjson"), output);
        }
        return new UuidDuplicateHistory.Source(source,
                new UuidDuplicateHistory.Metadata(2, 100_000, "all-report.json", events.size(), events.size(), 0, List.copyOf(shards.keySet())));
    }
    private static Map<String, Object> item(int level, String records) {
        JsonObject extra = new JsonObject(); extra.add("ench", GSON.toJsonTree(level == 0 ? List.of() : List.of("shark:" + level)));
        extra.addProperty("tier", 3); extra.addProperty("live", 19); extra.addProperty("records", records);
        JsonObject nbt = new JsonObject(); nbt.add("extra", extra);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("internalName", "mythic_sword"); item.put("material", "golden_sword"); item.put("displayName", "III 阶神话之剑");
        item.put("location", "inventory[0]"); item.put("amount", 1); item.put("lore", List.of("生命: 19/19", "原始 Lore")); item.put("nbt", nbt);
        return item;
    }
    @SafeVarargs
    private static Map<String, Object> event(String uuid, String holder, long timestamp, Map<String, Object>... items) {
        return Map.of("itemUuid", uuid, "playerUuid", "holder-uuid-" + holder, "playerName", holder, "prestige", 30,
                "backupDatabaseId", "id-" + holder + "-" + timestamp, "backupUuid", "backup-" + holder + "-" + timestamp,
                "timestamp", timestamp, "time", "readable-" + timestamp, "items", List.of(items));
    }
    private static String records(int first, int last) {
        List<String> records = new ArrayList<>();
        for (int i = first; i <= last; i++) records.add("Alice|Enchant Table|" + i * 100);
        return String.join(";", records);
    }
}
