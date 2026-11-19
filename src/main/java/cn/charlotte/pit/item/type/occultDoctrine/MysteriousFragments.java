package cn.charlotte.pit.item.type.occultDoctrine;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MysteriousFragments extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "mysterious_fragments";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&5神秘碎片";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.ECHO_SHARD)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .canDrop(false)
                .removeOnJoin(false)
                .canTrade(true)
                .deathDrop(false)
                .canSaveToEnderChest(true)
                .lore(
                        "",
                        "&7十分可疑的碎片.",
                        "",
                        "&e在 &5神秘造物台 &e中使用"
                )
                .build();
    }
}
