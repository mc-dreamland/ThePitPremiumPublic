package cn.charlotte.pit.perk.type.bundles.uber;

import cn.charlotte.pit.bot.PitBotManager;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.*;
import cn.charlotte.pit.item.type.UberDrop;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.*;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@AutoRegister
public class UberStreak extends TypedPerk<PerksBundlesEnum> implements Listener, MegaStreak {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "&d登峰造极";
    }

    @Override
    public Material getIcon() {
        return Material.GOLDEN_SWORD;
    }

    @Override
    public double requireCoins() {
        return 500000.0;
    }

    @Override
    public double requireRenown(int level) {
        return 100.0;
    }

    @Override
    public int requirePrestige() {
        return 20;
    }

    @Override
    public int requireLevel() {
        return 120;
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
        list.add("&7激活要求连杀数: &c100 连杀");
        list.add(" ");
        list.add("&7激活后:");
        list.add("  &a▶ &7获得神话物品的几率提升至原来的 &d150%");
        list.add("  &c▶ &7每击杀20名玩家,受到的伤害 &c+5%");
        list.add("  &c▶ &7100连杀后,攻击未精通玩家造成的伤害 &c-40%");
        list.add("  &c▶ &7200连杀后,生命上限 &c-2❤");
        list.add("  &c▶ &7300连杀后,所有药水效果持续时间 &c-50%");
        list.add("  &c▶ &7400连杀后,&c无法恢复生命值");
        list.add(" ");
        list.add("&7激活后死亡时:");
        list.add("  &a▶ &7若连杀数大于400,获得一件 &d登峰造极掉落物");
        list.add(" ");
        list.add("&c注意:若在400连杀前死亡,受连杀助理保护的神话物品仍然会掉落生命!");
        return list;
    }

    @Override
    public List<String> getDescriptionInRenownShop(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7超级连杀天赋: &d登峰造极");
        return lines;
    }

    @Override
    public int getMaxLevel() {
        return 0;
    }

    @Override
    public void onPerkActive(Player player) {
    }

    @Override
    public void onPerkInactive(Player player) {
    }

    private boolean hasUber(Player player) {
        return PlayerUtil.hasEquipPerk(player, this);
    }

    @Override
    public int getStreakNeed() {
        return 100;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRegen(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            if (!profile.isLoaded() || !hasUber(player) || profile.getStreakKills() < 400) return;
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRegenCustom(PitRegainHealthEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (!profile.isLoaded() || !hasUber(player) || profile.getStreakKills() < 400) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(PitDamageEvent event) {
        Entity victim = event.getTarget();
        if (event.getAttacker() instanceof Player attacker && victim instanceof Player defender) {
            if (hasUber(attacker)) {
                PlayerProfile profile = Util.getPitProfile(attacker);
                if (profile.getStreakKills() >= 100 && Util.getPitProfile(defender).getPrestige() < 1) {
                    event.getBoostDamage().getAndAdd(-0.4);
                }
            }

            if (hasUber(defender)) {
                PlayerProfile profile = Util.getPitProfile(defender);
                event.getBoostDamage().getAndAdd(profile.getStreakKills() / 20 * 0.05);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStreakChange(PitStreakKillChangeEvent event) {
        if (event.isBotKill()) return;
        Player player = Bukkit.getPlayer(event.getPlayerProfile().getPlayerUUID());
        if (!hasUber(player)) return;

        if (event.getFrom() < 100 && event.getTo() >= 100) {
            CC.boardCast(MessageType.COMBAT, "&c&l超级连杀! " + event.getPlayerProfile().getFormattedNameWithRoman() + " &7激活了 &c&l登峰造极 &7!");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.5f);
            }
        }

        if (event.getFrom() < 200 && event.getTo() >= 200) {
            double newMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() - 4;
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.max(1, newMaxHealth));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onKilled(PitKillEvent event) {
        if (!(event.getTarget() instanceof Player victim) || !victim.isOnline() || !hasUber(victim)) return;

        PlayerProfile profile = Util.getPitProfile(victim);
        if (profile.getStreakKills() < 400) return;
        if (CombatListener.isNight()) {
            victim.sendMessage("夜深了,已无法掉落UBER!");
            return;
        }
        Calendar now = Calendar.getInstance();
        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(profile.getTodayCompletedUberLastRefreshed());

        if (now.get(Calendar.YEAR) != last.get(Calendar.YEAR) || now.get(Calendar.DAY_OF_YEAR) != last.get(Calendar.DAY_OF_YEAR)) {
            profile.setTodayCompletedUberLastRefreshed(System.currentTimeMillis());
            profile.setTodayCompletedUber(0);
        }

        int times = profile.getTodayCompletedUber() + 1;
        profile.setTodayCompletedUber(times);

        switch (times) {
            case 2 -> {
                if (profile.getRenown() < 5) {
                    victim.sendMessage(CC.translate("&c登峰造极! &c你的声望不足以支付你今日的第二次&d登峰造极"));
                    return;
                }
                profile.changeRenown(profile.getRenown() - 5);
            }
            case 3 -> {
                if (profile.getRenown() < 10) {
                    victim.sendMessage(CC.translate("&c登峰造极! &c你的声望不足以支付你今日的第三次&d登峰造极"));
                    return;
                }
                profile.changeRenown(profile.getRenown() - 10);
            }
            case 1 -> {
            } // free
            default -> {
                victim.sendMessage(CC.translate("&c登峰造极! &c你今日完成次数已达上限!"));
                return;
            }
        }

        UberDrop item = new UberDrop();
        if (InventoryUtil.isInvFull(victim.getInventory())) {
            victim.sendMessage(CC.translate("&c由于你的背包已满, 物品已&4掉落至地面&c."));
            victim.getWorld().dropItemNaturally(victim.getLocation(), item.toItemStack());
        } else {
            victim.getInventory().addItem(item.toItemStack());
        }

        victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(profile.getMaxHealth());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotionAdd(PitPotionAddEvent event) {
        Player player = event.getPlayer();
        if (!hasUber(player)) return;

        PlayerProfile profile = Util.getPitProfile(player);
        if (profile.getStreakKills() < 300) return;

        PotionEffect effect = event.getEffect();
        if (!effect.isAmbient()) return;
        if (effect.getType() == PotionEffectType.NIGHT_VISION) return;
        event.setCancelled(true);
        player.removePotionEffect(effect.getType());
        player.addPotionEffect(new PotionEffect(effect.getType(), effect.getDuration() / 2, effect.getAmplifier()));
    }

    @EventHandler
    public void onSpawn(PitPlayerSpawnEvent event) {
        Util.getPitProfile(event.getPlayer()).setStreakKills(0.0);
    }

    @Override
    public void handleTick(int enchantLevel, Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (!profile.isInArena() && !PitBotManager.keepTrainingBotStreak(player, profile)) {
            profile.setStreakKills(0);
        }
        PlayerUtil.addEffectStreak(player, new PotionEffect(PotionEffectType.GLOWING, 86400 * 20, 0), getStreakNeed());
    }

    @Override
    public int loopTick(int enchantLevel) {
        return 3 * 20;
    }
}
