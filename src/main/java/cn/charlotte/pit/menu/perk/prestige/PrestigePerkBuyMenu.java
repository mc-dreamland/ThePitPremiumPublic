package cn.charlotte.pit.menu.perk.prestige;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.menu.perk.prestige.button.PrestigePerkBuyButton;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/4 18:37
 */
public class PrestigePerkBuyMenu extends Menu {
    private final int maxPage = 1;
    private final PerkGroup perkGroup;
    private int page = 0;

    public PrestigePerkBuyMenu(PerkGroup perkGroup) {
        this.perkGroup = perkGroup;
    }


    @Override
    public String getTitle(Player player) {
        if (page > 0) {
            return String.format("(%s/%s) 声望商店 - %s", page, maxPage, perkGroup.getDisplayName());
        }
        return String.format("声望商店 - %s", perkGroup.getDisplayName());
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> button = new HashMap<>();

        List<TypedPerk<?>> perks = ThePit.getInstance()
                .getPerkFactory()
                .getPerks()
                .stream()
                .sorted(Comparator.comparing(perk -> perk.requirePrestige() * 1000 + perk.requireLevel()))
                .filter(perk -> perk.getPerkGroup().equals(perkGroup) && perk.requirePrestige() != 0)
                .toList();

        int num = 21 * page;

        for (int i = 0; i < PRESTIGE_SLOTS.length; i++) {
            if (perks.size() > i + num) {
                button.put(PRESTIGE_SLOTS[i], new PrestigePerkBuyButton(perks.get(i + num), this.perkGroup));
            }
        }
        button.put(40, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return R_PAGE;
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                player.closeInventory();
                new PrestigeMenu().openMenu(player);
            }
        });


        if (page > 0) {
            button.put(36, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return P_PAGE;
                }

                @Override
                public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                    if (page == 0) {
                        return;
                    }
                    page--;
                }

                @Override
                public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                    return true;
                }
            });
        }

        if (perks.size() > 21 * (page + 1)) {
            button.put(44, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return N_PAGE;
                }

                @Override
                public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                    page++;
                }

                @Override
                public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                    return true;
                }
            });
        }


        return button;
    }

    @Override
    public int getSize() {
        return 5 * 9;
    }

    @Override
    public boolean isAutoUpdate() {
        return true;
    }
}
