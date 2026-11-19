package cn.charlotte.pit.activity.newyear.menu.perk;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.newyear.menu.main.NewYearMenu;
import cn.charlotte.pit.activity.newyear.menu.perk.button.MythicPerkButton;
import cn.charlotte.pit.activity.newyear.menu.perk.button.potionPerkButton;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class PerkMenu extends Menu {

    @Override
    public String getTitle(Player player) {
        return "天赋";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        PlayerProfile profile = Util.getPitProfile(player);
        return Map.of(
                10, new potionPerkButton(profile.getActivityPlayerData().getNewYearData()),
                12, new MythicPerkButton(),
                26, new Button() {

                    @Override
                    public ItemStack getButtonItem(Player player) {
                        return R_PAGE;
                    }

                    @Override
                    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                        new NewYearMenu().openMenu(player);
                    }
                }
        );
    }
}
