package cn.charlotte.pit.menu.customname;

import java.util.Objects;
import java.util.UUID;

/** One menu may authorize exactly one use of the approved voucher that opened it. */
final class CustomNamePaperUse {
    private final UUID playerUuid;
    private final UUID paperUuid;
    private final String name;
    private boolean active = true;

    CustomNamePaperUse(UUID playerUuid, UUID paperUuid, String name) {
        this.playerUuid = Objects.requireNonNull(playerUuid);
        this.paperUuid = Objects.requireNonNull(paperUuid);
        this.name = Objects.requireNonNull(name);
    }

    boolean isActive() {
        return active;
    }

    boolean tryUse(UUID playerUuid, UUID paperUuid, String name, boolean allowed) {
        if (!active || !allowed || this.name.isEmpty()
                || !this.playerUuid.equals(playerUuid)
                || !this.paperUuid.equals(paperUuid)
                || !this.name.equals(name)) {
            return false;
        }
        // Invalidate before changing inventory or calling events, including re-entrant callbacks.
        active = false;
        return true;
    }

    void close() {
        active = false;
    }
}
