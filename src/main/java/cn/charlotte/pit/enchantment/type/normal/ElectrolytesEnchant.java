package cn.charlotte.pit.enchantment.type.normal;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.parm.listener.IPlayerKilledEntity;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;

@ArmorOnly
public class ElectrolytesEnchant extends AbstractEnchantment implements IPlayerKilledEntity {

    @Override
    public String getEnchantName() {
        return "电解质";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "electrolytes_enchant";
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
        return "&7击杀时如自身存在 &b速度 &7效果,延长效果时间 &e" + (enchantLevel * 2) + " 秒"
                + "/s&7(如效果等级大于II则延长时间减半,上限" + ((enchantLevel + 2) * 6) + "秒)"
                + "&7(效果等级限制: I阶32秒, II阶16秒, III阶8秒, IV阶4秒)";
    }

    @PlayerOnly
    @Override
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {

        Optional<PotionEffect> speedEffect = myself.getActivePotionEffects().stream()
                .filter(effect -> effect.getType().equals(PotionEffectType.SPEED))
                .findFirst();

        if (speedEffect.isPresent()) {
            PotionEffect potion = speedEffect.get();
            int duration;
            int amplifier = potion.getAmplifier();

            if (amplifier >= 1) {
                duration = potion.getDuration() + enchantLevel * 2 * 20; // 效果等级大于II，延长时间减半
            } else {
                duration = potion.getDuration() + enchantLevel * 20;
            }

            // 根据药水效果等级设置最高持续时间限制
            int maxDuration = switch (amplifier) {
                case 0 -> // I阶
                        32 * 20;
                case 1 -> // II阶
                        16 * 20;
                case 2 -> // III阶
                        8 * 20;
                case 3 -> // IV阶
                        4 * 20;
                default -> 4 * 20;
            };

            if (duration > maxDuration) {
                duration = maxDuration;
            }
            int originalMaxDuration = (enchantLevel + 2) * 6 * 20;
            if (duration > originalMaxDuration) {
                duration = originalMaxDuration;
            }

            final int finalDuration = duration;

            new BukkitRunnable() {
                @Override
                public void run() {
                    PlayerUtil.addEffect(myself, new PotionEffect(
                            PotionEffectType.SPEED,
                            finalDuration,
                            amplifier
                    ));
                }
            }.runTaskLater(ThePit.getInstance(), 5L);
        }
    }
}