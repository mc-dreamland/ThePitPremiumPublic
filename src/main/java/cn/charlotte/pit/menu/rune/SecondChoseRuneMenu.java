package cn.charlotte.pit.menu.rune;

import cn.charlotte.pit.Assembly;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.menu.rune.button.DJEnchantButton;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class SecondChoseRuneMenu extends Menu {

    private final ItemStack itemStack;
    private final int index;

    public SecondChoseRuneMenu(ItemStack itemStack, int index) {
        this.itemStack = itemStack;
        this.index = index;
    }

    @Override
    public String getTitle(Player player) {
        return "选择一项附魔";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        if (Utils.getMythicItem(itemStack) == null) {
            return new HashMap<>();
        }

        Map<Integer, Button> map = new HashMap<>();
        for (int i = 0; i < Assembly.music.size(); i++) {
            AbstractEnchantment enchantment = Assembly.music.get(i);
            map.put(Assembly.musicIndex.get(i), new DJEnchantButton(enchantment, itemStack, index));
        }
        return map;
    }

    @Override
    public int getSize() {
        return 27;
    }
}
