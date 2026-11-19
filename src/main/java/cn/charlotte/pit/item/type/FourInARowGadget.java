package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class FourInARowGadget extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "4_in_a_row_gadget";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&6四子棋小道具";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.COMPASS)
                .name(getItemDisplayName())
                .lore(
                        "&7死亡后保留",
                        "",
                        "&7手持右键一名玩家以发起一局",
                        "&7四子棋游戏!",
                        "",
                        "&6阵营活动奖励"
                )
                .internalName(getInternalName())
                .canSaveToEnderChest(true)
                .canTrade(true)
                .shiny()
                .build();
    }
}
