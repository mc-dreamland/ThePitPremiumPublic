package cn.charlotte.pit.menu.admin.CopyInv;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.AdvancedItemIdentity;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CopyInvMenu extends Menu {
    private final PlayerProfile targetProfile;
    private final Player viewer;

    public CopyInvMenu(Player viewer, PlayerProfile targetProfile) {
        this.viewer = viewer;
        this.targetProfile = targetProfile;
        openMenu(viewer);
    }

    @Override
    public String getTitle(Player player) {
        return "查看玩家数据: " + targetProfile.getPlayerName();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        buttons.put(10, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.CHEST)
                        .name("&a玩家背包")
                        .lore(
                                "&7查看玩家的背包内容",
                                "&r",
                                "&e点击 &7查看背包物品"
                        )
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                PlayerInv inventory = targetProfile.getInventory();
                if (inventory != null) {
                    new InventoryViewMenu("背包 - " + targetProfile.getPlayerName(), inventory.getContents()).openMenu(player);
                } else {
                    player.sendMessage(CC.translate("&c该玩家的背包数据为空!"));
                }
            }
        });

        PlayerEnderChest[] enderChests = targetProfile.getEnderChests();
        for (int i = 0; i < 5; i++) {
            final int chestIndex = i;
            buttons.put(12 + i, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    PlayerEnderChest chest = enderChests != null && enderChests.length > chestIndex ? enderChests[chestIndex] : null;

                    if (chest == null) {
                        return new ItemBuilder(Material.ENDER_CHEST)
                                .name("&c末影箱 #" + (chestIndex + 1))
                                .lore(
                                        "&7该末影箱未解锁或为空",
                                        "&r",
                                        "&c无法查看"
                                )
                                .build();
                    }

                    return new ItemBuilder(Material.ENDER_CHEST)
                            .name("&a末影箱 #" + (chestIndex + 1))
                            .lore(
                                    "&7大小: &e" + chest.getRows() + "行",
                                    "&7查看玩家的末影箱内容",
                                    "&r",
                                    "&e点击 &7查看末影箱物品"
                            )
                            .build();
                }

                @Override
                public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                    if (enderChests != null && enderChests.length > chestIndex) {
                        PlayerEnderChest chest = enderChests[chestIndex];
                        if (chest != null) {
                            new InventoryViewMenu("末影箱 #" + (chestIndex + 1) + " - " + targetProfile.getPlayerName(),
                                    chest.getInventory().getContents()).openMenu(player);
                        } else {
                            player.sendMessage(CC.translate("&c该末影箱数据为空!"));
                        }
                    } else {
                        player.sendMessage(CC.translate("&c该末影箱未解锁!"));
                    }
                }
            });
        }

        return buttons;
    }

    @Override
    public int getSize() {
        return 27;
    }


    static class InventoryViewMenu extends Menu {
        private final String title;
        private final ItemStack[] items;

        public InventoryViewMenu(String title, ItemStack[] items) {
            this.title = title;
            this.items = items;
        }

        @Override
        public String getTitle(Player player) {
            return title;
        }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();

            for (int i = 0; i < items.length && i < getSize(); i++) {
                final ItemStack item = items[i];

                buttons.put(i, new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        if (item != null && item.getType() != Material.AIR) {
                            ItemStack displayItem = item.clone();
                            ItemBuilder builder = new ItemBuilder(displayItem);

                            builder.lore(
                                    "&r",
                                    "&e左键 &7复制物品数据",
                                    "&e右键 &7获取UUID"
                            );

                            return builder.build();
                        }
                        return new ItemStack(Material.AIR);
                    }

                    @Override
                    public void clicked(Player player, int clickedSlot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                        if (item == null || item.getType() == Material.AIR) {
                            return;
                        }

                        if (clickType.isLeftClick()) {
                            ItemStack cloned = item.clone();
                            player.getInventory().addItem(cloned);
                        } else if (clickType.isRightClick()) {
                            var uuid = AdvancedItemIdentity.readUuid(item);
                            player.sendMessage(uuid == null ? "当前物品没有物品 UUID。" : uuid.toString());
                        }
                    }
                });
            }

            buttons.put(getSize() - 1, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(Material.ARROW)
                            .name("&c返回")
                            .lore("&7返回上一级菜单")
                            .build();
                }

                @Override
                public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                    player.closeInventory();
                }
            });

            return buttons;
        }

        @Override
        public int getSize() {
            int size = (int) Math.ceil(items.length / 9.0) * 9;
            return Math.max(9, Math.min(54, size + 9));
        }

        @Override
        public void onOpen(Player player) {
            if (getSize() > 54) {
                player.sendMessage(CC.translate("&c物品太多，无法完全显示!"));
            }
        }
    }
}
