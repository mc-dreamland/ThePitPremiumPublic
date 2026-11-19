package cn.charlotte.pit.util.item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 需要唯一身份标识的高级物品目录。
 */
public final class AdvancedItemCatalog {

    private static final Map<String, String> ITEMS = createItems();
    private static final Set<String> NORMALIZED_INTERNAL_NAMES = ITEMS.keySet().stream()
            .map(AdvancedItemCatalog::normalize)
            .collect(Collectors.toUnmodifiableSet());

    private AdvancedItemCatalog() {
    }

    public static boolean isAdvanced(String internalName) {
        return internalName != null && NORMALIZED_INTERNAL_NAMES.contains(normalize(internalName));
    }

    public static Map<String, String> items() {
        return ITEMS;
    }

    private static Map<String, String> createItems() {
        Map<String, String> items = new LinkedHashMap<>();

        add(items, "mythic_sword", "神话之剑");
        add(items, "mythic_bow", "神话之弓");
        add(items, "mythic_leggings", "神话之甲");
        add(items, "MythicShovel", "神话铲");
        add(items, "jewel_sword", "宝石剑");
        add(items, "magic_fishing_rod", "魔法钓鱼竿");
        add(items, "angel_chestplate", "天使之甲");
        add(items, "armageddon_boots", "终末之靴");
        add(items, "kings_helmet", "国王的王冠");
        add(items, "Turtle_helmet", "海龟头");
        add(items, "lucky_chestplate", "幸运胸甲");
        add(items, "fly_boots", "小飞鞋");
        add(items, "exorcism_armor", "驱魔甲");
        add(items, "black_stone", "黑石");
        add(items, "gleam_stone", "辉光石");
        add(items, "greed_gold", "贪婪之金");
        add(items, "bounty_quest", "悬赏令");

        add(items, "phase_book", "相位书");
        add(items, "lucky_stone", "幸运石");
        add(items, "lucky_gem", "幸运宝石");
        add(items, "kill_counter_gem", "击杀计数宝石");
        add(items, "totally_legit_gem", "正品宝石");
        add(items, "global_attention_gem", "全局专注宝石");
        add(items, "mythic_repair_kit", "神话修复包");
        add(items, "Small_mythic_repair_kit", "小型神话修复包");
        add(items, "mythicRandomVoucher", "随机神话兑换券");
        add(items, "CustomNamePaper", "自定义名称纸");
        add(items, "mythic_reel", "附魔卷轴");
        add(items, "eternal_heart", "永恒之心");
        add(items, "broken_scroll", "破损卷轴");
        add(items, "chunk_of_vile_item", "邪恶碎片");
        add(items, "mysterious_bottle", "神话经验瓶");
        add(items, "bounty_solvent_potion", "赏金溶剂");

        add(items, "mystery_box_One", "神秘之盒 I");
        add(items, "mystery_box_two", "神秘之盒 II");
        add(items, "mystery_box_three", "神秘之盒 III");
        add(items, "mystery_box_fragment", "神秘之盒碎片");
        add(items, "mysterious_fragments", "神秘碎片");
        add(items, "Xing_Dong_Lihe", "行动礼包");
        add(items, "uber_drop", "超级掉落");
        add(items, "super_package", "超级礼包");

        add(items, "sword_bundle_empty", "空神话之剑收纳箱");
        add(items, "sword_bundle_full", "满神话之剑收纳箱");
        add(items, "bow_bundle_empty", "空神话之弓收纳箱");
        add(items, "bow_bundle_full", "满神话之弓收纳箱");
        add(items, "pants_bundle_empty", "空神话之甲收纳箱");
        add(items, "pants_bundle_full", "满神话之甲收纳箱");

        add(items, "funky_feather", "奇异羽毛");
        add(items, "musical_rune", "音乐符文");
        add(items, "dimensional_cactus", "维度仙人掌");
        add(items, "sacrosanct_cactus", "神圣仙人掌");
        add(items, "king_quest_refresh_coupon", "国王任务刷新券");
        add(items, "goden_bric", "金砖");
        add(items, "experience_bottle", "特殊经验瓶");
        add(items, "grim_reaper", "死神镰刀");
        add(items, "tactical_insertions", "战术插入器");
        add(items, "4_in_a_row_gadget", "四子棋道具");
        add(items, "LuckyLottery", "幸运彩票");
        add(items, "gain_banner", "增益旗帜");
        add(items, "rubbish", "魔法橡皮");
        add(items, "soul_magnet", "灵魂磁铁");
        add(items, "broken_soul", "破碎灵魂");
        add(items, "new_year_lucky_bag", "新年福袋");
        add(items, "commemorative_coin", "纪念币");

        return Collections.unmodifiableMap(items);
    }

    private static void add(Map<String, String> items, String internalName, String displayName) {
        items.put(internalName, displayName);
    }

    private static String normalize(String internalName) {
        return internalName.toLowerCase(Locale.ROOT);
    }
}
