package cn.charlotte.pit.menu.perk.prestige;

import cn.charlotte.pit.menu.prestige.PrestigeMainMenu;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/4 18:37
 */
public class PrestigeMenu extends Menu {
    private static final ItemStack UPGRADE_ITEM = new ItemBuilder(Material.EMERALD)
            .name("&a升级").lore("§7各类升级、加成", "§7助你成为战场强者!", "§r", "§e点击浏览!").build();
    private static final ItemStack PERK_LIST = new ItemBuilder(Material.DIAMOND)
            .name("&b增益效果").lore("§7解锁更多不一样的增益效果", "§7解锁后可在商店购买使用!", "§r", "§e点击浏览!").build();
    private static final ItemStack ITEM_SHOP = new ItemBuilder(Material.GOLDEN_HORSE_ARMOR)
            .name("&6商店物品").lore("§7在物品商店中展示更多道具", "§r", "§e点击浏览!").build();
    private static final ItemStack STREAK = new ItemBuilder(Material.BLAZE_POWDER)
            .name("&6超级连杀增益").lore("§7解锁更多的超级连杀增益效果", "§r", "§e点击浏览!").build();


    public PrestigeMenu() {
    }


    @Override
    public String getTitle(Player player) {
        return "声望商店";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> button = new HashMap<>();


        button.put(10, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return UPGRADE_ITEM;
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                new PrestigePerkBuyMenu(PerkGroup.PRESTIGE).openMenu(player);
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return true;
            }
        });

        button.put(12, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return PERK_LIST;
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                new PrestigePerkBuyMenu(PerkGroup.BOOST).openMenu(player);
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return true;
            }
        });

        button.put(14, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return ITEM_SHOP;
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                new PrestigePerkBuyMenu(PerkGroup.SHOP).openMenu(player);
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return true;
            }
        });

        button.put(16, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return STREAK;
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                new PrestigePerkBuyMenu(PerkGroup.STREAK).openMenu(player);
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return true;
            }
        });

        button.put(
                26, new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        return new ItemBuilder(Material.ARROW).name("&f返回至上一页").lore("", "&f点击返回上一页", "").build();
                    }

                    @Override
                    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                        new PrestigeMainMenu().openMenu(player);
                    }
                }
        );

        return button;
    }

    @Override
    public int getSize() {
        return 3 * 9;
    }

    @Override
    public boolean isAutoUpdate() {
        return true;
    }
}
