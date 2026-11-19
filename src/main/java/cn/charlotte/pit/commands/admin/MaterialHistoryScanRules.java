package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.util.item.AdvancedItemCatalog;
import java.util.UUID;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

/** Read-only classification. Never adds/repairs UUIDs in a historical item. */
final class MaterialHistoryScanRules {
    // Same item families recognized by Utils.getMythicItem0, without instantiating/reformatting an item.
    private static final Set<String> MYTHIC_ITEMS = Set.of("mythic_sword", "mythic_bow", "mythic_leggings",
            "angel_chestplate", "armageddon_boots", "kings_helmet", "turtle_helmet", "lucky_chestplate",
            "mythicshovel", "exorcism_armor", "black_stone", "bounty_quest", "gleam_stone", "greed_gold", "fly_boots");
    private MaterialHistoryScanRules() { }
    static Map<String, String> quantityItems(Map<String, String> legacyItems) {
        Map<String, String> items = new LinkedHashMap<>();
        legacyItems.forEach((id, label) -> items.put(id.toLowerCase(Locale.ROOT), label));
        AdvancedItemCatalog.items().forEach((id, label) -> items.putIfAbsent(id.toLowerCase(Locale.ROOT), label));
        return Collections.unmodifiableMap(items);
    }
    // Identity validation only. Never use this as a prerequisite for quantity statistics.
    static boolean uuidAdvanced(String internalName, String uuid) {
        if (!AdvancedItemCatalog.isAdvanced(internalName) || uuid == null || uuid.isBlank()) return false;
        try { return UUID.fromString(uuid).toString().equalsIgnoreCase(uuid); }
        catch (IllegalArgumentException ignored) { return false; }
    }
    static boolean mythic(String internalName) {
        return internalName != null && MYTHIC_ITEMS.contains(internalName.toLowerCase(Locale.ROOT));
    }
    static boolean phaseBookMarkedMythic(String internalName, boolean marked) { return marked && mythic(internalName); }
}
