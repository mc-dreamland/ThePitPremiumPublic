package cn.charlotte.pit.util.menu.builder;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public interface shouldCancelHandler {
    boolean shouldCancel(Player player, int slot, ClickType clickType);
}
