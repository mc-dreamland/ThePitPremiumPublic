package cn.charlotte.pit.bot;

import org.junit.jupiter.api.Test;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BotDefaultsConfigurationTest {
    @Test
    void editablePopulationSettingsAndAllLocationsAreBundledWithoutDuplicateYamlKeys() throws IOException {
        Map<?, ?> root = defaults();
        assertEquals(BotConfigUpgrade.VERSION, root.get("config-version"));
        assertEquals(true, root.get("enabled"));
        Map<?, ?> streaks = section(root, "kill-streak");
        assertInstanceOf(Boolean.class, streaks.get("enabled"));
        assertTrue(value(streaks, "max-prestige") >= 0);
        assertTrue(value(streaks, "max-streak") >= 0);
        Map<?, ?> training = section(root, "training");
        Map<?, ?> wild = section(root, "wild");
        // These are administrator tuning, not invariants of the conservative fallback preset.
        for (Map<?, ?> group : List.of(training, wild)) {
            for (String key : List.of("min-bots", "max-bots", "spawn-max-prestige", "bots-per-player")) {
                assertTrue(Double.isFinite(value(group, key)) && value(group, key) >= 0, key);
            }
            assertTrue(value(group, "respawn-seconds") >= 1 && value(group, "respawn-seconds") <= 3600);
        }
        assertTrue(value(root, "max-player-hit-damage") > 0);
        assertTrue(value(section(section(root, "rewards"), "prestige-decay"), "start-prestige") >= 0);
        assertEquals(300, section(root, "admin").get("forced-bot-lifetime-seconds"));
        assertLocation(section(training, "pos1"));
        assertLocation(section(training, "pos2"));
        assertEquals(section(training, "pos1").get("world"), section(training, "pos2").get("world"));
        List<?> points = assertInstanceOf(List.class, wild.get("spawn-points"));
        // Administrators may add spawn points; validate them without pinning a particular map revision.
        assertFalse(points.isEmpty());
        var uniqueBlocks = new HashSet<List<Object>>();
        for (Object entry : points) {
            Map<?, ?> point = assertInstanceOf(Map.class, entry);
            assertLocation(point);
            assertTrue(uniqueBlocks.add(List.of(point.get("world"), (int) Math.floor(value(point, "x")),
                    (int) Math.floor(value(point, "y")), (int) Math.floor(value(point, "z")))));
        }
        for (Map<?, ?> group : List.of(training, wild)) {
            for (String ignored : List.of("health", "damage", "movement-speed", "armor", "armor-toughness",
                    "knockback-resistance", "equipment", "max-prestige")) assertFalse(group.containsKey(ignored), ignored);
        }
        assertFalse(section(root, "rewards").containsKey("max-prestige"));
    }

    @Test
    void editableRewardSettingsAreValidWithoutForcingAdministratorsBackToTheFallbackPreset() throws IOException {
        Map<?, ?> root = defaults();
        Map<?, ?> bonus = section(root, "bonus-bot");
        assertInstanceOf(Boolean.class, bonus.get("enabled"));
        assertTrue(value(bonus, "chance") >= 0 && value(bonus, "chance") <= 1);
        for (String field : List.of("experience-multiplier", "coins-multiplier")) {
            assertTrue(value(bonus, field) >= 1 && value(bonus, field) <= 100);
        }
        for (String kind : List.of("training", "wild")) {
            Map<?, ?> group = section(root, kind);
            for (String field : List.of("experience", "coins")) {
                assertTrue(Double.isFinite(value(group, field)) && value(group, field) >= 0 && value(group, field) <= 10000);
            }
        }
    }

    @Test
    void conservativeFallbackPresetStillFitsTheUnboostedBasePvpBudget() {
        BotReward bonusReward = new BotReward(true, BotDefaults.BONUS_EXPERIENCE_MULTIPLIER, BotDefaults.BONUS_COINS_MULTIPLIER);
        BotPrestigeRewards scale = new BotPrestigeRewards(true, 10, .05, .1);
        for (boolean training : List.of(true, false)) {
            double xp = training ? BotDefaults.TRAINING_EXPERIENCE : BotDefaults.WILD_EXPERIENCE;
            double coins = training ? BotDefaults.TRAINING_COINS : BotDefaults.WILD_COINS;
            // PvP base is 5 XP / 10 coins, before prestige, VIP, streak or other positive bonuses.
            for (int prestige = 0; prestige <= 200; prestige++) {
                for (double share : new double[]{0, .05, .25, .5, 1}) {
                    for (BotReward reward : List.of(BotReward.NORMAL, bonusReward)) {
                        var payout = reward.payout(xp, coins, share, 10000, scale.multiplier(prestige));
                        assertTrue(payout.experience() <= 5 * share + 1e-9);
                        assertTrue(payout.coins() <= 10 * share + 1e-9);
                    }
                }
            }
            double expectedFactor = .95 + .05 * bonusReward.experienceMultiplier();
            assertEquals(training ? 2.1 : 2.625, xp * expectedFactor, 1e-9);
            assertEquals(training ? 2.1 : 4.2, coins * expectedFactor, 1e-9);
        }
    }

    private static Map<?, ?> defaults() throws IOException {
        try (InputStream in = BotDefaultsConfigurationTest.class.getResourceAsStream("/bots.yml")) {
            assertNotNull(in);
            return assertInstanceOf(Map.class, new Load(LoadSettings.builder().setAllowDuplicateKeys(false).build()).loadFromInputStream(in));
        }
    }

    private static Map<?, ?> section(Map<?, ?> root, String key) {
        return assertInstanceOf(Map.class, root.get(key), key);
    }

    private static double value(Map<?, ?> root, String key) {
        return assertInstanceOf(Number.class, root.get(key), key).doubleValue();
    }

    private static void assertLocation(Map<?, ?> point) {
        assertEquals("org.bukkit.Location", point.get("=="));
        assertFalse(assertInstanceOf(String.class, point.get("world")).isBlank());
        for (String field : List.of("x", "y", "z", "pitch", "yaw")) {
            assertTrue(Double.isFinite(value(point, field)), field);
        }
    }
}
