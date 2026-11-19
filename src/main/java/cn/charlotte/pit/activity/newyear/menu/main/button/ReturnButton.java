package cn.charlotte.pit.activity.newyear.menu.main.button;

import cn.charlotte.pit.activity.newyear.menu.main.NewYearMenu;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class ReturnButton extends Button {

    @Override
    public ItemStack getButtonItem(Player player) {
        return R_PAGE;
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        new NewYearMenu().openMenu(player);
    }
}
