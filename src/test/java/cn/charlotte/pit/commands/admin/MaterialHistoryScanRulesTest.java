package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.util.item.AdvancedItemCatalog;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MaterialHistoryScanRulesTest {
    @Test void quantityCatalogIncludesAllAdvancedItemsAndPreservesLegacyResourcesAndDisplayNames() {
        var items = MaterialHistoryScanRules.quantityItems(Map.of("cactus", "哲学仙人掌", "totally_legit_gem", "遵纪守法的宝石"));
        for (String id : AdvancedItemCatalog.items().keySet()) assertTrue(items.containsKey(id.toLowerCase(java.util.Locale.ROOT)), id);
        assertEquals("哲学仙人掌", items.get("cactus"));
        assertEquals("遵纪守法的宝石", items.get("totally_legit_gem"));
        assertTrue(items.containsKey("mythic_sword"));
        assertTrue(items.containsKey("mythic_bow"));
        assertTrue(items.containsKey("mythic_leggings"));
        assertFalse(items.containsKey("shopitem"));
        assertThrows(UnsupportedOperationException.class, () -> items.put("other", "other"));
    }
    @Test void phaseMarkerRequiresAKnownMythicFamilyWithoutConstructingAnItem() {
        assertTrue(MaterialHistoryScanRules.phaseBookMarkedMythic("mythic_sword", true));
        assertTrue(MaterialHistoryScanRules.phaseBookMarkedMythic("MythicShovel", true));
        assertTrue(MaterialHistoryScanRules.phaseBookMarkedMythic("kings_helmet", true));
        assertFalse(MaterialHistoryScanRules.phaseBookMarkedMythic("mythic_sword", false));
        assertFalse(MaterialHistoryScanRules.phaseBookMarkedMythic("shopitem", true));
        assertFalse(MaterialHistoryScanRules.phaseBookMarkedMythic(null, true));
    }
    @Test void everyCataloguedAdvancedItemIsAllowedNotJustTheOriginalRareMaterialWhitelist() {
        String uuid = UUID.randomUUID().toString();
        for (String name : AdvancedItemCatalog.items().keySet()) assertTrue(MaterialHistoryScanRules.uuidAdvanced(name, uuid), name);
        assertTrue(MaterialHistoryScanRules.uuidAdvanced("MYTHIC_SWORD", uuid.toUpperCase()));
        assertTrue(MaterialHistoryScanRules.uuidAdvanced("magic_fishing_rod", uuid));
        assertTrue(MaterialHistoryScanRules.uuidAdvanced("CustomNamePaper", uuid));
    }
    @Test void missingMalformedOrNoncanonicalUuidsAreNotCountedAndNothingIsGenerated() {
        assertFalse(MaterialHistoryScanRules.uuidAdvanced("mythic_sword", null));
        assertFalse(MaterialHistoryScanRules.uuidAdvanced("mythic_sword", ""));
        assertFalse(MaterialHistoryScanRules.uuidAdvanced("mythic_sword", "not-a-uuid"));
        assertFalse(MaterialHistoryScanRules.uuidAdvanced("mythic_sword", "0-0-0-0-1"));
    }
    @Test void basicShopOrUnknownItemsAreExcludedEvenIfTheyContainAUuidTag() {
        String uuid = UUID.randomUUID().toString();
        assertFalse(MaterialHistoryScanRules.uuidAdvanced("shopitem", uuid));
        assertFalse(MaterialHistoryScanRules.uuidAdvanced(null, uuid));
        assertFalse(MaterialHistoryScanRules.uuidAdvanced("unknown_custom_item", uuid));
    }
}
