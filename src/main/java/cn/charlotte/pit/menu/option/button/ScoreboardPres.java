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

public class ScoreboardPres extends Button {

    @Override
    public ItemStack getButtonItem(Player player) {
        List<String> lines = new ArrayList<>();
        try {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            lines.add("&7是否显示精通等级.");
            lines.add(" ");
            lines.add("&7当前: " + (profile.getPlayerOption().isScoreboard_Pres_opt() ? "&a显示" : "&c不显示"));
            lines.add("&e点击切换此选项!");
            return new ItemBuilder(Material.DIAMOND_BLOCK).name("&a计分板类型选项: 精通").lore(lines).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ItemBuilder(Material.DIAMOND_BLOCK).name("&a计分板类型选项: 精通").lore(lines).build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        profile.getPlayerOption().setScoreboard_Pres_opt(!profile.getPlayerOption().isScoreboard_Pres_opt());
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }
}
