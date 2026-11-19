package cn.charlotte.pit.enchantment.type.sewer_normal;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.parm.listener.ITickTask;
import cn.charlotte.pit.runnable.SewersRunnable;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.atomic.AtomicBoolean;

@ArmorOnly
public class WhitebaitEnchant extends AbstractEnchantment implements IActionDisplayEnchant, ITickTask, IAttackEntity {

    @Override
    public String getEnchantName() {
        return "银鱼";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "Whitebait";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.SEWER_NORMAL;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7处于下水道时获得 &f速度I &7效果/s" +
                "&7且攻击时额外造成 &c+" + enchantLevel * 4 + "%&7伤害";
    }

    @Override
    public String getText(int level, Player player) {

        if (SewersRunnable.inSewer(player)) {
            return "&a&l√ &7(下水道区域)";
        } else {
            return "&c&l✗ &7(非下水道区域)";
        }
    }

    @Override
    public void handle(int enchantLevel, Player player) {
        if (SewersRunnable.inSewer(player)) {
            PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.SPEED, 10 * enchantLevel * 20, 0, true, false));
        }
    }

    @Override
    @PlayerOnly
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (SewersRunnable.inSewer(attacker)) {
            boostDamage.set(boostDamage.get() + 0.04 * enchantLevel);
        }
    }

    @Override
    public int loopTick(int enchantLevel) {
        return 20; // 每20ticks(1秒)执行一次
    }
}