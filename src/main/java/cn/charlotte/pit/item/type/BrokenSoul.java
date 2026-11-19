package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class BrokenSoul extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "broken_soul";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&b断裂的灵魂";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.MAGMA_CREAM)
                .name(getItemDisplayName())
                .lore(
                        "&7事件物品",
                        "",
                        "&7使用后获得效果:",
                        "&3抗性提升 II (0:03)",
                        "&74&c❤&7 回复",
                        "&73&6❤&7 伤害吸收")
                .internalName(getInternalName())
                .removeOnJoin(true)
                .canDrop(false)
                .canTrade(false)
                .canSaveToEnderChest(false)
                .build();
    }
}
