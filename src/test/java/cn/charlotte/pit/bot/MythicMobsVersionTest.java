package cn.charlotte.pit.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MythicMobsVersionTest {
    @Test
    void acceptsReleaseAndTheActualLocalJarVersion() {
        assertTrue(MythicMobsVersion.supported("5.8.2"));
        assertTrue(MythicMobsVersion.supported("5.8.2-6a2683d7"));
        assertTrue(MythicMobsVersion.supported("5.8.2-SNAPSHOT"));
        assertTrue(MythicMobsVersion.supported("5.8.2+build.123"));
    }

    @Test
    void rejectsUnsupportedVersionsAndMisleadingPrefixes() {
        for (String version : new String[]{"5.13.0", "5.8.1", "5.8.3", "5.8.20", "5.8.2.1", "15.8.2", "5.8"}) {
            assertFalse(MythicMobsVersion.supported(version), version);
        }
    }

    @Test
    void handlesMissingVersionAndWhitespaceWithoutCrashing() {
        assertFalse(MythicMobsVersion.supported(null));
        assertFalse(MythicMobsVersion.supported(""));
        assertFalse(MythicMobsVersion.supported("  "));
        assertTrue(MythicMobsVersion.supported(" 5.8.2-6a2683d7 "));
    }
}
