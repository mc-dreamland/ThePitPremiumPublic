package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class mythicRandomVoucher extends AbstractPitItem {
    @Override
    public String getInternalName() {
        return "mythicRandomVoucher";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&d随机神话兑换券";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.PAPER;
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(getItemDisplayMaterial())
                .name(getItemDisplayName())
                .lore(Arrays.asList(
                        "&7死亡时保留",
                        "",
                        "&7获得一件随机的神话装备.",
                        "",
                        "&e右键使用&7(使用后消失)"
                ))
                .internalName(getInternalName())
                .canSaveToEnderChest(true)
                .canTrade(true)
                .deathDrop(false)
                .shiny()
                .build();
    }

}
