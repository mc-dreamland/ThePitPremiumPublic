package cn.charlotte.pit.enchantment.type.ragerare;

import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;


@ArmorOnly
public class SpeedForceEnchant extends AbstractEnchantment implements IAttackEntity, IActionDisplayEnchant {
    @Override
    public String getEnchantName() {
        return "强力击: 神速力";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "speed_force";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RAGE_RARE;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7每 &e3 &7次攻击"
                + "/s&7给予 &b速度 " + getEffect(enchantLevel + 1) + " &f(0:0" + enchantLevel + ")"
                + "/s&7在速度期间伤害多加 &c+" + String.format(Locale.US, "%.1f", enchantLevel * 0.2) + "❤ &7的&c必中&7伤害";
    }

    private String getEffect(int enchantLevel) {
        return switch (enchantLevel) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "未知";
        };
    }


    @Override
    @PlayerOnly
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId()).getMeleeHit() % 3 == 0) {
            PlayerUtil.addEffect(attacker, new PotionEffect(PotionEffectType.SPEED, 20 * (enchantLevel + 1), enchantLevel, true));

        }
        Player targeter = (Player) target;
        if (attacker.hasPotionEffect(PotionEffectType.SPEED)) {
            targeter.setHealth(Math.max(0, targeter.getHealth()
                    - PitBotManager.scalePvpDamage(attacker, targeter, 0.4 * enchantLevel)));
        }

    }

    @Override
    public String getText(int level, Player player) {
        int hit = player.getInventory().getItemInMainHand().getType() == Material.BOW ? PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).getBowHit() : PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).getMeleeHit();
        return (hit % 3 == 0 ? "&a&l✔" : "&e&l" + (3 - hit % 3));
    }

}
