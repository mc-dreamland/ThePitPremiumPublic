package cn.charlotte.pit.enchantment.type.sewer_normal;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.util.cooldown.Cooldown;

@ArmorOnly
public class FarmerEnchant extends AbstractEnchantment {
    @Override
    public String getEnchantName() {
        return "农夫";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "Farmer";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.SEWER_NORMAL;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7挖掘小麦(仅成熟的时候)时有 " + enchantLevel * 10 + "% 概率让小麦直接成熟";
    }
}
