package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * @Author: Misoryan
 * @Created_In: 2021/3/28 18:29
 */
public class CakeCherry extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "sweet_berries";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&c美味樱桃";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.SWEET_BERRIES)
                .deathDrop(true)
                .name(getItemDisplayName())
                .canSaveToEnderChest(false)
                .canDrop(false)
                .canTrade(false)
                .removeOnJoin(true)
                .lore(
                        "&7死亡后消失",
                        "",
                        "&a获得2金心"
                )
                .internalName(getInternalName())
                .build();
    }
}
