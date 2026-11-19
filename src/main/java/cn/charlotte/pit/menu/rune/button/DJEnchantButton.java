package cn.charlotte.pit.menu.rune.button;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class DJEnchantButton extends Button {

    private final AbstractEnchantment enchantment;
    private final ItemStack itemStack;
    private final int index;

    public DJEnchantButton(AbstractEnchantment enchantment, ItemStack itemStack, int index) {
        this.enchantment = enchantment;
        this.itemStack = itemStack;
        this.index = index;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        ItemStack playerItem = player.getItemInHand();
        if (playerItem == null || playerItem.getType() == Material.AIR || ItemUtil.getInternalName(playerItem) == null) {
            return new ItemBuilder(Material.AIR).build();
        }

        String[] lore = enchantment.getUsefulnessLore(1).split("/s");
        return new ItemBuilder(Material.JUKEBOX)
                .name("&9" + enchantment.getEnchantName())
                .lore(lore)
                .shiny()
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        if ("mythic_leggings".equals(ItemUtil.getInternalName(itemStack))) {
            MythicLeggingsItem leggingsItem = new MythicLeggingsItem();
            leggingsItem.loadFromItemStack(itemStack);
            leggingsItem.getEnchantments().put(enchantment, 1);

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 99f);
            player.sendMessage(CC.translate("&6&l铸造！ &7成功为你的神话之裤增加了" + enchantment.getEnchantName() + "附魔"));

            player.getInventory().setItem(index, leggingsItem.toItemStack());
            updatePlayerInventory(player);
            player.closeInventory();
        }
    }

    private void updatePlayerInventory(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInHand();
        if (itemInHand.getAmount() == 1) {
            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), null);
        } else {
            itemInHand.setAmount(itemInHand.getAmount() - 1);
            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), itemInHand);
        }
    }
}
