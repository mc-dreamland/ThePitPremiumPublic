package cn.charlotte.pit.perk.type.bundles.beastmode;

import cn.charlotte.pit.bot.PitBotManager;

import cn.charlotte.pit.perk.BotKillEffect;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.event.PitDamageEvent;
import cn.charlotte.pit.event.PitStreakKillChangeEvent;
import cn.charlotte.pit.item.PitItems;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Creator Misoryan
 * @Date 2021/4/27 17:11
 */
@AutoRegister
@BotKillEffect(BotKillEffect.Kind.REWARD)
public class BeastModeMegaStreak extends TypedPerk<PerksBundlesEnum> implements Listener, MegaStreak {

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
            CC.boardCast(MessageType.COMBAT, "&c&l超级连杀! " + event.getPlayerProfile().getFormattedNameWithRoman() + " &7激活了 &a&l野兽模式 &7!");
            Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8F, 1.5F));

            ItemStack helmet = myself.getInventory().getHelmet();
            if (helmet == null || helmet.getType().equals(Material.AIR) || helmet.getType().equals(Material.DIAMOND_HELMET)) {
                myself.getInventory().setHelmet(
                        PitItems.BEAST_MODE_HELMET.toItemStack()
                );
            } else {
                myself.getInventory().addItem(PitItems.BEAST_MODE_HELMET.toItemStack());
            }
        }
    }

    @Override
    public void handleTick(int enchantLevel, Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (!profile.isInArena() && !PitBotManager.keepTrainingBotStreak(player, profile)) {
            profile.setStreakKills(0);
        }
        PlayerUtil.addEffectStreak(player, new PotionEffect(PotionEffectType.GLOWING, 86400 * 20, 0), 50.0);
    }

    @Override
    public int loopTick(int enchantLevel) {
        return 3 * 20;
    }

    @Override
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId());
        if (profile.getStreakKills() >= 50) {
            boostDamage.getAndAdd(0.25);
        }
    }

    @Override
    @PlayerOnly
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        if (profile.getStreakKills() >= 50) {
            resourceManager.addGoldModifier(1.5, getDisplayName());
            resourceManager.addExpModifier(1.75, getDisplayName());
        }
    }

    @Override
    public void handleShootEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId());
        if (profile.getStreakKills() >= 50) {
            boostDamage.getAndAdd(0.25);
        }
    }

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "&a野兽模式";
    }

    @Override
    public Material getIcon() {
        return Material.DIAMOND_HELMET;
    }

    @Override
    public double requireCoins() {
        return 10000;
    }

    @Override
    public double requireRenown(int level) {
        return 10;
    }

    @Override
    public int requirePrestige() {
        return 3;
    }

    @Override
    public int requireLevel() {
        return 30;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> list = new ArrayList<>();
        list.add("&7激活要求连杀数: &c50 连杀");
        list.add(" ");
        list.add("&7激活后:");
        list.add("  &a▶ &7获得一个临时的 &b钻石头盔");
        list.add("  &a▶ &7攻击造成的伤害 &c+25%");
        list.add("  &a▶ &7击杀获得 &6+50% 硬币");
        list.add("  &a▶ &7击杀获得 &b+75% 经验值");
        list.add("  &c▶ &7激活后每击杀5名玩家,受到的伤害 &c+0.1颗心");
        list.add(" ");
        list.add("&7激活后死亡时:");
        list.add("  &6▶ &7保留获得的 &b钻石头盔");
        return list;
    }

    @Override
    public List<String> getDescriptionInRenownShop(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7包括以下连杀天赋:");
        lines.add("&8- &aAAA级牛排");
        lines.add("&8- &a休养与恢复");
        lines.add("&8- &a坚韧肌肤"); //ToughSkin
        lines.add("&8- &a战术撤退"); //Tactical Retreat
        lines.add("&8- &a怪物"); //Monster
        lines.add(" ");
        lines.add("&7超级连杀天赋: &a野兽模式"); //BeastMode
        return lines;
    }

    @Override
    public int getMaxLevel() {
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
    public void onPerkActive(Player player) {

    }

    @Override
    public void onPerkInactive(Player player) {

    }

    @Override
    public void handlePlayerBeKilledByEntity(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        if (profile.getStreakKills() >= 50) {
            if (CombatListener.isNight()) {
                myself.sendMessage(CC.translate("&c夜深了 &7无法获得野兽头盔"));
                return;
            }
            Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
                myself.getInventory().addItem(
                        PitItems.BEAST_MODE_HELMET_AWARD.toItemStack()
                );
                myself.sendMessage(CC.translate("&a&l野兽模式! &7你获得了 &b钻石头盔 &7."));
            }, 20L);
        }
    }

    @Override
    public int getStreakNeed() {
        return 50;
    }

    @EventHandler
    public void onDamage(PitDamageEvent event) {
        if (!(event.getTarget() instanceof Player player)) {
            return;
        }
        PlayerProfile playerProfile = Util.getPitProfile(player);
        if (PerksBundlesEnum.BEAST_MODE.getPerk().hasEquip(playerProfile) && playerProfile.getStreakKills() >= 50) {
            double boostLevel = Math.floor((playerProfile.getStreakKills() - 50) / 5);
            event.setDamage(event.getDamage() + 0.1 * boostLevel);
        }
    }
}
