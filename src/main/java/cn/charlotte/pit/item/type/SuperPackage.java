package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * @Author: Misoryan
 * @Created_In: 2021/4/3 16:57
 */
public class SuperPackage extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "super_package";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&b超级收纳箱";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.MINECART)
                .name(getItemDisplayName())
                .lore(
                        "&e特殊物品",
                        "",
                        "&7可储存大量物品!"
                )
                .shiny()
                .canSaveToEnderChest(true)
                .internalName(getInternalName())
                .build();
    }
}
