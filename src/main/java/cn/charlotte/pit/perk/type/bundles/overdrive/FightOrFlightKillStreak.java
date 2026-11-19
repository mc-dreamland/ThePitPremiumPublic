package cn.charlotte.pit.perk.type.bundles.overdrive;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitStreakKillChangeEvent;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.*;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: Misoryan
 * @Created_In: 2021/2/21 13:58
 */
@AutoRegister
public class FightOrFlightKillStreak extends TypedPerk<PerksBundlesEnum> implements Listener, Streakable {

    private static final HashMap<UUID, Cooldown> strength = new HashMap<>();

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "战与不战";
    }

    @Override
    public Material getIcon() {
        return Material.FEATHER;
    }

    @Override
    public double requireCoins() {
        return 5000;
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
        return 50;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> list = new ArrayList<>();
        list.add("&7此天赋每 &c5 连杀 &7触发一次.");
        list.add(" ");
        list.add("&7触发时:");
        list.add("  &7如自身生命值低于上限的 &c50% &7:");
        list.add("  &f▶ &7立刻获得 &b速度 I &f(00:07)");
        list.add("  &f▶ &7立刻获得 &3抗性提升 I &f(00:07)");
        list.add(" ");
        list.add("  &7反之:");
        list.add("  &f▶ &7立刻获得效果 &c攻击伤害 +20% &f(00:07)");
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
        int streak = 5;
        if (Math.floor(event.getFrom()) % streak != 0 && Math.floor(event.getTo()) % streak == 0) {
            if (myself.getHealth() / myself.getMaxHealth() < 0.5) {
                PlayerUtil.addEffect(myself, new PotionEffect(PotionEffectType.SPEED, 20 * 7, 0));
                PlayerUtil.addEffect(myself, new PotionEffect(PotionEffectType.RESISTANCE, 20 * 7, 0));
            } else {
                strength.put(myself.getUniqueId(), new Cooldown(7, TimeUnit.SECONDS));
            }
        }
    }

    @Override
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        strength.putIfAbsent(attacker.getUniqueId(), new Cooldown(0));
        if (!strength.get(attacker.getUniqueId()).hasExpired()) {
            boostDamage.getAndAdd(0.2);
        }
    }

    @Override
    public void handleShootEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        strength.putIfAbsent(attacker.getUniqueId(), new Cooldown(0));
        if (!strength.get(attacker.getUniqueId()).hasExpired()) {
            boostDamage.getAndAdd(0.2);
        }
    }

    @Override
    public int getActiveStreak() {
        return 5;
    }
}
