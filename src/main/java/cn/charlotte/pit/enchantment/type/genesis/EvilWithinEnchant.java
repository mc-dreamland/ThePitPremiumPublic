package cn.charlotte.pit.enchantment.type.genesis;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.util.cooldown.Cooldown;

/**
 * @Author: Misoryan
 * @Created_In: 2021/3/16 19:47
 */
public class EvilWithinEnchant extends AbstractEnchantment {
    @Override
    public String getEnchantName() {
        return "恶灵附身";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 1;
    }

    @Override
    public String getNbtName() {
        return "evil_within_enchant";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.GENESIS;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&&7攻击其他玩家时免疫 &9无尽黑暗 &7附魔效果./s&7(指终末之鞋效果,Ps:无法消除自身的无尽黑暗,比如你穿戴I阶暗黑之甲手持附魔物品则不触发附魔效果)";
    }
}
