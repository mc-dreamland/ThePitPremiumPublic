package cn.charlotte.pit.menu.gem.global;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.menu.gem.global.button.GlobalEnchantChoseButton;
import cn.charlotte.pit.util.FuncsKt;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class GlobalSecondChoseGemMenu extends Menu {

    private static final int[] SLOTS = {11, 13, 15};

    private final ItemStack itemStack;
    private final int index;

    public GlobalSecondChoseGemMenu(ItemStack itemStack, int index) {
        this.itemStack = itemStack;
        this.index = index;
    }

    @Override
    public String getTitle(Player player) {
        return "选择一项附魔";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        IMythicItem mythicItem = FuncsKt.toMythicItem(itemStack);
        if (mythicItem == null) {
            return new HashMap<>();
        }

        Map<Integer, Button> map = new HashMap<>();
        int slot = 0;
        for (Map.Entry<AbstractEnchantment, Integer> entry : mythicItem.getEnchantments().entrySet()) {
            map.put(SLOTS[Math.min(2, slot)], new GlobalEnchantChoseButton(itemStack, index, slot, entry.getKey(), entry.getValue()));
            slot++;
        }
        return map;
    }

    @Override
    public int getSize() {
        return 3 * 9;
    }
}
