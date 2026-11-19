package cn.charlotte.pit.career.careeritem.swordsman;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class DefaultSwordOne extends AbstractPitItem {

    @Override
    protected String getDefaultItemDisplayName() {
        return "&f一把普通的木剑";
    }

    @Override
    public String getInternalName() {
        return "default_sword_one";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.WOODEN_SWORD)
                .name(getItemDisplayName())
                .lore(
                        "",
                        "&7旅途开始的第一把剑,他是你旅途开始的第一步",
                        "&7特殊效果:",
                        "&7 - 无",
                        ""
                )
                .internalName(getInternalName())
                .removeOnJoin(false)
                .canDrop(false)
                .canSaveToEnderChest(false)
                .canTrade(false)
                .deathDrop(false)
                .build();
    }
}
