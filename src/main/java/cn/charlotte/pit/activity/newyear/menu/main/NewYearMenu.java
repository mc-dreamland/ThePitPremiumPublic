package cn.charlotte.pit.activity.newyear.menu.main;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.newyear.NewYearData;
import cn.charlotte.pit.activity.newyear.menu.buy.BuyMenu;
import cn.charlotte.pit.activity.newyear.menu.perk.PerkMenu;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class NewYearMenu extends Menu {

    private static final Map<Integer, Button> buttonMap = Map.of(
            10, new Button() {

                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(Material.CAULDRON).name("&b活动商店").lore("", "&e点击进入商店", "").build();
                }

                @Override
                public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                    new BuyMenu().openMenu(player);
                }
            },
            13, new Button() {

                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(Material.BELL).name("&e天赋").lore("", "&e查看天赋", "").build();
                }

                @Override
                public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                    new PerkMenu().openMenu(player);
                }
            },
            16, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    NewYearData data = Util.getPitProfile(player).getActivityPlayerData().getNewYearData();
                    return new ItemBuilder(Material.PAPER).name("&f活动点数").lore("", String.format("&e活动点数: &l%s", data.getCoins()), String.format("&b特殊活动点数: &l%s", data.getPoints()), "").build();
                }

                @Override
                public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {

                }
            }
    );

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public String getTitle(Player player) {
        return "新年";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        return buttonMap;
    }
}
