package cn.charlotte.pit.events.impl;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.util.chat.CC;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class ExperienceblessedlandEvents implements INormalEvent {
    @Override
    public String getEventInternalName() {
        return "experience_blessed_land";
    }

    @Override
    public String getEventName() {
        return "经验福地";
    }

    @Override
    public List<String> getLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&6&l>> &b经验福地 &6&l<<");
        lore.add("");
        lore.add("&6&l事件机制");
        lore.add("&7- 在中坑获得的XP翻倍!(助攻以及击杀)");
        lore.add("&c- 超过精通等级 X 时 增益减半");
        lore.add("");
        return lore;
    }

    @Override
    public int requireOnline() {
        return (NewConfiguration.getEventOnlineRequired().get(getEventInternalName()) == null ? 5 : NewConfiguration.getEventOnlineRequired().get(getEventInternalName()));
    }

    @Override
    public void onActive() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            PlayerProfile profile = Util.getPitProfile(player);
            profile.getEventData().callHistoricalEvents(getEventName(), 1);
        });
        CC.boardCast(CC.translate("&b&l经验福地！ &7现在你可以在中坑内获得翻倍的&b经验！"));
        scheduleEventDeactivation();
    }

    @Override
    public void onInactive() {
        CC.boardCast(CC.translate("&b&l经验福地！ &7结束了"));
    }

    private void scheduleEventDeactivation() {
        Bukkit.getScheduler().runTaskLater(
                ThePit.getInstance(),
                this::deactivateEvent, // 这里调用deactivateEvent方法
                20L * 60 * 5 // 计算延迟时间（20 ticks/秒 * 60秒/分钟 * 分钟数）

        );
    }

    private void deactivateEvent() {
        ThePit.getInstance().getEventFactory().inactiveEvent(this);
        onInactive();
    }
}