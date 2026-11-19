package cn.charlotte.pit.event;

import org.bukkit.entity.Player;

public class PitDeathEvent extends PitEvent {
    private final Player deadPlayer;
    private final boolean disconnected;

    public PitDeathEvent(Player deadPlayer, boolean disconnected) {
        this.deadPlayer = deadPlayer;
        this.disconnected = disconnected;
    }

    public Player getDeadPlayer() {
        return deadPlayer;
    }

    public boolean isDisconnected() {
        return disconnected;
    }
}
