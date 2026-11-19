package cn.charlotte.pit.menu.rune.button;

import cn.charlotte.pit.menu.rune.SecondChoseRuneMenu;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class ItemRuneButton extends Button {

    private final ItemStack item;
    private final int index;

    public ItemRuneButton(ItemStack item, int index) {
        this.item = item;
        this.index = index;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        return item;
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        new SecondChoseRuneMenu(item, index).openMenu(player);
    }
}
