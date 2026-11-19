package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Rubbin extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "rubbish";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&2下水道废弃物";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.SLIME_BALL;
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(getItemDisplayMaterial())
                .internalName(getInternalName())
                .canDrop(false)
                .canTrade(true)
                .deathDrop(true)
                .canSaveToEnderChest(true)
                .name(getItemDisplayName())
                .lore(
                        "&7死亡后掉落",
                        "&7使用 64 个废弃物可以找 &9下水道鱼&7",
                        "&7换取稀有护甲"
                )
                .build();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        // 空实现
    }

    public ItemStack toManyItem(int amount) {
        return new ItemBuilder(getItemDisplayMaterial())
                .internalName(getInternalName())
                .canDrop(false)
                .canTrade(true)
                .deathDrop(true)
                .amount(amount)
                .canSaveToEnderChest(true)
                .name(getItemDisplayName())
                .lore(
                        "&7死亡后掉落",
                        "&7使用 64 个废弃物可以找 &9下水道鱼&7",
                        "&7换取稀有护甲"
                )
                .build();
    }
}
