package cn.charlotte.pit.menu.admin.item.button;

import cn.charlotte.pit.util.item.AdvancedItemIdentity;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.log.ItemOperationJson;
import cn.charlotte.pit.util.menu.Button;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * @Author: Misoryan
 * @Created_In: 2021/3/28 18:56
 */
@Slf4j
public class PitItemButton extends Button {
    private final ItemStack itemStack;

    public PitItemButton(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        return itemStack;
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        ItemStack granted = itemStack.clone();
        AdvancedItemIdentity.regenerateUuid(granted);
        player.getInventory().addItem(granted);
        if ("phase_book".equals(ItemUtil.getInternalName(granted))) {
            JsonObject operationLog = ItemOperationJson.operation("GET_PHASE_BOOK", player);
            operationLog.addProperty("source", "ADMIN_ITEM_MENU");
            operationLog.add("item", ItemOperationJson.item(granted));
            log.info("{}", ItemOperationJson.toJson(operationLog));
        }
    }
}
