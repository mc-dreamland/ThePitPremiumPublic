package cn.charlotte.pit.enchantment.menu.button;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class EnchantBookButton extends Button {

    private final Menu menu;

    @Override
    public ItemStack getButtonItem(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        String bookItem = profile.getAttachedEnchant();
        if (bookItem == null) {
            return getNoneItem(profile);
        }
        ItemStack item = InventoryUtil.deserializeItemStack(bookItem);
        if (item == null || item.getType() == Material.AIR) {
            return getNoneItem(profile);
        }
        return item;
    }

    private ItemStack getNoneItem(PlayerProfile profile) {
        if (profile.getLevel(PerksPrestigeEnum.OCCULTDOCTRINE) <= 0) return PitItems.AIR.toItemStack();
        return new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .name("&e神秘附魔")
                .lore(
                        "&7将&6神话物品&7和&d神秘物品&7放入神话之井",
                        "&7将会为该&6神话物品&7一些奇怪的东西."
                )
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        String bookItem = profile.getAttachedEnchant();
        if (bookItem == null) {
            return;
        }
        ItemStack item = InventoryUtil.deserializeItemStack(bookItem);
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        if (InventoryUtil.isInvFull(player.getInventory())) {
            player.sendMessage("&c你的背包已满.");
            return;
        }
        profile.setAttachedEnchant(null);
        player.getInventory().addItem(item);
        player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1f, 0.65f);
        menu.openMenu(player);
    }
}
