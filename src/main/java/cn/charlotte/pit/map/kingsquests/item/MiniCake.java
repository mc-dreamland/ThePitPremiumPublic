package cn.charlotte.pit.map.kingsquests.item;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class MiniCake extends AbstractPitItem implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack itemStack = event.getItemInHand();

        if (itemStack.getType() == Material.CAKE &&
                "mini_cake".equals(ItemUtil.getInternalName(itemStack))) {
            event.setCancelled(true);
        }
    }

    @Override
    public String getInternalName() {
        return "mini_cake";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&d迷你蛋糕";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.CAKE;
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(getItemDisplayMaterial())
                .internalName(getInternalName())
                .canDrop(false)
                .canTrade(true)
                .deathDrop(false)
                .canSaveToEnderChest(true)
                .name(getItemDisplayName())
                .lore(
                        "&7死亡时保留",
                        "&7国王的最爱"
                )
                .dontStack()
                .build();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        // 保持空实现
    }
}
