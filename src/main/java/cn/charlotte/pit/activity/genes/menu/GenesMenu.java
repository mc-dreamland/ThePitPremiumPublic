package cn.charlotte.pit.activity.genes.menu;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.ActivityType;
import cn.charlotte.pit.activity.genes.menu.buy.GenesBuyMenu;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.genesis.team.GenesisTeam;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class GenesMenu extends Menu {
    private static final Button billboard = new Button() {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.PAPER)
                    .name("&f活动公告")
                    .lore(
                            "",
                            "&f活动简介:",
                            " &f加入阵营,击杀玩家,获得点数",
                            String.format("&f活动结束时间:%s", Util.tyyyy_MM_dd_h_min_s.format(ActivityType.GENES.getEndTime())),
                            ""
                    )
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            CC.send(MessageType.MISC, player,
                    "&f活动简介:",
                    " &f加入阵营,击杀玩家,获得点数",
                    String.format("&f活动结束时间:%s", Util.tyyyy_MM_dd_h_min_s.format(ActivityType.GENES.getEndTime())));
        }
    };

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public String getTitle(Player player) {
        return "天使恶魔活动界面";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        PlayerProfile profile = Util.getPitProfile(player);


        return new HashMap<>(Map.of(11, new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player1) {
                        PlayerProfile profile1 = Util.getPitProfile(player1);
                        return new ItemBuilder(Material.HOPPER)
                                .name("&b商店")
                                .lore(
                                        "",
                                        "&7售卖各种各样的物品",
                                        String.format("&7可用活动点数: %s", profile1.getActivityPlayerData().getGenesisData().getPoints())
                                )
                                .build();
                    }

                    @Override
                    public void clicked(Player player1, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                        if (Util.matchesAny(Util.getPitProfile(player1).getActivityPlayerData().getGenesisData().getTeam(), null, GenesisTeam.NONE))
                            return;
                        new GenesBuyMenu(player).openMenu(player1);
                    }
                },
                29, new GenesisStatusButton(GenesisTeam.ANGEL, profile),
                33, new GenesisStatusButton(GenesisTeam.DEMON, profile),
                15, new GenesPerkButton(),
                53, billboard
        ));
    }
}
