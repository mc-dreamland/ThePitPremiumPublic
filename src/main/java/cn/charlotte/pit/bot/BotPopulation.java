package cn.charlotte.pit.bot;

/** Population is based on eligible players in each region, not total server online count. */
public final class BotPopulation {
    private BotPopulation() { }

    public static int target(int players, int minimum, int maximum, double botsPerPlayer) {
        if (players <= 0 || maximum <= 0) return 0;
        double ratio = Double.isFinite(botsPerPlayer) && botsPerPlayer >= 0 ? botsPerPlayer : 1;
        double scaled = Math.ceil(players * ratio);
        return (int) Math.min(maximum, Math.max(Math.max(0, minimum), scaled));
    }

    public static int missing(int target, int alive) {
        return Math.max(0, Math.max(0, target) - Math.max(0, alive));
    }
}
