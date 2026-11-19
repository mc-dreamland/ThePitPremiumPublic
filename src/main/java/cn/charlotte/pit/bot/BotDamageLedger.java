package cn.charlotte.pit.bot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Reward pool is split by actual damage, never by last hit or number of participants. */
public final class BotDamageLedger {
    private record Contribution(double damage, long lastHit) { }
    private final Map<UUID, Contribution> damage = new HashMap<>();

    public void record(UUID player, double finalDamage, double remainingHealth, long now) {
        double credit = Math.max(0, Math.min(finalDamage, remainingHealth));
        if (!Double.isFinite(credit) || credit <= 0) return;
        Contribution old = damage.get(player);
        damage.put(player, new Contribution(credit + (old == null ? 0 : old.damage), now));
    }

    public Map<UUID, Double> shares(double maxHealth, long now, long timeoutMillis) {
        double total = damage.values().stream().mapToDouble(Contribution::damage).sum();
        double denominator = Math.max(maxHealth, total);
        Map<UUID, Double> shares = new HashMap<>();
        if (denominator <= 0) return shares;
        damage.forEach((player, value) -> {
            if (now - value.lastHit <= timeoutMillis) shares.put(player, value.damage / denominator);
        });
        return shares;
    }
}
