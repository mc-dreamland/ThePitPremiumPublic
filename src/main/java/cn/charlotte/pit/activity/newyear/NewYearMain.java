package cn.charlotte.pit.activity.newyear;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.ActivityData;
import cn.charlotte.pit.activity.IActivity;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.util.random.RandomUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class NewYearMain implements IActivity, Listener {

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());
    }

    @Override
    public void end(ActivityData activityData) {

    }

    @EventHandler
    public void onKill(PitKillEvent event) {
        if(CombatListener.isNight()) return;
        Player killer = event.getKiller();
        PlayerProfile killProfile = Util.getPitProfile(killer);
        NewYearData data = killProfile.getActivityPlayerData().getNewYearData();
        Integer level = data.getPerkMap().get("PotionKill");
        if (RandomUtil.hasSuccessfullyByChance(0.001))
            Util.addItemOfMail(PitItems.NEW_YEAR_LUCKY_BAG.toItemStack(), killer, null);
        if (level == null || level <= 0) return;
        data.addKill();
        event.getMsg().add(String.format("&e+%s活动点数", data.addCoins(level)));
        if (RandomUtil.hasSuccessfullyByChance(level * 0.02)) {
            event.getMsg().add(String.format("&b+%s特殊点数", 1));
            data.addPotions();
        }
    }

}