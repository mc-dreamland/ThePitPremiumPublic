package cn.charlotte.pit.map.kingsquests.ui;

import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.util.FuncsKt;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class CakeBakeUI extends Menu {

    public static final CakeBakeUI INSTANCE = new CakeBakeUI();

    private CakeBakeUI() {
    }

    @Override
    public String getTitle(Player player) {
        return "烘焙大师";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(13, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.CAKE)
                        .name("&a制作&d迷你蛋糕")
                        .lore(
                                "&7你需要准备材料:",
                                " &d樱桃 &7x1",
                                " &f牛奶 &7x6",
                                " &a糖 &7x32",
                                " &e高级鸡蛋 &7x12",
                                " &e干草块 &7x96",
                                "",
                                "&a点击制作!"
                        )
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                Inventory inventory = player.getInventory();
                Map<String, Integer> playerItemCounts = new HashMap<>();
                for (ItemStack itemStack : inventory.getContents()) {
                    if (itemStack != null && itemStack.getType() != Material.AIR) {
                        String internalName = ItemUtil.getInternalName(itemStack);
                        if (internalName == null) {
                            continue;
                        }
                        if ("packaged_bale".equals(internalName)) {
                            boolean canDrop = ItemUtil.canDrop(itemStack);
                            if (!canDrop) {
                                playerItemCounts.put("packaged_bale_hay", playerItemCounts.getOrDefault(internalName, 0) + itemStack.getAmount());
                            }
                        } else {
                            playerItemCounts.put(internalName, playerItemCounts.getOrDefault(internalName, 0) + itemStack.getAmount());
                        }
                    }
                }

                int cherry = playerItemCounts.getOrDefault("cherry", 0);
                int sugar = playerItemCounts.getOrDefault("sugar", 0);
                int highGradeEggs = playerItemCounts.getOrDefault("high-grade_eggs", 0);
                int packagedBale = playerItemCounts.getOrDefault("packaged_bale_hay", 0);
                int milkBucket = playerItemCounts.getOrDefault("milk_bucket", 0);

                if (cherry < 1) {
                    player.sendMessage(CC.translate("&c你缺少 &d樱桃 &f*" + (1 - cherry) + "&c!"));
                    return;
                }
                if (milkBucket < 6) {
                    player.sendMessage(CC.translate("&c你缺少 &f牛奶 &f*" + (6 - milkBucket) + "&c!"));
                    return;
                }
                if (sugar < 32) {
                    player.sendMessage(CC.translate("&c你缺少 &a糖 &f*" + (32 - sugar) + "&c!"));
                    return;
                }
                if (highGradeEggs < 12) {
                    player.sendMessage(CC.translate("&c你缺少 &e高级鸡蛋 &f*" + (12 - highGradeEggs) + " &c!"));
                    return;
                }
                if (packagedBale < 96) {
                    player.sendMessage(CC.translate("&c你缺少 &e干草块 &f*" + (96 - packagedBale) + " &c!"));
                    return;
                }

                FuncsKt.takeItem(inventory, 1, it -> "cherry".equals(ItemUtil.getInternalName(it)));
                FuncsKt.takeItem(inventory, 32, it -> "sugar".equals(ItemUtil.getInternalName(it)));
                FuncsKt.takeItem(inventory, 12, it -> "high-grade_eggs".equals(ItemUtil.getInternalName(it)));
                FuncsKt.takeItem(inventory, 96, it -> "packaged_bale_hay".equals(ItemUtil.getInternalName(it))
                        || ("packaged_bale".equals(ItemUtil.getInternalName(it)) && ItemUtil.canDrop(it)));
                FuncsKt.takeItem(inventory, 6, it -> "milk_bucket".equals(ItemUtil.getInternalName(it)));
                inventory.addItem(PitItems.MINI_CAKE.toItemStack());
            }
        });
        buttons.put(26, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.PAPER)
                        .name("&f&o制作指南")
                        .lore(
                                " &d樱桃 &f-> &7击杀玩家有0.0001%概率获得",
                                " &f牛奶 &f-> &7下水道之箱中获得",
                                " &a糖 &f-> &7拿斧头挖掘西瓜获得",
                                " &e高级鸡蛋 &f-> &7拿美味的面包喂小鸡获得",
                                " &e美味的面包 &f-> &7拿一组小麦右键熔炉获得",
                                " &e干草块 &f-> &7拿一组小麦右键工作台获得",
                                " &a小麦 &f-> &7挖掘成熟的小麦获得"
                        )
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            }
        });
        return buttons;
    }

    @Override
    public int size(Map<Integer, Button> buttons) {
        return 3 * 9;
    }
}
