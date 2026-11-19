package cn.charlotte.pit.bot;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BotRulesTest {
    @Test
    void cornersIncludeEntireBlocksInEitherOrderButNotOtherWorldsOrUpperBoundary() {
        BotRegion region = BotRegion.of("pit", 10, 70, 20, -10, 60, -20);
        assertTrue(region.contains("pit", -10, 60, -20));
        assertTrue(region.contains("pit", 10.99, 70.99, 20.99));
        assertFalse(region.contains("pit", 11, 70, 20));
        assertFalse(region.contains("other", 0, 65, 0));
        assertEquals(region, BotRegion.of("pit", -10, 60, -20, 10, 70, 20));
    }

    @Test
    void wildScalingIsPerHitMonotonicAndBoundedThroughHighPrestiges() {
        double previousOut = Double.MAX_VALUE;
        double previousIn = 0;
        for (int prestige = 0; prestige <= 200; prestige++) {
            double out = BotBalance.outgoing(10000, prestige, 8, .15, .12);
            double in = BotBalance.incoming(4, prestige, .12, 5);
            assertTrue(out <= previousOut && out >= 8 * .12 && out <= 8);
            assertTrue(in >= previousIn && in >= 4 && in <= 20);
            previousOut = out;
            previousIn = in;
        }
        assertEquals(8, BotBalance.outgoing(8, 0, 8, .15, .12));
        assertEquals(8 / 6.25, BotBalance.outgoing(8, 35, 8, .15, .12), 1e-9);
        assertEquals(20, BotBalance.incoming(4, 35, .12, 5));
        assertEquals(0, BotBalance.outgoing(-3, 0, 8, .15, .12));
    }

    @Test
    void overkillDoesNotStealRewardPoolAndUncreditedDamageIsNotRedistributed() {
        UUID low = UUID.randomUUID();
        UUID high = UUID.randomUUID();
        BotDamageLedger ledger = new BotDamageLedger();
        ledger.record(high, 36, 40, 1000);
        ledger.record(low, 1000, 4, 1000);
        Map<UUID, Double> shares = ledger.shares(40, 1001, 30000);
        assertEquals(.1, shares.get(low), 1e-9);
        assertEquals(.9, shares.get(high), 1e-9);
        assertEquals(1, shares.values().stream().mapToDouble(Double::doubleValue).sum(), 1e-9);
        // Dropping the ineligible player's payout must not turn this 10% contribution into 100%.
        assertEquals(.1, shares.get(low), 1e-9);
    }

    @Test
    void staleOrInvalidContributionsCannotMintExtraRewards() {
        UUID player = UUID.randomUUID();
        BotDamageLedger ledger = new BotDamageLedger();
        ledger.record(player, Double.NaN, 40, 1000);
        ledger.record(player, -2, 40, 1000);
        assertTrue(ledger.shares(40, 1000, 30000).isEmpty());
        ledger.record(player, 5, 40, 1000);
        assertEquals(.125, ledger.shares(40, 2000, 30000).get(player), 1e-9);
        assertTrue(ledger.shares(40, 31001, 30000).isEmpty());
    }

    @Test
    void damageSharesUseActualMythicMobHealthRatherThanTheOldFortyHealthDefault() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        for (double health : new double[]{20, 80, 5000}) {
            BotDamageLedger ledger = new BotDamageLedger();
            ledger.record(first, health * .25, health, 1000);
            ledger.record(second, health * 10, health * .75, 1000);
            assertEquals(.25, ledger.shares(health, 1000, 30000).get(first), 1e-9);
            assertEquals(.75, ledger.shares(health, 1000, 30000).get(second), 1e-9);
        }
    }

    @Test
    void incomingScalingUsesTheActualAttackDamageAsItsBase() {
        assertEquals(9, BotBalance.incoming(9, 0, .12, 5));
        assertEquals(19.8, BotBalance.incoming(9, 10, .12, 5), 1e-9);
        assertEquals(45, BotBalance.incoming(9, 35, .12, 5));
        assertEquals(0, BotBalance.incoming(0, 35, .12, 5));
    }

    @Test
    void trainingAllowsInsidePvpAndOutsideAttacksOnBotsButNotCrossBoundaryPvp() {
        assertFalse(blocked(true, true, true, false, false, true, null));
        assertFalse(blocked(true, true, false, true, true, false, null));
        assertFalse(blocked(true, true, true, true, false, false, null));
        assertTrue(blocked(false, true, true, true, false, false, null));
        assertTrue(blocked(true, false, true, true, false, false, null));
        assertFalse(blocked(false, true, true, false, false, true, null));
        assertTrue(blocked(true, false, false, true, true, false, null));
        // TNT, pets and other mobs are not approved training bots.
        assertTrue(blocked(true, true, false, true, false, false, null));
        // Normal combat outside is unchanged.
        assertFalse(blocked(false, false, true, true, false, false, null));
        assertFalse(blocked(false, false, true, false, false, true, null));
    }

    @Test
    void projectileOriginCannotBeLaunderedByShooterMovingAcrossBoundary() {
        assertFalse(blocked(true, true, true, false, false, true, false));
        assertTrue(blocked(false, false, true, true, false, false, true));
        assertFalse(blocked(true, true, true, false, false, true, true));
        assertFalse(blocked(false, false, true, false, false, true, false));
    }

    private boolean blocked(boolean si, boolean ti, boolean sp, boolean tp, boolean sb, boolean tb, Boolean launch) {
        return TrainingCombatRules.blocked(si, ti, sp, tp, sb, tb, launch);
    }
}
