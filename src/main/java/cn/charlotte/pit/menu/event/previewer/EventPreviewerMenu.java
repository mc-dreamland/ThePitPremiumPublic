package cn.charlotte.pit.menu.event.previewer;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.events.IEpicEvent;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.menu.event.previewer.button.EventPreviewButton;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * @Creator Misoryan
 * @Date 2021/5/5 12:13
 */
public class EventPreviewerMenu extends Menu {
    @Override
    public String getTitle(Player player) {
        IEpicEvent epicEvent = ThePit.getInstance().getEventFactory().getActiveEpicEvent();
        INormalEvent normalEvent = ThePit.getInstance().getEventFactory().getActiveNormalEvent();
        String eventName = epicEvent != null ? epicEvent.getEventName() :
                (normalEvent != null ? normalEvent.getEventName() : "无");
        return eventName.equals("无") ? "天坑乱斗增值包: /events" : "当前执行的事件: " + eventName;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        for (int i = 0; i < 18; i++) {
            buttons.put(i, new EventPreviewButton(i, false));
        }
        for (int i = 0; i < 9; i++) {
            buttons.put(18 + i, new EventPreviewButton(i, true));
        }
        return buttons;
    }

    @Override
    public boolean isAutoUpdate() {
        return true;
    }
}
