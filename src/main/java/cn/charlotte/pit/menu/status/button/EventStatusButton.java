package cn.charlotte.pit.menu.status.button;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EventStatusButton extends Button {
    @Override
    public ItemStack getButtonItem(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        List<String> lores = new ArrayList<>();

        if (profile.getEventData().getHistoricalevents().isEmpty()) {
            lores.add("&c你没有参与任何的事件!");
        } else {
            lores.add("&6&l参与过的事件统计:");
            lores.add("&e你从事件中获得声望数量:&b" + profile.getEventData().getEventrenown());
            lores.add("");
            Set<String> keys = profile.getEventData().getHistoricalevents().keySet();
            for (String key : keys) {
                lores.add("&e" + key + ":" + profile.getEventData().getHistoricalevents().get(key));
            }
        }

        return new ItemBuilder(Material.MAP).name("&e个人参与事件表现").lore(lores).build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {

    }
}
