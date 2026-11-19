package cn.charlotte.pit.menu.shop;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.menu.shop.button.type.*;
import cn.charlotte.pit.menu.supporter.button.SupporterEntranceButton;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import cn.charlotte.pit.util.menu.builder.ButtonBuilder;
import cn.charlotte.pit.util.menu.builder.ClickHandler;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 16:57
 */
public class ShopMenu extends Menu {

    @Getter
    private final boolean b;

    public ShopMenu() {
        b = false;
    }

    public ShopMenu(boolean b) {
        this.b = b;
    }

    @Override
    public String getTitle(Player player) {
        return "商店";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> button = new HashMap<>();
        button.put(10, new DiamondSwordShopButton(b));
        button.put(11, new DiamondChestplateShopButton(b));
        button.put(12, new DiamondBootsShopButton(b));
        button.put(13, new BountySolventShopButton(b));
        button.put(14, new ObsidianShopButton(b));
        button.put(15, new ObsidianStackShopButton(b));
        button.put(16, new GoldPickaxeShopButton(b));

        button.put(19, new CombatSpadeShopButton(b));
        button.put(20, new DiamongLeggingsShopButton(b));
        button.put(21, new IronPackShopButton(b));
        button.put(22, new FancyHatShopButton(b));

        button.put(23, new FirstAidEggShopButton(b));
        button.put(24, new JumpBoostShopButton(b));
        button.put(25, new TacticalInsertionsShopButton(b));
        button.put(28, new SwordBundleShopButton(b));
        button.put(29, new BowBundleShopButton(b));
        button.put(30, new PantsBundleShopButton(b));
        button.put(31, new NameRevealEyeShopButton(b));
        button.put(35, new SupporterEntranceButton());

        if (CombatListener.isBedrockRoom() && PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.AUTO_BUY)) {
            button.put(34, new ButtonBuilder(
                    new ItemBuilder(Material.LEVER)
                            .name("&6切换模式")
                            .lore(
                                    "&7切换自动购买",
                                    "",
                                    "&7当前模式: " + (b ? "&a开启" : "&c关闭"),
                                    "&7在此模式中,你无法购买物品只能将物品添加到自动购买队列",
                                    "&e点击切换!"
                            ).build())
                    .addClickHandler((player1, slot, clickType, hotbarButton, currentItem) -> {
                        CC.send(player1, "切换成功!");
                        new ShopMenu(!b).openMenu(player1);
                    }).build());
        }

        return button;
    }

    @Override
    public int getSize() {
        return 4 * 9;
    }

}
