package cn.charlotte.pit.menu.mystery.button;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.mystery.MysteryMenu;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.log.ItemOperationJson;
import cn.charlotte.pit.util.menu.Button;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@Slf4j
public class MysteryMainButton extends Button {

    private static final ItemStack AIRTWO = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
            .name("&f放入神秘物品")
            .build();

    @Override
    public ItemStack getButtonItem(Player player) {
        return Util.ifNull(InventoryUtil.deserializeItemStack(Util.getPitProfile(player).getMysteryMainItem()), AIRTWO);
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        PlayerProfile profile = Util.getPitProfile(player);
        ItemStack itemStack = InventoryUtil.deserializeItemStack(profile.getMysteryMainItem());
        if (itemStack == null) return;
        int targetSlot = player.getInventory().firstEmpty();
        if (targetSlot < 0) {
            CC.send(MessageType.MISC, player, "&c背包已满!");
            return;
        }
        player.getInventory().setItem(targetSlot, itemStack);
        profile.setMysteryMainItem(null);
        JsonObject operationLog = ItemOperationJson.operation("MYSTERY_WITHDRAW", player);
        operationLog.addProperty("slot", "INPUT");
        operationLog.add("item", ItemOperationJson.item(itemStack));
        log.info("{}", ItemOperationJson.toJson(operationLog));
        new MysteryMenu().openMenu(player);
    }
}
