package cn.charlotte.pit.menu.silverfish.button;

import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.log.ItemOperationJson;
import cn.charlotte.pit.util.menu.Button;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@Slf4j
public class BuyMythicLeggingsItemButton extends Button {

    private static ItemStack itemStackLore = new ItemBuilder(PitItems.MYTHIC_LEGGING.toIMythicLeggingsItem(MythicColor.DARK_GREEN).toItemStack()).lore(
            "",
            "&2下水道之甲",
            "&f购买所需 : 64 &2下水道废弃物",
            ""
    ).build();

    @Override
    public ItemStack getButtonItem(Player player) {
        return itemStackLore;
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {

        if (InventoryUtil.getAmountOfItem(player, PitItems.RUBBIN.getInternalName()) < 64) {
            player.sendMessage(CC.translate("&2下水道之鱼 &c抱歉!你的下水道废弃之物不足"));
            return;
        }


        if (InventoryUtil.isInvFull(player.getInventory())) {
            player.sendMessage(CC.translate("&2下水道之鱼 &c抱歉!你的背包已满"));
            return;
        }

        if (!InventoryUtil.removeItem(player, PitItems.RUBBIN.getInternalName(), 64)) return;
        IMythicItem item = new MythicLeggingsItem();
        item.setColor(MythicColor.DARK_GREEN);
        ItemStack reward = item.toItemStack();
        player.getInventory().addItem(reward);
        log.info("{}", ItemOperationJson.toJson(
                ItemOperationJson.mythicGranted(player, "SILVERFISH_SHOP", reward)
        ));

        player.sendMessage(CC.translate("&a&l+ &2下水道之甲"));
    }
}
