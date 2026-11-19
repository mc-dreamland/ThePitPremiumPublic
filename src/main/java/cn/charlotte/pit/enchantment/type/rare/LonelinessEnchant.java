package cn.charlotte.pit.enchantment.type.rare;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.item.WeaponOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@WeaponOnly
public class LonelinessEnchant extends AbstractEnchantment implements IAttackEntity {
    @Override
    public String getEnchantName() {
        return "孤独";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "loneliness";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RARE;
    }

    @Nullable
    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7攻击玩家造成的伤害 &c+" + 30 * enchantLevel + "%" +
                "/s&7周围12格内有2名以及上的玩家则伤害降低&95%(最低增加&c" + 3 * enchantLevel + "%)";

    }

    @Override
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        double boostCount = 0;
        List<Entity> nearbyEntities = attacker.getNearbyEntities(12, 5, 12);
        for (Entity nearbyEntity : nearbyEntities) {
            if (nearbyEntity instanceof Player p) {
                if (attacker.getWorld().getName().equals(p.getWorld().getName())) {
                    boostCount++;
                }
            }
        }
        boostDamage.set(boostDamage.get() + (boostCount > 2 ? Math.max(0.03 * enchantLevel, 0.3 * enchantLevel - (boostCount - 2) * 0.05) : 0.3 * enchantLevel));
    }
}
