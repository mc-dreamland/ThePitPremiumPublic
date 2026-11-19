package cn.charlotte.pit.enchantment.type.rage;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.parm.listener.IPlayerDamaged;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.atomic.AtomicBoolean;

public class BrakesEnchant extends AbstractEnchantment implements IAttackEntity, IPlayerDamaged {

    @Override
    public String getEnchantName() {
        return "停下!";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "brakes";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RAGE;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7攻击目标的玩家如果拥有 &f速度&7 且效果为 &fI&7 级及以上, /s&7将其效果等级降至&f I&7 级."
                + (enchantLevel > 1 ? "/s&7高处玩家攻击你时减少 &9" + (enchantLevel == 2 ? "5" : "10") + "%  &7" : "");
    }

    @Override
    public void handleAttackEntity(
            int enchantLevel,
            Player attacker,
            Entity target,
            double damage,
            AtomicDouble finalDamage,
            AtomicDouble boostDamage,
            AtomicBoolean cancel
    ) {
        if (!(target instanceof Player targetPlayer)) {
            return;
        }

        for (PotionEffect effect : targetPlayer.getActivePotionEffects()) {
            if (effect.getType() == PotionEffectType.SPEED && effect.getAmplifier() > 0) {
                PlayerUtil.addEffect(targetPlayer, new PotionEffect(PotionEffectType.SPEED, effect.getDuration(), 0));
            }
        }
    }

    @Override
    public void handlePlayerDamaged(
            int enchantLevel,
            Player myself,
            Entity attacker,
            double damage,
            AtomicDouble finalDamage,
            AtomicDouble boostDamage,
            AtomicBoolean cancel
    ) {
        if (attacker instanceof Player attackerPlayer && attackerPlayer.getLocation().getY() > myself.getLocation().getY()) {
            boostDamage.set(boostDamage.get() * (1 - (enchantLevel == 2 ? 0.05 : 0.1)));
        }
    }
}
