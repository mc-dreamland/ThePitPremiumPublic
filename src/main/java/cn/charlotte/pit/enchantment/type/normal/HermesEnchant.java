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
 * @Author: EmptyIrony
 * @Date: 2020/12/29 23:54
 */
@WeaponOnly
@BowOnly
@ArmorOnly
public class HermesEnchant extends AbstractEnchantment implements IPlayerKilledEntity {
    @Override
    public String getEnchantName() {
        return "赫耳墨斯";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "hermes";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.NORMAL;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    private int getCoins(int level) {
        return 4 * level;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        int coins = this.getCoins(enchantLevel);
        return "&7击杀获得 &6+" + coins + " 硬币";
    }


    @Override
    @PlayerOnly
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        int extraCoins = this.getCoins(enchantLevel);
        if (Utils.getEnchantLevel(myself.getInventory().getLeggings(), getNbtName()) > 0) {
            resourceManager.addGold(extraCoins, getEnchantName() + "(神话之甲)");
        }
        if (Utils.getEnchantLevel(myself.getInventory().getItemInMainHand(), getNbtName()) > 0 && myself.getInventory().getItemInMainHand().getType().name().contains("SWORD")) {
            resourceManager.addGold(extraCoins, getEnchantName() + "(神话之剑)");
        }
        if (Utils.getEnchantLevel(myself.getInventory().getItemInMainHand(), getNbtName()) > 0 && myself.getInventory().getItemInMainHand().getType() == Material.BOW) {
            resourceManager.addGold(extraCoins, getEnchantName() + "(神话之弓)");
        }
    }
}
