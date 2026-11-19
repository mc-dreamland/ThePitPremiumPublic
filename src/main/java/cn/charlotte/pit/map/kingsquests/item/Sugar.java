package cn.charlotte.pit.map.kingsquests.item;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class Sugar extends AbstractPitItem implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() != Material.MELON) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.getType().name().contains("_AXE")) {
            return;
        }

        event.setCancelled(false);
        block.setType(Material.AIR);

        if (!CombatListener.isNight()) {
            player.getInventory().addItem(toItemStack());
        } else {
            player.sendMessage(CC.translate("&c夜已深 &7已禁用糖获得"));
        }

        // 6小时后重生
        long delay = 20L * 60 * 60 * 6; // tick
        Bukkit.getScheduler().runTaskLater(
                cn.charlotte.pit.ThePit.getInstance(),
                () -> {
                    block.setType(Material.MELON);
                    block.getWorld().playSound(block.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                },
                delay
        );
    }

    @Override
    public String getInternalName() {
        return "sugar";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&a糖";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.SUGAR;
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(getItemDisplayMaterial())
                .internalName(getInternalName())
                .canDrop(false)
                .canTrade(true)
                .deathDrop(true)
                .canSaveToEnderChest(true)
                .name(getItemDisplayName())
                .lore("&7死亡时掉落")
                .build();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        // 保持空实现
    }

    public ItemStack toManyItem(int amount) {
        return new ItemBuilder(getItemDisplayMaterial())
                .internalName(getInternalName())
                .canDrop(false)
                .amount(amount)
                .canTrade(true)
                .deathDrop(true)
                .canSaveToEnderChest(true)
                .name(getItemDisplayName())
                .lore("&7死亡时掉落")
                .build();
    }
}
