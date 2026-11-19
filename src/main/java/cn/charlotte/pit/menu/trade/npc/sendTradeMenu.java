package cn.charlotte.pit.menu.trade.npc;

import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Slf4j
public class sendTradeMenu extends Menu {

    private int page = 0;

    @Override
    public String getTitle(Player player) {
        return String.format("发送请求 - 第%d页", page + 1);
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> map = new HashMap<>();
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.remove(player);
        int startIndex = page * 45;
        IntStream.range(0, 45).forEach(i -> {
            int index = startIndex + i;
            if (index < onlinePlayers.size()) {
                Player target = onlinePlayers.get(index);
                map.put(i, new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        return new ItemBuilder(Material.PLAYER_HEAD).durability(3).name("&a发送给 " + target.getName()).setSkullProperty(target).build();
                    }

                    @Override
                    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                        player.closeInventory();
                        player.performCommand(String.format("trade %s", target.getName()));
                    }
                });
            }
        });
        if (page > 0) map.put(45, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.ARROW).name("&a上一页").lore(String.format("&7第%d页", page)).build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                page--;
                openMenu(player);
            }
        });
        if ((page + 1) * 45 < onlinePlayers.size()) map.put(53, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.ARROW).name("&a下一页").lore(String.format("&7第%d页", page + 2)).build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                page++;
                openMenu(player);
            }
        });
        return map;
    }

    @Override
    public int getSize() {
        return 54;
    }
}