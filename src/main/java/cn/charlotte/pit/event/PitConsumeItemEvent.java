package cn.charlotte.pit.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

@Getter
public class PitConsumeItemEvent extends PitEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String itemName;

    public PitConsumeItemEvent(Player player, String itemName) {
        this.player = player;
        this.itemName = itemName;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
