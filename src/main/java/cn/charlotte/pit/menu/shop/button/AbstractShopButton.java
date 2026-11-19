package cn.charlotte.pit.menu.shop.button;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 16:58
 */
public abstract class AbstractShopButton extends Button {

    private boolean b = false;

    public AbstractShopButton(boolean b) {
        this.b = b;
    }

    public static int getDiscountPrice(Player player, int price) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        int perkLevel = profile.getLevel(PerksPrestigeEnum.SCAM_ARTIST);
        if (perkLevel > 0) {
            return (int) ((1.0f - 0.05f * perkLevel) * price);
        }
        return price;
    }

    public abstract String getInternalName();

    public ItemStack getButtonItem(Player player) {
        if (!PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.AUTO_BUY) || !b) return getDisplayButtonItem(player);

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        ItemStack itemStack = getDisplayButtonItem(player);
        try {
            List<String> lines = itemStack.getItemMeta().getLore();
            lines.add(" ");
            lines.add("&7自动购买 " + (profile.getAutoBuyButtons().contains(getInternalName()) ? "&a开启" : "&c关闭"));
            lines.add("&b使用Shift点击以" + (profile.getAutoBuyButtons().contains(getInternalName()) ? "关闭" : "开启") + "自动购买!");
            return new ItemBuilder(itemStack).lore(lines).build();
        } catch (Exception e) {
            return getDisplayButtonItem(player);
        }
    }

    public abstract ItemStack getDisplayButtonItem(Player player);

    public abstract ItemStack[] getResultItem(Player player);

    public abstract int getPrice(Player player);

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (getButtonItem(player).getType().equals(Material.AIR)) {
            return;
        }

        if (clickType.isShiftClick() || b) {
            if (PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.AUTO_BUY)) {
                if (profile.getAutoBuyButtons().contains(getInternalName())) {
                    profile.getAutoBuyButtons().remove(getInternalName());
                } else {
                    profile.getAutoBuyButtons().add(getInternalName());
                }
                return;
            }
        }

        if (profile.getCoins() >= getDiscountPrice(player, getPrice(player))) {
            if (InventoryUtil.isInvFull(player.getInventory())) {
                player.sendMessage(CC.translate("&c&l背包已满! &7你的背包已满,暂时无法购买物品."));
                return;
            }
            profile.takeCoins(getDiscountPrice(player, getPrice(player)));
//            player.getInventory().addItem(this.getResultItem(player));
            PlayerUtil.addOrReplaceShopItem(player, this.getResultItem(player));
            player.sendMessage(CC.translate("&a购买成功!"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
        } else {
            player.sendMessage(CC.translate("&c你的硬币不足!"));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        }
    }
}
