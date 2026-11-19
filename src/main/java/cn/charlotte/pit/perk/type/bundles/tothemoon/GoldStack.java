package cn.charlotte.pit.perk.type.bundles.tothemoon;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.event.PitStreakKillChangeEvent;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.*;
import cn.charlotte.pit.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

@AutoRegister
public class GoldStack extends TypedPerk<PerksBundlesEnum> implements Listener, Streakable {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "黄金助推器";
    }

    @Override
    public Material getIcon() {
        return Material.GOLD_INGOT;
    }

    @Override
    public double requireCoins() {
        return 25000.0;
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
        return 90;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> description = new ArrayList<>();
        description.add("&7此天赋每 &c10 连杀 &7触发一次.");
        description.add("");
        description.add("&7触发时: ");
        description.add("  &a▶ &7永久增加击杀&6 0.1金币 &7奖励, 直至下一次精通升级,");
        description.add("  &7最高可堆叠至 &60.5金币&7, 每次精通将提升&6 0.1金币&7 上限");
        description.add("  &6金币奖励&7 将会在任何时候生效");
        return description;
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
        // 无额外逻辑
    }

    @Override
    public void onPerkInactive(Player player) {
        // 无额外逻辑
    }

    @EventHandler
    public void onKill(PitKillEvent event) {
        Player player = event.getKiller();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getKiller().getUniqueId());
        if(profile == null || !PlayerUtil.hasEquipPerk(player,PerksBundlesEnum.GOLD_STACK.getPerk())) return;
        event.getResourceManager().getGoldAdditions().put("黄金助推器", profile.getGoldStackAddon());
    }

    @EventHandler
    public void onStreakChange(PitStreakKillChangeEvent event) {
        PlayerProfile profile = event.getPlayerProfile();
        Player player = Bukkit.getPlayer(event.getPlayerProfile().getPlayerUUID());
        if(profile == null || !PlayerUtil.hasEquipPerk(player,PerksBundlesEnum.GOLD_STACK.getPerk())) return;
        if (((int) event.getTo()) % 10 == 0) {
            double newAddon = Math.min(profile.getGoldStackMax(), profile.getGoldStackAddon() + 0.1);
            profile.setGoldStackAddon(newAddon);
        }
    }

    @Override
    public int getActiveStreak() {
        return 10;
    }
}
