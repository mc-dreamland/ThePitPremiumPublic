package cn.charlotte.pit.activity.newyear.menu.buy;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.newyear.BuyData;
import cn.charlotte.pit.activity.newyear.NewYearData;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Slf4j
public class BuyMenu extends Menu {

    private static final ItemStack itemStack = new ItemBuilder(Material.BARRIER).name("该商品已被购买!").lore("", "&7空荡荡的商品架子", "").build();

    private static final Map<String, BuyData> map = Map.ofEntries(
            Map.entry("renown", new BuyData(
                    10, 4000, new ItemBuilder(Material.DIAMOND_BLOCK).name("&b声望").lore(getLore(10, 4000)).build(),
                    (p) -> {
                        Util.getPitProfile(p).addRenown(1);
                        CC.send(p, "&a获得了声望");
                    }
            )),
            Map.entry("1000exp", new BuyData(
                    0, 100, new ItemBuilder(Material.EXPERIENCE_BOTTLE).name("&b1000经验").lore(getLore(0, 2000)).build(),
                    (p) -> {
                        Util.getPitProfile(p).addExp(1000);
                        CC.send(p, "&a获得了1000经验");
                    }
            )),
            Map.entry("1000coin", new BuyData(
                    0, 100, new ItemBuilder(Material.GOLD_INGOT).name("&e1000金币").lore(getLore(0, 2000)).build(),
                    (p) -> {
                        Util.getPitProfile(p).addCoins(1000);
                        CC.send(p, "&a获得了1000金币");
                    }
            )),
            Map.entry("5000coin", new BuyData(
                    0, 400, new ItemBuilder(Material.GOLD_BLOCK).name("&e5000金币").lore(getLore(0, 8000)).build(),
                    (p) -> {
                        Util.getPitProfile(p).addCoins(5000);
                        CC.send(p, "&a获得了5000金币");
                    }
            )),
            Map.entry("5000exp", new BuyData(
                    0, 400, new ItemBuilder(Material.EXPERIENCE_BOTTLE).name("&b5000经验").lore(getLore(0, 2000)).build(),
                    (p) -> {
                        Util.getPitProfile(p).addExp(5000);
                        CC.send(p, "&a获得了5000经验");
                    }
            )),
            Map.entry("mythic", new BuyData(
                    5, 5000, new ItemBuilder(Material.GOLDEN_SWORD).name("&d随机神话装备").lore(getLore(10, 10000)).build(),
                    (p) -> {
                        ItemStack mythic = RandomUtil.helpMeToChooseOne(new MythicSwordItem(), new MythicBowItem(), new MythicLeggingsItem()).toItemStack();
                        Util.addItemOfMail(mythic, p, null);
                        log.info("{}", ItemOperationJson.toJson(
                                ItemOperationJson.mythicGranted(p, "NEW_YEAR_SHOP", mythic)
                        ));
                        CC.send(p, "&a获得了随机神话装备");
                    }
            )),
            Map.entry("coin", new BuyData(
                    50, 1000, new ItemBuilder(Material.GOLD_NUGGET).name("&e纪念币").lore(getLore(50, 50000)).build(),
                    (p) -> {
                        Util.addItemOfMail(PitItems.COMMEMORATIVE_COIN.toItemStack(), p, null);
                        CC.send(p, "&a获得了纪念币");
                    }
            )),
            Map.entry("coal", new BuyData(
                    50, 4000, new ItemBuilder(Material.COAL).name(PitItems.CHUNK_OF_VILE.getSingletonItem().getItemDisplayName()).lore(getLore(50, 3000)).build(),
                    (p) -> {
                        Util.addItemOfMail(PitItems.CHUNK_OF_VILE.toItemStack(), p, null);
                        CC.send(p, "&a获得了" + PitItems.CHUNK_OF_VILE.getSingletonItem().getItemDisplayName());
                    }
            )),
            Map.entry("null", new BuyData(
                    2048, 2048, itemStack,
                    (p) -> {
                    }
            )),
            Map.entry("fly_boots", new BuyData(
                    1000, 20000, new ItemBuilder(Material.IRON_BOOTS).name("&b小飞鞋").lore(getLore(1000, 20000)).build(),
                    (p) -> {
                        Util.addItemOfMail(PitItems.FLY_BOOTS.toItemStack(), p, null);
                        CC.send(p, "&a获得了" + PitItems.FLY_BOOTS.getSingletonItem().getItemDisplayName());
                    }
            )),
            Map.entry("exorcism_armor", new BuyData(
                    1500, 30000, new ItemBuilder(Material.DIAMOND_CHESTPLATE).name("&e驱魔甲").lore(getLore(1500, 30000)).build(),
                    (p) -> {
                        Util.addItemOfMail(PitItems.EXORCISM_ARMOR.toItemStack(), p, null);
                        CC.send(p, "&a获得了驱魔甲");
                    }
            )),
            Map.entry("funky_feather", new BuyData(
                    1500, 5000,PitItems.FUNKY_FEATHER.toItemStack(),
                    (p) -> {
                        Util.addItemOfMail(PitItems.FUNKY_FEATHER.toItemStack(), p, null);
                        CC.send(p, "&a获得了&3时髦的羽毛");
                    }
            ))
    );

    private static final RandomList<String> randomList = new RandomList<>(
            Map.entry("renown", 50),
            Map.entry("funky_feather",50),
            Map.entry("1000exp", 200),
            Map.entry("1000coin", 200),
            Map.entry("5000coin", 200),
            Map.entry("5000exp", 200),
            Map.entry("mythic", 50),
            Map.entry("coin", 40),
            Map.entry("coal", 8),
            Map.entry("fly_boots", 1),
            Map.entry("exorcism_armor", 1)
    );

    private static Button newButton(BuyData buyData) {
        if (buyData == null || buyData.spec() == 2048)
            return new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return itemStack;
                }

                @Override
                public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                }
            };
        return new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return buyData.itemStack();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                PlayerProfile profile = Util.getPitProfile(player);
                NewYearData data = profile.getActivityPlayerData().getNewYearData();
                if (buyData.coins() > 0 && data.getCoins() < buyData.coins()) {
                    CC.send(player, "&c你的活动点数不足!");
                    return;
                }

                if (buyData.spec() > 0 && data.getPoints() < buyData.spec()) {
                    CC.send(player, "&c你的特殊活动点数不足!");
                    return;
                }

                data.removePoint(buyData.spec());
                data.removeCoins(buyData.coins());
                switch (slot) {
                    case 10 -> data.getBuyMap().replace(0, "null");
                    case 13 -> data.getBuyMap().replace(1, "null");
                    case 16 -> data.getBuyMap().replace(2, "null");
                    default -> {
                        return;
                    }
                }
                buyData.lambda().execute(player);
                new BuyMenu().openMenu(player);
            }
        };
    }

    private static List<String> getLore(int spec, int coin) {
        List<String> list = new ArrayList<>();
        list.add("");
        if (coin > 0) list.add(String.format("&e活动点数: %s", coin));
        if (spec > 0) list.add(String.format("&b特殊活动点数: %s", spec));
        list.add("");
        list.add("&a左键点击购买");
        return list;
    }

    @Override
    public String getTitle(Player player) {
        return "活动商店";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        PlayerProfile profile = Util.getPitProfile(player);
        NewYearData data = profile.getActivityPlayerData().getNewYearData();
        if (data.getBuyMap() == null) refresh(data);

        Map<Integer, Button> map1 = new HashMap<>();

        int[] itemSlots = {10, 13, 16};
        for (int i = 0; i < 3; i++) {
            map1.put(itemSlots[i], newButton(map.get(data.getBuyMap().get(i))));
        }

        map1.put(4, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                NewYearData data1 = Util.getPitProfile(player).getActivityPlayerData().getNewYearData();
                return new ItemBuilder(Material.BOOK)
                        .name("&a商店说明")
                        .lore(
                                "",
                                "&7这里出售各种活动商品",
                                "",
                                String.format("&e活动点数: &l%s", data1.getCoins()),
                                String.format("&b特殊活动点数: &l%s", data1.getPoints()),
                                "",
                                "&c左键点击购买商品"
                        ).build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {

            }
        });

        map1.put(22, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.SLIME_BALL)
                        .name("&a刷新商品")
                        .lore(
                                "",
                                "&7点击刷新商店商品",
                                "",
                                "&7花费 &e50 &7活动点数",
                                "",
                                "&e当前剩余: &l" + Util.getPitProfile(player).getActivityPlayerData().getNewYearData().getCoins()
                        ).build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                NewYearData data1 = Util.getPitProfile(player).getActivityPlayerData().getNewYearData();
                if (data1.getCoins() < 50) {
                    CC.send(player, "&c你没有 &e50 活动点数");
                    return;
                }
                refresh(data1);
                data1.removeCoins(50);
                CC.send(player, "&a已刷新商品!");
                openMenu(player);
            }
        });

        return map1;
    }

    private void refresh(NewYearData data) {
        data.clearBuyMap();
        IntStream.range(0, 3).forEach(i -> data.getBuyMap().put(i, randomList.random()));
    }

    @Override
    public int getSize() {
        return 27;
    }
}
