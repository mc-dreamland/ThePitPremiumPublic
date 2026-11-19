package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class BakedPotato extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "angry_potato";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&c&l愤怒的土豆";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.BAKED_POTATO).name(getItemDisplayName()).internalName(getInternalName()).lore("", "&7这个土豆看起来", "&7好像格外暴躁...", "").deathDrop(true)
                .removeOnJoin(true)
                .canSaveToEnderChest(false)
                .canDrop(false)
                .isHealingItem(true)
                .canTrade(false).build();
    }
}
