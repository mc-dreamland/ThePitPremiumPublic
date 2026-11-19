package cn.charlotte.pit;

import cn.charlotte.pit.enchantment.AbstractEnchantment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Assembly {
    public static final List<AbstractEnchantment> music = new ArrayList<>();
    public static final List<Integer> musicIndex = Arrays.asList(11, 12, 13, 14, 15, 4, 22);
    public static List<AbstractEnchantment> all = new ArrayList<>();
    public static List<AbstractEnchantment> armor = new ArrayList<>();
    public static List<AbstractEnchantment> bow = new ArrayList<>();
    public static List<AbstractEnchantment> weapon = new ArrayList<>();
    public static List<String> enchantNames = new ArrayList<>();
    public static int enchantmentInt = 0;
    public static int perkInt = 0;
}
