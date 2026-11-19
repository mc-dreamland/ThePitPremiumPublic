package cn.charlotte.pit.enchantment.type.normal;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.item.BowOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IPlayerShootEntity;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/30 15:29
 */
@BowOnly
public class ParasiteEnchant extends AbstractEnchantment implements IPlayerShootEntity {
    @Override
    public String getEnchantName() {
        return "吸血鬼: 箭矢";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "parasite_enchant";
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
        return switch (enchantLevel) {
            case 1 -> "&7弓箭击中恢复自身生命值/s&7蓄力3级: &c0.5❤";
            case 2 -> "&7弓箭击中恢复自身生命值/s&7蓄力3级: &c1.0❤/s&7蓄力2级: &c0.5❤";
            case 3 -> "&7弓箭击中恢复自身生命值/s&7蓄力3级: &c1.5❤/s&7蓄力2级: &c1.0❤/s&7蓄力1级: &c0.5❤";
            default -> "&7弓箭击中恢复自身生命值";
        };
    }

    @Override
    @cn.charlotte.pit.parm.type.BowOnly
    public void handleShootEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        Float force = Util.getPitProfile(attacker).getForce();
        if (force == null) {
            return;
        }
        int drawLevel;
        if (force < 0.5f) {
            drawLevel = 1;
        } else if (force < 0.9f) {
            drawLevel = 2;
        } else {
            drawLevel = 3;
        }

        double healAmount = 0;

        switch (enchantLevel) {
            case 1:
                if (drawLevel == 3) {
                    healAmount = 1.0;
                }
                break;
            case 2:
                if (drawLevel == 3) {
                    healAmount = 2.0;
                } else if (drawLevel == 2) {
                    healAmount = 1.0;
                }
                break;
            case 3:
                if (drawLevel == 3) {
                    healAmount = 3.0;
                } else if (drawLevel == 2) {
                    healAmount = 2.0;
                } else {
                    healAmount = 1.0;
                }
                break;
        }

        if (healAmount > 0) {
            PlayerUtil.heal(attacker, healAmount);
        }
    }
}