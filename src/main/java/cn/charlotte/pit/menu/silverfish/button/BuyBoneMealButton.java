package cn.charlotte.pit.menu.silverfish.button;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class BuyBoneMealButton extends Button {

    private static final ItemStack item = new ItemBuilder(Material.BONE_MEAL)
            .lore(
                    "",
                    "&f购买所需 : 192 &2任意下水道方块",
                    "",
                    "&f肥料",
                    " &7手持对准小麦进行右键使用,将以你为中心的 4×4×4 区域内的小麦催熟",
                    ""
            )
            .build();

    @Override
    public ItemStack getButtonItem(Player player) {
        return item;
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        if (InventoryUtil.getAmountOfItem(player, "sewer_block") < 192) {
            player.sendMessage(CC.translate("&2下水道之鱼 &c抱歉!你的下水道废弃方块不足"));
            return;
        }

        if (!InventoryUtil.removeItem(player,"sewer_block", 192)) return;

        Util.addItemOfMail(PitItems.BONE_MEAL.toItemStack(), player,null);

        player.sendMessage(CC.translate("&a&l+ &f肥料"));
    }
}
