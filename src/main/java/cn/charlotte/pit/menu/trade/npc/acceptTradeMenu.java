package cn.charlotte.pit.menu.trade.npc;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.temp.TradeRequest;
import cn.charlotte.pit.npc.type.TradeNPC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.IntStream;

@Getter
@Setter
public class acceptTradeMenu extends Menu {

    private final List<Button> buttonList;
    private List<TradeRequest> tradeRequests;
    private int page;

    public acceptTradeMenu(List<TradeRequest> tradeRequests, Player player) {
        this.tradeRequests = Util.ifNullOrEmpty(tradeRequests, TradeNPC.getPendingRequests(player));
        this.page = 0;
        this.buttonList = new ArrayList<>(8);
        this.tradeRequests.stream()
                .filter(tradeRequest -> {
                    Player player1 = Bukkit.getPlayer(tradeRequest.getPlayer());
                    return player1 != null && player1.isOnline();
                })
                .forEach(tradeRequest -> buttonList.add(new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        Player player1 = Bukkit.getPlayer(tradeRequest.getPlayer());
                        if (player1 == null || !player1.isOnline()) return null;
                        return new ItemBuilder(Material.DIAMOND).name("&a接受交易").lore(String.format("&f%s", player1.getName())).setSkullProperty(player1).build();
                    }

                    @Override
                    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                        player.closeInventory();
                        Player player1 = Bukkit.getPlayer(tradeRequest.getPlayer());
                        if (player1 == null || !player1.isOnline()) return;
                        player.performCommand(String.format("trade %s", player1.getName()));
                    }
                }));
    }

    @Override
    public String getTitle(Player player) {
        return String.format("接受交易 - 第%d页", page + 1);
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> map = new HashMap<>();
        int startIndex = page * 45;
        IntStream.range(0, 45).forEach(i -> {
            int index = startIndex + i;
            if (index < buttonList.size()) map.put(i, buttonList.get(index));
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
        if ((page + 1) * 45 < buttonList.size()) map.put(53, new Button() {
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
