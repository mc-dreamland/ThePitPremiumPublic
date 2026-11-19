package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class GainBanner extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "gain_banner";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&a增益旗帜";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.GREEN_BANNER)
                .name(getItemDisplayName())
                .lore(
                        "&e死亡后保留",
                        "",
                        "&7手持左键创建一个增益旗帜",
                        "",
                        "&e拍卖物品"
                )
                .internalName(getInternalName())
                .canSaveToEnderChest(true)
                .canTrade(true)
                .shiny()
                .build();
    }
}
