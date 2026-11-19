package cn.charlotte.pit.menu.shop.button.type;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.menu.shop.button.AbstractShopButton;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FancyHatShopButton extends AbstractShopButton {

    public FancyHatShopButton(boolean b) {
        super(b);
    }

    @Override
    public String getInternalName() {
        return "FancyHatShop";
    }

    @Override
    public ItemStack getDisplayButtonItem(Player player) {
        if (!PerksPrestigeEnum.FANCY_HAT.getPerk().hasUnlock(player)) {
            return new ItemBuilder(Material.AIR).build();
        }
        List<String> lines = new ArrayList<>();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        lines.add("");
        lines.add("&9五彩斑斓");
        lines.add("&7可以在神话之井中改变颜色(&c需要天坑乱斗增值包&7).");
        lines.add("");
        lines.add("&6穿着时提供锁链头盔相同的伤害减免效果");
        lines.add(" ");
        lines.add("&7价格: &62000 硬币");

        if (profile.getCoins() >= 2000) {
            lines.add("&e点击购买!");
        } else {
            lines.add("&c硬币不足!");
        }

        return new ItemBuilder(Material.LEATHER_HELMET)
                .lore(lines)
                .canTrade(false)
                .itemArmor(2.0)
                .canSaveToEnderChest(true)
                .deathDrop(false)
                .removeOnJoin(false)
                .internalName("FancyHat")
                .canDrop(false)
                .shiny()
                .buildWithUnbreakable();

    }

    @Override
    public ItemStack[] getResultItem(Player player) {
        return new ItemStack[]{PitItems.FANCY_HAT_HELMET.toItemStack()};
    }

    @Override
    public int getPrice(Player player) {
        return 2000;
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }
}
