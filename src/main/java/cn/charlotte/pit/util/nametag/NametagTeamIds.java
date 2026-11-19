package cn.charlotte.pit.util.nametag;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player teams: truncating a username may put two players in one team and hide both names. */
public final class NametagTeamIds {
    private final Map<UUID, String> tokens = new HashMap<>();
    private long next;

    public String name(int priority, UUID player) {
        String token = tokens.computeIfAbsent(player, ignored -> {
            String value = Long.toString(next++, 36);
            if (value.length() > 6) throw new IllegalStateException("名牌 Team ID 已耗尽");
            return "0".repeat(6 - value.length()) + value;
        });
        long sortablePriority = (long) priority - Integer.MIN_VALUE;
        return String.format(java.util.Locale.ROOT, "%010d", sortablePriority) + token;
    }

    public void retain(Collection<UUID> online) { tokens.keySet().retainAll(online); }
}
