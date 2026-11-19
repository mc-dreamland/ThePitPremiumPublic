package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Fish_6 extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "fishing_fish";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&c大鲑鱼";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.SALMON)
                .internalName(getInternalName())
                .name(getItemDisplayName())
                .lore(
                        "&7一条很大的鲑鱼",
                        "",
                        "&7吃下它给予你 &c生命恢复III &f(00:03)",
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
