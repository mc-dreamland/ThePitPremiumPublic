package cn.charlotte.pit.menu.mythicrepair;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitConsumeItemEvent;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.type.mythic.LuckyChestplate;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import cn.charlotte.pit.util.menu.builder.ButtonBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class SmallMythicRepairKitMenu extends Menu {

    @Override
    public String getTitle(Player player) {
        return "选择要修复的神话装备";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        int slot = 0;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack itemStack = player.getInventory().getItem(i);
            if (itemStack == null || itemStack.getType() == org.bukkit.Material.AIR) continue;

            IMythicItem mythicItem = MythicUtil.getMythicItem(itemStack);
            if (mythicItem == null) continue;

            if (mythicItem instanceof LuckyChestplate) continue;

            if (mythicItem.live >= mythicItem.maxLive) continue;

            final int inventorySlot = i;
            Button button = new ButtonBuilder(itemStack).addClickHandler((player1, slot1, clickType, hotbarButton, currentItem) -> {
                PlayerUtil.takeOneItem(player, EquipmentSlot.HAND);
                PlayerProfile profile = Util.getPitProfile(player1);

                int newLive = Math.min(mythicItem.live + 3, mythicItem.maxLive);
                int repairedAmount = newLive - mythicItem.live;
                mythicItem.live = newLive;

                player.getInventory().setItem(inventorySlot, mythicItem.toItemStack());

                new PitConsumeItemEvent(player, "Small_mythic_repair_kit").callEvent();
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1.0F, 1.0F);
                CC.send(player, "&d&l简易工匠包! &7成功修复 &b" + repairedAmount + " &7条生命.");
                CC.send(player, "&a当前命数: &e" + newLive + "&7/&6" + mythicItem.maxLive);
                player.closeInventory();
            }).build();
            buttons.put(slot++, button);
        }

        return buttons;
    }
}