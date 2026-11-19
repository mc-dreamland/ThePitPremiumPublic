package cn.charlotte.pit.enchantment.type.sewer_normal;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IPlayerDamaged;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

@ArmorOnly
public class GargoyleEnchant extends AbstractEnchantment implements IPlayerDamaged {

    @Override
    public String getEnchantName() {
        return "石像鬼";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "GargoyleEnchant";
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
        return "&7每少 &c1颗❤ &9减少" + enchantLevel + "%伤害 &7上限(" + (enchantLevel * 5) + "%)";
    }

    @Override
    @PlayerOnly
    public void handlePlayerDamaged(int enchantLevel, Player myself, Entity attacker,
                                    double damage, AtomicDouble finalDamage,
                                    AtomicDouble boostDamage, AtomicBoolean cancel) {

        double maxHealth = myself.getMaxHealth();
        double currentHealth = myself.getHealth();
        double lostHealth = maxHealth - currentHealth;

        double lostHearts = lostHealth / 2.0;

        double damageReduction = lostHearts * (enchantLevel * 0.01);
        damageReduction = Math.min(damageReduction, enchantLevel * 0.05);

        boostDamage.set(boostDamage.get() - damageReduction);
    }
}