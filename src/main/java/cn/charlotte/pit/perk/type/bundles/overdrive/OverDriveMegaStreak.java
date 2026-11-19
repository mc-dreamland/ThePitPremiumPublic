package cn.charlotte.pit.perk.type.bundles.overdrive;

import cn.charlotte.pit.bot.PitBotManager;

import cn.charlotte.pit.perk.BotKillEffect;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.event.PitStreakKillChangeEvent;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.*;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/14 23:37
 */
@AutoRegister
@BotKillEffect(BotKillEffect.Kind.REWARD)
public class OverDriveMegaStreak extends TypedPerk<PerksBundlesEnum> implements Listener, MegaStreak {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "&e超速传动";
    }

    @Override
    public Material getIcon() {
        return Material.BLAZE_POWDER;
    }

    @Override
    public double requireCoins() {
        return 0;
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
        return 0;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.MEGA_STREAK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.STREAK;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> list = new ArrayList<>();
        list.add("&7激活要求连杀数: &c50 连杀");
        list.add(" ");
        list.add("&7激活后:");
        list.add("  &a▶ &7获得 &b速度 I &6&o持续时间无限");
        list.add("  &a▶ &7击杀获得 &6+50% 硬币");
        list.add("  &a▶ &7击杀获得 &b+100% 经验值");
        list.add("  &c▶ &7激活后每击杀5名玩家,受击时");
        list.add("  &7额外受到 &c+0.1❤ &7的&c必中&7伤害 (可堆叠,无上限)");
        list.add("  &c(必中伤害无法被免疫与抵抗)");
        list.add(" ");
        list.add("&7激活后死亡时:");
        list.add("  &a▶ &7获得 &b4,000 经验值");
        return list;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public void onPerkActive(Player player) {

    }

    @Override
    public void onPerkInactive(Player player) {
        player.removePotionEffect(PotionEffectType.SPEED);
    }

    //MegaStreak
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onStreak(PitStreakKillChangeEvent event) {
        if (event.isBotKill()) return;
        Player myself = Bukkit.getPlayer(event.getPlayerProfile().getPlayerUUID());
        if (myself == null || !myself.isOnline()) {
            return;
        }
        if (!PlayerUtil.hasEquipPerk(myself, this)) {
            return;
        }
        int trigger = 50;
        //trigger check (get X streak)
        if (event.getFrom() < trigger && event.getTo() >= trigger) {
            CC.boardCast(MessageType.COMBAT, "&c&l超级连杀! " + event.getPlayerProfile().getFormattedNameWithRoman() + " &7激活了 &e&l超速传动 &7!");
            Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8F, 1.5F));
        }
    }

    @Override
    @PlayerOnly
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        if (profile.getStreakKills() >= 50) {
            resourceManager.addGoldModifier(1.5, getDisplayName());
            resourceManager.addExpModifier(2, getDisplayName());
        }
    }

    @Override
    public void handleTick(int enchantLevel, Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (!profile.isInArena() && !PitBotManager.keepTrainingBotStreak(player, profile)) {
            profile.setStreakKills(0);
        }
        PlayerUtil.addEffectStreak(player, new PotionEffect(PotionEffectType.SPEED, 86400 * 20, 0), 50.0);
        PlayerUtil.addEffectStreak(player, new PotionEffect(PotionEffectType.GLOWING, 86400 * 20, 0), 50.0);
    }

    @Override
    public int loopTick(int enchantLevel) {
        return 3 * 20;
    }

    @Override
    public void handlePlayerBeKilledByEntity(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        if (profile.getStreakKills() >= 50) {
            if (CombatListener.isNight()) {
                myself.sendMessage(CC.translate("&c夜深了 &7无法获得&b4000 &7经验值"));
                return;
            }
            profile.setExperience(profile.getExperience() + 4000);
            myself.sendMessage(CC.translate("&e&l超速传动! &7你获得了 &b4,000 &7经验值."));
        }
    }


    @Override
    public void handlePlayerDamaged(int enchantLevel, Player myself, Entity attacker, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        if (profile.getStreakKills() > 50 && profile.getChosePerk()[4] != null && profile.perkEqualsChosePerk(this.getInternalPerkName(), 4)) {
            double tier = (profile.getStreakKills() - 50 - (profile.getStreakKills() - 50) % 5) / 5;
            if (myself.getHealth() > tier * 0.2) {
                myself.setHealth(Math.max(0.1, myself.getHealth() - tier * 0.2));
            } else {
//                cancel.set(true);
                finalDamage.addAndGet(2000.0);
            }
        }
    }

    @Override
    public int getStreakNeed() {
        return 50;
    }

    @Override
    public List<String> getDescriptionInRenownShop(Player player) {
        return List.of();
    }
}
