package cn.charlotte.pit.enchantment.type.rage;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
import cn.charlotte.pit.parm.listener.IPlayerDamaged;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemUtil;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@ArmorOnly
public class NewDealEnchant extends AbstractEnchantment implements IPlayerDamaged {

    @Override
    public String getEnchantName() {
        return "新的交易";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "new_deal";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RAGE;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7受到的伤害 &9-" + (enchantLevel * 2 + 2) + "% &7且自身免疫附魔 &6亿万富翁 &7的效果";
    }

    @Override
    public void handlePlayerDamaged(
            int enchantLevel,
            Player myself,
            Entity attacker,
            double damage,
            AtomicDouble finalDamage,
            AtomicDouble boostDamage,
            AtomicBoolean cancel
    ) {
        if (attacker instanceof Player attackerPlayer) {
            ItemStack itemInHand = attackerPlayer.getInventory().getItemInHand();
            if ("mythic_sword".equals(ItemUtil.getInternalName(itemInHand))) {
                MythicSwordItem item = new MythicSwordItem();
                item.loadFromItemStack(itemInHand);
                for (Map.Entry<cn.charlotte.pit.enchantment.AbstractEnchantment, Integer> entry : item.getEnchantments().entrySet()) {
                    if ("billionaire".equals(entry.getKey().getNbtName())) {
                        boostDamage.set(boostDamage.get() / (1 + 35 * entry.getValue() * 0.01));
                        break;
                    }
                }
            }
        }
        boostDamage.getAndAdd(enchantLevel * -0.02 - 0.02);
    }
}
