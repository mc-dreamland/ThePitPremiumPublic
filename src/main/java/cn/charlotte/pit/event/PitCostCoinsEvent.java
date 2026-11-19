package cn.charlotte.pit.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
@Data
public class PitCostCoinsEvent extends PitEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final double coins;

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
