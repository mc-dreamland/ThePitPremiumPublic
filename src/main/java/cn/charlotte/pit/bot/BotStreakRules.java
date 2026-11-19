package cn.charlotte.pit.bot;

/** Bot kills may advance an ordinary streak, but never reach any registered mega-streak threshold. */
public record BotStreakRules(boolean enabled, int maxPrestige, int maxStreak) {
    public BotStreakRules {
        maxPrestige = Math.max(0, maxPrestige);
        maxStreak = Math.max(0, maxStreak);
    }

    public int effectiveCap(int firstMegaThreshold) {
        int threshold = firstMegaThreshold > 0 ? firstMegaThreshold : 50;
        return Math.min(maxStreak, threshold - 1);
    }

    public double next(int prestige, double current, int firstMegaThreshold) {
        if (!enabled || prestige > maxPrestige || !Double.isFinite(current) || current < 0) return current;
        int cap = effectiveCap(firstMegaThreshold);
        return current >= cap ? current : Math.min(cap, current + 1);
    }

    public boolean preserveTraining(int prestige, double current, int firstMegaThreshold, boolean earnedFromBot) {
        return enabled && earnedFromBot && prestige <= maxPrestige && current > 0
                && current <= effectiveCap(firstMegaThreshold);
    }
}
