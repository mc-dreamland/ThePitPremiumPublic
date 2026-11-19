package cn.charlotte.pit.menu.exorcismaltar.button;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ExorcismButton extends Button {
    @Override
    public ItemStack getButtonItem(Player player) {
        int count = PerksPrestigeEnum.HERESY.getPerk().getLevel(player);
        boolean isMax = count >= 3;
        boolean isUnlock = PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.PROMOTION);
        return new ItemBuilder(Material.GRINDSTONE)
                .name("&d去魔")
                .lore(
                        "",
                        "&7将你用不到的神话装备转换成&3时髦的羽毛",
                        String.format("&7价格: &5暗聚块 &7* %s,&e金币 &7 * 10000", isMax ? 2 : 3),
                        String.format("&7神话物品剩余命数转换成&3时髦的羽毛&7比例 %s",isUnlock ? "1/2" : "1/3"),
                        ""
                )
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        PlayerProfile profile = Util.getPitProfile(player);
        ItemStack itemStack = InventoryUtil.deserializeItemStack(profile.getExorcismAltarItem());
        if (itemStack == null ) {
            CC.send(player,"&c请先放入");
            return;
        }
    }
}
