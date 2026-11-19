package cn.charlotte.pit.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BotForceRulesTest {
    @Test
    void onlyRemainingCapacityCanBeReservedIncludingEarlierCommands() {
        assertEquals(7, BotForceRules.accepted(100, 10, 3, 0));
        assertEquals(2, BotForceRules.accepted(100, 10, 3, 5));
        assertEquals(1, BotForceRules.accepted(1, 20, 10, 2));
        assertEquals(0, BotForceRules.accepted(10, 20, 20, 0));
        assertEquals(0, BotForceRules.accepted(10, 10, 5, 5));
    }

    @Test
    void InvalidAndExtremeCountsCannotOverflowOrOverfill() {
        assertEquals(0, BotForceRules.accepted(-1, 20, 0, 0));
        assertEquals(0, BotForceRules.accepted(1, -1, 0, 0));
        assertEquals(20, BotForceRules.accepted(Integer.MAX_VALUE, 20, 0, 0));
        assertEquals(0, BotForceRules.accepted(Integer.MAX_VALUE, 20, Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(20, BotForceRules.accepted(Integer.MAX_VALUE, 20, -1, -1));
    }

    @Test
    void ForcedBotsExpireOnlyAfterDeadlineAndAfterCombatGrace() {
        assertFalse(BotForceRules.expired(0, 0, Long.MAX_VALUE));
        assertFalse(BotForceRules.expired(300_000, 0, 299_999));
        assertTrue(BotForceRules.expired(300_000, 0, 300_000));
        assertFalse(BotForceRules.expired(300_000, 299_000, 300_000));
        assertFalse(BotForceRules.expired(300_000, 299_000, 309_000));
        assertTrue(BotForceRules.expired(300_000, 299_000, 309_001));
    }

    @Test
    void forcedRequestsUseTheConfiguredCapacityIncludingCapsAboveTheFormerCeilings() {
        assertEquals(60, BotForceRules.accepted(100, 60, 0, 0));
        assertEquals(35, BotForceRules.accepted(100, 60, 20, 5));
        assertEquals(1000, BotForceRules.accepted(1000, 1000, 0, 0));
    }
}
