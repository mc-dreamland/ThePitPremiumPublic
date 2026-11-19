package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * @Author: Misoryan
 * @Created_In: 2021/3/28 18:29
 */
public class MilkBucket extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "milk_bucket";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.MILK_BUCKET)
                .deathDrop(false)
                .canSaveToEnderChest(true)
                .canDrop(false)
                .canTrade(true)
                .lore(
                        "&7死亡后保留",
                        "",
                        "&a生命恢复I(0:20)",
                        "&7补钙"
                )
                .internalName(getInternalName())
                .build();
    }
}
