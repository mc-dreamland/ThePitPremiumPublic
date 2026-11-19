package cn.charlotte.pit.menu.cactus;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.EnchantmentRecord;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.enchantment.type.rage.NewDealEnchant;
import cn.charlotte.pit.enchantment.type.ragerare.RegularityEnchant;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.ChatComponentBuilder;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SacrosanctCactusMenu extends Menu {

    private final Button pantsButton = new Button() {
        private final Map<MythicColor, Integer> colorMaxLiveMap = Map.of(
                MythicColor.DARK, 40,
                MythicColor.RED, 24,
                MythicColor.ORANGE, 24,
                MythicColor.BLUE, 24,
                MythicColor.GREEN, 24,
                MythicColor.YELLOW, 24,
                MythicColor.AQUA, 24,
                MythicColor.RAGE, 24
        );

        private final Map<MythicColor, Integer> colorTierMap = Map.of(
                MythicColor.DARK, 2,
                MythicColor.RED, 1,
                MythicColor.ORANGE, 1,
                MythicColor.BLUE, 1,
                MythicColor.GREEN, 1,
                MythicColor.YELLOW, 1,
                MythicColor.AQUA, 1,
                MythicColor.RAGE, 1
        );

        @Override
        public ItemStack getButtonItem(Player player) {
            List<Map<AbstractEnchantment, Integer>> enchantments = new ArrayList<>();
            List<AbstractEnchantment> possibleEnchantments = ThePit.getApi().armorEnchantments();
            ThreadLocalRandom random = ThreadLocalRandom.current();
            MythicColor[] colors = {
                    MythicColor.RED,
                    MythicColor.DARK,
                    MythicColor.ORANGE,
                    MythicColor.BLUE,
                    MythicColor.GREEN,
                    MythicColor.YELLOW,
                    MythicColor.AQUA,
                    MythicColor.RAGE
            };

            do {
                populateEnchantments(enchantments, possibleEnchantments, random);
            } while (enchantments.isEmpty());

            MythicColor color = colors[random.nextInt(colors.length)];
            IMythicItem item = new MythicLeggingsItem();
            item.setColor(color);

            Map<AbstractEnchantment, Integer> randomEnchantmentMap = enchantments.get(random.nextInt(enchantments.size()));
            item.setEnchantments(randomEnchantmentMap);
            item.setTier(colorTierMap.getOrDefault(color, 1));
            item.setMaxLive(colorMaxLiveMap.getOrDefault(color, 24));
            item.setLive(colorMaxLiveMap.getOrDefault(color, 0));
            item.getEnchantmentRecords().add(new EnchantmentRecord(
                    player.getName(),
                    "神圣仙人掌",
                    System.currentTimeMillis()
            ));

            if (containsRarity(item.getEnchantments(), EnchantmentRarity.DARK_RARE, EnchantmentRarity.DARK_NORMAL)
                    && item.getColor() != MythicColor.DARK) {
                item.setColor(MythicColor.DARK);
            }
            if (containsRarity(item.getEnchantments(), EnchantmentRarity.RAGE_RARE, EnchantmentRarity.RAGE)
                    && item.getColor() != MythicColor.RAGE) {
                item.setColor(MythicColor.RAGE);
            }

            if (item.getColor() == MythicColor.RAGE
                    && hasNonMatchingRarity(item.getEnchantments(), EnchantmentRarity.RAGE, EnchantmentRarity.RAGE_RARE)) {
                item.setColor(MythicColor.RED);
                item.setLive(24);
                item.setMaxLive(24);
                item.setTier(1);
            }
            if (item.getColor() == MythicColor.DARK
                    && hasNonMatchingRarity(item.getEnchantments(), EnchantmentRarity.DARK_RARE, EnchantmentRarity.DARK_NORMAL)) {
                item.setColor(MythicColor.BLUE);
                item.setLive(24);
                item.setMaxLive(24);
                item.setTier(1);
            }
            if (item.getColor() == MythicColor.DARK && (item.getTier() == 1 || item.getLive() == 24 || item.getMaxLive() == 24)) {
                item.setLive(40);
                item.setMaxLive(40);
                item.setTier(2);
            }
            return item.toItemStack();
        }

        private void populateEnchantments(
                List<Map<AbstractEnchantment, Integer>> enchantments,
                List<AbstractEnchantment> possibleEnchantments,
                ThreadLocalRandom random
        ) {
            enchantments.clear();
            for (AbstractEnchantment possibleEnchantment : possibleEnchantments) {
                Map<AbstractEnchantment, Integer> enchantment = new HashMap<>();
                double probability;
                if (possibleEnchantment.getRarity() == EnchantmentRarity.RAGE_RARE) {
                    probability = 0.01;
                } else if (possibleEnchantment.getRarity() == EnchantmentRarity.RAGE) {
                    probability = 0.1;
                } else if (possibleEnchantment.getRarity() == EnchantmentRarity.DARK_RARE) {
                    probability = 0.02;
                } else if (possibleEnchantment.getRarity() == EnchantmentRarity.DARK_NORMAL) {
                    probability = 0.1;
                } else if (possibleEnchantment.getRarity() == EnchantmentRarity.RARE) {
                    probability = 0.1;
                } else if (possibleEnchantment.getRarity() == EnchantmentRarity.NORMAL) {
                    probability = 0.5;
                } else if (possibleEnchantment instanceof RegularityEnchant || possibleEnchantment instanceof NewDealEnchant) {
                    probability = 0.01;
                } else {
                    probability = 0.0;
                }

                if (random.nextDouble() < probability) {
                    enchantment.put(possibleEnchantment, 1);
                    enchantments.add(enchantment);
                }
            }
        }

        private boolean containsRarity(Map<AbstractEnchantment, Integer> enchantments, EnchantmentRarity... rarities) {
            for (AbstractEnchantment enchantment : enchantments.keySet()) {
                for (EnchantmentRarity rarity : rarities) {
                    if (enchantment.getRarity() == rarity) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean hasNonMatchingRarity(Map<AbstractEnchantment, Integer> enchantments, EnchantmentRarity... allowedRarities) {
            for (AbstractEnchantment enchantment : enchantments.keySet()) {
                boolean matched = false;
                for (EnchantmentRarity rarity : allowedRarities) {
                    if (enchantment.getRarity() == rarity) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            if (InventoryUtil.isInvFull(player.getInventory())) {
                player.sendMessage(CC.translate("&c你的背包已满！"));
                return;
            }

            IMythicItem mythicItem = Utils.getMythicItem(currentItem);
            if (mythicItem != null && containsRarity(mythicItem.getEnchantments(), EnchantmentRarity.DARK_RARE, EnchantmentRarity.RAGE_RARE, EnchantmentRarity.RARE)) {
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                var text = new ChatComponentBuilder(CC.translate(
                        "&d&l稀有附魔! &7"
                                + profile.getFormattedNameWithRoman()
                                + " &7在神圣仙人掌中抽到了稀有物品: "
                                + currentItem.getItemMeta().getDisplayName()
                                + " &e[查看]"
                )).build();
                text.hoverEvent(currentItem.asHoverEvent());

                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendMessage(text);
                }
            }

            player.getInventory().addItem(currentItem);
            player.closeInventory();
            player.sendMessage(CC.translate("&a&l奖励已领取！ " + currentItem.getItemMeta().getDisplayName()));
        }
    };

    @Override
    public String getTitle(Player player) {
        return "神圣仙人掌 (选择其一)";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        PlayerUtil.takeOneItem(player, EquipmentSlot.HAND);
        player.sendMessage(CC.translate("&a成功使用，请不要关闭领取界面，请选择您的奖励！"));
        Map<Integer, Button> buttons = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            buttons.put(i, pantsButton);
        }
        return buttons;
    }
}
