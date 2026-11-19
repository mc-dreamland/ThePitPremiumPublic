package cn.charlotte.pit.util;

import cn.charlotte.pit.item.PitItems;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public class ItemUtil {
    public static ItemStack findItem(String name, int amount) {
        if (name == null) return null;
        String key = name.toLowerCase(Locale.ROOT);
        try {
            return PitItems.valueOf(key.toUpperCase(Locale.ROOT)).toManyItem(amount);
        } catch (Throwable e) {
            return null;
        }
    }
}
