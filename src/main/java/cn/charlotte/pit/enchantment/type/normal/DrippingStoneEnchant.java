package cn.charlotte.pit.enchantment.type.normal;

import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.item.BowOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IPlayerShootEntity;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

@BowOnly
public class DrippingStoneEnchant extends AbstractEnchantment implements IPlayerShootEntity, IActionDisplayEnchant {

    @Override
    public String getEnchantName() {
        return "滴水石穿";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "dripping_stone";
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
        return "&7每 &e3 &7次射击后，下次攻击造成 &c" + (0.5 + enchantLevel * 0.5) + "❤必中伤害"
                + "/s&c(必中伤害无法被免疫与抵抗)";
    }

    @Override
    @cn.charlotte.pit.parm.type.BowOnly
    public void handleShootEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId());
        int bowHits = profile.getBowHit();

        if (bowHits % 3 == 0) {
            if (target instanceof LivingEntity livingTarget) {
                double heartDamage = 0.5 + enchantLevel * 0.5;
                double damagePoints = PitBotManager.scalePvpDamage(attacker, target, heartDamage * 2);
                double currentHealth = livingTarget.getHealth();
                if (currentHealth > damagePoints) {
                    livingTarget.setHealth(currentHealth - damagePoints);
                } else if (PitBotManager.isTrainingPvp(attacker, target)) {
                    livingTarget.setHealth(0);
                } else {
                    livingTarget.damage(livingTarget.getMaxHealth() * 100);
                }
            }
        }
    }

    @Override
    public String getText(int level, Player player) {

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        int bowHits = profile.getBowHit();
        int remaining = 3 - (bowHits % 3);

        return (remaining == 3) ? "&a&l✔" : "&e&l" + remaining;
    }

}
