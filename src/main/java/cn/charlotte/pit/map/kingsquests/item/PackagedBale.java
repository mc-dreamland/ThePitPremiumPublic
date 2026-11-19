package cn.charlotte.pit.map.kingsquests.item;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class PackagedBale extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "packaged_bale_hay";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&e干草块";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.HAY_BLOCK;
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(getItemDisplayMaterial())
                .internalName(getInternalName())
                .canDrop(false)
                .canTrade(true)
                .deathDrop(true)
                .canSaveToEnderChest(true)
                .name(getItemDisplayName())
                .lore("&7死亡后掉落")
                .build();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        // 保持空实现
    }
}
