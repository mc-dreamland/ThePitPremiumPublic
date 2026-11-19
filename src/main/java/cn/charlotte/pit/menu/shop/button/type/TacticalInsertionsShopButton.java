package cn.charlotte.pit.menu.shop.button.type;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.menu.shop.button.AbstractShopButton;
import cn.charlotte.pit.perk.PerksShopEnum;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/3/29 19:15
 */
public class TacticalInsertionsShopButton extends AbstractShopButton {

    public TacticalInsertionsShopButton(boolean b) {
        super(b);
    }

    @Override
    public String getInternalName() {
        return "tactical_insertions";
    }

    @Override
    public ItemStack getDisplayButtonItem(Player player) {
        ItemStack itemStack = PitItems.TACTICAL_INSERTIONS.toItemStack();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        List<String> lines = new ArrayList<>(itemStack.getItemMeta().getLore());
        lines.add(" ");
        lines.add("&7价格: &6" + getDiscountPrice(player, getPrice(player)) + " 硬币");

        if (profile.getCoins() >= getDiscountPrice(player, getPrice(player))) {
            lines.add("&e点击购买!");
        } else {
            lines.add("&c硬币不足!");
        }

        int data = profile.getLevel(PerksShopEnum.TACTICAL_INSERTION);
        if (data > 0) {
            return new ItemBuilder(itemStack)
                    .lore(lines)
                    .build();
        }

        return new ItemBuilder(Material.AIR)
                .build();
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }

    @Override
    public ItemStack[] getResultItem(Player player) {
        return new ItemStack[]{
                PitItems.TACTICAL_INSERTIONS.toItemStack()
        };
    }

    @Override
    public int getPrice(Player player) {
        return 200;
    }
}
