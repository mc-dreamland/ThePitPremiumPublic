package cn.charlotte.pit.bot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Migrate known reward presets / missing settings; never replace coordinates or explicit population tuning. */
final class BotConfigUpgrade {
    static final int VERSION = 9;
    private BotConfigUpgrade() { }

    static Map<String, Object> updates(int version, Function<String, Object> current) {
        Map<String, Object> updates = new LinkedHashMap<>();
        if (version >= VERSION) return updates;
        if (version < 2) {
            replaceDefault(updates, current, "training.respawn-seconds", 8, 2);
            replaceDefault(updates, current, "wild.respawn-seconds", 12, 2);
            missing(updates, current, "training.min-bots", 4);
            missing(updates, current, "training.bots-per-player", 1.0);
            missing(updates, current, "wild.min-bots", 15);
            missing(updates, current, "wild.bots-per-player", .75);
            missing(updates, current, "bonus-bot.enabled", true);
            missing(updates, current, "bonus-bot.chance", .05);
            missing(updates, current, "bonus-bot.experience-multiplier", BotDefaults.BONUS_EXPERIENCE_MULTIPLIER);
            missing(updates, current, "bonus-bot.coins-multiplier", BotDefaults.BONUS_COINS_MULTIPLIER);
        }
        if (version < 4) {
            missing(updates, current, "wild.spawn-points", List.of());
            int previousRewardLimit = prestige(current.apply("rewards.max-prestige"), 5);
            int previousTrainingLimit = prestige(current.apply("training.max-prestige"), 5);
            missing(updates, current, "training.spawn-max-prestige", Math.min(previousTrainingLimit, previousRewardLimit));
            missing(updates, current, "wild.spawn-max-prestige", previousRewardLimit);
            missing(updates, current, "rewards.prestige-decay.enabled", true);
            missing(updates, current, "rewards.prestige-decay.start-prestige", previousRewardLimit);
            missing(updates, current, "rewards.prestige-decay.reduction-per-prestige", .05);
            missing(updates, current, "rewards.prestige-decay.minimum-multiplier", .10);
            missing(updates, current, "admin.forced-bot-lifetime-seconds", 300);
        }
        for (String kind : List.of("training", "wild")) {
            String p = kind + ".";
            boolean training = kind.equals("training");
            missing(updates, current, p + "max-bots", training ? 15 : 60);
            if (version < 5) {
                missing(updates, current, p + "display-name", training ? "&a[Bot] 训练陪练" : "&e[Bot] 荒野陪练");
                missing(updates, current, p + "bonus-display-name", "&6[Bot] " + (training ? "训练陪练" : "荒野陪练"));
            }
        }
        if (version < 8) {
            // Includes the supplied server's 15/5 wild pool and 5x bonus, as well as old shipped defaults.
            // The supplied training section has no rewards: explicitly fill it instead of falling back to 40/15.
            rebalanceDefault(updates, current, "training.experience", BotDefaults.TRAINING_EXPERIENCE, 40);
            rebalanceDefault(updates, current, "training.coins", BotDefaults.TRAINING_COINS, 15);
            rebalanceDefault(updates, current, "wild.experience", BotDefaults.WILD_EXPERIENCE, 60, 15);
            rebalanceDefault(updates, current, "wild.coins", BotDefaults.WILD_COINS, 20, 5);
            rebalanceDefault(updates, current, "bonus-bot.experience-multiplier", BotDefaults.BONUS_EXPERIENCE_MULTIPLIER, 3, 5);
            rebalanceDefault(updates, current, "bonus-bot.coins-multiplier", BotDefaults.BONUS_COINS_MULTIPLIER, 3, 5);
        }
        if (version < 9) {
            missing(updates, current, "kill-streak.enabled", true);
            missing(updates, current, "kill-streak.max-prestige", 10);
            missing(updates, current, "kill-streak.max-streak", 30);
        }
        updates.put("config-version", VERSION);
        return updates;
    }

    private static int prestige(Object value, int fallback) {
        return value instanceof Number n ? Math.max(0, n.intValue()) : fallback;
    }

    private static void missing(Map<String, Object> updates, Function<String, Object> current, String path, Object value) {
        if (current.apply(path) == null) updates.put(path, value);
    }

    private static void replaceDefault(Map<String, Object> updates, Function<String, Object> current,
                                       String path, int oldValue, int newValue) {
        Object value = current.apply(path);
        if (value == null || (value instanceof Number n && n.doubleValue() == oldValue)) updates.put(path, newValue);
    }

    private static void rebalanceDefault(Map<String, Object> updates, Function<String, Object> current,
                                         String path, double replacement, double... previousValues) {
        Object value = current.apply(path);
        if (value == null) {
            updates.put(path, replacement);
        } else if (value instanceof Number n) {
            for (double previous : previousValues) {
                if (n.doubleValue() == previous) {
                    updates.put(path, replacement);
                    return;
                }
            }
        }
    }
}
