package cn.charlotte.pit.menu.gem.button;

import cn.charlotte.pit.menu.gem.SecondChoseGemMenu;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class ItemGemButton extends Button {

    private final ItemStack item;
    private final int index;

    public ItemGemButton(ItemStack item, int index) {
        this.item = item;
        this.index = index;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        return item;
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        new SecondChoseGemMenu(item, index).openMenu(player);
    }
}
