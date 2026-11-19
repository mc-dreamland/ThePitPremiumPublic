package cn.charlotte.pit.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrainingCombatRulesTest {
    @Test
    void playersMayHitTrainingBotsRegardlessOfCurrentOrLaunchSide() {
        for (boolean sourceInside : new boolean[]{false, true}) {
            for (Boolean launchInside : new Boolean[]{null, false, true}) {
                assertFalse(TrainingCombatRules.blocked(sourceInside, true, true, false, false, true, launchInside));
            }
        }
    }

    @Test
    void playersCanFightInsideButCannotCrossTheBoundaryIncludingArrowsAndMovingShooters() {
        for (boolean sourceInside : new boolean[]{false, true}) {
            for (boolean targetInside : new boolean[]{false, true}) {
                for (Boolean launchInside : new Boolean[]{null, false, true}) {
                    boolean expected = sourceInside != targetInside
                            || (launchInside != null && launchInside != targetInside);
                    assertEquals(expected, TrainingCombatRules.blocked(sourceInside, targetInside,
                            true, true, false, false, launchInside));
                }
            }
        }
    }

    @Test
    void trainingPvpUsesTenPercentAndOrdinaryPvpIsUnchanged() {
        assertEquals(.1, TrainingCombatRules.playerDamageMultiplier(true, true));
        assertEquals(1, TrainingCombatRules.playerDamageMultiplier(false, false));
        assertEquals(0, TrainingCombatRules.playerDamageMultiplier(true, false));
        assertEquals(0, TrainingCombatRules.playerDamageMultiplier(false, true));
        for (double damage : new double[]{0, .05, 1, 10, 2000}) {
            assertEquals(damage / 10, damage * TrainingCombatRules.playerDamageMultiplier(true, true), 1e-9);
        }
    }

    @Test
    void ordinaryAndDirectTrueDamageAreEachReducedOnceNotTwice() {
        double multiplier = TrainingCombatRules.playerDamageMultiplier(true, true);
        double ordinary = 10 * multiplier;
        double directTrueDamage = 4 * multiplier;
        double reflectedTrueDamage = 4 * .5 * multiplier;
        assertEquals(1.4, ordinary + directTrueDamage, 1e-9);
        assertEquals(.2, reflectedTrueDamage, 1e-9);
    }

    @Test
    void botsCannotAttackPlayersAcrossTheBoundaryInEitherDirection() {
        assertTrue(TrainingCombatRules.blocked(true, false, false, true, true, false, null));
        assertTrue(TrainingCombatRules.blocked(false, true, false, true, true, false, null));
        assertFalse(TrainingCombatRules.blocked(true, true, false, true, true, false, null));
        assertTrue(TrainingCombatRules.blocked(false, true, false, true, true, false, false));
    }

    @Test
    void unrelatedCreaturesAndPlayerAttacksOnWildBotsDoNotGainABoundaryBypass() {
        assertTrue(TrainingCombatRules.blocked(false, true, false, false, false, true, null));
        assertTrue(TrainingCombatRules.blocked(false, true, true, false, false, false, false));
        assertTrue(TrainingCombatRules.blocked(true, false, true, false, false, true, null));
        assertFalse(TrainingCombatRules.blocked(false, false, true, false, false, true, false));
    }
}
