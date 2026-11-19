package cn.charlotte.pit.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BotSpawnPointTest {
    @Test
    void proximityUsesTheConfiguredPointAndIncludesTheRadiusBoundary() {
        BotSpawnPoint point = new BotSpawnPoint("pit", 10, 64, 10);
        assertTrue(point.near("pit", 10, 64, 10, 16));
        assertTrue(point.near("pit", 26, 64, 10, 16));
        assertFalse(point.near("pit", 26.01, 64, 10, 16));
        assertFalse(point.near("pit", 10, 81, 10, 16));
    }

    @Test
    void differentWorldOrInvalidRadiusDoesNotAttractBots() {
        BotSpawnPoint point = new BotSpawnPoint("pit", 0, 64, 0);
        assertFalse(point.near("other", 0, 64, 0, 16));
        assertFalse(point.near("pit", 0, 64, 0, -16));
        assertFalse(point.near("pit", 0, 64, 0, Double.NaN));
    }

    @Test
    void duplicateBlocksIgnoreExactPositionButKeepWorldAndNegativeCoordinates() {
        BotSpawnPoint point = new BotSpawnPoint("pit", -1.5, 64, 5.5);
        assertTrue(point.sameBlock("pit", -1.01, 64.9, 5.01));
        assertFalse(point.sameBlock("pit", -.99, 64, 5.5));
        assertFalse(point.sameBlock("other", -1.5, 64, 5.5));
    }

    @Test
    void invalidSavedCoordinatesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BotSpawnPoint("pit", Double.NaN, 64, 0));
        assertThrows(IllegalArgumentException.class, () -> new BotSpawnPoint("", 0, 64, 0));
    }
}
