package cn.charlotte.pit.enchantment.type.normal;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.ITickTask;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Araykal
 * @since 2025/1/16
 */
@ArmorOnly
public class LastStandEnchant extends AbstractEnchantment implements IActionDisplayEnchant, ITickTask {

    public static final HashMap<UUID, Cooldown> cooldown = new HashMap<>();

    @Override
    public String getEnchantName() {
        return "背水一战";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "last_stand";
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
        int time = 0;
        String level = "I";
        time = switch (enchantLevel) {
            case 1, 2 -> 3;
            case 3 -> 4;
            default -> time;
        };
        level = switch (enchantLevel) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> level;
        };
        return "&7剩余 &c3❤ &7时获得 &b抗性提升 " + level + " &f(00:0" + time + ") &7(10秒冷却)";
    }

    @Override
    public String getText(int level, Player player) {
        return this.getCooldownActionText(cooldown.getOrDefault(player.getUniqueId(), new Cooldown(0)));
    }

    @Override
    public void handle(int enchantLevel, Player player) {
        int time = switch (enchantLevel) {
            case 1, 2 -> 3;
            case 3 -> 4;
            default -> 0;
        };
        if (cooldown.getOrDefault(player.getUniqueId(), new Cooldown(0)).hasExpired()) {
            if (player.getHealth() <= 3 * 2) {
                player.removePotionEffect(PotionEffectType.RESISTANCE);
                Bukkit.getScheduler().runTask(
                        ThePit.getInstance(),
                        () -> PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.RESISTANCE, 20 * time, enchantLevel - 1, true))
                );
                cooldown.put(player.getUniqueId(), new Cooldown(10, TimeUnit.SECONDS));
            }
        }
    }

    @Override
    public int loopTick(int enchantLevel) {
        return 20;
    }
}