package cn.charlotte.pit.menu.option.button;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;


public class SewerChestButton extends Button {
    @Override
    public ItemStack getButtonItem(Player player) {
        List<String> lines = new ArrayList<>();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (!profile.isSupporter() && !PlayerUtil.isStaff(player)) {
            profile.getPlayerOption().setInventoryVisibility(false);
        }
        lines.add("&7允许系统向您播报关于下水道箱子刷新的时间.");
        lines.add(" ");
        lines.add("&7当前: " + (profile.getPlayerOption().isSewersChest() ? "&a允许" : "&c不允许"));
        lines.add((profile.isSupporter() || PlayerUtil.isStaff(player) ? "&e&e点击切换此选项!" : "&c需要&e天坑乱斗增值包"));
        return new ItemBuilder(Material.CHEST).name("&a信息选项: 下水道箱子").lore(lines).build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (!profile.isSupporter() && !PlayerUtil.isStaff(player)) {
            return;
        }
        profile.getPlayerOption().setSewersChest(!profile.getPlayerOption().isSewersChest());
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }
}
