package cn.charlotte.pit.nametag.hiding;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NameHidingPolicyTest {
    @Test void revealIsPerObserverAndIdentityStaysStableWhenVisibilityChanges() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), target = UUID.randomUUID();
        String alias = NameHidingAliases.encode(1);
        var hidden = new NameHidingPolicy(true, Map.of(target, alias), Set.of(target), Set.of(first));
        assertFalse(hidden.hides(first, target));
        assertTrue(hidden.hides(second, target));
        assertFalse(hidden.hides(target, target));
        assertEquals(alias, hidden.alias(first, target));
        assertEquals(alias, hidden.alias(second, target));
        assertNull(hidden.alias(target, target));
        var shown = new NameHidingPolicy(true, Map.of(target, alias), Set.of(), Set.of());
        assertFalse(shown.hides(second, target));
        assertEquals(hidden.alias(second, target), shown.alias(second, target));
    }
    @Test void packetSnapshotsCannotBeChangedByTheMainThreadsMutableCollections() {
        UUID viewer = UUID.randomUUID(), target = UUID.randomUUID();
        var identities = new HashMap<UUID, String>();
        var hidden = new HashSet<UUID>();
        identities.put(target, NameHidingAliases.encode(1));
        hidden.add(target);
        var policy = new NameHidingPolicy(true, identities, hidden, Set.of());
        identities.clear(); hidden.clear();
        assertTrue(policy.hides(viewer, target));
        assertNotNull(policy.alias(viewer, target));
        assertFalse(NameHidingPolicy.OFF.hides(viewer, target));
        assertNull(NameHidingPolicy.OFF.alias(viewer, target));
    }
}
