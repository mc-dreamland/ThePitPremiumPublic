package cn.charlotte.pit.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BotPrestigeRewardsTest {
    private final BotPrestigeRewards defaults = new BotPrestigeRewards(true, 5, .05, .10);

    @Test
    void playersThroughThresholdKeepFullRewards() {
        for (int prestige = -1; prestige <= 5; prestige++) assertEquals(1, defaults.multiplier(prestige));
    }

    @Test
    void rewardsDecayLinearlyAfterThresholdAndKeepTheFloor() {
        assertEquals(.95, defaults.multiplier(6), 1e-9);
        assertEquals(.75, defaults.multiplier(10), 1e-9);
        assertEquals(.50, defaults.multiplier(15), 1e-9);
        assertEquals(.25, defaults.multiplier(20), 1e-9);
        assertEquals(.10, defaults.multiplier(23), 1e-9);
        assertEquals(.10, defaults.multiplier(35), 1e-9);
        double previous = 1;
        for (int prestige = 0; prestige <= 200; prestige++) {
            double value = defaults.multiplier(prestige);
            assertTrue(value <= previous && value >= .1 && value <= 1);
            previous = value;
        }
    }

    @Test
    void thresholdSlopeFloorAndDisableSwitchAreConfigurable() {
        BotPrestigeRewards custom = new BotPrestigeRewards(true, 10, .10, .20);
        assertEquals(1, custom.multiplier(10));
        assertEquals(.9, custom.multiplier(11), 1e-9);
        assertEquals(.2, custom.multiplier(20), 1e-9);
        assertEquals(1, new BotPrestigeRewards(false, 5, .05, .10).multiplier(35));
        assertEquals(1, new BotPrestigeRewards(true, 5, 0, .1).multiplier(35));
    }

    @Test
    void invalidConfigAndExtremePrestigesCannotProduceBoostedOrNegativeRewards() {
        BotPrestigeRewards invalid = new BotPrestigeRewards(true, -100, Double.NaN, Double.POSITIVE_INFINITY);
        assertEquals(0, invalid.startPrestige());
        assertEquals(.05, invalid.reductionPerPrestige());
        assertEquals(.10, invalid.minimumMultiplier());
        assertEquals(.1, invalid.multiplier(Integer.MAX_VALUE));
        assertEquals(1, invalid.multiplier(Integer.MIN_VALUE));
        assertEquals(1, new BotPrestigeRewards(true, 5, -1, 100).multiplier(35));
        assertEquals(0, new BotPrestigeRewards(true, 0, 100, -1).multiplier(1));
    }
}
