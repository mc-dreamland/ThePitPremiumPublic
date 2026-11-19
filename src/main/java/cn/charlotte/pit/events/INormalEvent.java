package cn.charlotte.pit.events;

import cn.charlotte.pit.activity.ActivityType;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/9 12:59
 */
public interface INormalEvent extends IEvent {

    default int duration() {
        return 5;
    }

    default ActivityType getEventType() {
        return IEvent.super.getEventType();
    }

}
