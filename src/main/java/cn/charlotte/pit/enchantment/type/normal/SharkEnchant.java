package cn.charlotte.pit.enchantment.type.normal;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.item.BowOnly;
import cn.charlotte.pit.enchantment.param.item.WeaponOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: Misoryan
 * @Created_In: 2020/12/30 21:27
 */
@WeaponOnly
@BowOnly
public class SharkEnchant extends AbstractEnchantment implements IAttackEntity, IActionDisplayEnchant {

    @Override
    public String getEnchantName() {
        return "鲸鲨";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "shark";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.NORMAL;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        int boostRate = switch (enchantLevel) {
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 7;
            default -> 0;
        };
        return "&7以你为中心12格内,每有玩家生命值低于 &c6❤ &7,你造成的伤害 &c+" + boostRate + "% &7(最高30%)";
    }

    private double getBoost(Player myself, int enchantLevel) {
        double boostCount = 0;

        List<Entity> nearbyEntities = myself.getNearbyEntities(12, 5, 12);
        for (Entity nearbyEntity : nearbyEntities) {
            if (nearbyEntity instanceof Player p && p.getHealth() <= 12) {
                if (myself.getWorld().getName().equals(p.getWorld().getName())) {
                    boostCount++;
                }
            }
        }

        double boostRate = switch (enchantLevel) {
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 7;
            default -> 0;
        };
        return Math.min(0.3, boostRate * boostCount * 0.01);
    }

    @Override
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        boostDamage.set(boostDamage.get() + Math.min(0.3, getBoost(attacker, enchantLevel)));
    }


    @Override
    public String getText(int level, Player player) {
        return Util.ifNull(String.format("鲸鲨伤害加成 : %s", getBoost(player, level)), "");
    }
}
