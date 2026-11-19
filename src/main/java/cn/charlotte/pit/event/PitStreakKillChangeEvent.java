package cn.charlotte.pit.event;

import cn.charlotte.pit.data.PlayerProfile;
import lombok.EqualsAndHashCode;
import org.bukkit.event.Cancellable;

/**
 * @Author: EmptyIrony
 * @Date: 2021/4/25 22:25
 */
@EqualsAndHashCode(callSuper = true)
public class PitStreakKillChangeEvent extends PitEvent implements Cancellable {
    public enum Source { NORMAL, BOT }

    private final PlayerProfile playerProfile;
    private final double from;
    private final double to;
    private final Source source;
    private boolean cancelled;

    public PitStreakKillChangeEvent(PlayerProfile playerProfile, double from, double to) {
        this(playerProfile, from, to, false);
    }

    public PitStreakKillChangeEvent(PlayerProfile playerProfile, double from, double to, boolean cancelled) {
        this(playerProfile, from, to, Source.NORMAL);
        this.cancelled = cancelled;
    }

    public PitStreakKillChangeEvent(PlayerProfile playerProfile, double from, double to, Source source) {
        this.playerProfile = playerProfile;
        this.from = from;
        this.to = to;
        this.source = source == null ? Source.NORMAL : source;
    }

    public Source getSource() { return source; }

    public boolean isBotKill() { return source == Source.BOT; }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }

    public PlayerProfile getPlayerProfile() {
        return playerProfile;
    }

    public double getFrom() {
        return from;
    }

    public double getTo() {
        return to;
    }
}
