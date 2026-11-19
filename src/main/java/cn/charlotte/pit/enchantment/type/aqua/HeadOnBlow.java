package cn.charlotte.pit.enchantment.type.aqua;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.util.cooldown.Cooldown;

public class HeadOnBlow extends AbstractEnchantment {

    @Override
    public String getEnchantName() {
        return "当头一棒";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "head_on_blow";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.FISH_NORMAL;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "甩出鱼钩击退玩家力度增加20%";
    }
}
