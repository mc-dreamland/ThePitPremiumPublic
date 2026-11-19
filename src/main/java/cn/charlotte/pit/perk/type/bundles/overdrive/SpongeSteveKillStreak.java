package cn.charlotte.pit.perk.type.bundles.overdrive;

import cn.charlotte.pit.event.PitStreakKillChangeEvent;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.*;
import cn.charlotte.pit.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/2/21 14:28
 */
@AutoRegister
public class SpongeSteveKillStreak extends TypedPerk<PerksBundlesEnum> implements Listener, Streakable {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "海绵蒂夫";
    }

    @Override
    public Material getIcon() {
        return Material.SPONGE;
    }

    @Override
    public double requireCoins() {
        return 12000;
    }

    @Override
    public double requireRenown(int level) {
        return 0;
    }

    @Override
    public int requirePrestige() {
        return 0;
    }

    @Override
    public int requireLevel() {
        return 70;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> list = new ArrayList<>();
        list.add("&7此天赋每 &c25 连杀 &7触发一次.");
        list.add(" ");
        list.add("&7触发时:");
        list.add("  &a▶ &7立刻获得 &615❤ 伤害吸收");
        return list;
    }

    @Override
    public int getMaxLevel() {
        return 0;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.KILL_STREAK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.KILL_STREAK;
    }

    @Override
    public void onPerkActive(Player player) {

    }

    @Override
    public void onPerkInactive(Player player) {

    }

    //KillStreak
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onStreak(PitStreakKillChangeEvent event) {
        Player myself = Bukkit.getPlayer(event.getPlayerProfile().getPlayerUUID());
        if (myself == null || !myself.isOnline()) {
            return;
        }
        if (!PlayerUtil.hasEquipPerk(myself, this)) {
            return;
        }
        //trigger check (every X streak)
        int streak = 25;
        if (Math.floor(event.getFrom()) % streak != 0 && Math.floor(event.getTo()) % streak == 0) {
            double heart = myself.getAbsorptionAmount();
            if (heart >= 30) {
                return;
            } else {
                heart = 30;
            }
//            myself.setAbsorptionAmount(heart + 30);
            PlayerUtil.setAbsorption(myself, heart);
        }
    }

    @Override
    public int getActiveStreak() {
        return 25;
    }
}
