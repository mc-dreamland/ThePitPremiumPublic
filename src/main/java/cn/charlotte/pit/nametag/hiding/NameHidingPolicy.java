package cn.charlotte.pit.nametag.hiding;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Immutable snapshot only. Network callbacks never inspect Bukkit players/profiles/regions. */
record NameHidingPolicy(boolean enabled, Map<UUID, String> aliases, Set<UUID> hidden, Set<UUID> revealed) {
    static final NameHidingPolicy OFF = new NameHidingPolicy(false, Map.of(), Set.of(), Set.of());
    NameHidingPolicy {
        aliases = Map.copyOf(aliases);
        hidden = Set.copyOf(hidden);
        revealed = Set.copyOf(revealed);
    }
    String alias(UUID viewer, UUID target) { return enabled && !viewer.equals(target) ? aliases.get(target) : null; }
    boolean hides(UUID viewer, UUID target) {
        return enabled && !viewer.equals(target) && hidden.contains(target) && !revealed.contains(viewer);
    }
}
