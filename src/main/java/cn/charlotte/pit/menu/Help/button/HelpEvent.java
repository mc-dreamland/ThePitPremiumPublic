package cn.charlotte.pit.menu.Help.button;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.activity.ActivityType;
import cn.charlotte.pit.events.IEpicEvent;
import cn.charlotte.pit.events.IEvent;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelpEvent extends Menu {

    public HelpEvent() {
    }

    @Override
    public String getTitle(Player player) {
        return "事件介绍";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttonMap = new HashMap<>();

        List<IEvent> allEvents = new ArrayList<>();

        for (INormalEvent event : ThePit.getInstance().getEventFactory().getNormalEvents()) {
            ActivityType type = event.getEventType();
            if (type == ActivityType.NULL || type == ThePit.getInstance().getActivityFactory().getFactoryType()) {
                allEvents.add(event);
            }
        }
        for (IEpicEvent event : ThePit.getInstance().getEventFactory().getEpicEvents()) {
            ActivityType type = event.getEventType();
            if (type == ActivityType.NULL || type == ThePit.getInstance().getActivityFactory().getFactoryType()) {
                allEvents.add(event);
            }
        }
        int slot = 0;
        for (IEvent event : allEvents) {
            if (slot >= 54) break;

            buttonMap.put(slot, createEventButton(event));
            slot++;
        }

        return buttonMap;
    }

    private Button createEventButton(IEvent event) {
        return new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                List<String> lore = new ArrayList<>();
                String eventName = event.getEventName();

                String eventType = (event instanceof IEpicEvent) ? "&6[大型] " : "&a[普通] ";

                lore.add(CC.translate("&7事件名称: " + eventType + "&e" + eventName));
                if (event.requireOnline() >= 999) {
                    lore.add(CC.translate("&c该事件暂未启用 ouo"));
                } else {
                    lore.add(CC.translate("&7所需在线: &a" + event.requireOnline() + "人"));
                }
                ActivityType ac = event.getEventType();
                if (ac != ActivityType.NULL) {
                    lore.add(CC.translate(String.format("&7[%s&7]活动限定", ac.getName())));
                }
                lore.add("");

                for (String line : event.getLore()) {
                    lore.add(CC.translate(line));
                }

                Material material = (event instanceof IEpicEvent) ?
                        Material.ENCHANTED_BOOK : Material.BOOK;

                return new ItemBuilder(material)
                        .name("&e" + eventName)
                        .lore(lore)
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return false;
            }
        };
    }

    @Override
    public int getSize() {
        return 6 * 9;
    }
}