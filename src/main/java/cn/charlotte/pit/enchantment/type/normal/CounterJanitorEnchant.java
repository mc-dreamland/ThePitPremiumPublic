package cn.charlotte.pit.enchantment.type.normal;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.item.WeaponOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.parm.listener.IPlayerKilledEntity;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * @Author: Misoryan
 * @Created_In: 2021/2/8 13:58
 */
@WeaponOnly
public class CounterJanitorEnchant extends AbstractEnchantment implements IPlayerKilledEntity {
    @Override
    public String getEnchantName() {
        return "反侦察";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "counter_janitor_enchant";
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
        return "&7击杀获得 &3抗性提升 I &f(00:0" + (enchantLevel + (enchantLevel >= 3 ? 2 : 1) + ")");
    }

    @Override
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        PlayerUtil.addEffect(myself, new PotionEffect(PotionEffectType.RESISTANCE, 20 * (enchantLevel + (enchantLevel >= 3 ? 2 : 1)), 0));
    }
}
