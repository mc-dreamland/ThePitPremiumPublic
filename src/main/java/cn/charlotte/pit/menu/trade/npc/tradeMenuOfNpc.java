package cn.charlotte.pit.menu.trade.npc;

import cn.charlotte.pit.data.temp.TradeRequest;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class tradeMenuOfNpc extends Menu {

    private final List<TradeRequest> tradeRequests;

    public tradeMenuOfNpc(List<TradeRequest> tradeRequests) {
        this.tradeRequests = tradeRequests;
    }

    @Override
    public String getTitle(Player player) {
        return "交易界面 /trade tradeGui";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        return Map.of(
                11, new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        return new ItemBuilder(Material.GOLD_NUGGET).name("&f点击查询可接受的交易").build();
                    }

                    @Override
                    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                        new acceptTradeMenu(tradeRequests, player).openMenu(player);
                    }
                },
                15, new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        return new ItemBuilder(Material.DIAMOND).name("&f发送交易").build();
                    }

                    @Override
                    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                        new sendTradeMenu().openMenu(player);
                    }
                }
        );
    }

    @Override
    public int getSize() {
        return 3 * 9;
    }
}
