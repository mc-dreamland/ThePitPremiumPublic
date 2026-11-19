package cn.charlotte.pit.bot;

import cn.charlotte.pit.manager.ResourceManager;

/** The variant is rolled once at spawn and used by the existing damage-share reward ledger. */
public record BotReward(boolean bonus, double experienceMultiplier, double coinsMultiplier) {
    public static final BotReward NORMAL = new BotReward(false, 1, 1);
    public record Payout(double experience, double coins) { }

    public BotReward {
        experienceMultiplier = multiplier(experienceMultiplier);
        coinsMultiplier = multiplier(coinsMultiplier);
    }

    public static BotReward roll(boolean enabled, double chance, double random,
                                 double experienceMultiplier, double coinsMultiplier) {
        if (!enabled || !Double.isFinite(chance) || random < 0 || random >= Math.min(1, chance)) return NORMAL;
        if (!Double.isFinite(random)) return NORMAL;
        return new BotReward(true, experienceMultiplier, coinsMultiplier);
    }

    public Payout payout(double baseExperience, double baseCoins, double damageShare, double remainingExperience) {
        return payout(baseExperience, baseCoins, damageShare, remainingExperience, 1);
    }

    /** Apply normal player reward math/caps first; never round each contributor's share up. */
    public Payout payout(ResourceManager resources, double damageShare, double remainingExperience, double prestigeMultiplier) {
        return NORMAL.payout(resources.calculateEXP(experienceMultiplier), resources.calculateGold(coinsMultiplier),
                damageShare, remainingExperience, prestigeMultiplier);
    }

    public Payout payout(double baseExperience, double baseCoins, double damageShare, double remainingExperience, double prestigeMultiplier) {
        double share = Double.isFinite(damageShare) ? Math.clamp(damageShare, 0, 1) : 0;
        double prestige = Double.isFinite(prestigeMultiplier) ? Math.clamp(prestigeMultiplier, 0, 1) : 0;
        double xp = nonNegative(baseExperience) * share * experienceMultiplier * prestige;
        double coins = nonNegative(baseCoins) * share * coinsMultiplier * prestige;
        return new Payout(experienceWithinLevelCap(xp, remainingExperience), coins);
    }

    public static double experienceWithinLevelCap(double experience, double remainingExperience) {
        return Math.min(nonNegative(experience), nonNegative(remainingExperience));
    }

    private static double nonNegative(double value) { return Double.isFinite(value) ? Math.max(0, value) : 0; }
    private static double multiplier(double value) { return Double.isFinite(value) ? Math.clamp(value, 1, 100) : 1; }
}
