package cn.charlotte.pit.menu.workshop.button;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.DyeColor;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class DyeButton extends Button {

    private final DyeColor dyeColor;

    public DyeButton(DyeColor dyeColor) {
        this.dyeColor = dyeColor;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        String current = "";
        ItemStack itemStack = InventoryUtil.deserializeItemStack(profile.getEnchantingItem());
        int coins = itemStack.getType() == Material.LEATHER_LEGGINGS ? 200 : 2000;
        if (profile.getCoins() < coins) {
            current = "&c你的硬币不足!";
        } else {
            current = "&e点击染色!";
        }
        return new ItemBuilder(itemStack)
                .name("&7将神话之井中物品染色为: " + dyeColor.getChatColor() + dyeColor.name())
                .lore(
                        "&7价格: &6" + coins + " 硬币",
                        "",
                        current
                )
                .setLetherColor(dyeColor.getColor())
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        ItemStack stack = InventoryUtil.deserializeItemStack(profile.getEnchantingItem());
        int coins = stack.getType() == Material.LEATHER_LEGGINGS ? 200 : 2000;

        if (profile.getCoins() < coins) {
            return;
        }

        profile.takeCoins(coins);
        ItemStack itemStack;
        if (stack.getType().equals(Material.LEATHER_HELMET)) {
            LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
            meta.setColor(dyeColor.getColor());
            stack.setItemMeta(meta);
            itemStack = new ItemStack(stack);

        } else {
            itemStack = new ItemBuilder(stack).changeNbt("dyeColor", dyeColor.name()).build();
        }

        profile.setEnchantingItem(InventoryUtil.serializeItemStack(itemStack.getType().equals(Material.LEATHER_HELMET) ? itemStack : ThePit.getApi().reformatPitItem(itemStack)));
        player.closeInventory();
        CC.send(MessageType.MISC, player, "&a成功染色!");
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }
}
