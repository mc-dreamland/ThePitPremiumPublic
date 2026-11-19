package cn.charlotte.pit.menu.gem.global.button;

import cn.charlotte.pit.menu.gem.global.GlobalSecondChoseGemMenu;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class GlobalItemGemButton extends Button {

    private final ItemStack item;
    private final int index;

    public GlobalItemGemButton(ItemStack item, int index) {
        this.item = item;
        this.index = index;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        return item;
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        new GlobalSecondChoseGemMenu(item, index).openMenu(player);
    }
}
