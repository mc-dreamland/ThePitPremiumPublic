package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.menu.gem.TotallyLegitGemMenu;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class TotallyLegitGem extends AbstractPitItem implements Listener {

    @EventHandler
    public void interact(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if ("totally_legit_gem".equals(ItemUtil.getInternalName(item))) {
            event.setCancelled(true);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
            new TotallyLegitGemMenu().openMenu(event.getPlayer());
        }
    }

    @Override
    public String getInternalName() {
        return "totally_legit_gem";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&a遵纪守法的宝石";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.EMERALD;
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(getItemDisplayMaterial())
                .name(getItemDisplayName())
                .lore(
                        "&7死亡时保留",
                        "",
                        "&7增加附魔物品的一级附魔, 并使神话物品生命后添加 &a♦ &7的字符",
                        "&7(稀有及特殊附魔除外, 不可超过上限)",
                        "&8一件物品只能使用一次",
                        "",
                        "&e右键使用"
                )
                .internalName(getInternalName())
                .shiny()
                .removeOnJoin(false)
                .deathDrop(false)
                .canDrop(false)
                .canTrade(true)
                .canSaveToEnderChest(true)
                .build();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        // no-op
    }
}
