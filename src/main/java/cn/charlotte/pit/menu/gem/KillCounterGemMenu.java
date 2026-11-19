package cn.charlotte.pit.menu.gem;

import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
import cn.charlotte.pit.menu.gem.button.KillCounterGemButton;
import cn.charlotte.pit.util.FuncsKt;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class KillCounterGemMenu extends Menu {

    private final ItemStack gem;

    public static boolean filters(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        return Optional.ofNullable(ItemUtil.getInternalName(item))
                .filter(l -> l.equals(MythicBowItem.INTERNAL_NAME) ||
                        l.equals(MythicSwordItem.INTERNAL_NAME))
                .map(l -> FuncsKt.toMythicItem(item))
                .map(l -> l.getKillCounterGem() == -1)
                .orElse(false);
    }

    @Override
    public String getTitle(Player player) {
        return "宝石点缀";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = Maps.newHashMap();
        PlayerInventory slots = player.getInventory();
        int size = slots.getSize();
        int slot = 0;
        for (int slt = 0; slt < size; slt++) {
            ItemStack item = slots.getItem(slt);
            if (filters(item)) {
                buttons.put(slot++, new KillCounterGemButton(gem, slt, item));
            }
        }
        return buttons;
    }

    @Override
    public void onClickEvent(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
