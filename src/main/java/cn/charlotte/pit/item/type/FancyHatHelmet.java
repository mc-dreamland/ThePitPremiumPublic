package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class FancyHatHelmet extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "FancyHat";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&e好看的头盔";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.LEATHER_HELMET)
                .name(getItemDisplayName())
                .lore(
                        "",
                        "&9五彩斑斓",
                        "&7可以在神话之井中改变颜色(&c需要天坑乱斗增值包&7)",
                        "",
                        "&6穿着时提供锁链头盔相同的伤害减免效果"
                )
                .canTrade(false)
                .itemArmor(2.0)
                .canSaveToEnderChest(true)
                .deathDrop(false)
                .removeOnJoin(false)
                .internalName(getInternalName())
                .canDrop(false)
                .shiny()
                .buildWithUnbreakable();
    }
}

