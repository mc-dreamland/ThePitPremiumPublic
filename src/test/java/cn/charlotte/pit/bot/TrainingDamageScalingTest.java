package cn.charlotte.pit.bot;

import com.google.common.base.Function;
import org.bukkit.damage.DamageSource;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"deprecation", "removal"})
class TrainingDamageScalingTest {
    @Test
    void armorResistanceMagicAndAbsorptionAreReducedWithTheFinalHitExactlyOnce() {
        EntityDamageEvent event = event(Map.of(BASE, 10.0, ARMOR, -2.0, RESISTANCE, -1.0,
                MAGIC, -1.0, ABSORPTION, -3.0));
        assertEquals(3, event.getFinalDamage(), 1e-9);
        TrainingDamageScaling.scale(event, .1);
        assertEquals(.3, event.getFinalDamage(), 1e-9);
        assertEquals(1, event.getDamage(), 1e-9);
        assertEquals(-.3, event.getDamage(ABSORPTION), 1e-9);
        assertEquals(-.2, event.getDamage(ARMOR), 1e-9);
    }

    @Test
    void blockedHitsAndFullyAbsorbedHitsStayBlockedOrHarmless() {
        EntityDamageEvent cancelled = event(Map.of(BASE, 10.0));
        cancelled.setCancelled(true);
        TrainingDamageScaling.scale(cancelled, .1);
        assertTrue(cancelled.isCancelled());
        assertEquals(10, cancelled.getDamage());

        EntityDamageEvent absorbed = event(Map.of(BASE, 10.0, ABSORPTION, -10.0));
        TrainingDamageScaling.scale(absorbed, .1);
        assertEquals(0, absorbed.getFinalDamage());
    }

    @Test
    void ordinaryCombatAndFlatSmallDamageAreNotClampedToOneOrRecalculated() {
        EntityDamageEvent outside = event(Map.of(BASE, 10.0, ARMOR, -2.0));
        TrainingDamageScaling.scale(outside, 1);
        assertEquals(8, outside.getFinalDamage());

        EntityDamageEvent small = event(Map.of(BASE, .05));
        TrainingDamageScaling.scale(small, .1);
        assertEquals(.005, small.getFinalDamage(), 1e-9);
    }

    private static EntityDamageEvent event(Map<EntityDamageEvent.DamageModifier, Double> modifiers) {
        Map<EntityDamageEvent.DamageModifier, Function<? super Double, Double>> functions = new EnumMap<>(BASE.getDeclaringClass());
        for (EntityDamageEvent.DamageModifier modifier : modifiers.keySet()) {
            functions.put(modifier, damage -> {
                throw new AssertionError("Final scaling must not recalculate vanilla reduction functions");
            });
        }
        return new EntityDamageEvent(null, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                (DamageSource) null, new EnumMap<>(modifiers), functions);
    }
}
