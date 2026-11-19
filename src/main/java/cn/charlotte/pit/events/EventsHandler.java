package cn.charlotte.pit.events;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.activity.ActivityType;
import cn.charlotte.pit.data.EventQueue;
import cn.charlotte.pit.util.DualQueue;
import cn.charlotte.pit.util.Pair;
import cn.charlotte.pit.util.ShufflePool;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.random.RandomUtil;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EventsHandler {
    
    public static final DualQueue<Pair<Pair<String, Boolean>, Cooldown>> eventQueue = new DualQueue<>();
    private static ShufflePool<IEpicEvent> epicPool;
    private static ShufflePool<INormalEvent> normalPool;
    
    public static void refreshEvents() {
        if (eventQueue.size() >= 150) return;
        
        EventFactory factory = ThePit.getInstance().getEventFactory();
        
        if (epicPool == null) epicPool = new ShufflePool<>(factory.getEpicEvents());
        if (normalPool == null) normalPool = new ShufflePool<>(factory.getNormalEvents());
        
        int count = 0;
        int eventsToAdd = 150 - eventQueue.size();
        Long cacheTime = null;
        for (int i = 0; i < eventsToAdd; i++) {
            IEvent event;
            boolean isEpic = false;
            if (count == 5) {
                event = epicPool.next();
                
                ActivityType type = event.getEventType();
                if (!type.equals(ActivityType.NULL) && !type.equals(ThePit.getInstance().getActivityFactory().getFactoryType())) {
                    do {
                        event = epicPool.next();
                        type = event.getEventType();
                    } while (!type.equals(ActivityType.NULL) && !type.equals(ThePit.getInstance().getActivityFactory().getFactoryType()));
                }
                count = 0;
                isEpic = true;
            } else {
                event = normalPool.next();
                if ("auction".equals(event.getEventInternalName()) && RandomUtil.hasSuccessfullyByChance(0.75)) {
                    event = normalPool.next();
                }
                if ("hunt".equals(event.getEventInternalName()) && RandomUtil.hasSuccessfullyByChance(0.75)) {
                    event = normalPool.next();
                }
                if ("enchantingBangEvent".equals(event.getEventInternalName()) && RandomUtil.hasSuccessfullyByChance(0.75)) {
                    event = normalPool.next();
                }
                ActivityType type = event.getEventType();
                if (!type.equals(ActivityType.NULL) && !type.equals(ThePit.getInstance().getActivityFactory().getFactoryType())) {
                    do {
                        event = normalPool.next();
                        type = event.getEventType();
                    } while (!type.equals(ActivityType.NULL) && !type.equals(ThePit.getInstance().getActivityFactory().getFactoryType()));
                }
                count++;
            }
            
            if (cacheTime == null) {
                cacheTime = (!eventQueue.isEmpty() ? eventQueue.get(eventQueue.size() - 1).value().getExpire() : 0);
            }
            eventQueue.add(new Pair<>(new Pair<>(event.getEventInternalName(), isEpic),
                    new Cooldown(
                            cacheTime + 5 * 60 * 1000L
                    )));
            cacheTime += 5 * 60 * 1000L + event.getCanceltime();
        }
        
        List<String> epicList = new ArrayList<>();
        List<String> normalList = new ArrayList<>();
        
        for (int i = 0; i < eventQueue.size(); i++) {
            Pair<Pair<String, Boolean>, Cooldown> pair = eventQueue.get(i);
            if (pair.key().value())
                epicList.add(pair.key().key());
            else
                normalList.add(pair.key().key());
            
        }
        
        EventQueue queue = new EventQueue();
        queue.epicEvents.addAll(epicList);
        queue.normalEvents.addAll(normalList);
        
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> ThePit.getInstance().getMongoDB().getEventQueueCollection()
                .replaceOne(Filters.eq("id", "1"), queue, new ReplaceOptions().upsert(true)));
    }
    
    public static void loadFromDatabase() {
        eventQueue.clear();
        EventQueue queue = ThePit.getInstance().getMongoDB().getEventQueueCollection().findOne();
        if (queue == null) {
            refreshEvents();
            return;
        }
        
        int count = 0;
        EventFactory factory = ThePit.getInstance().getEventFactory();
        Long cacheTime = null;
        if (queue.normalEvents.size() + queue.epicEvents.size() < 70) {
            refreshEvents();
            return;
        }
        int totalSize = queue.normalEvents.size() + queue.epicEvents.size();
        for (int i = 0; i < totalSize; i++) {
            if (cacheTime == null) {
                cacheTime = eventQueue.isEmpty() ? System.currentTimeMillis() : eventQueue.get(eventQueue.size() - 1).value().getExpire();
            }
            
            IEvent event;
            boolean isEpic = false;
            if (count == 5) {
                String eventName = queue.epicEvents.getFirst();
                event = factory.getEpicEvents().stream().filter(e -> eventName.equals(e.getEventInternalName())).findFirst().orElse(null);
                if (event == null) {
                    log.warn("无法查找此到事件: {}", eventName);
                    queue.epicEvents.removeFirst();
                    cacheTime += 5 * 60 * 1000L + 5 * 60 * 1000L;
                    count = 0;
                    continue;
                }
                queue.epicEvents.removeFirst();
                count = 0;
                isEpic = true;
            } else {
                String eventName = queue.normalEvents.getFirst();
                event = factory.getNormalEvents().stream().filter(e -> eventName.equals(e.getEventInternalName())).findFirst().orElse(null);
                if (event == null) {
                    log.warn("无法查找此到事件: {}", eventName);
                    queue.normalEvents.removeFirst();
                    cacheTime += 5 * 60 * 1000L + 5 * 60 * 1000L;
                    count++;
                    continue;
                }
                queue.normalEvents.removeFirst();
                count++;
            }
            
            eventQueue.add(new Pair<>(new Pair<>(event.getEventInternalName(), isEpic),
                    new Cooldown(cacheTime + 5 * 60 * 1000L)));
            cacheTime += 5 * 60 * 1000L + event.getCanceltime();
        }
    }
}