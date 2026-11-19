package cn.charlotte.pit.menu.cdk.generate.button;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.util.callback.Callback;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.exception.SignGUIVersionException;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;

/**
 * @Author: Misoryan
 * @Created_In: 2021/3/25 20:18
 */
public class CDKEditButton extends Button {
    private final ItemStack material;
    private final Menu menu;
    private final Callback<Integer> doubleCallback;

    public CDKEditButton(ItemStack material, Menu menu, Callback<Integer> doubleCallback) {
        this.material = material;
        this.menu = menu;
        this.doubleCallback = doubleCallback;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        return material;
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {

        try {
            SignGUI signGUI = SignGUI.builder().setType(Material.BAMBOO_SIGN).setColor(DyeColor.CYAN)
                    .setLines("", "~~~~~~~~~~~~~", "请在此输入", "内容")
                    .setHandler((player1, result) -> {
                                String[] lines = result.getLines();
                                player.closeInventory();
                                menu.setClosedByMenu(true);
                                Bukkit.getScheduler()
                                        .runTaskLater(ThePit.getInstance(), () -> menu.openMenu(player), 1L);

                                try {
                                    final int v = Integer.parseInt(lines[0]);
                                    doubleCallback.call(v);
                                } catch (Exception ignore) {
                                    player.sendMessage(CC.translate("&c不是一个有效的数字"));
                                }
                                return Collections.emptyList();
                            }
                    ).build();
            signGUI.open(player);
        } catch (SignGUIVersionException e) {
            e.printStackTrace();
        }
    }
}
