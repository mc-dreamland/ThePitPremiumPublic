package cn.charlotte.pit.events;


import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.activity.ActivityType;

import java.util.List;
import java.util.UUID;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/9 13:11
 */
public interface IEvent {

    String getEventInternalName();

    String getEventName();

    List<String> getLore();

    int requireOnline();

    void onActive();

    void onInactive();

    default void onPreActive() {
    }

    default int getRank(UUID playerUUID) {
        return 0;
    }

    default ActivityType getEventType() {
        return ActivityType.NULL;
    }

    default boolean isActive() {
        String eventName;
        if (INormalEvent.class.isAssignableFrom(this.getClass()))
            eventName = ThePit.getInstance().getEventFactory().getActiveNormalEventName();
        else eventName = ThePit.getInstance().getEventFactory().getActiveEpicEventName();
        if (eventName == null) return false;
        else return this.getEventInternalName().equals(eventName);
    }

    default long getCanceltime() {
        return 1000L * 60 * 5;
    }
}
