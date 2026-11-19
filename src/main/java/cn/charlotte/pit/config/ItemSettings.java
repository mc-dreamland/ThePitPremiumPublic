package cn.charlotte.pit.config;

import cn.charlotte.pit.util.item.AdvancedItemCatalog;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemSettings {
    private record Entry(Boolean uuid, String displayName) {}

    private static volatile Map<String, Entry> entries = Map.of();
    private static final Map<String, Boolean> typeDefaults = new ConcurrentHashMap<>();

    private ItemSettings() {}

    public static void load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "Items.yml");
        if (!file.exists()) plugin.saveResource("Items.yml", false);
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new IllegalStateException("无法读取 " + file, e);
        }
        configure(yaml);
    }

    static void configure(ConfigurationSection yaml) {
        Map<String, Entry> settings = new HashMap<>();
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection item = yaml.getConfigurationSection(id);
            if (item == null || (item.contains("uuid") && !item.isBoolean("uuid"))
                    || (item.contains("displayName") && !item.isString("displayName"))) {
                throw new IllegalArgumentException("Items.yml 中 " + id + " 需要布尔值 uuid 和字符串 displayName");
            }
            String name = item.getString("displayName");
            Entry entry = new Entry(item.isBoolean("uuid") ? item.getBoolean("uuid") : null,
                    name == null || name.isBlank() ? null : name);
            if (settings.put(id.toLowerCase(Locale.ROOT), entry) != null) {
                throw new IllegalArgumentException("Items.yml 中的 InternalName 重复：" + id);
            }
        }
        entries = Map.copyOf(settings);
    }

    public static boolean uuidEnabled(String id) {
        if (id == null) return false;
        String key = id.toLowerCase(Locale.ROOT);
        Entry entry = entries.get(key);
        if (entry != null && entry.uuid() != null) return entry.uuid();
        Boolean typeDefault = typeDefaults.get(key);
        return typeDefault != null ? typeDefault : defaultUuidEnabled(id);
    }

    public static void registerUuidDefault(String id, boolean enabled) {
        typeDefaults.put(id.toLowerCase(Locale.ROOT), enabled);
    }

    public static boolean defaultUuidEnabled(String id) {
        return AdvancedItemCatalog.isAdvanced(id) || "mini_cake".equalsIgnoreCase(id) || "ham".equalsIgnoreCase(id);
    }

    public static String displayName(String id, String fallback) {
        Entry entry = id == null ? null : entries.get(id.toLowerCase(Locale.ROOT));
        return entry != null && entry.displayName() != null ? entry.displayName() : fallback;
    }
}
