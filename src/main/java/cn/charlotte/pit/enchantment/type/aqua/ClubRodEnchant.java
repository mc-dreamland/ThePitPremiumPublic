package cn.charlotte.pit.enchantment.type.aqua;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.util.cooldown.Cooldown;

@ArmorOnly
public class ClubRodEnchant extends AbstractEnchantment {

    @Override
    public String getEnchantName() {
        return "鱼竿会员";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 1;
    }

    @Override
    public String getNbtName() {
        return "club_rod";
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
        return "&7装备此神话之甲时,自身额外获得一条钓鱼竿";
    }
}
