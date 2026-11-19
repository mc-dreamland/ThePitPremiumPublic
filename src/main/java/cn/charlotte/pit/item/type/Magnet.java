package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Magnet extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "soul_magnet";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&2下水道磁石";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.RECOVERY_COMPASS)
                .internalName(getInternalName())
                .name(getItemDisplayName())
                .lore(
                        "",
                        "&7来自下水道的建筑者所制作的",
                        "&7右键即可获得最近的下水道箱子的位置",
                        "",
                        "&7死后保留",
                        ""
                )
                .removeOnJoin(false)
                .canDrop(false)
                .canTrade(true)
                .canSaveToEnderChest(true)
                .build();
    }
}
