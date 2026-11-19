package cn.charlotte.pit.perk.type.bundles.tothemoon;

import cn.charlotte.pit.perk.BotKillEffect;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitAssistEvent;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.*;
import cn.charlotte.pit.util.PlayerUtil;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

@AutoRegister
@BotKillEffect(BotKillEffect.Kind.REWARD)
public class SuperStreaker extends TypedPerk<PerksBundlesEnum> implements Listener, Streakable {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "超级连杀者";
    }

    @Override
    public Material getIcon() {
        return Material.FEATHER;
    }

    @Override
    public double requireCoins() {
        return 20000.0;
    }

    @Override
    public double requireRenown(int level) {
        return 0.0;
    }

    @Override
    public int requirePrestige() {
        return 14;
    }

    @Override
    public int requireLevel() {
        return 80;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> desc = new ArrayList<>();
        desc.add("&7此天赋每 &c10 连杀 &7触发一次.");
        desc.add("");
        desc.add("&7触发时: ");
        desc.add(" &a▶ &7增加 &b50经验 &7, 且累计增加 &b+5% &7经验(最高增加50%)");
        return desc;
    }

    @Override
    public int getMaxLevel() {
        return 1;
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

    @EventHandler
    public void onAssist(PitAssistEvent event) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getAssist().getUniqueId());
        if (profile == null) return;

        if (PlayerUtil.hasEquipPerk(event.getAssist(), PerksBundlesEnum.SUPER_STREAKER.getPerk())) {
            if (profile.getStreakKills() >= 10) {
                double numbers = profile.getStreakKills() / 10;
                double add = Math.min(numbers * 0.05, 0.5);
                event.getResourceManager().addExpModifier((1.0 + add), getDisplayName());
                event.getResourceManager().addExp(50, getDisplayName());
//                event.setExp(event.getExp() * (1.0 + add) + 50);
            }
        }
    }

    @Override
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        if (profile == null) return;

        if (profile.getStreakKills() >= 10) {
            double numbers = profile.getStreakKills() / 10;
            double add = Math.min(numbers * 0.05, 0.5);
            resourceManager.addExpModifier(1 + (1.0 + add), getDisplayName());
            resourceManager.addExp(50, getDisplayName());
        }
    }

    @Override
    public int getActiveStreak() {
        return 10;
    }
}
