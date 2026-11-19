package cn.charlotte.pit.bot;

/** Applied per hit, not to the shared entity's attributes (multiple players can fight it). */
public final class BotBalance {
    private BotBalance() { }

    public static double outgoing(double raw, int prestige, double cap, double perPrestige, double floor) {
        return Math.max(0, Math.min(raw, cap)) * Math.max(floor, 1 / (1 + Math.max(0, prestige) * perPrestige));
    }

    public static double incoming(double base, int prestige, double perPrestige, double ceiling) {
        return base * Math.min(ceiling, 1 + Math.max(0, prestige) * perPrestige);
    }
}
