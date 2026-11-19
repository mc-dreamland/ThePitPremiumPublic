package cn.charlotte.pit.perk.type.bundles.tothemoon;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.event.PitStreakKillChangeEvent;
import cn.charlotte.pit.perk.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class XPStack extends TypedPerk<PerksBundlesEnum> implements Listener, Streakable {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "经验助推器";
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
        List<String> desc = new ArrayList<>();
        desc.add("&7此天赋每 &c10 连杀 &7触发一次.");
        desc.add("");
        desc.add("&7触发时: ");
        desc.add("  &a▶ &7永久增加击杀&b 0.05经验 &7奖励, 直至下一次精通, 最高");
        desc.add("  &7可堆叠至 &b1经验&7, 每次精通将提升&b 0.2经验&7 上限");
        desc.add("  &b经验奖励&7 将会在任何时候");
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
        // 无需操作
    }

    @Override
    public void onPerkInactive(Player player) {
        // 无需操作
    }

    @EventHandler
    public void onKill(PitKillEvent event) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getKiller().getUniqueId());
        if (profile == null) return;
        event.getResourceManager().getExpAdditions().put("经验助推器", profile.getXpStackAddon());
    }

    @EventHandler
    public void onStreakChange(PitStreakKillChangeEvent event) {
        PlayerProfile profile = event.getPlayerProfile();
        if (((int) event.getTo()) % 10 == 0) {
            double newValue = Math.min(profile.getXpStackMax(), profile.getXpStackAddon() + 0.05);
            profile.setXpStackAddon(newValue);
        }
    }

    @Override
    public int getActiveStreak() {
        return 10;
    }
}
