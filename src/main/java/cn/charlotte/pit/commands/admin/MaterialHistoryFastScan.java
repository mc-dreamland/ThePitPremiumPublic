package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.util.item.AdvancedItemCatalog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Read-only scan of the serialized backup format, without player login refresh or Bukkit inventories. */
final class MaterialHistoryFastScan {
    static final String PHASE_BOOK_MARKED_MYTHIC = "phase_book_marked_mythic";
    record Item(String internalName, String material, int amount, String uuid, boolean phaseBookMarked) {
        Item {
            internalName = internalName == null ? "" : internalName.toLowerCase(Locale.ROOT);
            material = material == null ? "" : material.toLowerCase(Locale.ROOT);
        }
    }
    static final Item EMPTY = new Item("", "air", 0, null, false);
    record Counts(int total, Map<String, Integer> counts) { }
    record Stack(String location, String internalName, String material, int amount, String uuid) { }
    record Contents(Counts inventory, Counts warehouse, Counts combined, List<Stack> stacked) { }
    record CacheStats(long hits, long decodes, int entries, long retainedCharacters) { }

    private final Set<String> tracked;
    private final Function<String, Item> inspect;
    private final int maxEntries;
    private final long maxCharacters;
    private final LinkedHashMap<String, Item> cache = new LinkedHashMap<>(128, 0.75f, true);
    private long hits, decodes, characters;

    MaterialHistoryFastScan(Set<String> tracked, Function<String, Item> inspect) {
        this(tracked, inspect, 4096, 4L * 1024 * 1024);
    }
    MaterialHistoryFastScan(Set<String> tracked, Function<String, Item> inspect, int maxEntries, long maxCharacters) {
        this.tracked = Set.copyOf(tracked); this.inspect = inspect;
        this.maxEntries = maxEntries; this.maxCharacters = maxCharacters;
    }
    CacheStats cacheStats() { return new CacheStats(hits, decodes, cache.size(), characters); }

    Contents scan(Map<String, ?> backup) {
        MutableCounts inventory = new MutableCounts(), warehouse = new MutableCounts(), combined = new MutableCounts();
        List<Stack> stacked = new ArrayList<>();
        visitSlots(backup, (location, serialized) -> {
            Item item = item(serialized);
            if (item.amount <= 0 || "air".equals(item.material)) return;
            boolean phase = MaterialHistoryScanRules.phaseBookMarkedMythic(item.internalName, item.phaseBookMarked);
            // Quantity statistics do not require an identity: legacy white items and consumables still count.
            boolean trackedType = tracked.contains(item.internalName) || AdvancedItemCatalog.isAdvanced(item.internalName);
            if (trackedType || phase) {
                MutableCounts target = location.startsWith("warehouse[") ? warehouse : inventory;
                target.add(item, trackedType, phase);
                combined.add(item, trackedType, phase);
            }
            if (item.amount > 1 && item.uuid != null && !item.uuid.isBlank())
                stacked.add(new Stack(location, item.internalName, item.material, item.amount, item.uuid));
        });
        return new Contents(inventory.freeze(), warehouse.freeze(), combined.freeze(), List.copyOf(stacked));
    }

    private Item item(String encoded) {
        Item value = cache.get(encoded);
        if (value != null) { hits++; return value; }
        decodes++;
        value = inspect.apply(encoded);
        if (value == null) value = EMPTY;
        if (maxEntries > 0 && encoded.length() <= maxCharacters) {
            while (!cache.isEmpty() && (cache.size() >= maxEntries || characters + encoded.length() > maxCharacters)) {
                var oldest = cache.entrySet().iterator();
                characters -= oldest.next().getKey().length();
                oldest.remove();
            }
            cache.put(encoded, value);
            characters += encoded.length();
        }
        return value;
    }

    private static final class MutableCounts {
        int total;
        final Map<String, Integer> counts = new HashMap<>();
        void add(Item item, boolean tracked, boolean phase) {
            total = Math.addExact(total, item.amount);
            if (tracked) counts.merge(item.internalName, item.amount, Math::addExact);
            if (phase) counts.merge(PHASE_BOOK_MARKED_MYTHIC, item.amount, Math::addExact);
        }
        Counts freeze() { return new Counts(total, Map.copyOf(counts)); }
    }

    static void visitSlots(Map<String, ?> backup, BiConsumer<String, String> visitor) {
        Object rawInventory = backup.get("inv");
        if (rawInventory != null) {
            Object encoded = rawInventory instanceof Map<?, ?> map ? map.get("inv") : rawInventory;
            if (encoded != null) {
                String text = text(encoded);
                if (!absent(text)) {
                    int split = text.indexOf('|');
                    if (split < 0 || text.indexOf('|', split + 1) >= 0)
                        throw new IllegalArgumentException("历史背包格式错误：缺少或重复的分隔符");
                    String armor = text.substring(0, split);
                    boolean[] explicitArmor = new boolean[4];
                    int start = 0, slot = 0;
                    while (start <= armor.length()) {
                        int end = armor.indexOf(';', start);
                        if (end < 0) end = armor.length();
                        if (slot < 4 && end > start) {
                            explicitArmor[slot] = true;
                            visitor.accept("armor[" + slot + "]", armor.substring(start, end));
                        }
                        slot++;
                        if (end == armor.length()) break;
                        start = end + 1;
                    }
                    indexed(text.substring(split + 1), "inventory", explicitArmor, visitor);
                }
            } else if (rawInventory instanceof Map<?, ?>) {
                throw new IllegalArgumentException("历史背包缺少 inv 字段");
            }
        }
        Object rawChests = backup.get("chest");
        if (rawChests == null) return;
        if (!(rawChests instanceof List<?> chests)) throw new IllegalArgumentException("历史仓库不是数组");
        for (int index = 0; index < chests.size(); index++) {
            Object rawChest = chests.get(index);
            if (rawChest == null) continue;
            if (!(rawChest instanceof Map<?, ?> chest) || !chest.containsKey("content"))
                throw new IllegalArgumentException("历史仓库缺少 content 字段");
            Object content = chest.get("content");
            if (content != null) indexed(text(content), "warehouse[" + index + "]", null, visitor);
        }
    }

    private static void indexed(String text, String container, boolean[] explicitArmor, BiConsumer<String, String> visitor) {
        if (absent(text)) return;
        // Last value for a slot wins, just as when restoring a serialized inventory. Sparse slots are supported.
        Map<Integer, String> slots = new LinkedHashMap<>();
        int start = 0;
        while (start < text.length()) {
            int end = text.indexOf(';', start);
            if (end < 0) end = text.length();
            if (end > start) {
                int hash = text.indexOf('#', start);
                if (hash < 0 || hash >= end) throw new IllegalArgumentException("历史槽位缺少 # 分隔符");
                int slot = Integer.parseInt(text.substring(start, hash));
                if (slot < 0 || slot > 255) throw new IllegalArgumentException("历史槽位越界: " + slot);
                slots.put(slot, text.substring(hash + 1, end));
            }
            start = end + 1;
        }
        slots.forEach((slot, encoded) -> {
            if (encoded.isEmpty()) return;
            // Prefer the separately serialized armor, but retain legacy backups that only have contents[36..39].
            if (explicitArmor != null && slot >= 36 && slot <= 39) {
                if (!explicitArmor[slot - 36]) visitor.accept("armor[" + (slot - 36) + "]", encoded);
            } else if (explicitArmor == null || slot < 36 || slot == 40) {
                visitor.accept(container + "[" + slot + "]", encoded);
            }
        });
    }
    private static String text(Object value) {
        if (!(value instanceof String text)) throw new IllegalArgumentException("历史物品容器不是字符串");
        return text;
    }
    private static boolean absent(String text) {
        return text == null || text.isEmpty() || "unset".equals(text) || "null".equals(text) || "'null'".equals(text);
    }
}
