package cn.charlotte.pit.menu.enderchests;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.impl.PlayerPointsAPIImpl;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import cn.charlotte.pit.util.menu.menus.ConfirmMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/13 14:57
 */
public class EnderChestsMenu extends Menu {
    private final PlayerProfile profile;

    public EnderChestsMenu(PlayerProfile profile) {
        this.profile = profile;
    }

    @Override
    public String getTitle(Player player) {
        return "末影箱";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> button = new HashMap<>();


        int index = 0;
        PlayerEnderChest defaultEnderChest = profile.getDefaultEnderChest();

        boolean isVip = profile.isSupporter();
        if (isVip && defaultEnderChest.getRows() <= 3) {
            PlayerEnderChest newDefaultEnderChest = new PlayerEnderChest(6);
            newDefaultEnderChest.getInventory().setContents(defaultEnderChest.getInventory().getContents());
            profile.setEnderChests(newDefaultEnderChest, 0);
        }

        PlayerEnderChest[] enderChests = profile.getEnderChests();
        for (PlayerEnderChest playerEnderChest : enderChests) {
            int finalIndex = index;
            index++;
            button.put(9 + finalIndex * 2, new Button() {
                int index = 0;

                @Override
                public ItemStack getButtonItem(Player player) {
                    ItemStack build;

                    if (finalIndex == 0) {
                        ItemBuilder itemBuilder = new ItemBuilder(Material.ENDER_CHEST)
                                .name("&a末影箱 #" + finalIndex);
                        if (isVip) {
                            itemBuilder
                                    .lore(
                                            "§7大小: §e6§7/§66 §7(§eVIP+3§7)",
                                            "§7存储你的物资!",
                                            "§r",
                                            "§e左键 §7浏览末影箱!"
                                    );
                        } else {
                            itemBuilder
                                    .lore(
                                            "§7大小: §e3§7/§66 §7增值包或VIP5及以上+3行",
                                            "§7存储你的物资!",
                                            "§r",
                                            "§e左键 §7浏览末影箱!"
                                    );
                        }
                        build = itemBuilder.build();
                    } else if (playerEnderChest == null) {
                        build = new ItemBuilder(Material.CHEST)
                                .name("&a末影箱 #" + finalIndex + "§7(§c未解锁§7)")
                                .lore(
                                        "§7大小: §e0" + "§7/§66",
                                        "§7存储你的物资!",
                                        "§r",
                                        "§e右键 §c解锁一行 §7(§b1000点券§7)"
                                )
                                .build();
                    } else {
                        int rows = playerEnderChest.getRows();
                        ItemBuilder itemBuilder = new ItemBuilder(Material.ENDER_CHEST)
                                .name("&a末影箱 #" + finalIndex);
                        if (rows == 6) {
                            itemBuilder.lore(
                                    "§7大小: §e" + rows + "§7/§66",
                                    "§7存储你的物资!",
                                    "§r",
                                    "§e左键 §7浏览末影箱!"
                            );
                        } else {
                            itemBuilder.lore(
                                    "§7大小: §e" + rows + "§7/§66",
                                    "§7存储你的物资!",
                                    "§r",
                                    "§e左键 §7浏览末影箱!",
                                    "§e右键 §c解锁一行 §7(§b1000点券§7)"
                            );
                        }
                        build = itemBuilder.build();
                    }
                    return build;
                }

                @Override
                public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                    if (index != 0) {
                        return;
                    }
                    index++;
                    if (profile.isTempInvUsing()) {
                        player.sendMessage(CC.translate("&c事件期间无法使用末影箱!"));
                        return;
                    }
                    boolean leftClick = clickType.isLeftClick();

                    if (leftClick) {
                        PlayerEnderChest eChest = profile.getEnderChests(finalIndex);
                        if (eChest != null) {
                            eChest.openEnderChest(player);
                        }
                        return;
                    }
                    if (finalIndex == 0) {
                        return;
                    }

                    boolean hasPoints = PlayerPointsAPIImpl.INSTANCE.hasPoints(player, 1000);
                    if (!hasPoints) {
                        player.sendMessage("§c点券余额不足!");
                        return;
                    }
                    PlayerEnderChest ec = profile.getEnderChests(finalIndex);
                    if (ec == null) {
                        ec = new PlayerEnderChest(1);
                    } else {
                        int rows = ec.getRows();
                        if (rows == 6) {
                            return;
                        }
                        PlayerEnderChest newEc = new PlayerEnderChest(rows + 1);
                        newEc.getInventory().setContents(ec.getInventory().getContents());
                        ec = newEc;
                    }
                    PlayerPointsAPIImpl.INSTANCE.costPoints(player, 1000, "扩容末影箱");
                    profile.setEnderChests(ec, finalIndex);

                    openMenu(player);
                }
            });
        }
        button.put(18, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.EMERALD)
                        .name("&a自动扩容末影箱")
                        .lore(
                                "§7自动为第一个可扩容的末影箱增加一行",
                                "§7价格: §b1000 点券",
                                "§r",
                                "§e点击 §7自动扩容"
                        )
                        .build();
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return true;
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                if (profile.isTempInvUsing()) {
                    player.sendMessage(CC.translate("&c事件期间无法购买末影箱!"));
                    return;
                }

                boolean hasPoints = PlayerPointsAPIImpl.INSTANCE.hasPoints(player, 1000);
                if (!hasPoints) {
                    player.sendMessage("§c点券余额不足! 需要 1000 点券。");
                    return;
                }

                int targetIndex = findFirstExpandableEnderChest();
                if (targetIndex == -1) {
                    player.sendMessage(CC.translate("&a所有末影箱都已达到最大容量!"));
                    return;
                }

                new ConfirmMenu("确认对末影箱进行扩容?", confirm -> {
                    if (confirm) {
                        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
                            expandEnderChest(targetIndex, player);
                        }, 2L);
                    }
                    new EnderChestsMenu(Util.getPitProfile(player)).openMenu(player);
                }, true, 3, new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        return new ItemBuilder(Material.ENDER_CHEST).name("&e末影箱扩容").lore("", "&f你将要花费 &b100 点券", "&f进行末影箱扩容", "").build();
                    }

                    @Override
                    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {

                    }
                }).openMenu(player);
            }

            private int findFirstExpandableEnderChest() {
                PlayerEnderChest[] enderChests = profile.getEnderChests();
                if (enderChests == null) return -1;

                for (int i = 1; i < enderChests.length; i++) {
                    PlayerEnderChest chest = enderChests[i];
                    if (chest == null) {
                        return i;
                    }
                    if (chest.getRows() < 6) {
                        return i;
                    }
                }
                return -1;
            }

            private void expandEnderChest(int index, Player player) {
                PlayerEnderChest currentChest = profile.getEnderChests(index);
                PlayerEnderChest newChest;

                if (currentChest == null) {
                    newChest = new PlayerEnderChest(1);
                } else {
                    int newRows = currentChest.getRows() + 1;
                    newChest = new PlayerEnderChest(newRows);
                    newChest.getInventory().setContents(currentChest.getInventory().getContents());
                }
                PlayerPointsAPIImpl.INSTANCE.costPoints(player, 1000, "自动扩容末影箱");
                profile.setEnderChests(newChest, index);

                player.sendMessage(CC.translate("&a成功扩容末影箱 #" + index + "! 当前大小: " + newChest.getRows() + "/6 行"));
                new EnderChestsMenu(profile).openMenu(player);
            }
        });

        return button;
    }

    @Override
    public int getSize() {
        return 27;
    }

}