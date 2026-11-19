package cn.charlotte.pit.bot;

import cn.charlotte.pit.manager.KillStreakRewards;
import cn.charlotte.pit.manager.ResourceManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BotStreakRulesTest {
    private final BotStreakRules defaults = new BotStreakRules(true, 10, 30);

    @Test
    void lowPrestigeAdvancesOneKillAndThePrestigeLimitIsInclusive() {
        assertEquals(1, defaults.next(0, 0, 50));
        assertEquals(11, defaults.next(10, 10, 50));
        assertEquals(10, defaults.next(11, 10, 50));
        assertEquals(10, defaults.next(100, 10, 50));
    }

    @Test
    void capsApplyToTheCurrentTotalAndNeverReduceAnExistingPvpStreak() {
        assertEquals(30, defaults.next(0, 29, 50));
        assertEquals(30, defaults.next(0, 29.5, 50));
        assertEquals(30, defaults.next(0, 30, 50));
        assertEquals(49, defaults.next(0, 49, 50));
        assertEquals(150, defaults.next(0, 150, 50));
    }

    @Test
    void evenAnExtremeConfiguredLimitCannotReachTheFirstMegaThreshold() {
        BotStreakRules highCap = new BotStreakRules(true, 10, Integer.MAX_VALUE);
        assertEquals(49, highCap.effectiveCap(50));
        assertEquals(49, highCap.next(0, 48.9, 50));
        assertEquals(49.9, highCap.next(0, 49.9, 50));
        assertEquals(19, highCap.effectiveCap(20));
        assertEquals(19, highCap.next(0, 18, 20));
        assertEquals(49, highCap.effectiveCap(0));
    }

    @Test
    void disabledZeroCapsAndInvalidCountersDoNotAdvance() {
        assertEquals(5, new BotStreakRules(false, 10, 30).next(0, 5, 50));
        assertEquals(0, new BotStreakRules(true, 10, 0).next(0, 0, 50));
        assertEquals(-1, defaults.next(0, -1, 50));
        assertTrue(Double.isNaN(defaults.next(0, Double.NaN, 50)));
        assertEquals(Double.POSITIVE_INFINITY, defaults.next(0, Double.POSITIVE_INFINITY, 50));
    }

    @Test
    void trainingProtectionDoesNotPreservePvpOrMegaStreaksOrHighPrestigeCounters() {
        assertTrue(defaults.preserveTraining(10, 30, 50, true));
        assertFalse(defaults.preserveTraining(0, 20, 50, false));
        assertFalse(defaults.preserveTraining(11, 20, 50, true));
        assertFalse(defaults.preserveTraining(0, 50, 50, true));
        assertFalse(defaults.preserveTraining(0, 0, 50, true));
        assertFalse(defaults.preserveTraining(0, Double.NaN, 50, true));
        assertFalse(new BotStreakRules(false, 10, 30).preserveTraining(0, 20, 50, true));
    }

    @Test
    void milestonesCannotBeRepeatedByContinuingToKillBotsAtTheCap() {
        double current = 0;
        int milestones = 0, ordinaryTriggers = 0;
        for (int kills = 0; kills < 1000; kills++) {
            double next = defaults.next(0, current, 50);
            if (next > current) {
                ordinaryTriggers++;
                ResourceManager resources = new ResourceManager();
                KillStreakRewards.apply(resources, next, false, false);
                if (resources.getAdditionalExperience().containsKey("连杀奖励")) milestones++;
            }
            current = next;
        }
        assertEquals(30, current);
        assertEquals(30, ordinaryTriggers);
        assertEquals(3, milestones);
    }
}
