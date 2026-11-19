package cn.charlotte.pit.bot;

import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.perk.AbstractHandle;
import cn.charlotte.pit.perk.BotKillEffect;
import cn.charlotte.pit.perk.MegaStreak;
import cn.charlotte.pit.perk.type.boosts.*;
import cn.charlotte.pit.perk.type.boosts.based.*;
import cn.charlotte.pit.perk.type.bundles.beastmode.BeastModeMegaStreak;
import cn.charlotte.pit.perk.type.passives.CoinPassivePerk;
import cn.charlotte.pit.perk.type.passives.XPPassivePerk;
import cn.charlotte.pit.perk.type.prestige.CelebrityPerk;
import cn.charlotte.pit.perk.type.prestige.TenacityPerk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BotPerkEffectsTest {
    @Test
    void currencyPerksRunForContributorsAndFinalAttackersWithoutRunningOtherKillEffects() {
        RewardPerk perk = new RewardPerk();
        ResourceManager pool = new ResourceManager();
        pool.addBaseExp(2, "bot");
        assertTrue(BotPerkEffects.apply(perk, 3, null, null, pool, false));
        assertEquals(0, perk.calls);
        assertEquals(1, perk.assistCalls);
        assertEquals(3, perk.level);
        assertEquals(3, pool.calculateEXP());
        assertTrue(BotPerkEffects.apply(perk, 3, null, null, new ResourceManager(), true));
        assertEquals(1, perk.calls);
        assertEquals(1, perk.assistCalls);
    }

    @Test
    void healingAndItemsAreOnlyGivenToTheFinalAttacker() {
        HealingPerk perk = new HealingPerk();
        for (int i = 0; i < 20; i++) assertFalse(BotPerkEffects.apply(perk, 0, null, null, null, false));
        assertEquals(0, perk.calls);
        assertTrue(BotPerkEffects.apply(perk, 0, null, null, null, true));
        assertEquals(1, perk.calls); // Level 0 is valid for one-level selected perks.
    }

    @Test
    void unreviewedProgressionHooksAreNeverCalledEvenForTheFinalAttacker() {
        ProgressionPerk perk = new ProgressionPerk();
        assertFalse(BotPerkEffects.apply(perk, 1, null, null, null, false));
        assertFalse(BotPerkEffects.apply(perk, 1, null, null, null, true));
        assertEquals(0, perk.calls);
        assertEquals(BotKillEffect.Kind.KILL_EFFECT, StrengthChainingPerk.class.getAnnotation(BotKillEffect.class).value());
        assertEquals(BotKillEffect.Kind.ASSIST_REWARD, AssistantToTheStreakerPerk.class.getAnnotation(BotKillEffect.class).value());
    }

    @Test
    void megaStreakHooksAreAlwaysRejectedEvenIfMarkedAsCurrencyOnly() {
        MegaPerk perk = new MegaPerk();
        assertFalse(BotPerkEffects.apply(perk, 1, null, null, null, true));
        assertFalse(BotPerkEffects.apply(perk, 1, null, null, null, false));
        assertEquals(0, perk.calls);
    }

    @Test
    void assistOnlyRewardHooksCannotInvokeTheirUnsafeStreakChangingKillHook() {
        AssistPerk perk = new AssistPerk();
        ResourceManager pool = new ResourceManager();
        pool.addBaseGold(10, "bot");
        assertTrue(BotPerkEffects.apply(perk, 1, null, null, pool, false));
        assertEquals(15, pool.calculateGold());
        assertFalse(BotPerkEffects.apply(perk, 1, null, null, pool, true));
        assertEquals(0, perk.calls);
        assertEquals(BotKillEffect.Kind.ASSIST_REWARD, CoopCatPerk.class.getAnnotation(BotKillEffect.class).value());
    }

    @Test
    void explicitBotOverrideCanKeepAnItemRewardWithoutAdvancingAMedalOrTask() throws Exception {
        BotOverridePerk perk = new BotOverridePerk();
        assertTrue(BotPerkEffects.apply(perk, 1, null, null, null, true));
        assertEquals(1, perk.botCalls);
        assertEquals(0, perk.calls);
        assertEquals(MineManPerk.class, MineManPerk.class.getMethod("handleBotKilled", int.class,
                Player.class, Entity.class, ResourceManager.class).getDeclaringClass());
    }

    @Test
    void realHealingAndRewardPerksHaveTheReviewedDispatchKind() {
        for (Class<?> type : List.of(GoldenHeadPerk.class, VampirePerk.class, TenacityPerk.class,
                DirtyPerk.class, RamboPerk.class, OlympusPerk.class, SoupPerk.class, MineManPerk.class)) {
            assertEquals(BotKillEffect.Kind.KILL_EFFECT, type.getAnnotation(BotKillEffect.class).value(), type.getName());
        }
        for (Class<?> type : List.of(XPPassivePerk.class, CoinPassivePerk.class, CelebrityPerk.class,
                BountyHunterPerk.class, BeastModeMegaStreak.class)) {
            assertEquals(BotKillEffect.Kind.REWARD, type.getAnnotation(BotKillEffect.class).value(), type.getName());
        }
    }

    @BotKillEffect(BotKillEffect.Kind.REWARD)
    static class RewardPerk extends AbstractHandle {
        int calls, assistCalls, level;
        @Override public void handlePlayerKilled(int level, Player player, Entity target, ResourceManager pool) {
            calls++;
            this.level = level;
            pool.addExpModifier(1.5, "perk");
        }
        @Override public void handlePlayerAssist(int level, Player player, Entity target, double damage,
                                                 double finalDamage, ResourceManager pool) {
            assistCalls++;
            this.level = level;
            pool.addExpModifier(1.5, "perk");
        }
    }

    @BotKillEffect(BotKillEffect.Kind.KILL_EFFECT)
    static class HealingPerk extends ProgressionPerk { }

    static class ProgressionPerk extends AbstractHandle {
        int calls;
        @Override public void handlePlayerKilled(int level, Player player, Entity target, ResourceManager pool) { calls++; }
    }

    @BotKillEffect(BotKillEffect.Kind.KILL_EFFECT)
    static class BotOverridePerk extends ProgressionPerk {
        int botCalls;
        @Override public void handleBotKilled(int level, Player player, Entity target, ResourceManager pool) { botCalls++; }
    }

    @BotKillEffect(BotKillEffect.Kind.ASSIST_REWARD)
    static class AssistPerk extends ProgressionPerk {
        @Override public void handlePlayerAssist(int level, Player player, Entity target, double damage,
                                                 double finalDamage, ResourceManager pool) {
            pool.addGoldModifier(1.5, "assist");
        }
    }

    @BotKillEffect(BotKillEffect.Kind.REWARD)
    static class MegaPerk extends ProgressionPerk implements MegaStreak {
        @Override public int getStreakNeed() { return 50; }
        @Override public List<String> getDescriptionInRenownShop(Player player) { return List.of(); }
    }
}
