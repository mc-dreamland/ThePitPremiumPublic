package cn.charlotte.pit.enchantment.type.rare;

import cn.charlotte.pit.data.PlayerProfile;
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

import java.util.concurrent.atomic.AtomicBoolean;

@WeaponOnly
public class VitalEchoEnchant extends AbstractEnchantment implements IAttackEntity, IActionDisplayEnchant {
    @Override
    public String getEnchantName() {
        return "生命回响";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "vital_echo";
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
        return "&7每次命中目标时将回复 &c0.25❤ &7生命值/s" +
                "&7每命中目标 &e" + (6 - enchantLevel) + " &7次时，下次攻击将造成 &c" + ((double) enchantLevel / 2) + "❤ &7的伤害/s" +
                "&7同时，将额外回复 &c" + ((double) enchantLevel / 2) + "❤ &7生命值";
    }

    @Override
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId()).getMeleeHit() % (6 - enchantLevel) == 0) {
            boostDamage.getAndAdd(enchantLevel * 0.1D);
            PlayerUtil.heal(attacker, 1.0 * enchantLevel);
        } else {
            PlayerUtil.heal(attacker, 0.5);
        }
    }

    @Override
    public String getText(int enchantLevel, Player player) {
        int hit = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).getMeleeHit();
        return (hit % (6 - enchantLevel) == 0 ? "&a&l✔" : "&e&l" + ((6 - enchantLevel) - hit % (6 - enchantLevel)));
    }
}