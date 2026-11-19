package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class TacticalInsertions extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "tactical_insertions";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&6战术重部署装置";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.BLAZE_ROD)
                .name(getItemDisplayName())
                .lore("&7右键设置一个重生点")
                .internalName(getInternalName())
                .canSaveToEnderChest(true)
                .deathDrop(true)
                .canDrop(false)
                .canTrade(false)
                .shiny()
                .build();
    }
}
