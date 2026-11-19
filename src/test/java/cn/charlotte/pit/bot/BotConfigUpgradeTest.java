package cn.charlotte.pit.bot;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BotConfigUpgradeTest {
    @Test
    void previousConfigurationKeepsExplicitPopulationLimitsAndCoordinates() {
        Map<String, Object> config = new HashMap<>(Map.of("training.max-bots", 4, "wild.max-bots", 4,
                "training.respawn-seconds", 8, "wild.respawn-seconds", 12, "training.pos1", "existing-position"));
        var changes = BotConfigUpgrade.updates(0, config::get);
        assertFalse(changes.containsKey("training.max-bots"));
        assertFalse(changes.containsKey("wild.max-bots"));
        assertEquals(2, changes.get("training.respawn-seconds"));
        assertEquals(2, changes.get("wild.respawn-seconds"));
        assertEquals(4, changes.get("training.min-bots"));
        assertEquals(.05, changes.get("bonus-bot.chance"));
        config.putAll(changes);
        assertEquals(4, config.get("training.max-bots"));
        assertEquals(4, config.get("wild.max-bots"));
        assertEquals("existing-position", config.get("training.pos1"));
        assertTrue(BotConfigUpgrade.updates((int) config.get("config-version"), config::get).isEmpty());
    }

    @Test
    void customizedPopulationTimersAndBonusOptOutArePreserved() {
        Map<String, Object> config = Map.of("training.max-bots", 6, "wild.max-bots", 15,
                "training.respawn-seconds", 5, "wild.respawn-seconds", 6,
                "bonus-bot.enabled", false, "bonus-bot.chance", 0.0);
        var changes = BotConfigUpgrade.updates(0, config::get);
        for (String custom : config.keySet()) assertFalse(changes.containsKey(custom), custom);
        assertEquals(2.0, changes.get("bonus-bot.experience-multiplier"));
    }

    @Test
    void currentVersionIsNeverRewrittenEvenIfValuesMatchOldDefaults() {
        Map<String, Object> config = Map.of("training.max-bots", 4, "training.respawn-seconds", 8);
        assertTrue(BotConfigUpgrade.updates(BotConfigUpgrade.VERSION, config::get).isEmpty());
    }

    @Test
    void versionTwoAddsNewSettingsAndDoesNotResetCustomPopulation() {
        Map<String, Object> config = Map.of("training.max-bots", 4, "training.respawn-seconds", 8,
                "wild.pos1", "old-corner", "training.entry", "unused-entry");
        var changes = BotConfigUpgrade.updates(2, config::get);
        assertEquals(java.util.List.of(), changes.get("wild.spawn-points"));
        assertEquals(BotConfigUpgrade.VERSION, changes.get("config-version"));
        assertFalse(changes.containsKey("training.max-bots"));
        assertFalse(changes.containsKey("training.respawn-seconds"));
        assertFalse(changes.containsKey("wild.pos1"));
        assertFalse(changes.containsKey("training.entry"));
    }

    @Test
    void versionThreeMigratesLegacyThresholdsToSpawnLimitsAndDecayStartWithoutDroppingData() {
        Map<String, Object> config = new HashMap<>(Map.of("training.max-prestige", 8, "rewards.max-prestige", 12,
                "wild.spawn-points", java.util.List.of("saved-point"), "training.pos1", "existing-corner"));
        var changes = BotConfigUpgrade.updates(3, config::get);
        assertEquals(8, changes.get("training.spawn-max-prestige"));
        assertEquals(12, changes.get("wild.spawn-max-prestige"));
        assertEquals(12, changes.get("rewards.prestige-decay.start-prestige"));
        assertEquals(.05, changes.get("rewards.prestige-decay.reduction-per-prestige"));
        assertEquals(.10, changes.get("rewards.prestige-decay.minimum-multiplier"));
        assertEquals(true, changes.get("rewards.prestige-decay.enabled"));
        assertEquals(300, changes.get("admin.forced-bot-lifetime-seconds"));
        config.putAll(changes);
        assertEquals(java.util.List.of("saved-point"), config.get("wild.spawn-points"));
        assertEquals("existing-corner", config.get("training.pos1"));
        assertTrue(BotConfigUpgrade.updates((int) config.get("config-version"), config::get).isEmpty());
    }

    @Test
    void migrationPreservesExistingDecaySettingsAndOldEffectiveTrainingSpawnLimit() {
        Map<String, Object> config = Map.of("training.max-prestige", 12, "rewards.max-prestige", 5,
                "rewards.prestige-decay.enabled", false, "rewards.prestige-decay.start-prestige", 20,
                "rewards.prestige-decay.reduction-per-prestige", .1, "rewards.prestige-decay.minimum-multiplier", .2,
                "admin.forced-bot-lifetime-seconds", 600);
        var changes = BotConfigUpgrade.updates(3, config::get);
        assertEquals(5, changes.get("training.spawn-max-prestige"));
        for (String key : config.keySet()) assertFalse(changes.containsKey(key), key);
    }

    @Test
    void versionFourAddsNamesButDoesNotAddObsoleteBaseAttributesOrEquipment() {
        Map<String, Object> config = new HashMap<>(Map.of("training.health", 80.0, "wild.damage", 8.0,
                "training.pos1", "corner", "wild.spawn-points", java.util.List.of("point"),
                "rewards.prestige-decay.minimum-multiplier", .25));
        var changes = BotConfigUpgrade.updates(4, config::get);
        assertEquals("&a[Bot] 训练陪练", changes.get("training.display-name"));
        assertEquals("&6[Bot] 荒野陪练", changes.get("wild.bonus-display-name"));
        for (String kind : java.util.List.of("training", "wild")) {
            for (String field : java.util.List.of("health", "damage", "movement-speed", "armor", "armor-toughness", "knockback-resistance", "equipment.helmet")) {
                assertFalse(changes.containsKey(kind + "." + field));
            }
        }
        for (String key : config.keySet()) assertFalse(changes.containsKey(key), key);
        assertFalse(changes.containsKey("training.spawn-max-prestige"));
        config.putAll(changes);
        assertTrue(BotConfigUpgrade.updates((int) config.get("config-version"), config::get).isEmpty());
    }

    @Test
    void administratorsCustomNamesAndBaseAttributesSurviveUpgrade() {
        Map<String, Object> config = Map.of("training.display-name", "Custom Bot", "wild.bonus-display-name", "Rare Bot",
                "training.movement-speed", .15, "wild.armor", 10.0,
                "training.armor-toughness", 5.0, "wild.knockback-resistance", 1.0);
        var changes = BotConfigUpgrade.updates(4, config::get);
        for (String key : config.keySet()) assertFalse(changes.containsKey(key), key);
        assertEquals(BotConfigUpgrade.VERSION, changes.get("config-version"));
    }

    @Test
    void versionFiveAddsMissingPopulationLimitsWithoutReintroducingEquipment() {
        Map<String, Object> config = Map.of("training.health", 80.0, "wild.armor", 10.0,
                "training.display-name", "Custom Bot", "training.pos1", "existing-region");
        var changes = BotConfigUpgrade.updates(5, config::get);
        assertEquals(15, changes.get("training.max-bots"));
        assertEquals(60, changes.get("wild.max-bots"));
        assertEquals(BotConfigUpgrade.VERSION, changes.get("config-version"));
        assertEquals(BotDefaults.TRAINING_EXPERIENCE, changes.get("training.experience"));
        assertEquals(BotDefaults.WILD_COINS, changes.get("wild.coins"));
        assertFalse(changes.containsKey("training.equipment.helmet"));
        for (String key : config.keySet()) assertFalse(changes.containsKey(key), key);
    }

    @Test
    void legacyEquipmentRemainsAvailableForManualMigrationAndUpgradeIsIdempotent() {
        Map<String, Object> config = new HashMap<>(Map.of("training.equipment.helmet", "DIAMOND_HELMET",
                "wild.equipment.main-hand", "IRON_SWORD", "wild.equipment.off-hand", "SHIELD"));
        var changes = BotConfigUpgrade.updates(5, config::get);
        for (String key : config.keySet()) assertFalse(changes.containsKey(key), key);
        config.putAll(changes);
        assertEquals("DIAMOND_HELMET", config.get("training.equipment.helmet"));
        assertEquals("SHIELD", config.get("wild.equipment.off-hand"));
        assertTrue(BotConfigUpgrade.updates((int) config.get("config-version"), config::get).isEmpty());
    }

    @Test
    void versionSixKeepsLargeConfiguredLimitsRatiosAndLegacyAttributesWithoutDeletingThem() {
        Map<String, Object> config = Map.of("training.max-bots", 100, "wild.max-bots", 600,
                "wild.min-bots", 60, "wild.bots-per-player", 50.0, "training.health", 400.0,
                "training.equipment.helmet", "IRON_HELMET", "wild.spawn-points", java.util.List.of("saved-point"));
        var changes = BotConfigUpgrade.updates(6, config::get);
        for (String key : config.keySet()) assertFalse(changes.containsKey(key), key);
        assertEquals(BotConfigUpgrade.VERSION, changes.get("config-version"));
        assertEquals(BotDefaults.TRAINING_EXPERIENCE, changes.get("training.experience"));
    }

    @Test
    void suppliedServerRewardsAreReducedWithoutReplacingLocationsPopulationOrPrestigeSettings() {
        Map<String, Object> config = new HashMap<>(Map.ofEntries(
                Map.entry("training.max-bots", 15), Map.entry("wild.min-bots", 15),
                Map.entry("wild.max-bots", 60), Map.entry("wild.bots-per-player", .75),
                Map.entry("training.pos1", "server-corner-1"), Map.entry("training.pos2", "server-corner-2"),
                Map.entry("wild.spawn-points", java.util.List.of("server-point")),
                Map.entry("training.spawn-max-prestige", 15), Map.entry("wild.spawn-max-prestige", 10),
                Map.entry("rewards.prestige-decay.start-prestige", 10), Map.entry("max-player-hit-damage", 10.0),
                Map.entry("wild.experience", 15.0), Map.entry("wild.coins", 5.0),
                Map.entry("bonus-bot.chance", .05), Map.entry("bonus-bot.experience-multiplier", 5.0),
                Map.entry("bonus-bot.coins-multiplier", 5.0)));
        var changes = BotConfigUpgrade.updates(7, config::get);
        assertEquals(Map.of("training.experience", 2.0, "training.coins", 2.0,
                "wild.experience", 2.5, "wild.coins", 4.0,
                "bonus-bot.experience-multiplier", 2.0, "bonus-bot.coins-multiplier", 2.0,
                "kill-streak.enabled", true, "kill-streak.max-prestige", 10, "kill-streak.max-streak", 30,
                "config-version", BotConfigUpgrade.VERSION), changes);
        config.forEach((key, value) -> {
            if (!key.equals("wild.experience") && !key.equals("wild.coins") && !key.endsWith("-multiplier")) {
                assertFalse(changes.containsKey(key), key);
            }
        });
        config.putAll(changes);
        assertTrue(BotConfigUpgrade.updates(BotConfigUpgrade.VERSION, config::get).isEmpty());
    }

    @Test
    void previousShippedRewardDefaultsAreReducedButExplicitPopulationCapsAreNotRaised() {
        Map<String, Object> config = new HashMap<>(Map.of(
                "training.max-bots", 10, "wild.max-bots", 20,
                "training.experience", 40, "training.coins", 15,
                "wild.experience", 60, "wild.coins", 20,
                "bonus-bot.experience-multiplier", 3, "bonus-bot.coins-multiplier", 3));
        config.putAll(BotConfigUpgrade.updates(7, config::get));
        assertEquals(10, config.get("training.max-bots"));
        assertEquals(20, config.get("wild.max-bots"));
        assertEquals(2.0, config.get("training.experience"));
        assertEquals(2.0, config.get("training.coins"));
        assertEquals(2.5, config.get("wild.experience"));
        assertEquals(4.0, config.get("wild.coins"));
        assertEquals(2.0, config.get("bonus-bot.experience-multiplier"));
        assertEquals(2.0, config.get("bonus-bot.coins-multiplier"));
    }

    @Test
    void customRewardsAndZeroRewardsAreNotSilentlyReset() {
        Map<String, Object> config = Map.of(
                "training.max-bots", 0, "wild.max-bots", 0,
                "training.experience", 0.0, "training.coins", .5,
                "wild.experience", 1.0, "wild.coins", 2.0,
                "bonus-bot.experience-multiplier", 1.0, "bonus-bot.coins-multiplier", 1.5);
        assertEquals(Map.of("kill-streak.enabled", true, "kill-streak.max-prestige", 10,
                "kill-streak.max-streak", 30, "config-version", BotConfigUpgrade.VERSION), BotConfigUpgrade.updates(7, config::get));
    }

    @Test
    void versionEightOnlyAddsStreakSettingsWithoutRebalancingCurrentCustomRewards() {
        Map<String, Object> config = new HashMap<>(Map.of("training.experience", 5.0, "training.coins", 5.0,
                "wild.experience", 10, "wild.coins", 7.5, "bonus-bot.experience-multiplier", 5.0,
                "bonus-bot.coins-multiplier", 5.0, "training.respawn-seconds", 5, "wild.respawn-seconds", 15));
        config.put("training.max-bots", 10);
        config.put("wild.max-bots", 50);
        var changes = BotConfigUpgrade.updates(8, config::get);
        assertEquals(Map.of("kill-streak.enabled", true, "kill-streak.max-prestige", 10,
                "kill-streak.max-streak", 30, "config-version", BotConfigUpgrade.VERSION), changes);
        config.putAll(changes);
        assertEquals(5.0, config.get("bonus-bot.experience-multiplier"));
        assertTrue(BotConfigUpgrade.updates(BotConfigUpgrade.VERSION, config::get).isEmpty());
    }

    @Test
    void disabledOrCustomStreakPolicyIsNotOverwrittenDuringUpgrade() {
        Map<String, Object> config = Map.of("training.max-bots", 10, "wild.max-bots", 50,
                "kill-streak.enabled", false, "kill-streak.max-prestige", 3, "kill-streak.max-streak", 200);
        assertEquals(Map.of("config-version", BotConfigUpgrade.VERSION), BotConfigUpgrade.updates(8, config::get));
    }
}
