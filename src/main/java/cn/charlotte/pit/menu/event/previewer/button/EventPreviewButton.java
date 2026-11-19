package cn.charlotte.pit.menu.event.previewer.button;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.events.EventsHandler;
import cn.charlotte.pit.events.IEpicEvent;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.util.Pair;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.time.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @Creator Misoryan
 * @Date 2021/5/5 12:13
 */
@Slf4j
@RequiredArgsConstructor
public class EventPreviewButton extends Button {

    private final int page;
    private final boolean major;

    private static String getEventNameByInternalName(String internal, boolean isEpic) {
        if (isEpic) {
            for (IEpicEvent epicEvent : ThePit.getInstance().getEventFactory().getEpicEvents()) {
                if (epicEvent.getEventInternalName().equalsIgnoreCase(internal)) {
                    return epicEvent.getEventName();
                }
            }
        } else {
            for (INormalEvent normalEvent : ThePit.getInstance().getEventFactory().getNormalEvents()) {
                if (normalEvent.getEventInternalName().equalsIgnoreCase(internal)) {
                    return normalEvent.getEventName();
                }
            }
        }
        return "&c???";
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        List<String> lines = new ArrayList<>();

        EventsHandler.refreshEvents();
        var eventQueue = EventsHandler.eventQueue;

        List<Pair<Pair<String, Boolean>, Cooldown>> filteredEvents = new ArrayList<>();
        for (int i = 0; i < eventQueue.size(); i++) {
            try {
                var eventPair = eventQueue.get(i);
                if (eventPair.key().value() == major) {
                    filteredEvents.add(eventPair);
                }
            } catch (IndexOutOfBoundsException ignored) {
                break;
            }
        }

        int startIndex = page * 5;
        int endIndex = Math.min(startIndex + 5, filteredEvents.size());

        if (startIndex >= filteredEvents.size()) {
            return new ItemBuilder(Material.BARRIER)
                    .name("&c没有更多事件")
                    .lore("&7当前页没有" + (major ? "大型" : "小型") + "事件")
                    .build();
        }
        int x = 0;
        for (int i = startIndex; i < endIndex; i++) {
            try {
                var eventPair = filteredEvents.get(i);
                String internalName = eventPair.key().key();
                Cooldown cooldown = eventPair.value();

                long timeUntil = cooldown.getExpire() - System.currentTimeMillis();

                String eventName = getEventNameByInternalName(internalName, major);

                String tag;
                if (major) {
                    tag = "&e" + TimeUtil.millisToRoundedTime(timeUntil).replace(" ", "") +
                            "后: " + eventName;
                } else {
                    tag = "&e" + (i + 1) + ". " + eventName +
                            " &7(" + TimeUtil.millisToRoundedTime(timeUntil).replace(" ", "") + "后)";
                }
                lines.add(tag);
            } catch (Exception ignored) {
                lines.add("&c获取事件信息失败");
            }
        }

        while (lines.size() < 5) {
            lines.add("&7- 暂无事件 -");
        }

        ItemBuilder itemBuilder = new ItemBuilder(Material.OAK_SIGN)
                .amount(page + 1)
                .name("&7即将到来的" + (major ? "大" : "小") + "型事件")
                .lore(lines);

        if (major) itemBuilder.shiny();

        return itemBuilder.build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {

    }
}