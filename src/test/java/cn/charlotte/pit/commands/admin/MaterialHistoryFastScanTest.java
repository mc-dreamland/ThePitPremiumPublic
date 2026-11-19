package cn.charlotte.pit.commands.admin;

import org.junit.jupiter.api.Test;
import cn.charlotte.pit.util.item.AdvancedItemCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MaterialHistoryFastScanTest {
    private static final String UUID = "bb7fbf1d-c915-493c-abc7-297e478b47ed";
    private static final Set<String> TRACKED = Set.of("mythic_sword", "mystery_box_i");

    @Test
    void scansInventoryArmorOffhandAndAllWarehousesWithoutDoubleCountingArmor() {
        var scan = new MaterialHistoryFastScan(TRACKED, ignored -> item("mythic_sword", 1, UUID, false));
        var backup = backup("boots;legs;chest;helmet|0#main;36#boots;37#legs;38#chest;39#helmet;40#offhand",
                List.of(Map.of("content", "0#warehouse-one"), Map.of("content", "53#warehouse-two")));
        var result = scan.scan(backup);
        assertEquals(6, result.inventory().total());
        assertEquals(2, result.warehouse().total());
        assertEquals(8, result.combined().total());
        assertEquals(8, result.combined().counts().get("mythic_sword"));
        assertTrue(result.stacked().isEmpty());
    }

    @Test
    void legacyBackupsWithArmorOnlyInContentsRemainVisibleExactlyOnce() {
        var scan = new MaterialHistoryFastScan(TRACKED, ignored -> item("mythic_sword", 1, UUID, false));
        assertEquals(4, scan.scan(backup(";;;|36#boots;37#legs;38#chest;39#helmet", List.of())).inventory().total());
        assertEquals(4, scan.scan(backup("boots;;;|36#boots;37#legs;38#chest;39#helmet", List.of())).inventory().total());
    }

    @Test
    void quantityScanKeepsAllAdvancedTypesLegacyWhitelistPhaseMarkersAndAnyAbnormalUuidStacks() {
        var items = Map.of("rare", item("mythic_sword", 2, UUID, false),
                "phase", item("angel_chestplate", 1, UUID, true),
                "basic", item("shopitem", 3, UUID, false),
                "fake-phase", item("shopitem", 1, UUID, true));
        var scan = new MaterialHistoryFastScan(TRACKED, items::get);
        var result = scan.scan(backup(";;;|0#rare;1#phase;2#basic;3#fake-phase", List.of()));
        assertEquals(3, result.combined().total());
        assertEquals(Map.of("mythic_sword", 2, "angel_chestplate", 1, "phase_book_marked_mythic", 1), result.combined().counts());
        assertEquals(2, result.stacked().size());
        assertEquals("shopitem", result.stacked().get(1).internalName());
    }

    @Test
    void quantitiesIncludeAdvancedItemsWithMissingOrMalformedUuidWithoutRepairingThem() {
        var items = Map.of("good", item("MYTHIC_SWORD", 2, UUID, true),
                "missing", item("mythic_sword", 5, null, false),
                "bad", item("mythic_sword", 6, "not-a-uuid", false),
                "basic", item("shopitem", 10, UUID, false));
        var result = new MaterialHistoryFastScan(TRACKED, items::get)
                .scan(backup(";;;|0#good;1#missing;2#bad;3#basic", List.of()));
        assertEquals(13, result.combined().total());
        assertEquals(Map.of("mythic_sword", 13, "phase_book_marked_mythic", 2), result.combined().counts());
        assertEquals(3, result.stacked().size());
        assertEquals(UUID, result.stacked().getFirst().uuid());
        assertNull(items.get("missing").uuid());
    }

    @Test
    void onePhysicalPhaseMarkedItemIsNotCountedTwiceInTotal() {
        var scan = new MaterialHistoryFastScan(TRACKED, ignored -> item("mythic_sword", 1, UUID, true));
        var result = scan.scan(backup(";;;|0#one", List.of()));
        assertEquals(1, result.combined().total());
        assertEquals(2, result.combined().counts().size());
    }

    @Test
    void reportsExactWarehouseArmorAndOffhandLocationsForAbnormalStacks() {
        var scan = new MaterialHistoryFastScan(TRACKED, ignored -> item("mythic_sword", 2, UUID, false));
        var result = scan.scan(backup("armor;;;|0#main;40#offhand", List.of(Map.of("content", "53#chest"))));
        assertEquals(List.of("armor[0]", "inventory[0]", "inventory[40]", "warehouse[0][53]"),
                result.stacked().stream().map(MaterialHistoryFastScan.Stack::location).toList());
    }

    @Test
    void cachesIdenticalEncodedStacksAcrossThousandsOfSnapshotsButKeepsEachOccurrenceCount() {
        AtomicInteger decodes = new AtomicInteger();
        var scan = new MaterialHistoryFastScan(TRACKED, ignored -> {
            decodes.incrementAndGet(); return item("mythic_sword", 1, UUID, false);
        });
        var backup = backup(";;;|0#same;1#same", List.of(Map.of("content", "0#same")));
        for (int i = 0; i < 10_000; i++) assertEquals(3, scan.scan(backup).combined().total());
        assertEquals(1, decodes.get());
        assertEquals(29_999, scan.cacheStats().hits());
        assertEquals(1, scan.cacheStats().decodes());
    }

    @Test
    void cacheIsBoundedByBothEntryCountAndRetainedCharactersWithLruEviction() {
        var scan = new MaterialHistoryFastScan(TRACKED, ignored -> item("mythic_sword", 1, UUID, false), 2, 5);
        scan.scan(backup(";;;|0#aa;1#bb", List.of()));
        scan.scan(backup(";;;|0#aa;1#ccc", List.of())); // aa is most recently used, bb is evicted.
        assertEquals(2, scan.cacheStats().entries());
        assertEquals(5, scan.cacheStats().retainedCharacters());
        scan.scan(backup(";;;|0#too-large-to-cache", List.of()));
        assertEquals(2, scan.cacheStats().entries());
        assertEquals(5, scan.cacheStats().retainedCharacters());
        scan.scan(backup(";;;|0#bb", List.of()));
        assertEquals(5, scan.cacheStats().decodes());
        assertTrue(scan.cacheStats().retainedCharacters() <= 5);
    }

    @Test
    void aChangedSerializedStackIsDecodedAgainAndPreviousResultsRemainImmutable() {
        var scan = new MaterialHistoryFastScan(TRACKED,
                encoded -> item("mythic_sword", encoded.equals("old") ? 1 : 2, UUID, false));
        var old = scan.scan(backup(";;;|0#old", List.of()));
        var changed = scan.scan(backup(";;;|0#new", List.of()));
        assertEquals(1, old.combined().total());
        assertEquals(2, changed.combined().total());
        assertEquals(2, scan.cacheStats().decodes());
        assertThrows(UnsupportedOperationException.class, () -> old.combined().counts().put("new", 3));
    }

    @Test
    void nullEmptyContainersAndAirDoNotCauseDecodeWork() {
        AtomicInteger calls = new AtomicInteger();
        var scan = new MaterialHistoryFastScan(TRACKED, encoded -> { calls.incrementAndGet(); return MaterialHistoryFastScan.EMPTY; });
        assertEquals(0, scan.scan(Map.of()).combined().total());
        for (String sentinel : List.of("null", "'null'", "unset", ""))
            assertEquals(0, scan.scan(backup(sentinel, List.of(Map.of("content", sentinel)))).combined().total());
        assertEquals(0, calls.get());
        assertEquals(0, scan.scan(backup(";;;|0#air", List.of())).combined().total());
        assertEquals(1, calls.get());
    }

    @Test
    void sparseSlotsAndRepeatedSlotAssignmentsUseTheFinalValueOnly() {
        var scan = new MaterialHistoryFastScan(TRACKED,
                encoded -> item("mythic_sword", encoded.equals("a") ? 1 : 3, UUID, false));
        var result = scan.scan(backup(";;;|35#a;35#b;40#", List.of(Map.of("content", "53#a"))));
        assertEquals(3, result.inventory().total());
        assertEquals(1, result.warehouse().total());
        assertEquals(2, scan.cacheStats().decodes());
    }

    @Test
    void corruptContainerOrDecoderFailureIsNotSilentlyReportedAsAnEmptyBackup() {
        var scan = new MaterialHistoryFastScan(TRACKED, ignored -> { throw new IllegalArgumentException("corrupt encoded item"); });
        assertThrows(IllegalArgumentException.class, () -> scan.scan(backup("no separator", List.of())));
        assertThrows(IllegalArgumentException.class, () -> scan.scan(backup(";;;|bad-slot", List.of())));
        assertThrows(IllegalArgumentException.class, () -> scan.scan(backup(";;;|-1#a", List.of())));
        assertThrows(IllegalArgumentException.class, () -> scan.scan(backup(";;;|0#a", List.of())));
        assertThrows(IllegalArgumentException.class, () -> scan.scan(Map.of("chest", List.of(Map.of("rows", 6)))));
        assertEquals(0, scan.cacheStats().entries());
    }

    @Test
    void decodingNeverRewritesRawHistoricalDataOrGeneratesUuid() {
        var scan = new MaterialHistoryFastScan(TRACKED, ignored -> item("mythic_sword", 1, null, false));
        var backup = backup(";;;|0#original-encoded-bytes", List.of(Map.of("content", "3#same-bytes")));
        String original = backup.toString();
        assertEquals(2, scan.scan(backup).combined().total());
        assertEquals(original, backup.toString());
    }

    @Test
    void supportsNullWarehouseEntriesAndLegacyDirectInventoryString() {
        List<Object> chests = new ArrayList<>();
        chests.add(null); chests.add(Map.of("content", "7#item"));
        var raw = Map.of("inv", ";;;|0#item", "chest", chests);
        var result = new MaterialHistoryFastScan(TRACKED, ignored -> item("mythic_sword", 1, UUID, false)).scan(raw);
        assertEquals(2, result.combined().total());
    }

    @Test
    void everyCataloguedAdvancedTypeCountsWithoutUuidAndWithoutBeingInTheOriginalWhitelist() {
        for (String name : AdvancedItemCatalog.items().keySet()) {
            var scan = new MaterialHistoryFastScan(Set.of(), ignored -> item(name, 2, null, false));
            var result = scan.scan(backup(";;;|0#item", List.of()));
            assertEquals(2, result.combined().total(), name);
            assertEquals(2, result.combined().counts().get(name.toLowerCase(java.util.Locale.ROOT)), name);
            assertTrue(result.stacked().isEmpty(), "UUID 缺失不是异常 UUID 堆叠: " + name);
        }
    }

    @Test
    void whiteMythicSwordBowAndLeggingsAndUuidlessConsumablesAllRemainVisible() {
        var items = Map.of("sword", item("mythic_sword", 1, null, false),
                "bow", item("mythic_bow", 1, "", false), "pants", item("mythic_leggings", 1, "invalid", false),
                "phase", item("phase_book", 7, null, false), "feather", item("funky_feather", 9, null, false));
        var result = new MaterialHistoryFastScan(Set.of(), items::get)
                .scan(backup(";;;|0#sword;1#bow;2#pants;3#phase", List.of(Map.of("content", "0#feather"))));
        assertEquals(10, result.inventory().total());
        assertEquals(9, result.warehouse().total());
        assertEquals(19, result.combined().total());
        assertEquals(5, result.combined().counts().size());
        assertTrue(result.stacked().isEmpty());
    }

    @Test
    void retainsLegacyMaterialsOutsideTheCatalogButExcludesUnlistedBasicItemsEvenWithUuid() {
        var items = Map.of("cactus", item("cactus", 4, null, false), "basic", item("shopitem", 1, UUID, false));
        var result = new MaterialHistoryFastScan(Set.of("cactus"), items::get)
                .scan(backup(";;;|0#cactus;1#basic", List.of()));
        assertEquals(Map.of("cactus", 4), result.combined().counts());
        assertEquals(4, result.combined().total());
    }

    private static MaterialHistoryFastScan.Item item(String type, int amount, String uuid, boolean phase) {
        return new MaterialHistoryFastScan.Item(type, "golden_sword", amount, uuid, phase);
    }
    private static Map<String, ?> backup(String inventory, List<?> chests) {
        return Map.of("inv", Map.of("inv", inventory), "chest", chests);
    }
}
