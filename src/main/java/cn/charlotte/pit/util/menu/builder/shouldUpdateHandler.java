package cn.charlotte.pit.util.menu.builder;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public interface shouldUpdateHandler {
    boolean shouldUpdate(Player player, int slot, ClickType clickType);
}
