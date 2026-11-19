package cn.charlotte.pit.menu.workshop.button;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.workshop.DyeWorkshopMenu;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WorkshopEntranceButton extends Button {
    @Override
    public ItemStack getButtonItem(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        List<String> lines = new ArrayList<>();
        lines.add("&7在这里，你可以为神话之井上的");
        lines.add("&7神话之甲进行染色!");
        lines.add("");
        lines.add("&c染色仅影响外观，不影响其升阶所需神话之甲颜色/改变自身颜色属性。");
        lines.add("");

        if (profile.isSupporter() || PlayerUtil.isStaff(player)) {
            ItemStack stack = null;
            if (profile.getEnchantingItem() != null) {
                stack = InventoryUtil.deserializeItemStack(profile.getEnchantingItem());
            }

            if (stack == null || !(stack.getType().equals(Material.LEATHER_LEGGINGS) ||
                    stack.getType().equals(Material.LEATHER_HELMET))) {
                lines.add("&c请先在神话之井中放入神话之甲后重试!");
            } else {
                String mythicColor = ItemUtil.getItemStringData(stack, "mythic_color");
                boolean canDye;

                if (stack.getType().equals(Material.LEATHER_HELMET)) {
                    canDye = mythicColor == null || !mythicColor.equalsIgnoreCase("dark");
                } else {
                    canDye = mythicColor != null && !mythicColor.equalsIgnoreCase("dark");
                }

                if (canDye) {
                    lines.add("&e点击前往!");
                } else {
                    lines.add("&c此颜色的神话之甲无法染色!");
                }
            }
        } else {
            lines.add("&c你需要购买 &e天坑乱斗增值包 &c后才能使用此功能!");
        }

        return new ItemBuilder(Material.ANVIL)
                .name((profile.getPrestige() >= 2 ? "&a" : "&c") + "前往染色工坊")
                .lore(lines)
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        if (!profile.isSupporter() && !PlayerUtil.isStaff(player)) {
            player.sendMessage(CC.translate("&c你需要购买天坑乱斗增值包后才能使用此功能!"));
            return;
        }

        ItemStack stack = InventoryUtil.deserializeItemStack(profile.getEnchantingItem());
        if (stack == null) {
            player.sendMessage(CC.translate("&c请先在神话之井中放入神话之甲!"));
            return;
        }

        if (!stack.getType().equals(Material.LEATHER_LEGGINGS) &&
                !stack.getType().equals(Material.LEATHER_HELMET)) {
            player.sendMessage(CC.translate("&c请放入神话之甲!"));
            return;
        }

        String mythicColor = ItemUtil.getItemStringData(stack, "mythic_color");

        boolean canDye;
        if (stack.getType().equals(Material.LEATHER_HELMET)) {
            canDye = mythicColor == null || !mythicColor.equalsIgnoreCase("dark");
        } else {
            canDye = mythicColor != null && !mythicColor.equalsIgnoreCase("dark");
        }

        if (canDye) {
            new DyeWorkshopMenu().openMenu(player);
        } else {
            player.sendMessage(CC.translate("&c此颜色的神话之甲无法染色!"));
        }
    }
}