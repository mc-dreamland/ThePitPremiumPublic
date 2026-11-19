package cn.charlotte.pit.enchantment.type.rage;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.parm.listener.IPlayerShootEntity;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@ArmorOnly
public class BreachingChargeEnchant extends AbstractEnchantment implements IAttackEntity, IPlayerShootEntity, IActionDisplayEnchant {

    private static final Map<UUID, Cooldown> COOLDOWN = new HashMap<>();

    @Override
    public String getEnchantName() {
        return "强力击: 违约金";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "breaching_charge";
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
        return "&7攻击清空命中玩家的 &3抗性提升 &7效果 (" + (4 - enchantLevel) + "秒冷却)";
    }

    @PlayerOnly
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
        Player targetPlayer = (Player) target;
        if (COOLDOWN.getOrDefault(attacker.getUniqueId(), new Cooldown(0)).hasExpired()) {
            COOLDOWN.put(attacker.getUniqueId(), new Cooldown(4L - enchantLevel, TimeUnit.SECONDS));
            targetPlayer.removePotionEffect(PotionEffectType.RESISTANCE);
        }
    }

    @PlayerOnly
    @Override
    public void handleShootEntity(
            int enchantLevel,
            Player attacker,
            Entity target,
            double damage,
            AtomicDouble finalDamage,
            AtomicDouble boostDamage,
            AtomicBoolean cancel
    ) {
        Player targetPlayer = (Player) target;
        if (COOLDOWN.getOrDefault(attacker.getUniqueId(), new Cooldown(0)).hasExpired()) {
            COOLDOWN.put(attacker.getUniqueId(), new Cooldown(4L - enchantLevel, TimeUnit.SECONDS));
            targetPlayer.removePotionEffect(PotionEffectType.RESISTANCE);
        }
    }

    @Override
    public String getText(int level, Player player) {
        return getCooldownActionText(COOLDOWN.getOrDefault(player.getUniqueId(), new Cooldown(0)));
    }
}
