package cn.charlotte.pit.menu.option.button;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BarTimeOptionButton extends Button {

    @Override
    public ItemStack getButtonItem(Player player) {
        List<String> lines = new ArrayList<>();
        try {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            lines.add("&7调整攻击到玩家时显示对方状态持续时间.");
            lines.add(" ");
//            lines.add("&7当前: &e" + profile.getPlayerOption().getBartime() + "秒");
            lines.add("&e点击切换此选项!");
            return new ItemBuilder(Material.CLOCK).name("&a信息选项: 提示栏").lore(lines).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ItemBuilder(Material.CLOCK).name("&a画面选项: 提示栏").lore(lines).build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
//        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
//        int bartime = profile.getPlayerOption().getBartime();
//        profile.getPlayerOption().setBartime(bartime == 3 ? 1 : bartime + 1);
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }
}
