package cn.charlotte.pit.perk.type.bundles.overdrive;

import cn.charlotte.pit.bot.BotReward;
import cn.charlotte.pit.util.level.LevelUtil;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitStreakKillChangeEvent;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.*;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
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
 * @Created_In: 2021/2/21 13:46
 */
@AutoRegister
public class ExpliciousKillStreak extends TypedPerk<PerksBundlesEnum> implements Listener, Streakable {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "目的明确";
    }

    @Override
    public Material getIcon() {
        return Material.EXPERIENCE_BOTTLE;
    }

    @Override
    public double requireCoins() {
        return 3000;
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
        return 30;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> list = new ArrayList<>();
        list.add("&7此天赋每 &c3 连杀 &7触发一次.");
        list.add(" ");
        list.add("&7触发时:");
        list.add("  &a▶ &7立刻获得 &b+12 经验值");
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
        PlayerProfile profile = event.getPlayerProfile();
        Player myself = Bukkit.getPlayer(event.getPlayerProfile().getPlayerUUID());
        if (myself == null || !myself.isOnline()) {
            return;
        }
        if (!PlayerUtil.hasEquipPerk(myself, this)) {
            return;
        }
        //trigger check (every X streak)
        int streak = 3;
        if (Math.floor(event.getFrom()) % streak != 0 && Math.floor(event.getTo()) % streak == 0) {
            double xp = event.isBotKill() ? BotReward.experienceWithinLevelCap(12,
                    LevelUtil.getLevelTotalExperience(profile.getPrestige(), 120) - profile.getExperience()) : 12;
            if (xp > 0) {
                profile.setExperience(profile.getExperience() + xp);
                if (event.isBotKill()) profile.applyExperienceToPlayer(myself);
                myself.sendMessage(CC.translate("&b目的明确 &7你获得了 &b" + (xp == 12 ? "12" : Double.toString(xp)) + " 经验值"));
            }
        }
    }

    @Override
    public int getActiveStreak() {
        return 3;
    }
}
