package cn.charlotte.pit.menu.perk.normal.buy.button;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.perk.normal.choose.PerkChooseMenu;
import cn.charlotte.pit.perk.AbstractPerk;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 23:56
 */
public class ResetButton extends Button {
    private final int page;

    public ResetButton(int page) {
        this.page = page;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.DIAMOND_BLOCK)
                .name("&c重置天赋")
                .lore(" ", "&7重置当前天赋栏为未选定状态.", "", "&7你已经足够强大了吗?")
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        String perkName = profile.getChosePerk()[page - 1];
        if (perkName != null) {
            AbstractPerk abstractPerk = ThePit.getInstance().getPerkFactory().getPerkMap().get(perkName);
            if (abstractPerk != null) {
                abstractPerk.onPerkInactive(player);
            }
        }
        profile.getChosePerk()[page - 1] = null;
        new PerkChooseMenu().openMenu(player);
        player.sendMessage(CC.translate("&a&l重置天赋栏! &7当前天赋栏目前已被重置为未选定状态."));
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }
}
