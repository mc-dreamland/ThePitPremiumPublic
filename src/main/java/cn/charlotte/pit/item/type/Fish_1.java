package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Fish_1 extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "fishing_fish_irony";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&c小热带鱼";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.TROPICAL_FISH)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .lore(
                        "&7一条很小的热带鱼",
                        "",
                        "&7吃下它给予你 &e1❤",
                        "&8即使你不是很饿"
                )
                .removeOnJoin(false)
                .canDrop(false)
                .deathDrop(false)
                .canTrade(true)
                .canSaveToEnderChest(true)
                .build();
    }
}
