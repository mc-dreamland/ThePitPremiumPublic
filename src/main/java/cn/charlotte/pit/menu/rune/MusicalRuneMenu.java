package cn.charlotte.pit.menu.rune;

import cn.charlotte.pit.Assembly;
import cn.charlotte.pit.menu.rune.button.ItemRuneButton;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class MusicalRuneMenu extends Menu {

    @Override
    public String getTitle(Player player) {
        return "乐章谱写";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> map = new HashMap<>();
        int index = 0;
        int slot = 0;
        for (ItemStack itemStack : player.getInventory()) {
            if (isItemValid(itemStack)) {
                map.put(index++, new ItemRuneButton(itemStack, slot));
            }
            slot++;
        }
        return map;
    }

    private boolean isItemValid(ItemStack itemStack) {
        return itemStack != null
                && itemStack.getType() == Material.LEATHER_LEGGINGS
                && Utils.getMythicItem(itemStack) != null
                && Assembly.music.stream().noneMatch(enchant -> enchant.isItemHasEnchant(itemStack));
    }

    @Override
    public void onClickEvent(InventoryClickEvent event) {
        if (event != null) {
            event.setCancelled(true);
        }
    }
}
