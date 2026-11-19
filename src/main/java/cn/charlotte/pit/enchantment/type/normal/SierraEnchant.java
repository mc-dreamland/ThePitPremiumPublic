package cn.charlotte.pit.enchantment.type.normal;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.param.item.BowOnly;
import cn.charlotte.pit.enchantment.param.item.WeaponOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.parm.listener.IPlayerKilledEntity;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * @Author: Misoryan
 * @Created_In: 2021/3/24 19:23
 */
@WeaponOnly
@ArmorOnly
@BowOnly
public class SierraEnchant extends AbstractEnchantment implements IPlayerKilledEntity {
    @Override
    public String getEnchantName() {
        return "钻石回收者";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "sierra_enchant";
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
        return "&7击杀的玩家每穿着一件&e商店 &b钻石装备 &7,获得 &6+" + (enchantLevel * 15) + " 硬币";
    }

    @Override
    @PlayerOnly
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        Player targetPlayer = (Player) target;
        if (CombatListener.isNight()) {
            return;
        }
        for (ItemStack itemStack : targetPlayer.getInventory().getArmorContents()) {
            if (itemStack != null && itemStack.getType().name().contains("DIAMOND") && ItemUtil.getInternalName(itemStack).equals("shopItem")) {
                if (Utils.getEnchantLevel(myself.getInventory().getItemInMainHand(), getNbtName()) > 0 && myself.getInventory().getItemInMainHand().getType().name().contains("SWORD")) {
                    resourceManager.addGold(enchantLevel * 15, "钻石回收者");
                }
                if (Utils.getEnchantLevel(myself.getInventory().getLeggings(), getNbtName()) > 0) {
                    resourceManager.addGold(enchantLevel * 15, "钻石回收者");
                }
                if (Utils.getEnchantLevel(myself.getInventory().getItemInMainHand(), getNbtName()) > 0 && myself.getInventory().getItemInMainHand().getType() == Material.BOW) {
                    resourceManager.addGold(enchantLevel * 15, "钻石回收者");
                }
            }
        }
    }

}
