package cn.charlotte.pit.menu;

import cn.charlotte.pit.util.MaterialConvert;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 17:16
 */
public class GlassPaneButton extends Button {
    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(MaterialConvert.GLASS_PANE[15])
                .name(" ")
                .build();

    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {

    }
}
