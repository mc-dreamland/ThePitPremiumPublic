package cn.charlotte.pit.menu.perk.normal.buy;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.menu.killstreak.StreakChooseMenu;
import cn.charlotte.pit.menu.perk.normal.buy.button.PerkBuyButton;
import cn.charlotte.pit.menu.perk.normal.buy.button.ResetButton;
import cn.charlotte.pit.menu.perk.normal.choose.PerkChooseMenu;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 22:43
 */
public class PerkBuyMenu extends Menu {
    private final int page;
    private final PerkType perkType;

    public PerkBuyMenu(int page, PerkType perkType) {
        this.page = page;
        this.perkType = perkType;
    }

    @Override
    public String getTitle(Player player) {
        return "天赋";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> button = new HashMap<>();

        List<TypedPerk<?>> perks = ThePit.getInstance()
                .getPerkFactory()
                .getPerks()
                .stream()
                .sorted(Comparator.comparing(perk -> perk.requirePrestige() * 1000 + perk.requireLevel()))
                .collect(Collectors.toList());

        if (perkType == PerkType.PERK) {
            perks.removeIf(perk -> perk.requirePrestige() > 0 && perk.requireLevel() == 0);
            perks.removeIf(perk -> perk.requirePrestige() == 0 && perk.requireLevel() == 0);
        }
        //仅选择类型为perkType的天赋
        perks.removeIf(perk -> perk.getPerkType() == null || perk.getPerkType() != perkType);

        for (int i = 0; i < perks.size(); i++) {
            button.put(i + 10, new PerkBuyButton(perks.get(i), page, perkType));
        }

        button.put(49, new Button() {

            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.ARROW)
                        .name("&a返回")
                        .lore("&7点击回到连杀天赋选择界面.")
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {

                if (perkType == PerkType.PERK) {
                    new PerkChooseMenu().openMenu(player);
                } else {
                    new StreakChooseMenu().openMenu(player);
                }
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return true; // 返回 true ⇒ 点击后立即刷新按钮显示
            }
        });
        if (perkType == PerkType.PERK) {
            button.put(50, new ResetButton(page));
        }

        return button;
    }

    @Override
    public int getSize() {
        return 6 * 9;
    }

    @Override
    public boolean isAutoUpdate() {
        return true;
    }
}
