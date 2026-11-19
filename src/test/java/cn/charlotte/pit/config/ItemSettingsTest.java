package cn.charlotte.pit.config;

import cn.charlotte.pit.item.type.BrokenSoul;
import cn.charlotte.pit.item.type.Fish_1;
import cn.charlotte.pit.item.type.Fish_3;
import cn.charlotte.pit.util.item.AdvancedItemCatalog;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ItemSettingsTest {
    @AfterEach
    void resetSettings() {
        ItemSettings.configure(new YamlConfiguration());
    }

    @Test
    void bundledDefaultsCoverAdvancedItemsAndPreserveSharedIdVariants() {
        var resource = Objects.requireNonNull(getClass().getResourceAsStream("/Items.yml"));
        var yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
        ItemSettings.configure(yaml);
        AdvancedItemCatalog.items().keySet().forEach(id -> assertEquals(true, yaml.get(id + ".uuid"), id));
        for (String id : yaml.getKeys(false)) {
            assertTrue(yaml.isBoolean(id + ".uuid"), id);
            assertTrue(yaml.isString(id + ".displayName"), id);
        }
        assertEquals("&c小热带鱼", new Fish_1().getItemDisplayName());
        assertEquals("&c大热带鱼", new Fish_3().getItemDisplayName());
        assertTrue(new BrokenSoul().isUuidEnabled());
        assertFalse(new Fish_1().isUuidEnabled());
    }

    @Test
    void eitherSettingCanBeOverriddenIndependently() throws Exception {
        var yaml = new YamlConfiguration();
        yaml.loadFromString("FISHING_FISH_IRONY: {uuid: true, displayName: '&b新鱼'}\nbroken_soul: {uuid: false}");
        ItemSettings.configure(yaml);
        assertTrue(new Fish_1().isUuidEnabled());
        assertEquals("&b新鱼", new Fish_3().getItemDisplayName());
        assertFalse(new BrokenSoul().isUuidEnabled());
        assertEquals("&b断裂的灵魂", new BrokenSoul().getItemDisplayName());
        assertEquals("原名", ItemSettings.displayName("new_item", "原名"));
    }

    @Test
    void registeredTypeDefaultAppliesUntilAnAdministratorOverridesIt() {
        String id = "new_test_equipment";
        ItemSettings.registerUuidDefault(id, true);
        var yaml = new YamlConfiguration();
        yaml.set(id + ".displayName", "新装备");
        ItemSettings.configure(yaml);
        assertTrue(ItemSettings.uuidEnabled(id));
        yaml.set(id + ".uuid", false);
        ItemSettings.configure(yaml);
        assertFalse(ItemSettings.uuidEnabled(id));
    }
}
