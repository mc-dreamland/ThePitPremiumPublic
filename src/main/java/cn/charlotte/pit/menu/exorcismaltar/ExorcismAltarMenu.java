package cn.charlotte.pit.menu.exorcismaltar;

import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public class ExorcismAltarMenu extends Menu {


    @Override
    public String getTitle(Player player) {
        return "驱魔台";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        return Map.of();
    }

    @Override
    public void onClickEvent(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

    }
}
