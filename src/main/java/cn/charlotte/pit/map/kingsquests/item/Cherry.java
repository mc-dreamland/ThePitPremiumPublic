package cn.charlotte.pit.map.kingsquests.item;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Cherry extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "cherry";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&d樱桃";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.APPLE;
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(getItemDisplayMaterial())
                .internalName(getInternalName())
                .canDrop(false)
                .canTrade(true)
                .deathDrop(false)
                .canSaveToEnderChest(true)
                .name(getItemDisplayName())
                .lore("&7死亡时保留")
                .build();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        // 保持空实现
    }
}
