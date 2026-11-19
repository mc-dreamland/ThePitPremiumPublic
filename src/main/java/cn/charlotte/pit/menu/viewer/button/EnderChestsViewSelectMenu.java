package cn.charlotte.pit.menu.viewer.button;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class EnderChestsViewSelectMenu extends Menu {
    private final PlayerProfile profile;

    public EnderChestsViewSelectMenu(PlayerProfile profile) {
        this.profile = profile;
    }

    @Override
    public String getTitle(Player player) {
        return "末影箱选择器";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> button = new HashMap<>();

        int index = 0;

        PlayerEnderChest[] enderChests = profile.getEnderChests();
        for (PlayerEnderChest playerEnderChest : enderChests) {
            int finalIndex = index;
            index++;
            button.put(9 + finalIndex * 2, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    if (finalIndex == 0) {
                        return new ItemBuilder(Material.ENDER_CHEST)
                                .name("&a末影箱 #" + finalIndex)
                                .lore(
                                        "§7大小: §e6§7/6",
                                        "§r",
                                        "§e左键 §7浏览末影箱!"
                                )
                                .build();
                    }

                    // 未解锁的末影箱
                    if (playerEnderChest == null) {
                        return new ItemBuilder(Material.CHEST)
                                .name("&c末影箱 #" + finalIndex + " §7(未解锁)")
                                .lore(
                                        "§7大小: §c0§7/6",
                                        "§r",
                                        "§c尚未解锁"
                                )
                                .build();
                    }

                    // 已解锁的末影箱
                    int rows = playerEnderChest.getRows();
                    return new ItemBuilder(Material.ENDER_CHEST)
                            .name("&a末影箱 #" + finalIndex)
                            .lore(
                                    "§7大小: §e" + rows + "§7/6",
                                    "§r",
                                    "§e左键 §7浏览末影箱!"
                            )
                            .build();
                }

                @Override
                public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                    // 只处理左键点击
                    if (!clickType.isLeftClick()) {
                        return;
                    }

                    // 第一个末影箱始终可用
                    if (finalIndex == 0) {
                        new EnderChestViewerMenu(profile, finalIndex).openMenu(player);
                        return;
                    }

                    PlayerEnderChest eChest = profile.getEnderChests(finalIndex);
                    if (eChest != null) {
                        new EnderChestViewerMenu(profile, finalIndex).openMenu(player);
                    } else {
                        player.sendMessage("§c该末影箱尚未解锁！");
                    }
                }
            });
        }

        return button;
    }

    @Override
    public int getSize() {
        return 27;
    }
}