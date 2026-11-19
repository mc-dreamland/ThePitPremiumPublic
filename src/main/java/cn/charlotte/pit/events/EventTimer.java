package cn.charlotte.pit.events;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.util.Pair;
import cn.charlotte.pit.util.cooldown.Cooldown;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;

import java.text.SimpleDateFormat;

@Slf4j
public class EventTimer implements Runnable {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy:MM:dd:HH:mm");
    
    @Override
    public void run() {
        
        if (CombatListener.isNight()) return;
        
        EventFactory factory = ThePit.getInstance().getEventFactory();
        Pair<Pair<String, Boolean>, Cooldown> pair = EventsHandler.eventQueue.peek();
        if (pair != null && pair.key().value() && (factory.isMajorEventBusy() || factory.getNextEpicEvent() != null)) return;
        if (pair != null && pair.value().hasExpired()) {
            EventsHandler.eventQueue.poll();
            
            String eventName = pair.key().key();
            boolean isEpic = pair.key().value();
            
            IEvent event;
            if (isEpic) {
                event = factory.getEpicEvents().stream()
                        .filter(e -> e.getEventInternalName().equals(eventName))
                        .findFirst().orElse(null);
            } else {
                event = factory.getNormalEvents().stream()
                        .filter(e -> e.getEventInternalName().equals(eventName))
                        .findFirst().orElse(null);
            }
            
            if (event == null) {
                log.warn("未找到匹配的事件：{}", eventName);
                return;
            }
            
            boolean enoughPlayers = ThePit.isDEBUG_SERVER() ||
                    (Bukkit.getOnlinePlayers().size() >= event.requireOnline() && !CombatListener.isNight());
            if (!enoughPlayers) {
                log.info("人数不足，跳过事件：{}", eventName);
                return;
            }
            
            if (isEpic) {
                factory.pushEvent((IEpicEvent) event, false, -1);
                int slotsLeft = 150 - EventsHandler.eventQueue.size();
                if (slotsLeft < 20) EventsHandler.refreshEvents();
            } else {
                factory.pushEvent((INormalEvent) event, false);
            }
        }
    }
}
