package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SacrosanctCactus extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "sacrosanct_cactus";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "§e神圣仙人掌";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.CACTUS)
                .name(getItemDisplayName())
                .lore(
                        "&e特殊物品",
                        "&7手持并右键可以从九件 I级 附魔的",
                        "&7随机 &a神&c话&e之&6甲 &7选择其一.",
                        " ",
                        "&c&l无法重复刷新!",
                        " ",
                        "&6(随机颜色选择)"
                )
                .internalName(getInternalName())
                .canTrade(true)
                .canSaveToEnderChest(true)
                .removeOnJoin(false)
                .shiny()
                .build();
    }
}
