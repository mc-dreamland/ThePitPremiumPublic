package cn.charlotte.pit.enchantment.type.normal;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.param.item.BowOnly;
import cn.charlotte.pit.enchantment.param.item.WeaponOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.parm.listener.IPlayerKilledEntity;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.cooldown.Cooldown;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/17 17:28
 */
@WeaponOnly
@BowOnly
@ArmorOnly
public class WisdomEnchant extends AbstractEnchantment implements IPlayerKilledEntity {
    @Override
    public String getEnchantName() {
        return "智慧";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "wisdom";
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
        return "&7击杀获得 &b+" + (10 * enchantLevel) + "% 经验值";
    }


    @Override
    @PlayerOnly
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        if (Utils.getEnchantLevel(myself.getInventory().getLeggings(), getNbtName()) > 0) {
            resourceManager.addExpModifier((1 + 0.1 * enchantLevel), getEnchantName() + "(神话之甲)");
        }
        if (Utils.getEnchantLevel(myself.getInventory().getItemInMainHand(), getNbtName()) > 0 && myself.getInventory().getItemInMainHand().getType().name().contains("SWORD")) {
            resourceManager.addExpModifier((1 + 0.1 * enchantLevel), getEnchantName() + "(神话之剑)");
        }
        if (Utils.getEnchantLevel(myself.getInventory().getItemInMainHand(), getNbtName()) > 0 && myself.getInventory().getItemInMainHand().getType() == Material.BOW) {
            resourceManager.addExpModifier((1 + 0.1 * enchantLevel), getEnchantName() + "(神话之弓)");
        }
    }
}
