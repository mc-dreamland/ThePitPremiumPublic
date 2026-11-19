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

public class ShowGlowingOptionButton extends Button {
    @Override
    public ItemStack getButtonItem(Player player) {
        List<String> lines = new ArrayList<>();
        try {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            lines.add("&7是否隐藏激活超级连杀玩家的发光效果.");
            lines.add(" ");
            lines.add("&7当前: " + (profile.getPlayerOption().isShowGlowing() ? "&a显示" : "&c隐藏"));
            lines.add("&e点击切换此选项!");
            return new ItemBuilder(profile.getPlayerOption().isShowGlowing() ? Material.GLOW_INK_SAC : Material.INK_SAC).name("&a画面选项: 超级连杀发光效果").lore(lines).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ItemBuilder(Material.GLOW_INK_SAC).name("&a画面选项: 超级连杀发光效果").lore(lines).build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        profile.getPlayerOption().setShowGlowing(!profile.getPlayerOption().isShowGlowing());
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }
}
