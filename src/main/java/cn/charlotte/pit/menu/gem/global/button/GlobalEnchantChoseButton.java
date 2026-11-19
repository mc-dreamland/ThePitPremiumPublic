package cn.charlotte.pit.menu.gem.global.button;

import cn.charlotte.pit.data.sub.EnchantmentRecord;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.util.FuncsKt;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class GlobalEnchantChoseButton extends Button {

    private final ItemStack item;
    private final int index;
    private final int slot;
    private final AbstractEnchantment enchantment;
    private final int enchantLevel;

    public GlobalEnchantChoseButton(ItemStack item, int index, int slot, AbstractEnchantment enchantment, int enchantLevel) {
        this.item = item;
        this.index = index;
        this.slot = slot;
        this.enchantment = enchantment;
        this.enchantLevel = enchantLevel;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        String actionLine;
        if (Utils.canUseGen(item, true)) {
            if (enchantLevel >= 3) {
                actionLine = "&c已达最大等级";
            } else if (enchantment.getRarity().getParentType() != EnchantmentRarity.RarityType.RARE) {
                actionLine = "&c无法升级此附魔！";
            } else {
                actionLine = "&a点击使用!";
            }
        } else {
            actionLine = "&c无法作用于此附魔上";
        }

        return new ItemBuilder(item.getType())
                .name("&e升级第" + (slot + 1) + "号附魔")
                .lore(
                        "",
                        "&7附魔属性: &9" + enchantment.getEnchantName(),
                        "&7等级: &e" + enchantLevel,
                        "",
                        "&7消耗: &b举世瞩目的宝石",
                        "",
                        actionLine
                )
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        if (!Utils.canUseGen(item, true)) {
            player.closeInventory();
            return;
        }

        IMythicItem mythicItem = FuncsKt.toMythicItem(item);
        if (mythicItem == null) {
            return;
        }

        ItemStack indexedItem = player.getInventory().getItem(index);
        IMythicItem indexedMythicItem = FuncsKt.toMythicItem(indexedItem);
        if (indexedMythicItem == null || !isSameEnchant(mythicItem, indexedMythicItem)) {
            return;
        }

        Integer level = indexedMythicItem.getEnchantments().get(enchantment);
        if (level == null || level >= 3) {
            return;
        }

        if (enchantment.getRarity().getParentType() != EnchantmentRarity.RarityType.RARE) {
            return;
        }

        int maxedLevel = 0;
        for (Integer currentLevel : indexedMythicItem.getEnchantments().values()) {
            if (currentLevel >= 3) {
                maxedLevel++;
            }
        }

        if (maxedLevel >= 2 && level >= 2) {
            player.sendMessage(CC.translate("&c不合法"));
            player.closeInventory();
            return;
        }

        indexedMythicItem.getEnchantments().put(enchantment, level + 1);
        indexedMythicItem.getEnchantmentRecords().add(
                new EnchantmentRecord(player.getName(), "GlobalAttGem", System.currentTimeMillis())
        );
        indexedMythicItem.setBoostedByGlobalGem(true);
        player.getInventory().setItem(index, indexedMythicItem.toItemStack());

        player.closeInventory();
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.5f, 1.5f);
        player.sendMessage(CC.translate("&a使用成功!"));
        for (int inventoryIndex = 0; inventoryIndex < player.getInventory().getSize(); inventoryIndex++) {
            ItemStack itemStack = player.getInventory().getItem(inventoryIndex);
            String internalName = ItemUtil.getInternalName(itemStack);
            if ("global_attention_gem".equals(internalName)) {
                if (itemStack.getAmount() == 1) {
                    player.getInventory().setItem(inventoryIndex, null);
                } else {
                    itemStack.setAmount(itemStack.getAmount() - 1);
                    player.getInventory().setItem(inventoryIndex, itemStack);
                }
                return;
            }
        }
    }

    private boolean isSameEnchant(IMythicItem itemA, IMythicItem itemB) {
        outer:
        for (Map.Entry<AbstractEnchantment, Integer> enchantmentEntry : itemA.getEnchantments().entrySet()) {
            for (Map.Entry<AbstractEnchantment, Integer> enchantmentB : itemB.getEnchantments().entrySet()) {
                if (enchantmentEntry.getKey().getNbtName().equals(enchantmentB.getKey().getNbtName())
                        && enchantmentEntry.getValue().equals(enchantmentB.getValue())) {
                    continue outer;
                }
            }
            return false;
        }
        return true;
    }
}
