package cn.charlotte.pit.menu.shop.button.type;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.type.swordBundleEmpty;
import cn.charlotte.pit.menu.shop.button.AbstractShopButton;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.PerksShopEnum;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@AutoRegister
public class SwordBundleShopButton extends AbstractShopButton {
    
    public SwordBundleShopButton(boolean b) {
        super(b);
    }
    
    @Override
    public String getInternalName() {
        return "sword_bundle";
    }
    
    @Override
    public ItemStack getDisplayButtonItem(Player player) {
        List<String> lines = new ArrayList<>();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        lines.add("&7死亡后保留");
        lines.add(" ");
        lines.add("&7手持右键即可将背包内");
        lines.add("&710件未附魔的神话之剑打包.");
        lines.add(" ");
        lines.add("&7价格: &6" + getDiscountPrice(player, getPrice(player)) + " 硬币");
        
        if (profile.getCoins() >= getDiscountPrice(player, getPrice(player))) {
            lines.add("&e点击购买!");
        } else {
            lines.add("&c硬币不足!");
        }
        
        int data = profile.getLevel(PerksShopEnum.SWORD_BUNDLE);
        if (data > 0) {
            return new ItemBuilder(Material.MINECART)
                    .name("&b神话之剑收纳箱")
                    .lore(lines)
                    .build();
        }
        return new ItemBuilder(Material.AIR).build();
    }
    
    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }
    
    @Override
    public ItemStack[] getResultItem(Player player) {
        return new ItemStack[]{new swordBundleEmpty().toItemStack()};
    }
    
    @Override
    public int getPrice(Player player) {
        return 50;
    }
}