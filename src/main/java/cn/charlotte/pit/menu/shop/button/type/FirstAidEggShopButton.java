package cn.charlotte.pit.menu.shop.button.type;

import cn.charlotte.pit.data.PlayerProfile;
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
 * @Created_In: 2021/3/21 18:55
 */
public class FirstAidEggShopButton extends AbstractShopButton {

    public FirstAidEggShopButton(boolean b) {
        super(b);
    }

    @Override
    public String getInternalName() {
        return "first_aid_egg";
    }

    @Override
    public ItemStack getDisplayButtonItem(Player player) {

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        int level = PerksShopEnum.FIRST_AID_EGG.getPerk().getLevel(profile);
        if (level <= 0) {
            return new ItemBuilder(Material.AIR)
                    .build();
        }

        List<String> lines = new ArrayList<>();
        lines.add("&7使用后恢复自身 &c2.5❤ &7(30秒冷却)");
        lines.add("&7击杀一名玩家将会减少5秒冷却");
        lines.add(" ");
        lines.add("&7&o死亡后消失");
        lines.add("&7价格: &6" + getDiscountPrice(player, getPrice(player)) + " 硬币");

        if (profile.getCoins() >= getDiscountPrice(player, getPrice(player))) {
            lines.add("&e点击购买!");
        } else {
            lines.add("&c硬币不足!");
        }
        return new ItemBuilder(Material.AXOLOTL_SPAWN_EGG)
                .name("&c温泉蛋")
                .lore(lines)
                .build();
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }

    @Override
    public ItemStack[] getResultItem(Player player) {
        return new ItemStack[]{
                new ItemBuilder(Material.AXOLOTL_SPAWN_EGG)
                        .name("&c温泉蛋")
                        .deathDrop(true)
                        .canSaveToEnderChest(true)
                        .internalName("first_aid_egg")
                        .build(),
        };
    }

    @Override
    public int getPrice(Player player) {
        return 200;
    }
}
