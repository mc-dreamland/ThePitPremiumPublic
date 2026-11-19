package cn.charlotte.pit.nametag.hiding;

import cn.charlotte.pit.util.nametag.NametagTeamIds;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NametagTeamIdsTest {
    @Test void everyPlayerGetsASeparateStableSixteenCharacterTeam() {
        NametagTeamIds ids = new NametagTeamIds();
        Set<String> names = new HashSet<>();
        for (int i = 0; i < 150; i++) {
            UUID player = UUID.randomUUID();
            String name = ids.name(100999999, player);
            assertEquals(16, name.length());
            assertEquals(name, ids.name(100999999, player));
            assertTrue(names.add(name));
        }
    }

    @Test void sortingPriorityStillLeadsAndRejoiningCannotCollideWithAnOldTeam() {
        NametagTeamIds ids = new NametagTeamIds();
        UUID player = UUID.randomUUID();
        String old = ids.name(100000000, player);
        assertTrue(old.compareTo(ids.name(999000000, UUID.randomUUID())) < 0);
        ids.retain(List.of());
        assertNotEquals(old, ids.name(100000000, player));
    }

    @Test void extremePrioritiesRemainWithinTheProtocolLimitAndKeepNumericOrder() {
        NametagTeamIds ids = new NametagTeamIds();
        String minimum = ids.name(Integer.MIN_VALUE, UUID.randomUUID());
        String maximum = ids.name(Integer.MAX_VALUE, UUID.randomUUID());
        assertEquals(16, minimum.length());
        assertEquals(16, maximum.length());
        assertTrue(minimum.compareTo(maximum) < 0);
    }
}
