package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.menu.gem.KillCounterGemMenu;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class KillCounterGem extends AbstractPitItem implements Listener {

    public static final String INTERNAL_NAME = "kill_counter_gem";

    @EventHandler
    public void interact(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (INTERNAL_NAME.equals(ItemUtil.getInternalName(item))) {
            event.setCancelled(true);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
            new KillCounterGemMenu(item).openMenu(event.getPlayer());
        }
    }

    @Override
    public String getInternalName() {
        return INTERNAL_NAME;
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&b计数器宝石";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.DIAMOND;
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(getItemDisplayMaterial())
                .name(getItemDisplayName())
                .lore(
                        "&7死亡时保留",
                        "",
                        "&7为神话物品添加击杀计数器，并在击杀时进行播报",
                        "&8一件物品只能使用一次",
                        "",
                        "&e右键使用"
                )
                .internalName(getInternalName())
                .shiny()
                .removeOnJoin(false)
                .deathDrop(false)
                .canDrop(false)
                .canTrade(false)
                .enchant(Enchantment.UNBREAKING, 1)
                .canSaveToEnderChest(true)
                .build();
    }
}
