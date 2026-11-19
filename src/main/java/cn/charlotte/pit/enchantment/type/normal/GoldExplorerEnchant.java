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
 * @Created_In: 2021/2/7 17:53
 */
@WeaponOnly
@BowOnly
@ArmorOnly
public class GoldExplorerEnchant extends AbstractEnchantment implements IPlayerKilledEntity {
    @Override
    public String getEnchantName() {
        return "探金之人";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "gold_explorer_enchant";
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
        return "&7击杀获得的硬币 &6+" + (enchantLevel * 20) + "%";
    }

    @Override
    @PlayerOnly
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        int leg = Utils.getEnchantLevel(myself.getInventory().getLeggings(), getNbtName());
        int item = Utils.getEnchantLevel(myself.getInventory().getItemInMainHand(), getNbtName());
        if (leg > 0) {
            resourceManager.addGoldModifier(1 + 0.20d * leg, getEnchantName() + "(神话之甲)");
        }
        if (item > 0 && myself.getInventory().getItemInMainHand().getType().name().contains("SWORD")) {
            resourceManager.addGoldModifier(1 + 0.20d * item, getEnchantName() + "(神话之剑)");
        }
        if (item > 0 && myself.getInventory().getItemInMainHand().getType() == Material.BOW) {
            resourceManager.addGoldModifier(1 + 0.20d * item, getEnchantName() + "(神话之弓)");
        }
    }
}
