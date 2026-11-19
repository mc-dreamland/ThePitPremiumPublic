package cn.charlotte.pit.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

@Getter
@RequiredArgsConstructor
public class PitPlayerHistoricalEventsEvent extends PitEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String historicalEvent;
    private final int count;

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
