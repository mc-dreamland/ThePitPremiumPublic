package cn.charlotte.pit.bot;

/** Per-recipient reward decay; never changes the shared bot or another player's share. */
public record BotPrestigeRewards(boolean enabled, int startPrestige, double reductionPerPrestige, double minimumMultiplier) {
    public BotPrestigeRewards {
        startPrestige = Math.max(0, startPrestige);
        reductionPerPrestige = Double.isFinite(reductionPerPrestige) ? Math.clamp(reductionPerPrestige, 0, 1) : .05;
        minimumMultiplier = Double.isFinite(minimumMultiplier) ? Math.clamp(minimumMultiplier, 0, 1) : .10;
    }

    public double multiplier(int prestige) {
        if (!enabled) return 1;
        long excess = Math.max(0L, (long) prestige - startPrestige);
        return Math.max(minimumMultiplier, 1 - excess * reductionPerPrestige);
    }
}
