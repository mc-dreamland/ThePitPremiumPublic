package cn.charlotte.pit.bot;

import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.parm.type.BowOnly;
import cn.charlotte.pit.parm.type.ThrowOnly;
import cn.charlotte.pit.perk.AbstractHandle;
import cn.charlotte.pit.perk.BotCombatEffect;
import cn.charlotte.pit.perk.MegaStreak;
import cn.charlotte.pit.perk.type.boosts.FirstStrikePerk;
import cn.charlotte.pit.perk.type.boosts.ReconPerk;
import cn.charlotte.pit.perk.type.boosts.based.GoldMinerPerk;
import cn.charlotte.pit.perk.type.boosts.based.VampirePerk;
import cn.charlotte.pit.perk.type.passives.BowPassivePerk;
import cn.charlotte.pit.perk.type.passives.DmgReducePassivePerk;
import cn.charlotte.pit.perk.type.passives.MeleePassivePerk;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class BotCombatEffectsTest {
    private static final Class<?>[] ARGS = {int.class, Player.class, Entity.class, double.class,
            AtomicDouble.class, AtomicDouble.class, AtomicBoolean.class};

    @Test
    void ordinaryAttackAndDefenceHooksAreDispatchedIndependentlyWithoutKillHooks() {
        Perk perk = new Perk();
        AtomicDouble multiplier = new AtomicDouble(1);
        assertTrue(attack(perk, 0, false, false, multiplier));
        assertEquals(1.4, multiplier.get(), 1e-9);
        assertTrue(attack(perk, 0, true, true, multiplier));
        assertEquals(1.6, multiplier.get(), 1e-9);
        assertTrue(BotCombatEffects.damaged(perk, 0, null, null, 5, new AtomicDouble(), multiplier, new AtomicBoolean()));
        assertEquals(1.5, multiplier.get(), 1e-9);
        assertEquals(1, perk.attacks);
        assertEquals(1, perk.shots);
        assertEquals(1, perk.defences);
    }

    @Test
    void megaStreakAttackAndDefenceAreRejectedEvenWhenExplicitlyAnnotated() {
        MegaPerk perk = new MegaPerk();
        assertFalse(attack(perk, 1, false, false, new AtomicDouble(1)));
        assertFalse(attack(perk, 1, true, true, new AtomicDouble(1)));
        assertFalse(BotCombatEffects.damaged(perk, 1, null, null, 5, new AtomicDouble(), new AtomicDouble(1), new AtomicBoolean()));
        assertEquals(0, perk.attacks + perk.shots + perk.defences);
    }

    @Test
    void playerSpecificHooksRequireExplicitReviewAndNegativeLevelsAreNeverEnabled() {
        PlayerPerk perk = new PlayerPerk();
        assertFalse(attack(perk, 0, false, false, new AtomicDouble(1)));
        assertTrue(attack(perk, 0, true, true, new AtomicDouble(1)));
        assertFalse(attack(perk, -1, true, true, new AtomicDouble(1)));
        assertEquals(0, perk.attacks);
        assertEquals(1, perk.shots);
    }

    @Test
    void bowAndThrowRestrictionsStillApply() {
        BowPerk bow = new BowPerk();
        assertFalse(attack(bow, 0, true, false, new AtomicDouble(1)));
        assertTrue(attack(bow, 0, true, true, new AtomicDouble(1)));
        ThrowPerk thrown = new ThrowPerk();
        assertFalse(attack(thrown, 0, true, true, new AtomicDouble(1)));
        assertTrue(attack(thrown, 0, true, false, new AtomicDouble(1)));
        assertEquals(1, bow.shots);
        assertEquals(1, thrown.shots);
    }

    @Test
    void ordinaryBoostsAreAppliedAfterBotScalingInsteadOfBeingLostToTheRawHitCap() {
        assertEquals(7, BotCombatEffects.damage(5, 1.4, 0), 1e-9);
        assertEquals(8.2, BotCombatEffects.damage(10, .82, 0), 1e-9);
        assertEquals(1, BotCombatEffects.damage(5, -1, 0), 1e-9);
        assertEquals(0, BotCombatEffects.damage(0, 1.4, 0));
        assertEquals(0, BotCombatEffects.damage(Double.NaN, 1, 0));
        assertEquals(0, BotCombatEffects.damage(5, Double.POSITIVE_INFINITY, 0));
    }

    @Test
    void reviewedRealPlayerOnlyPerksSupportBotTargetsWithoutRelaxingPvpAnnotations() throws Exception {
        for (Class<?> type : List.of(VampirePerk.class, FirstStrikePerk.class, ReconPerk.class,
                GoldMinerPerk.class, BowPassivePerk.class)) assertReviewed(type, "handleShootEntity");
        for (Class<?> type : List.of(FirstStrikePerk.class, MeleePassivePerk.class)) assertReviewed(type, "handleAttackEntity");
        assertReviewed(DmgReducePassivePerk.class, "handlePlayerDamaged");
    }

    private static void assertReviewed(Class<?> type, String method) throws Exception {
        assertTrue(type.getMethod(method, ARGS).isAnnotationPresent(BotCombatEffect.class), type.getName());
        assertTrue(type.getMethod(method, ARGS).isAnnotationPresent(PlayerOnly.class), type.getName());
    }

    private static boolean attack(AbstractHandle perk, int level, boolean projectile, boolean arrow, AtomicDouble multiplier) {
        return BotCombatEffects.attack(perk, level, null, null, projectile, arrow, 5,
                new AtomicDouble(), multiplier, new AtomicBoolean());
    }

    static class Perk extends AbstractHandle {
        int attacks, shots, defences;
        @Override public void handleAttackEntity(int level, Player player, Entity target, double damage,
                                                  AtomicDouble extra, AtomicDouble multiplier, AtomicBoolean cancel) {
            attacks++;
            multiplier.addAndGet(.4);
        }
        @Override public void handleShootEntity(int level, Player player, Entity target, double damage,
                                                 AtomicDouble extra, AtomicDouble multiplier, AtomicBoolean cancel) {
            shots++;
            multiplier.addAndGet(.2);
        }
        @Override public void handlePlayerDamaged(int level, Player player, Entity target, double damage,
                                                   AtomicDouble extra, AtomicDouble multiplier, AtomicBoolean cancel) {
            defences++;
            multiplier.addAndGet(-.1);
        }
    }

    static class PlayerPerk extends Perk {
        @Override @PlayerOnly public void handleAttackEntity(int level, Player player, Entity target, double damage,
                                                              AtomicDouble extra, AtomicDouble multiplier, AtomicBoolean cancel) {
            super.handleAttackEntity(level, player, target, damage, extra, multiplier, cancel);
        }
        @Override @PlayerOnly @BotCombatEffect public void handleShootEntity(int level, Player player, Entity target, double damage,
                                                                             AtomicDouble extra, AtomicDouble multiplier, AtomicBoolean cancel) {
            super.handleShootEntity(level, player, target, damage, extra, multiplier, cancel);
        }
    }

    static class BowPerk extends Perk {
        @Override @BowOnly public void handleShootEntity(int level, Player player, Entity target, double damage,
                                                          AtomicDouble extra, AtomicDouble multiplier, AtomicBoolean cancel) {
            super.handleShootEntity(level, player, target, damage, extra, multiplier, cancel);
        }
    }

    static class ThrowPerk extends Perk {
        @Override @ThrowOnly public void handleShootEntity(int level, Player player, Entity target, double damage,
                                                            AtomicDouble extra, AtomicDouble multiplier, AtomicBoolean cancel) {
            super.handleShootEntity(level, player, target, damage, extra, multiplier, cancel);
        }
    }

    static class MegaPerk extends PlayerPerk implements MegaStreak {
        @Override public int getStreakNeed() { return 50; }
        @Override public List<String> getDescriptionInRenownShop(Player player) { return List.of(); }
    }
}
