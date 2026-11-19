package cn.charlotte.pit.menu.shop.button.type;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.type.NameRevealEye;
import cn.charlotte.pit.menu.shop.button.AbstractShopButton;
import cn.charlotte.pit.nametag.hiding.NameHidingManager;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class NameRevealEyeShopButton extends Button {
    private final boolean autoBuyMode;
    public NameRevealEyeShopButton(boolean autoBuyMode) { this.autoBuyMode = autoBuyMode; }

    private boolean available() {
        var manager = NameHidingManager.getInstance();
        return manager != null && manager.config().rules().enabled() && manager.config().revealItemEnabled();
    }

    private int price(Player player) {
        return Math.max(0, AbstractShopButton.getDiscountPrice(player, NameHidingManager.getInstance().config().price()));
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        if (!available()) return new ItemStack(Material.AIR);
        return new ItemBuilder(new NameRevealEye().toItemStack())
                .lore("").lore("&7价格: &6" + price(player) + " 硬币")
                .lore(autoBuyMode ? "&c请切换回手动购买模式。" : "&e点击购买一个！").build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType click, int hotbar, ItemStack current) {
        if (!available() || autoBuyMode || (click != ClickType.LEFT && click != ClickType.RIGHT)) return;
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        int price = price(player);
        if (!profile.isLoaded() || !Double.isFinite(profile.getCoins()) || profile.getCoins() < price) {
            player.sendMessage(CC.translate("&c硬币不足或玩家数据尚未就绪。"));
            return;
        }
        if (player.getInventory().firstEmpty() < 0) {
            player.sendMessage(CC.translate("&c背包已满，未扣除硬币。"));
            return;
        }
        ItemStack item = new NameRevealEye().toItemStack();
        if (!player.getInventory().addItem(item).isEmpty()) {
            player.sendMessage(CC.translate("&c背包空间不足，未扣除硬币。"));
            return;
        }
        profile.takeCoins(price);
        player.sendMessage(CC.translate("&a已购买一个显形药水。"));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
    }

    @Override public boolean shouldUpdate(Player player, int slot, ClickType click) { return true; }
}
