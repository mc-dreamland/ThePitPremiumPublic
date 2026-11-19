package cn.charlotte.pit.listener;

import cn.charlotte.pit.parm.AutoRegister;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@AutoRegister
public class TrashBinListener implements Listener {

    //时间匆忙暂时先不处理这个
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
//        if (event.getView().title().toString().equals(TrashBin.INVENTORY_TITLE)) {
//            ItemStack stack = event.getCurrentItem();
//            Boolean result = ItemUtil.isTrash(stack);
//            if (result != null) {
//                stack = new ItemBuilder(stack).changeNbt("canTrash", !result).build();
//                event.setCurrentItem(stack);
//            }
//        }
    }

    private void moveItem(@NotNull InventoryView view, int rawSlot, ItemStack l) {
//        Inventory container = view.getTopInventory();
//        if (rawSlot < container.getSize()) {
//            container = view.getBottomInventory();
//        }
//        container.addItem(l.clone());
//        l.setAmount(0);
//        l.setType(Material.AIR);
    }
}