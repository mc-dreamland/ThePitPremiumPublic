package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ChunkOfVileItem extends AbstractPitItem {

    @Override
    protected String getDefaultItemDisplayName() {
        return "&5暗聚块";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.COAL)
                .name("&5暗聚块")
                .lore(
                        "&7死亡后保留",
                        ""
                )
                .canSaveToEnderChest(true)
                .canTrade(true)
                .internalName(getInternalName())
                .build();
    }

    @Override
    public String getInternalName() {
        return "chunk_of_vile_item";
    }
}
