package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * @Author: Misoryan
 * @Created_In: 2021/3/28 18:29
 */
public class Cookies extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "cookie";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.COOKIE)
                .deathDrop(false)
                .canSaveToEnderChest(false)
                .canDrop(false)
                .removeOnJoin(true)
                .canTrade(false)
                .lore(
                        "&7死亡后保留",
                        "",
                        "&a速度 II(0:25)"
                )
                .internalName(getInternalName())
                .build();
    }
}
