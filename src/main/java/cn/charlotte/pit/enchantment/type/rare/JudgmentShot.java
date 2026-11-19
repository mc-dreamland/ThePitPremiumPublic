package cn.charlotte.pit.enchantment.type.rare;

import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.item.BowOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IPlayerShootEntity;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Araykal
 * @since 2025/1/16
 */
@BowOnly
public class JudgmentShot extends AbstractEnchantment implements IPlayerShootEntity {
    @Override
    public String getEnchantName() {
        return "亡灵之箭";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "judgment_shot_bow";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RARE;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7攻击玩家时若当次攻击使玩家的生命值低于 &c" + (1.5 + (enchantLevel * 0.5)) + "❤ &7,"
                + "/s&7则该次攻击直接致死.";
    }

    @Override
    public void handleShootEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        Player targetPlayer = (Player) target;
        if (targetPlayer.getHealth() - damage * boostDamage.get() < (1.5 + (enchantLevel * 0.5))) {
            cancel.set(true);
            if (PitBotManager.isTrainingPvp(attacker, targetPlayer)) {
                targetPlayer.setHealth(Math.max(0, targetPlayer.getHealth()
                        - PitBotManager.scalePvpDamage(attacker, targetPlayer, 9999)));
            } else {
                targetPlayer.damage(9999);
            }
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_VILLAGER_DEATH, 1, 1F);

            Location deathLoc = target.getLocation();

            attacker.getWorld().playEffect(deathLoc, Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
            attacker.getWorld().playEffect(deathLoc.clone().add(0.0, -1.0, 0.0), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
        }
    }
}
