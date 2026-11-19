package cn.charlotte.pit.util.menu;

import cn.charlotte.pit.util.item.ItemBuilder;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class Button {

    public static final ItemStack AIR = new ItemBuilder(Material.AIR).build();
    protected final static ItemStack R_PAGE = new ItemBuilder(Material.ARROW)
            .name("&f主页")
            .build();

    public static Button placeholder(final Material material, String... title) {
        return (new Button() {
            public ItemStack getButtonItem(Player player) {
                ItemStack it = new ItemStack(material, 1);
                ItemMeta meta = it.getItemMeta();

                meta.setDisplayName(StringUtils.join(title));
                it.setItemMeta(meta);

                return it;
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {

            }
        });
    }

    public static void playFail(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_GRASS_HIT, 20F, 0.1F);

    }

    public static void playSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 20F, 15F);
    }

    public static void playNeutral(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 20F, 1F);
    }

    public abstract ItemStack getButtonItem(Player player);

    public abstract void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem);

    public boolean shouldCancel(Player player, int slot, ClickType clickType) {
        return (true);
    }

    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return (false);
    }

}