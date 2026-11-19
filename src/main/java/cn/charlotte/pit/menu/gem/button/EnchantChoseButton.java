package cn.charlotte.pit.menu.gem.button;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EnchantChoseButton extends Button {

    private final ItemStack item;
    private final int index;
    private final int slot;
    private final AbstractEnchantment enchantment;
    private final int enchantLevel;

    public EnchantChoseButton(ItemStack item, int index, int slot,
                              AbstractEnchantment enchantment, int enchantLevel) {
        this.item = item;
        this.index = index;
        this.slot = slot;
        this.enchantment = enchantment;
        this.enchantLevel = enchantLevel;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        String actionLine;
        if (Utils.canUseGen(item, false)) {
            if (enchantLevel >= 3) {
                actionLine = "&c已达最大等级";
            } else if (enchantment.getRarity().getParentType() == EnchantmentRarity.RarityType.RARE) {
                actionLine = "&c稀有附魔";
            } else {
                actionLine = "&a点击使用!";
            }
        } else {
            actionLine = "&c无法作用于此附魔上";
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7附魔属性: &9" + enchantment.getEnchantName());
        lore.add("&7等级: &e" + enchantLevel);
        lore.add("");
        lore.add("&7消耗: &a一个遵纪守法的宝石");
        lore.add("");
        lore.add(actionLine);

        return new ItemBuilder(item.getType())
                .name("&e升级第" + (slot + 1) + "号附魔")
                .lore(lore)
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        if (!Utils.canUseGen(item, false)) {
            player.closeInventory();
            return;
        }

        IMythicItem mythicItem = FuncsKt.toMythicItem(item);
        if (mythicItem == null) {
            return;
        }

        ItemStack indexedItem = player.getInventory().getItem(this.index);
        IMythicItem indexedMythicItem = indexedItem != null ? FuncsKt.toMythicItem(indexedItem) : null;

        if (indexedMythicItem != null && isSameEnchant(mythicItem, indexedMythicItem)) {
            Integer currentLevel = indexedMythicItem.getEnchantments().get(enchantment);
            if (currentLevel == null) return;

            if (currentLevel >= 3) return;
            if (enchantment.getRarity().getParentType() == EnchantmentRarity.RarityType.RARE) return;

            int maxedLevel = 0;
            for (Map.Entry<AbstractEnchantment, Integer> entry : indexedMythicItem.getEnchantments().entrySet()) {
                Integer lvl = entry.getValue();
                if (lvl != null && lvl >= 3) {
                    maxedLevel++;
                }
            }

            // 不能从 3-3-2 => 3-3-3
            if (maxedLevel >= 2 && currentLevel >= 2) {
                player.sendMessage(CC.translate("&c不合法"));
                player.closeInventory();
                return;
            }

            indexedMythicItem.getEnchantments().put(enchantment, currentLevel + 1);

            indexedMythicItem.getEnchantmentRecords().add(new EnchantmentRecord(
                    player.getName(),
                    "totally_legit_gem",
                    System.currentTimeMillis()
            ));

            indexedMythicItem.setBoostedByGem(true);

            player.getInventory().setItem(this.index, indexedMythicItem.toItemStack());
        } else {
            return;
        }

        player.closeInventory();
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.5f, 1.5f);
        player.sendMessage(CC.translate("&a使用成功!"));

        // 扣除一枚 “totally_legit_gem”
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack is = player.getInventory().getItem(i);
            if (is == null) continue;

            String internalName = ItemUtil.getInternalName(is);
            if ("totally_legit_gem".equals(internalName)) {
                if (is.getAmount() == 1) {
                    player.getInventory().setItem(i, null);
                } else {
                    is.setAmount(is.getAmount() - 1);
                    player.getInventory().setItem(i, is);
                }
                return;
            }
        }
    }

    private boolean isSameEnchant(IMythicItem itemA, IMythicItem itemB) {
        for (Map.Entry<AbstractEnchantment, Integer> ea : itemA.getEnchantments().entrySet()) {
            boolean found = false;
            for (Map.Entry<AbstractEnchantment, Integer> eb : itemB.getEnchantments().entrySet()) {
                if (Objects.equals(ea.getKey().getNbtName(), eb.getKey().getNbtName())
                        && Objects.equals(ea.getValue(), eb.getValue())) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }
}
