package cn.charlotte.pit.menu.shop.button.type;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.menu.shop.button.AbstractShopButton;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.PerksShopEnum;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Araykal
 * @since 2025/2/4
 */
public class BowBundleShopButton extends AbstractShopButton implements Listener {

    public boolean admin;

    public BowBundleShopButton(boolean b) {
        super(b);
    }


    @Override
    public String getInternalName() {
        return "bow_bundle";
    }

    @Override
    public ItemStack getDisplayButtonItem(Player player) {
        List<String> lines = new ArrayList<>();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        lines.add("&7死亡后保留");
        lines.add(" ");
        lines.add("&7手持右键即可将背包内");
        lines.add("&710件未附魔的神话之弓打包.");
        lines.add(" ");
        lines.add("&7价格: &6" + getDiscountPrice(player, getPrice(player)) + " 硬币");

        if (profile.getCoins() >= getDiscountPrice(player, getPrice(player))) {
            lines.add("&e点击购买!");
        } else {
            lines.add("&c硬币不足!");
        }

        int perkLevel = profile.getLevel(PerksShopEnum.BOW_BUNDLE);
        if (perkLevel > 0) {
            return new ItemBuilder(Material.MINECART)
                    .name("&b神话之弓收纳箱")
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
        List<String> lines = new ArrayList<>();
        lines.add("&7死亡后保留");
        lines.add(" ");
        lines.add("&7手持右键即可将背包内");
        lines.add("&710件未附魔的神话之弓打包.");
        return new ItemStack[]{
                new ItemBuilder(Material.MINECART)
                        .name("&b神话之弓收纳箱")
                        .lore(lines)
                        .canSaveToEnderChest(true)
                        .internalName(PitItems.BOW_BUNDLE_EMPTY.getInternalName())
                        .build(),
        };
    }

    @Override
    public int getPrice(Player player) {
        return 50;
    }
}