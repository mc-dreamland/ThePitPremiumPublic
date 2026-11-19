package cn.charlotte.pit.events.impl;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.random.RandomUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/2/8 21:25
 */
public class EveOneBountyEvents implements INormalEvent {
    @Override
    public String getEventInternalName() {
        return "everyone_bounty_event";
    }

    @Override
    public String getEventName() {
        return "全员通缉";
    }

    @Override
    public List<String> getLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&6&l>> &e全员通缉 &6&l<<");
        lore.add("");
        lore.add("&6&l事件机制");
        lore.add("&7- 全体玩家增加 100 赏金");
        lore.add("&7- 1%概率触发 全体玩家增加 250 赏金");
        lore.add("");
        return lore;
    }

    @Override
    public int requireOnline() {
        return NewConfiguration.getEventOnlineRequired().get(getEventInternalName());
    }

    @Override
    public void onActive() {
        int gold = 100;
        if (RandomUtil.hasSuccessfullyByChance(0.01)) {
            gold = 250;
        }
        Bukkit.broadcastMessage(CC.translate("&6&l全员通缉! &7所有人现在都被以 &6" + gold + "g &7的赏金悬赏了!"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            if (profile.isInArena()) {
                profile.setBounty(profile.getBounty() + gold);
            }
            profile.getEventData().callHistoricalEvents(getEventName(), 1);
        }
        ThePit.getInstance()
                .getEventFactory()
                .inactiveEvent(this);
    }

    @Override
    public void onInactive() {

    }
}
