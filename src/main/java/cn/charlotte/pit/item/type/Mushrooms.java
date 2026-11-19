package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class Mushrooms extends AbstractPitItem implements IPlayerInteract {

    @Override
    protected String getDefaultItemDisplayName() {
        return "蘑菇";
    }

    @Override
    public String getInternalName() {
        return "mushrooms";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.BROWN_MUSHROOM)
                .name(getItemDisplayName())
                .lore("", "&f奇特的蘑菇貌似带有某种魔力?", "")
                .internalName("mushrooms")
                .canDrop(false)
                .canTrade(true)
                .canSaveToEnderChest(true)
                .build();
    }

    @Override
    public void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent event) {

    }
}
