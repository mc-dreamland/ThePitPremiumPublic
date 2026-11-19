package cn.charlotte.pit.medal.impl.challenge;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitEnchBangEvent;
import cn.charlotte.pit.medal.AbstractMedal;
import cn.charlotte.pit.parm.AutoRegister;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@AutoRegister
public class PrecisionHuntMedal extends AbstractMedal implements Listener {

    @Override
    public String getInternalName() {
        return "precision_hunt_medal";
    }

    @Override
    public String getDisplayName(int level) {
        return "精准猎杀";
    }

    @Override
    public String getRequirementDescription(int level) {
        return "在单次事件[附魔爆发]中, 击杀三名幸运玩家获得";
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public int getProgressRequirement(int level) {
        return 1;
    }

    @Override
    public int getRarity(int level) {
        return 5;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public void handleProfileLoaded(PlayerProfile profile) {

    }

    @EventHandler
    public void onkill(PitEnchBangEvent e) {
        if (e.getKillList().size() != 1) return;
        Player player = Bukkit.getPlayer(e.getKillList().stream().toList().getFirst());
        if (player == null) return;
        addProgress(Util.getPitProfile(player),1);
    }
}
