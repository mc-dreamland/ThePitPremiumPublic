package cn.charlotte.pit.util.menu.builder;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public interface ClickHandler {
    void onClick(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem);
}
