package cn.charlotte.pit.menu.quest.sanctuary.button;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ActionGiftBox extends Button {
    @Override
    public ItemStack getButtonItem(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        int data = profile.getLevel(PerksPrestigeEnum.CONTRACTOR);
        List<String> lore = new ArrayList<>();
        lore.add("&7死亡时保留");
        lore.add("");
        lore.add("&e拿着并右键获得物品!");
        lore.add(" ");
        lore.add("&7价格: &e50 行动赏金");
        lore.add("&7你当前持有: &e" + profile.getActionBounty() + " 行动赏金");
        lore.add(" ");
        if (profile.isBetterPlayer()) {
            if (50 > profile.getActionBounty()) {
                lore.add("&c你没有足够的行动赏金!");
            } else {
                lore.add("&e点击购买!");
            }
        } else {
            if (data >= 3) {
                if (50 > profile.getActionBounty()) {
                    lore.add("&c你没有足够的行动赏金!");
                } else {
                    lore.add("&e点击购买!");
                }
            } else {
                lore.add("&7你的精通增益 " + PerksPrestigeEnum.CONTRACTOR.getPerk().getDisplayName() + " &7等级为&c&l" + data);
                lore.add("&7需要 &c&l3 &7级 才能购买此物品");
            }
        }
        return new ItemBuilder(Material.CHEST).name("&e行动礼盒").lore(lore).build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        if (InventoryUtil.isInvFull(player.getInventory())) {
            player.sendMessage(CC.translate("&c你的背包已满!"));
            return;
        }
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile.getLevel(PerksPrestigeEnum.CONTRACTOR) < 3 && !profile.isBetterPlayer()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.sendMessage(CC.translate("&c你的声望增益 &e" + PerksPrestigeEnum.CONTRACTOR.getCategory().getDisplayName() + " &c并未达到 3 级"));
            return;
        }
        if (profile.getActionBounty() >= 50) {
            profile.setActionBounty(profile.getActionBounty() - 50);
            player.getInventory().addItem(PitItems.ACTION_GIFT_BOX.toItemStack());
            player.sendMessage(CC.translate("&a&l+ &e行动礼盒"));
            player.sendMessage(CC.translate("&c&l- &e&l50 &e行动赏金"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.sendMessage(CC.translate("&c你的行动赏金没有足够的数量来支付此物品"));
        }
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }
}
