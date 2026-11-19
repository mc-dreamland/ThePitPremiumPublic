package cn.charlotte.pit.menu.shop.button;

import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractItemToItemButton extends Button {

    public abstract String getInternalName();

    @Override
    public ItemStack getButtonItem(Player player) {
        ItemStack itemStack = getDisplayButtonItem(player);
        try {
            List<String> lines = itemStack.getItemMeta().getLore();
            if (lines == null) {
                lines = new ArrayList<>();
            }
            lines.add(" ");
            lines.addAll(getNeedItem(player));
            return new ItemBuilder(itemStack).lore(lines).build();
        } catch (Exception e) {
            return getDisplayButtonItem(player);
        }
    }

    public abstract ItemStack getDisplayButtonItem(Player player);

    public abstract ItemStack[] getResultItem(Player player);

    public abstract List<String> getNeedItem(Player player);

    public abstract boolean hasItems(Player player);

    public abstract void costItems(Player player);

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        if (hasItems(player)) {
            int freeSlots = InventoryUtil.getInventoryEmptySlots(player.getInventory().getContents());
            if (freeSlots < getResultItem(player).length) {
                player.sendMessage(CC.translate("&c&l背包已满! &7你的背包已满,暂时无法购买物品."));
                return;
            }
            costItems(player);
            player.getInventory().addItem(getResultItem(player));
            player.sendMessage(CC.translate("&a购买成功!"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        } else {
            player.sendMessage(CC.translate("&c你的材料不足!"));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        }
    }
}
