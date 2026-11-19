package cn.charlotte.pit.bot;

import cn.charlotte.pit.manager.KillStreakRewards;
import cn.charlotte.pit.manager.ResourceManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KillStreakRewardsTest {
    @Test
    void ordinaryStreakRewardsMatchThePreviousPlayerFormula() {
        assertReward(1, false, false, 4, 4);
        assertReward(3, false, false, 7, 5);
        assertReward(10, false, false, 8, 5);
        assertReward(20, false, false, 12, 6);
        assertReward(30, false, false, 18, 9);
        assertReward(30, true, false, 39, 9);
    }

    @Test
    void pvpStreakerCapAndHighlanderGoldStillWorkAboveBotLimits() {
        ResourceManager pool = new ResourceManager();
        KillStreakRewards.apply(pool, 100, false, true);
        assertEquals(30.0, pool.getAdditionalExperience().get("连杀"));
        assertEquals(15.0, pool.getAdditionalGold().get("连杀"));
        ResourceManager streaker = new ResourceManager();
        KillStreakRewards.apply(streaker, 1000, true, false);
        assertEquals(180.0, streaker.getAdditionalExperience().get("连杀(嗜血者加成)"));
        assertEquals(100.0, streaker.getExpAdditions().get("嗜血者基础连杀经验"));
    }

    private static void assertReward(double streak, boolean streaker, boolean highlander, double xp, double coins) {
        ResourceManager pool = new ResourceManager();
        KillStreakRewards.apply(pool, streak, streaker, highlander);
        assertEquals(xp, pool.calculateEXP(), 1e-9);
        assertEquals(coins, pool.calculateGold(), 1e-9);
    }
}
