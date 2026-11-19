package cn.charlotte.pit.bot;

import cn.charlotte.pit.manager.ResourceManager;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BotRewardTest {
    @Test
    void bonusRollHasExactProbabilityBoundaryAndCanBeDisabled() {
        assertTrue(BotReward.roll(true, .05, .049999, 3, 3).bonus());
        assertFalse(BotReward.roll(true, .05, .05, 3, 3).bonus());
        assertFalse(BotReward.roll(true, 0, 0, 3, 3).bonus());
        assertFalse(BotReward.roll(false, 1, 0, 3, 3).bonus());
        assertTrue(BotReward.roll(true, 1, .999999, 3, 3).bonus());
        assertFalse(BotReward.roll(true, 1, 1, 3, 3).bonus());
    }

    @Test
    void invalidRollInputsCannotCreateBonusBots() {
        assertFalse(BotReward.roll(true, Double.NaN, 0, 3, 3).bonus());
        assertFalse(BotReward.roll(true, .05, Double.NaN, 3, 3).bonus());
        assertFalse(BotReward.roll(true, .05, -.01, 3, 3).bonus());
    }

    @Test
    void ordinaryBotsKeepBaseRewardsAndBonusBotsMultiplyBothPools() {
        assertEquals(new BotReward.Payout(40, 15), BotReward.NORMAL.payout(40, 15, 1, 1000));
        BotReward bonus = BotReward.roll(true, 1, .5, 3, 3);
        assertEquals(new BotReward.Payout(120, 45), bonus.payout(40, 15, 1, 1000));
        assertEquals(new BotReward.Payout(30, 11.25), bonus.payout(40, 15, .25, 1000));
    }

    @Test
    void experienceCeilingIsAppliedAfterMultiplierWithoutClippingCoins() {
        BotReward bonus = new BotReward(true, 3, 5);
        assertEquals(new BotReward.Payout(50, 75), bonus.payout(40, 15, 1, 50));
        assertEquals(new BotReward.Payout(0, 75), bonus.payout(40, 15, 1, 0));
    }

    @Test
    void bonusDamageSharingStillCannotExceedOneRewardPoolOrRewardOverkill() {
        BotDamageLedger ledger = new BotDamageLedger();
        UUID first = UUID.randomUUID();
        UUID last = UUID.randomUUID();
        ledger.record(first, 30, 40, 1000);
        ledger.record(last, 10000, 10, 1000);
        BotReward bonus = new BotReward(true, 3, 3);
        var shares = ledger.shares(40, 1001, 30000);
        var a = bonus.payout(40, 15, shares.get(first), 1000);
        var b = bonus.payout(40, 15, shares.get(last), 1000);
        assertEquals(120, a.experience() + b.experience(), 1e-9);
        assertEquals(45, a.coins() + b.coins(), 1e-9);
        assertEquals(30, b.experience(), 1e-9);
    }

    @Test
    void invalidRewardValuesAreBoundedAndCannotMintNegativeOrNanBalances() {
        BotReward bonus = new BotReward(true, Double.NaN, 10000);
        assertEquals(1, bonus.experienceMultiplier());
        assertEquals(100, bonus.coinsMultiplier());
        assertEquals(new BotReward.Payout(0, 0), bonus.payout(40, 15, Double.NaN, 1000));
        assertEquals(new BotReward.Payout(0, 0), bonus.payout(40, 15, -1, 1000));
    }

    @Test
    void highPrestigeKeepsPositiveDecayedRewardForOrdinaryAndBonusBots() {
        double factor = new BotPrestigeRewards(true, 5, .05, .10).multiplier(35);
        var ordinary = BotReward.NORMAL.payout(40, 15, 1, 1000, factor);
        var bonus = new BotReward(true, 3, 3).payout(40, 15, 1, 1000, factor);
        assertEquals(4, ordinary.experience(), 1e-9);
        assertEquals(1.5, ordinary.coins(), 1e-9);
        assertEquals(12, bonus.experience(), 1e-9);
        assertEquals(4.5, bonus.coins(), 1e-9);
    }

    @Test
    void decayHappensBeforeExperienceCeilingAndNeverReducesAnotherPlayersShare() {
        BotPrestigeRewards scale = new BotPrestigeRewards(true, 5, .05, .10);
        BotReward bonus = new BotReward(true, 3, 3);
        var solo = bonus.payout(40, 15, 1, 50, scale.multiplier(20));
        assertEquals(30, solo.experience(), 1e-9); // min(120 * 25%, 50), not min(120, 50) * 25%.
        var low = bonus.payout(40, 15, .5, 1000, scale.multiplier(0));
        var high = bonus.payout(40, 15, .5, 1000, scale.multiplier(35));
        assertEquals(60, low.experience(), 1e-9);
        assertEquals(6, high.experience(), 1e-9);
        assertEquals(22.5, low.coins(), 1e-9);
        assertEquals(2.25, high.coins(), 1e-9);
    }

    @Test
    void invalidPrestigeMultiplierCannotMintCurrency() {
        assertEquals(new BotReward.Payout(0, 0), BotReward.NORMAL.payout(40, 15, 1, 1000, Double.NaN));
        assertEquals(new BotReward.Payout(0, 0), BotReward.NORMAL.payout(40, 15, 1, 1000, -1));
        assertEquals(new BotReward.Payout(40, 15), BotReward.NORMAL.payout(40, 15, 1, 1000, 100));
    }

    @Test
    void sharedPlayerFormulaAppliesAdditiveBonusesFlatRewardsAndCelebrityBeforeDamageSharing() {
        ResourceManager pool = new ResourceManager();
        pool.addBaseExp(2, "bot");
        pool.addBaseGold(2, "bot");
        pool.addExpModifier(1.2, "VIP");
        pool.addExpModifier(1.3, "perk");
        pool.addGoldModifier(1.5, "perk");
        pool.addExp(2, "renown");
        pool.addGold(4, "perk");
        pool.setHasCelebrityPerk(true);
        var payout = new BotReward(true, 2, 2).payout(pool, .25, 1000, .1);
        assertEquals(.25, payout.experience(), 1e-9); // ((2 * 1.5 + 2) * 2) * .25 * .1
        assertEquals(.7, payout.coins(), 1e-9); // ((2 * 1.5 + 4) * celebrity 2 * bonus 2) * .25 * .1
    }

    @Test
    void normalPlayerExperienceRoundingHappensAfterBonusButBeforeSharing() {
        ResourceManager pool = new ResourceManager();
        pool.addBaseExp(2.5, "wild");
        pool.addBaseGold(4, "wild");
        assertEquals(new BotReward.Payout(3, 4), BotReward.NORMAL.payout(pool, 1, 1000, 1));
        BotReward bonus = new BotReward(true, 2, 2);
        assertEquals(new BotReward.Payout(5, 8), bonus.payout(pool, 1, 1000, 1));
        var first = bonus.payout(pool, .4, 1000, 1);
        var second = bonus.payout(pool, .6, 1000, 1);
        assertEquals(5, first.experience() + second.experience(), 1e-9);
        assertEquals(8, first.coins() + second.coins(), 1e-9);
    }

    @Test
    void bonusAndPerksCannotBypassPlayerRewardCapsAndLevelCeilingIsStillLast() {
        ResourceManager pool = new ResourceManager();
        pool.addBaseExp(10000, "boosted");
        pool.addBaseGold(10000, "boosted");
        BotReward bonus = new BotReward(true, 100, 100);
        assertEquals(new BotReward.Payout(400, 600), bonus.payout(pool, 1, 10000, 1));
        assertEquals(new BotReward.Payout(3, 30), bonus.payout(pool, .5, 3, .1));
        assertEquals(new BotReward.Payout(0, 600), bonus.payout(pool, 1, 0, 1));
    }

    @Test
    void conglomerateAndCurfewPenaltiesStayMultiplicativeWithTheSharedRewardMath() {
        ResourceManager pool = new ResourceManager();
        pool.addBaseExp(2, "bot");
        pool.addBaseGold(2, "bot");
        pool.addExp(5, "perk");
        pool.addExpModifier(0, "财团分红");
        pool.addGoldModifier(1.2, "财团分红");
        pool.addGoldModifier(.01, "宵禁");
        var payout = new BotReward(true, 2, 2).payout(pool, 1, 1000, 1);
        assertEquals(0, payout.experience());
        assertEquals(.048, payout.coins(), 1e-9);
    }

    @Test
    void directOrdinaryStreakExperienceCannotBypassTheLevelCap() {
        assertEquals(12, BotReward.experienceWithinLevelCap(12, 100));
        assertEquals(.5, BotReward.experienceWithinLevelCap(12, .5));
        assertEquals(0, BotReward.experienceWithinLevelCap(12, 0));
        assertEquals(0, BotReward.experienceWithinLevelCap(12, -10));
        assertEquals(0, BotReward.experienceWithinLevelCap(Double.NaN, 100));
    }
}
