package cn.charlotte.pit.enchantment.type.rare;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.item.WeaponOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@WeaponOnly
public class HealerEnchant extends AbstractEnchantment implements IAttackEntity, IActionDisplayEnchant {

    private static final Map<UUID, Cooldown> cooldown = new HashMap<>();

    @Override
    public String getEnchantName() {
        return "医师";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "healer_enchant";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RARE;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    public int getHealAmount(int enchantLevel) {
        switch (enchantLevel) {
            case 1:
                return 2;
            case 2:
                return 4;
            case 3:
                return 5;
            default:
                return 0;
        }
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7攻击命中玩家时恢复自身 &c" + (0.25 + 0.25 * enchantLevel)
                + "❤ &7并恢复对方 &c" + getHealAmount(enchantLevel)
                + "❤ &7(1秒冷却)";
    }

    @Override
    public String getText(int level, Player player) {
        return getCooldownActionText(
                cooldown.getOrDefault(player.getUniqueId(), new Cooldown(0))
        );
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
        if (target instanceof Player) {
            UUID uuid = attacker.getUniqueId();
            Cooldown cd = cooldown.getOrDefault(uuid, new Cooldown(0));
            if (cd.hasExpired()) {
                cooldown.put(uuid, new Cooldown(1, TimeUnit.SECONDS));
                PlayerUtil.heal(attacker, 0.5 + 0.5 * enchantLevel);
                PlayerUtil.heal((Player) target, 2.0 * getHealAmount(enchantLevel));
            }
        }
    }
}
