package cn.charlotte.pit.activity.genes.menu.buy;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.genes.data.GenesPlayerData;
import cn.charlotte.pit.activity.genes.menu.GenesMenu;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.genesis.team.GenesisTeam;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
import cn.charlotte.pit.lambda.PitProfilePlayerReturnStringLambda;
import cn.charlotte.pit.util.RandomList;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.log.ItemOperationJson;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import cn.charlotte.pit.util.random.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.IntStream;

@Slf4j
public class GenesBuyMenu extends Menu {

    private static final HashMap<String, b> buyMap = new HashMap<>(Map.ofEntries(
            Map.entry("500coin", new b(50, new ItemBuilder(Material.GOLD_INGOT).amount(5).name("&e500金币").lore(getLines(50, "&7购买后获得 &e500金币")).build(), (player, profile) -> {
                profile.addCoins(500);
                return "&e500金币";
            })),
            Map.entry("500exp", new b(50, new ItemBuilder(Material.EXPERIENCE_BOTTLE).amount(5).name("&b500经验").lore(getLines(50, "&7购买后获得 &b500经验")).build(), (player, profile) -> {
                profile.addExp(500);
                return "&b500经验";
            })),
            Map.entry("1000coin", new b(100, new ItemBuilder(Material.GOLD_INGOT).amount(10).name("&e1000金币").lore(getLines(100, "&7购买后获得 &e1000金币")).build(), (player, profile) -> {
                profile.addCoins(1000);
                return "&e1000金币";
            })),
            Map.entry("1000exp", new b(100, new ItemBuilder(Material.EXPERIENCE_BOTTLE).amount(10).name("&b1000经验").lore(getLines(100, "&7购买后获得 &b1000经验")).build(), (player, profile) -> {
                profile.addExp(1000);
                return "&b1000经验";
            })),
            Map.entry("funky_feather", new b(600, new ItemBuilder(Material.FEATHER).amount(1).name("&3时髦的羽毛").lore(getLines(600, "&7购买后获得 &3时髦的羽毛")).build(), (player, profile) -> {
                Util.addItemOfMail(PitItems.FUNKY_FEATHER.toItemStack(), player, null);
                return "&3时髦的羽毛";
            })),
            Map.entry("chest", new b(200, new ItemBuilder(Material.CHEST).amount(1).name("&e行动礼盒").lore(getLines(200, "&7购买后获得 &e行动礼盒")).build(), (player, profile) -> {
                Util.addItemOfMail(PitItems.ACTION_GIFT_BOX.toItemStack(), player, null);
                return "&e行动礼盒";
            })),
            Map.entry("mythic", new b(1000, new ItemBuilder(Material.GOLDEN_SWORD).amount(1).name("&d随机神话装备").lore(getLines(1000, "&7购买后获得 &d随机神话装备")).build(), (player, profile) -> {
                ItemStack mythic = RandomUtil.helpMeToChooseOne(
                        new MythicSwordItem().toItemStack(),
                        new MythicBowItem().toItemStack(),
                        new MythicLeggingsItem().toItemStack()
                );
                Util.addItemOfMail(mythic, player, null);
                log.info("{}", ItemOperationJson.toJson(
                        ItemOperationJson.mythicGranted(player, "GENES_SHOP", mythic)
                ));
                return "&d随机神话装备";
            })),
            Map.entry("angel", new b(20000, new ItemBuilder(Material.DIAMOND_CHESTPLATE).amount(1).name("&f天使之甲").lore(getLines(20000, "&7购买后获得 &f天使之甲")).build(), (player, profile) -> {
                Util.addItemOfMail(PitItems.ANGEL_CHEST_PLATE.toSingletonItemStack(), player, null);
                return "&f天使之甲";
            })),
            Map.entry("demon", new b(20000, new ItemBuilder(Material.DIAMOND_CHESTPLATE).amount(1).name("&c终末之靴").lore(getLines(20000, "&7购买后获得 &c终末之靴")).build(), (player, profile) -> {
                Util.addItemOfMail(PitItems.ARMAGEDDON_BOOTS.toSingletonItemStack(), player, null);
                return "&c终末之靴";
            })),
            Map.entry("mysterious_fragments", new b(200, new ItemBuilder(Material.ECHO_SHARD).amount(1).name("&5神秘碎片").lore(getLines(200, "&7购买后获得 &5神秘碎片")).build(), (player, profile) -> {
                Util.addItemOfMail(PitItems.MYSTERIOUS_FRAGMENTS.toItemStack(), player, null);
                return "&5神秘碎片";
            })),
            Map.entry("coal", new b(800, new ItemBuilder(Material.COAL).amount(1).name("&5暗聚块").lore(getLines(800, "&7购买后获得 &5暗聚块")).build(), (player, profile) -> {
                Util.addItemOfMail(PitItems.CHUNK_OF_VILE.toItemStack(), player, null);
                return "&5暗聚块";
            }))
    ));

    private final RandomList<String> refreshMap = new RandomList<>(
            Map.entry("500coin", 250),
            Map.entry("500exp", 250),
            Map.entry("1000coin", 100),
            Map.entry("1000exp", 100),
            Map.entry("funky_feather", 100),
            Map.entry("chest", 100),
            Map.entry("mythic", 49),
            Map.entry("mysterious_fragments", 50)
    );

    private static final ItemStack nullItem = new ItemBuilder(Material.BARRIER)
            .name("&c商品架子")
            .lore(
                    "",
                    "&7这是商品架子已空,需要补货",
                    ""
            ).build();

    private final Button refreshButton = new Button() {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.EMERALD)
                    .name("&a刷新")
                    .lore(
                            "",
                            "&7花费 &e10 &7点数进行刷新.",
                            ""
                    ).build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            PlayerProfile profile = Util.getPitProfile(player);
            if (!profile.getActivityPlayerData().getGenesisData().takePotions(10)) {
                CC.send(player, "&c你的点数不足");
                return;
            }
            refresh(profile, profile.getActivityPlayerData().getGenesisData());
            CC.send(player, "&a刷新!");
            openMenu(player);
        }
    };

    private static final Button returnButton = new Button() {
        @Override
        public ItemStack getButtonItem(Player player) {
            return R_PAGE;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            new GenesMenu().openMenu(player);
        }
    };

    public GenesBuyMenu(Player player) {
        PlayerProfile profile = Util.getPitProfile(player);
        GenesPlayerData data = profile.getActivityPlayerData().getGenesisData();
        if (data.getBuyMap() == null) refresh(profile, data);
        if (data.getTeam() == GenesisTeam.ANGEL) this.refreshMap.add("angel", 1);
        else this.refreshMap.add("demon", 1);
    }

    private void refresh(PlayerProfile profile, GenesPlayerData data) {
        Map<Integer, String> map = data.getBuyMap();
        if (map == null) map = new HashMap<>(3);
        Map<Integer, String> finalMap = map;
        IntStream.rangeClosed(1, 3).forEach(i ->
                finalMap.compute(i, (k, v) -> refreshMap.random())
        );
        profile.getActivityPlayerData().getGenesisData().setBuyMap(finalMap);
    }

    @Override
    public String getTitle(Player player) {
        return "商店";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> map = new HashMap<>(5);
        map.put(18, returnButton);
        map.put(26, refreshButton);
        PlayerProfile profile = Util.getPitProfile(player);
        Map<Integer, String> buyMap = profile.getActivityPlayerData().getGenesisData().getBuyMap();
        IntStream.rangeClosed(1, 3).forEach(i -> {
            String value = buyMap.get(i);
            Button button;
            if (value != null && GenesBuyMenu.buyMap.containsKey(value))
                button = newButonn(GenesBuyMenu.buyMap.get(value));
            else
                button = new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        return nullItem;
                    }

                    @Override
                    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                        CC.send(player, "&c这个商品架子是空的！");
                    }
                };
            map.put(9 + i * 2, button);
        });
        return map;
    }

    private record b(int price, ItemStack itemStack, PitProfilePlayerReturnStringLambda lambda) {
    }

    private static List<String> getLines(int price, String... text) {
        List<String> list = new ArrayList<>();
        list.add("");
        list.addAll(Arrays.stream(text).toList());
        list.add("");
        list.add(String.format("&7价格: %s", price));
        list.add("");
        return list;
    }

    private Button newButonn(b b) {
        return new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return b.itemStack;
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                PlayerProfile profile = Util.getPitProfile(player);
                if (!profile.getActivityPlayerData().getGenesisData().takePotions(b.price)) {
                    CC.send(player, "&c你的点数不足以购买!");
                    return;
                }
                profile.getActivityPlayerData().getGenesisData().getBuyMap().remove((slot - 9) / 2);
                CC.send(player, String.format("&a你成功购买了 %s！", b.lambda.execute(player, profile)));
                new GenesBuyMenu(player).openMenu(player);
            }
        };
    }
}
