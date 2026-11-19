package cn.charlotte.pit.perk.type.boosts.based;

import cn.charlotte.pit.perk.BotKillEffect;
import cn.charlotte.pit.perk.BotCombatEffect;
import cn.charlotte.pit.bot.PitBotManager;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.medal.impl.challenge.VampireMedal;
import cn.charlotte.pit.parm.type.BowOnly;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.Utils;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/1 22:22
 */
@BotKillEffect(BotKillEffect.Kind.KILL_EFFECT)
public class VampirePerk extends TypedPerk<PerksBoostsEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "吸血鬼";
    }

    @Override
    public Material getIcon() {
        return Material.SPIDER_EYE;
    }

    @Override
    public double requireCoins() {
        return 4000;
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
        return 55;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.PERK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.BOOST;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7攻击击中其他玩家时恢复自身 &c0.5❤ &7生命值.");
        lines.add("&7如果攻击为箭矢命中,则恢复量根据你的弓箭蓄力条来计算最大&c1.5❤ &7.");
        lines.add("&7击杀敌人额外获得 &c生命恢复 I &f(00:08) &7效果,");
        lines.add("&7但&c不会获得回复道具&7.");
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

    @Override
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if ("rage_pit".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
            return;
        }
        if (PlayerUtil.hasEquipPerk(attacker, PerksBoostsEnum.RAMBO.getPerk())) {
            return;
        }
        if (attacker.getInventory().getItemInMainHand().getType() == Material.FISHING_ROD) {
            return;
        }
        if (attacker.hasMetadata("grim_reaper_master")) {
            return;
        }
        if (!PitBotManager.isBot(target)) {
            new VampireMedal().addProgress(PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId()), (int) Math.max(0, attacker.getMaxHealth() - Math.min(attacker.getHealth() + 1, attacker.getMaxHealth())));
        }
        attacker.setHealth(Math.min(attacker.getHealth() + 1, attacker.getMaxHealth()));
    }

    @Override
    @BowOnly
    @PlayerOnly
    @BotCombatEffect
    public void handleShootEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if ("rage_pit".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
            return;
        }
        if (PlayerUtil.hasEquipPerk(attacker, PerksBoostsEnum.RAMBO.getPerk())) {
            return;
        }
        if (attacker.getInventory().getItemInMainHand().getType() == Material.FISHING_ROD) {
            return;
        }
        if (target instanceof Player targetPlayer
                && Utils.getEnchantLevel(targetPlayer.getInventory().getItemInMainHand(), "BulletTime") > 0 && targetPlayer.isBlocking()) {
            return;
        }
        Float force = Util.getPitProfile(attacker).getForce();
        if (force == null) {
            force = 0f;
        }
        double healthToAdd;
        if (!PlayerUtil.isVenom(attacker) && !PlayerUtil.isEquippingSomber(attacker) && Utils.getEnchantLevel(attacker.getInventory().getItemInMainHand(), "mega_long_bow_enchant") > 0) {
            healthToAdd = 3.0;
        } else {
            if (force == 0) {
                healthToAdd = 0.0;
            } else if (force < 1.8) {
                healthToAdd = 1.0;
            } else if (force < 2.8) {
                healthToAdd = 2.0;
            } else {
                healthToAdd = 3.0;
            }
        }

        double newHealth = Math.min(attacker.getHealth() + healthToAdd, attacker.getMaxHealth());
        double actualHeal = newHealth - attacker.getHealth();

        if (actualHeal > 0) {
            if (!PitBotManager.isBot(target)) {
                new VampireMedal().addProgress(PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId()), (int) actualHeal);
            }
            attacker.setHealth(newHealth);
        }
    }

    @Override
    @PlayerOnly
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        if ("rage_pit".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
            return;
        }
        if (PlayerUtil.hasEquipPerk(myself, PerksBoostsEnum.RAMBO.getPerk())) {
            return;
        }
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            PlayerUtil.addEffect(myself, new PotionEffect(PotionEffectType.REGENERATION, 20 * 8, 0));
        });
    }
}
