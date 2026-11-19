package cn.charlotte.pit.menu.gem;

import cn.charlotte.pit.menu.gem.button.ItemGemButton;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class TotallyLegitGemMenu extends Menu {

    @Override
    public String getTitle(Player player) {
        return "宝石点缀";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> map = new HashMap<>();
        int index = 0;
        int slot = 0;
        for (ItemStack itemStack : player.getInventory()) {
            if (Utils.canUseGen(itemStack, false)) {
                map.put(index, new ItemGemButton(itemStack, slot));
                index++;
            }
            slot++;
        }
        return map;
    }

    @Override
    public void onClickEvent(InventoryClickEvent event) {
        if (event != null) {
            event.setCancelled(true);
        }
    }
}
