package cn.charlotte.pit.item.listenerType;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public interface IPlayerInteract {
    /**
     * @param player    传递玩家
     * @param itemStack 触发物品
     * @param hand      使用手
     * @param event     事件本身
     */
    void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent event);
}
