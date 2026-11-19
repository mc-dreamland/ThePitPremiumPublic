package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * @Author: Misoryan
 * @Created_In: 2021/3/28 18:29
 */
public class JumpBoostPotion extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "jump_boost_4_potion";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&a跳跃提升药水";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.POTION)
                .name(getItemDisplayName())
                .lore("&7跳跃提升 IV (00:30)")
                .internalName(getInternalName())
                .canSaveToEnderChest(true)
                .deathDrop(true)
                .addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 30 * 20, 3), true)
                .shiny()
                .build();
    }
}
