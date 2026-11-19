package cn.charlotte.pit.nametag.hiding;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class NameHidingAliasesTest {
    @Test void identifiersAreStableUniqueAndFitTheProtocolLimitWithoutVisibleCharacters() {
        var aliases = new NameHidingAliases();
        var seen = new HashSet<String>();
        for (int i = 0; i < 10000; i++) {
            UUID player = UUID.randomUUID();
            String name = aliases.name(player);
            assertEquals(16, name.length());
            assertTrue(name.matches("(\\u00a7[0-9a-f]){7}\\u00a7r"));
            assertEquals("", name.replaceAll("\\u00a7[0-9a-fr]", ""));
            assertEquals(name, aliases.name(player));
            assertTrue(seen.add(name));
        }
    }
    @Test void reconnectNeverReusesAnOldClientEntryAndTeamSortingKeepsTheOriginalPrefix() {
        var aliases = new NameHidingAliases();
        UUID player = UUID.randomUUID();
        String old = aliases.name(player);
        aliases.remove(player);
        String current = aliases.name(player);
        assertNotEquals(old, current);
        assertTrue(NameHidingAliases.teamId("0000000123456789", current).startsWith("0000000123456789_nh_"));
        assertNotEquals(NameHidingAliases.teamId("team", old), NameHidingAliases.teamId("team", current));
    }
    @Test void invalidOrExhaustedIdentifiersFailExplicitlyInsteadOfCreatingDuplicates() {
        assertThrows(IllegalStateException.class, () -> NameHidingAliases.encode(0));
        assertThrows(IllegalStateException.class, () -> NameHidingAliases.encode(0x10000000));
        assertEquals(16, NameHidingAliases.encode(0xfffffff).length());
    }
}
