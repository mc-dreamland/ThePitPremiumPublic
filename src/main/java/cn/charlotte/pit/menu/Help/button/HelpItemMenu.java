package cn.charlotte.pit.menu.Help.button;

import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelpItemMenu extends Menu {

    public HelpItemMenu() {
    }

    public static List<String> getLore(int version) {
        List<String> logEntries = new ArrayList<>();
        if (version == 1) {
            logEntries.add("&e物品特性:");
            logEntries.add("&7是否可以交易:&a&l√");
            logEntries.add("&7是否可以丢出:&c&l×");
            logEntries.add("&7是否可以存放至末影箱:&a&l√");
            logEntries.add("&7死后保留:&a&l√");
            logEntries.add("&e获取方式:");
            logEntries.add("&7任务NPC的兑换所里用100行动赏金兑换（需契约者声望3级）");
            logEntries.add("");
            logEntries.add("&e物品用途：");
            logEntries.add("&7在坑下手持物品并右键即可抽取物品");
            logEntries.add("");
            logEntries.add("&e物品概率表");
            logEntries.add("&f钻石护腿&7:&e15%");
            logEntries.add("&f钻石鞋子&7:&e30%");
            logEntries.add("&f钻石胸甲&7:&e30%");
            logEntries.add("&b(200 ~ 500)经验&7:&e50%");
            logEntries.add("&e(200 ~ 500)金币&7:&e50%");
            logEntries.add("&e行动头盔&7:&e10%");
            logEntries.add("&d随机的神话装备&7:&e5%");
            logEntries.add("&a增益旗帜&7:&e5%");
            logEntries.add("&5暗聚块&7:&e1%");
        } else if (version == 2) {
            logEntries.add("&e物品特性:");
            logEntries.add("&7是否可以交易:&c&l×");
            logEntries.add("&7是否可以丢出:&c&l×");
            logEntries.add("&7是否可以存放至末影箱:&a&l√");
            logEntries.add("&7死后保留:&a&l√");
            logEntries.add("");
            logEntries.add("&e获取方式:");
            logEntries.add("&7精通20级后，400连杀死亡获得");
            logEntries.add("&c每日限3个：首个免费→第二个需要5声望→第三个需要10声望");
            logEntries.add("");
            logEntries.add("&e物品用途：");
            logEntries.add("&7在坑下手持物品并右键即可抽取物品");
            logEntries.add("");
            logEntries.add("&e物品概率表");
            logEntries.add("&3(1 ~ 3)时髦的羽毛&7:&e30%");
            logEntries.add("&a(2 ~ 4)哲学仙人掌&7:&e30%");
            logEntries.add("&a遵纪守法的宝石&7:&e20%");
            logEntries.add("&b举世瞩目的宝石&7:&e1%");
            logEntries.add("&d宝石之剑&7:&e30%");
        } else if (version == 3) {
            logEntries.add("&e物品特性:");
            logEntries.add("&7是否可以交易:&a&l√");
            logEntries.add("&7是否可以丢出:&c&l×");
            logEntries.add("&7是否可以存放至末影箱:&a&l√");
            logEntries.add("&7死后保留:&a&l√");
            logEntries.add("");
            logEntries.add("&e获取方式:");
            logEntries.add("&71.行动礼盒获得");
            logEntries.add("&72.拍卖事件获得");
            logEntries.add("");
            logEntries.add("&e物品用途：");
            logEntries.add("&7在坑下手持物品并左键即可放置一个维持10分钟的增益旗帜");
            logEntries.add("&7在增益旗帜附近12格内即可获得50%的金币,经验增益");
            logEntries.add("&7如果你是精通等级0-精通等级5的玩家则你获得的增益变为100%");
        } else if (version == 4) {
            logEntries.add("&e物品特性:");
            logEntries.add("&7是否可以交易:&a&l√");
            logEntries.add("&7是否可以丢出:&c&l×");
            logEntries.add("&7是否可以存放至末影箱:&a&l√");
            logEntries.add("&7死后保留:&a&l√");
            logEntries.add("");
            logEntries.add("&e获取方式:");
            logEntries.add("&71.UBER中获得");
            logEntries.add("&72.拍卖事件获得");
            logEntries.add("");
            logEntries.add("&e物品用途：");
            logEntries.add("&7副手持要修复物品,主手对地右键即可修复物品的3条命");
        } else if (version == 5) {
            logEntries.add("&e物品特性:");
            logEntries.add("&7是否可以交易:&a&l√");
            logEntries.add("&7是否可以丢出:&c&l×");
            logEntries.add("&7是否可以存放至末影箱:&a&l√");
            logEntries.add("&7死后保留:&c&l×");
            logEntries.add("");
            logEntries.add("&e获取方式:");
            logEntries.add("&71.下水道箱子中获得");
            logEntries.add("");
            logEntries.add("&e物品用途：");
            logEntries.add("&7与正常钻石装备一致但是可以用于交易");
            logEntries.add("&e在&2下水道箱子&e中的概率");
            logEntries.add("&2下水道之钻石裤子&f:&e15%");
            logEntries.add("&2下水道之钻石胸甲&f:&e50%");
            logEntries.add("&2下水道之钻石鞋子&f:&e50%");
        } else if (version == 6) {
            logEntries.add("&e物品特性:");
            logEntries.add("&7是否可以交易:&a&l√");
            logEntries.add("&7是否可以丢出:&c&l×");
            logEntries.add("&7是否可以存放至末影箱:&a&l√");
            logEntries.add("&7死后保留:&c&l×");
            logEntries.add("");
            logEntries.add("&e获取方式:");
            logEntries.add("&71.挖掘成熟的小麦获得");
            logEntries.add("");
            logEntries.add("&e物品用途：");
            logEntries.add("&7用于制作 &e美味的面包 &7和 &e干草块");
        } else if (version == 7) {
            logEntries.add("&e物品特性:");
            logEntries.add("&7是否可以交易:&a&l√");
            logEntries.add("&7是否可以丢出:&c&l×");
            logEntries.add("&7是否可以存放至末影箱:&a&l√");
            logEntries.add("&7死后保留:&c&l×");
            logEntries.add("");
            logEntries.add("&e获取方式:");
            logEntries.add("&71.用斧头挖掘西瓜获得");
            logEntries.add("");
            logEntries.add("&e物品用途：");
            logEntries.add("&7用于完成国王任务");
            logEntries.add("&7");
        } else if (version == 8) {
            logEntries.add("&e物品特性:");
            logEntries.add("&7是否可以交易:&a&l√");
            logEntries.add("&7是否可以丢出:&c&l×");
            logEntries.add("&7是否可以存放至末影箱:&a&l√");
            logEntries.add("&7死后保留:&a&l√");
            logEntries.add("");
            logEntries.add("&e获取方式:");
            logEntries.add("&71.用64个小麦在工作台那制作");
            logEntries.add("");
            logEntries.add("&e物品用途：");
            logEntries.add("&7用于完成国王任务");
        } else if (version == 9) {
            logEntries.add("&e物品特性:");
            logEntries.add("&7是否可以交易:&a&l√");
            logEntries.add("&7是否可以丢出:&c&l×");
            logEntries.add("&7是否可以存放至末影箱:&a&l√");
            logEntries.add("&7死后保留:&a&l√");
            logEntries.add("");
            logEntries.add("&e获取方式:");
            logEntries.add("&71.用64个小麦在熔炉那制作");
            logEntries.add("");
            logEntries.add("&e物品用途：");
            logEntries.add("&7用于完成国王任务");
            logEntries.add("&7可以食用并瞬间恢复一颗心");
        } else if (version == 10) {
            logEntries.add("&e物品特性:");
            logEntries.add("&7是否可以交易:&a&l√");
            logEntries.add("&7是否可以丢出:&c&l×");
            logEntries.add("&7是否可以存放至末影箱:&a&l√");
            logEntries.add("&7死后保留:&c&l×");
            logEntries.add("");
            logEntries.add("&e获取方式:");
            logEntries.add("&71.钓鱼获得(需要钓鱼俱乐部 3 级)");
            logEntries.add("");
            logEntries.add("&e物品用途：");
            logEntries.add("&7与正常钻石装备一致但是可以用于交易");
        } else if (version == 11) {
            logEntries.add("&e物品特性:");
            logEntries.add("&7是否可以交易:&a&l√");
            logEntries.add("&7是否可以丢出:&c&l×");
            logEntries.add("&7是否可以存放至末影箱:&a&l√");
            logEntries.add("&7死后保留:&a&l√");
            logEntries.add("");
            logEntries.add("&e获取方式:");
            logEntries.add("&71.用美味的面包在小鸡那兑换");
            logEntries.add("");
            logEntries.add("&e物品用途：");
            logEntries.add("&7用于完成国王任务");
        } else if (version == 12) {
            logEntries.add("&e物品特性:");
            logEntries.add("&7是否可以交易:&a&l√");
            logEntries.add("&7是否可以丢出:&c&l×");
            logEntries.add("&7是否可以存放至末影箱:&a&l√");
            logEntries.add("&7死后保留:&c&l×");
            logEntries.add("");
            logEntries.add("&e获取方式:");
            logEntries.add("&71.在下水道箱子中获得");
            logEntries.add("");
            logEntries.add("&e物品用途：");
            logEntries.add("&7用于跟下水道之鱼交易");
        } else {
            logEntries.add("&c该图鉴暂未上架");
            logEntries.add("&c神秘人太懒了!!! ouo");
        }

        return logEntries;
    }

    @Override
    public String getTitle(Player player) {
        return "天坑物品";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttonMap = new HashMap<>();
        buttonMap.put(0, createUpdateLogButton(Material.CHEST, "&e行动礼盒", getLore(1)));
        buttonMap.put(1, createUpdateLogButton(Material.ENDER_CHEST, "&d登峰造极掉落物", getLore(2)));
        buttonMap.put(2, createUpdateLogButton(Material.GREEN_BANNER, "&a增益旗帜", getLore(3)));
        buttonMap.put(3, createUpdateLogButton(Material.SHEARS, "&d简易工匠包", getLore(4)));
        buttonMap.put(4, createUpdateLogButton(Material.DIAMOND_CHESTPLATE, "&2下水道之钻石装备", getLore(5)));
        buttonMap.put(5, createUpdateLogButton(Material.WHEAT, "&a小麦", getLore(6)));
        buttonMap.put(6, createUpdateLogButton(Material.SUGAR, "&a糖", getLore(7)));
        buttonMap.put(7, createUpdateLogButton(Material.HAY_BLOCK, "&e干草块", getLore(8)));
        buttonMap.put(8, createUpdateLogButton(Material.BREAD, "&e美味面包", getLore(9)));
        buttonMap.put(9, createUpdateLogButton(Material.DIAMOND_CHESTPLATE, "&b钓鱼者的钻石装备", getLore(10)));
        buttonMap.put(10, createUpdateLogButton(Material.EGG, "&e高级鸡蛋", getLore(11)));
        buttonMap.put(11, createUpdateLogButton(Material.SLIME_BALL, "&2下水道废弃之物", getLore(12)));
        buttonMap.put(buttonMap.size(), createUpdateLogButton(Material.BARRIER, "&c该图鉴尚未上架", getLore(0)));
        return buttonMap;
    }

    private Button createUpdateLogButton(Material item, String name, List<String> loreContent) {
        return new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {

                List<String> lore = new ArrayList<>(loreContent);

                return new ItemBuilder(item)
                        .name(name)
                        .lore(lore)
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return false;
            }
        };
    }

    @Override
    public int getSize() {
        return 6 * 9;
    }
}