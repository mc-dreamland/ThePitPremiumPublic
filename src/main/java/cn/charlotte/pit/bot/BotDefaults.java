package cn.charlotte.pit.bot;

/** Conservative per-bot pools, shared by config migration and missing-value fallbacks. */
final class BotDefaults {
    static final double TRAINING_EXPERIENCE = 2.0;
    static final double TRAINING_COINS = 2.0;
    static final double WILD_EXPERIENCE = 2.5;
    static final double WILD_COINS = 4.0;
    static final double BONUS_EXPERIENCE_MULTIPLIER = 2.0;
    static final double BONUS_COINS_MULTIPLIER = 2.0;

    private BotDefaults() { }
}
