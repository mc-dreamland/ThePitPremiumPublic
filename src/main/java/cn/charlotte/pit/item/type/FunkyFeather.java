package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FunkyFeather extends AbstractPitItem {


    @Override
    public String getInternalName() {
        return "funky_feather";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&3时髦的羽毛";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.FEATHER)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .canTrade(true)
                .canSaveToEnderChest(true)
                .lore(
                        "&e特殊物品"  ,
                        "&7放于物品栏或副手时,可以保护",
                        "&7背包内的神话物品不会在死亡后扣除生命.",
                        "&7&o此物品会在死亡后消耗"
                )
                .build();
    }
}
