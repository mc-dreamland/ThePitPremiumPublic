package cn.charlotte.pit.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BotPopulationTest {
    @Test
    void emptyRegionHasNoMinimumPopulation() {
        assertEquals(0, BotPopulation.target(0, 4, 10, 1));
        assertEquals(0, BotPopulation.target(-1, 4, 10, 1));
    }

    @Test
    void trainingTracksParticipantsBetweenFourAndTen() {
        for (int players = 1; players <= 150; players++) {
            assertEquals(Math.min(10, Math.max(4, players)), BotPopulation.target(players, 4, 10, 1));
        }
    }

    @Test
    void wildHasOnePerTwoEligiblePlayersAndNeverExceedsTwenty() {
        assertEquals(1, BotPopulation.target(1, 1, 20, .5));
        assertEquals(1, BotPopulation.target(2, 1, 20, .5));
        assertEquals(2, BotPopulation.target(3, 1, 20, .5));
        assertEquals(20, BotPopulation.target(40, 1, 20, .5));
        assertEquals(20, BotPopulation.target(150, 1, 20, .5));
    }

    @Test
    void administratorLimitsAndInvalidRatiosRemainBounded() {
        assertEquals(0, BotPopulation.target(10, 4, 0, 1));
        assertEquals(3, BotPopulation.target(10, 4, 3, 1));
        assertEquals(4, BotPopulation.target(2, 4, 10, Double.NaN));
        assertEquals(10, BotPopulation.target(Integer.MAX_VALUE, 4, 10, 10));
    }

    @Test
    void refillRechecksCurrentDeficitInsteadOfBlindlyExecutingOldQueue() {
        assertEquals(6, BotPopulation.missing(10, 4));
        assertEquals(0, BotPopulation.missing(4, 10));
        assertEquals(0, BotPopulation.missing(0, 4));
        assertEquals(0, BotPopulation.missing(10, 10));
    }

    @Test
    void configuredLimitsAndMinimumsMayExceedTheFormerTenAndTwentyCeilings() {
        assertEquals(60, BotPopulation.target(150, 10, 60, .75));
        assertEquals(100, BotPopulation.target(150, 4, 100, 1));
        assertEquals(40, BotPopulation.target(1, 40, 60, .75));
        assertEquals(500, BotPopulation.target(20, 1, 1000, 25));
        assertEquals(0, BotPopulation.target(0, 40, 60, .75));
        assertEquals(60, BotPopulation.target(Integer.MAX_VALUE, 1, 60, Double.MAX_VALUE));
    }
}
