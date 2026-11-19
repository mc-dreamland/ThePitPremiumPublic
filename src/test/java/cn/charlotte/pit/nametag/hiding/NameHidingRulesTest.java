package cn.charlotte.pit.nametag.hiding;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static cn.charlotte.pit.nametag.hiding.NameHidingRules.Area.*;
import static org.junit.jupiter.api.Assertions.*;

class NameHidingRulesTest {
    private final NameHidingRules.Settings rules = new NameHidingRules.Settings(true, 0, 5, 30, 1000, EnumSet.of(MIDDLE, WILD));

    @Test void prestigeBoundsAreInclusiveAndRegionsHaveIndependentSwitches() {
        assertTrue(hidden(MIDDLE, 0, 0, 0));
        assertTrue(hidden(WILD, 5, 0, 0));
        assertFalse(hidden(WILD, 6, 0, 0));
        assertFalse(hidden(SPAWN, 0, 0, 0));
        assertFalse(hidden(NONE, 0, 0, 0));
    }

    @Test void eitherStatShowsNamesAtTheExactThresholdEvenInsideTheHiddenPrestigeRange() {
        assertTrue(hidden(MIDDLE, 3, 29.99, 999));
        assertFalse(hidden(MIDDLE, 3, 30, 0));
        assertFalse(hidden(WILD, 3, 0, 1000));
        assertFalse(hidden(MIDDLE, 3, 30.5, 0));
        assertFalse(hidden(WILD, 3, 0, 1000.1));
        assertTrue(rules.showsByStats(30, 0));
        assertTrue(rules.showsByStats(0, 1000));
        assertFalse(hidden(WILD, 20, 0, 0));
        assertFalse(hidden(WILD, 20, 999, 99999));
    }

    @Test void eventsAndActiveMegaStreaksOverrideAllHidingConditions() {
        for (boolean event : new boolean[]{false, true}) for (boolean mega : new boolean[]{false, true}) {
            boolean hidden = rules.hides(MIDDLE, 3, 0, 0, event, mega, false, false);
            assertEquals(!event && !mega, hidden);
        }
    }

    @Test void revealIsPerViewerAndSelfIsNeverHidden() {
        assertFalse(rules.hides(MIDDLE, 3, 0, 0, false, false, true, false));
        assertTrue(rules.hides(MIDDLE, 3, 0, 0, false, false, false, false));
        assertFalse(rules.hides(MIDDLE, 3, 0, 0, false, false, false, true));
    }

    @Test void disabledFeatureAndDisabledThresholdsDoNotMaskAnyoneUnexpectedly() {
        var off = new NameHidingRules.Settings(false, 0, 5, 30, 1000, Set.of(MIDDLE));
        assertFalse(off.hides(MIDDLE, 3, 500, 50000, false, false, false, false));
        var thresholdsOff = new NameHidingRules.Settings(true, 0, 5, -1, -1, Set.of(MIDDLE));
        assertFalse(thresholdsOff.hides(MIDDLE, 20, 500, 50000, false, false, false, false));
        assertTrue(thresholdsOff.hides(MIDDLE, 3, 500, 50000, false, false, false, false));
        assertFalse(thresholdsOff.showsByStats(500, 50000));
    }

    @Test void individualExemptionsCanBeDisabledAndZeroShowsEveryoneInTheRange() {
        var streakOnly = new NameHidingRules.Settings(true, 0, 5, 30, -1, Set.of(MIDDLE));
        assertTrue(streakOnly.hides(MIDDLE, 3, 29, 99999, false, false, false, false));
        assertFalse(streakOnly.hides(MIDDLE, 3, 30, 99999, false, false, false, false));
        var bountyOnly = new NameHidingRules.Settings(true, 0, 5, -1, 1000, Set.of(MIDDLE));
        assertTrue(bountyOnly.hides(MIDDLE, 3, 999, 999, false, false, false, false));
        assertFalse(bountyOnly.hides(MIDDLE, 3, 999, 1000, false, false, false, false));
        var zero = new NameHidingRules.Settings(true, 0, 5, 0, -1, Set.of(MIDDLE));
        assertFalse(zero.hides(MIDDLE, 3, 0, 0, false, false, false, false));
    }

    @Test void invalidThresholdsAndPrestigeRangesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new NameHidingRules.Settings(true, 5, 0, 0, 0, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new NameHidingRules.Settings(true, 0, 5, Double.NaN, 0, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new NameHidingRules.Settings(true, 0, 5, 0, Double.POSITIVE_INFINITY, Set.of()));
    }

    @Test void regionsReuseSpawnCuboidAndRagePitCylinderInTheSameWorld() {
        var region = new NameHidingRules.Regions("world", new NameHidingRules.Box("world", -2, 100, -2, 2, 105, 2), 0, 80, 0, 10, 30);
        assertEquals(SPAWN, region.area("world", 0, 103, 0)); // Spawn wins an overlap.
        assertEquals(SPAWN, region.area("world", 2.99, 105.99, 2.99));
        assertEquals(MIDDLE, region.area("world", 10.9, 80, 0));
        assertEquals(MIDDLE, region.area("world", 0, 109.99, 0));
        assertEquals(WILD, region.area("world", 11, 80, 0));
        assertEquals(WILD, region.area("world", 0, 110, 0));
        assertEquals(WILD, region.area("world", 0, 79.99, 0));
        assertEquals(NONE, region.area("other", 0, 100, 0));
        assertEquals(NONE, region.area("world", Double.NaN, 100, 0));
    }

    @Test void revealTimerUsesSecondsAndCannotOverflowIntoAlreadyExpiredTimes() {
        assertEquals(301000, NameHidingRules.revealExpiry(1000, 300));
        assertEquals(Long.MAX_VALUE, NameHidingRules.revealExpiry(1000, Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> NameHidingRules.revealExpiry(0, 0));
    }

    @Test void megaStreakShowsNamesFromTheExactActivationThresholdNotTheNextKill() {
        assertFalse(NameHidingRules.megaThresholdReached(49.99, 50));
        assertTrue(NameHidingRules.megaThresholdReached(50, 50));
        assertTrue(NameHidingRules.megaThresholdReached(100, 100));
        assertFalse(NameHidingRules.megaThresholdReached(500, 0));
    }

    @Test void aSeparateSpawnWorldIsRecognizedButUnrelatedWorldsAreNeverTreatedAsWild() {
        var region = new NameHidingRules.Regions("arena", new NameHidingRules.Box("lobby", -5, 60, -5, 5, 80, 5), 0, 60, 0, 10, 20);
        assertEquals(SPAWN, region.area("lobby", 0, 70, 0));
        assertEquals(NONE, region.area("lobby", 20, 70, 0));
        assertEquals(MIDDLE, region.area("arena", 0, 70, 0));
        assertEquals(WILD, region.area("arena", 20, 70, 0));
    }

    private boolean hidden(NameHidingRules.Area area, int prestige, double streak, double bounty) {
        return rules.hides(area, prestige, streak, bounty, false, false, false, false);
    }
}
