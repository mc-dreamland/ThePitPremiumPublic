package cn.charlotte.pit.bot;

import org.bukkit.event.entity.EntityDamageEvent;

/** Scale the already-calculated hit, including armor and absorption, without recalculating armor. */
final class TrainingDamageScaling {
    private TrainingDamageScaling() { }

    @SuppressWarnings({"deprecation", "removal"})
    static void scale(EntityDamageEvent event, double multiplier) {
        if (event.isCancelled() || multiplier == 1) return;
        for (EntityDamageEvent.DamageModifier modifier : EntityDamageEvent.DamageModifier.values()) {
            if (event.isApplicable(modifier)) {
                event.setDamage(modifier, event.getDamage(modifier) * multiplier);
            }
        }
    }
}
