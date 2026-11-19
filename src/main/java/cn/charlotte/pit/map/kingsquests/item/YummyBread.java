package cn.charlotte.pit.map.kingsquests.item;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.FuncsKt;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.hologram.HologramAPI;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class YummyBread extends AbstractPitItem implements Listener {

    @EventHandler
    public void onInteractBlock(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (block.getType() == Material.FURNACE) {
            event.setCancelled(true);
        } else {
            return;
        }

        // 统计小麦数量
        int count = FuncsKt.countItem(player.getInventory(), i -> i.getType() == Material.WHEAT);
        if (count < 64) return;

        // 移除 64 个小麦
        FuncsKt.takeItem(player.getInventory(), 64, i -> i.getType() == Material.WHEAT);

        Furnace furnace = (Furnace) block.getState();
        furnace.setBurnTime((short) 9999);
        furnace.update();

        // 面前生成全息文字
        org.bukkit.util.Vector vector = player.getLocation().getDirection().clone().multiply(-1);
        vector.setY(0);
        org.bukkit.Location hologramLoc = block.getLocation().add(vector);

        var hologram = HologramAPI.createHologram(hologramLoc, CC.translate("&a10"));
        hologram.spawn();

        new BukkitRunnable() {
            int countdown = 10;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    hologram.deSpawn();
                    block.setType(Material.FURNACE);
                    furnace.setBurnTime((short) 0);
                    furnace.setCookTime((short) 0);
                    furnace.update();
                    cancel();
                    return;
                }

                countdown--;
                if (countdown <= 0) {
                    player.getInventory().addItem(toItemStack());
                    player.playSound(block.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    hologram.deSpawn();
                    block.setType(Material.FURNACE);
                    furnace.setBurnTime((short) 0);
                    furnace.setCookTime((short) 0);
                    furnace.update();
                    cancel();
                    return;
                }

                String text;
                if (countdown <= 3) {
                    text = "&c" + countdown;
                } else if (countdown <= 7) {
                    text = "&e" + countdown;
                } else {
                    text = "&a" + countdown;
                }
                hologram.setText(CC.translate(text));
            }
        }.runTaskTimer(cn.charlotte.pit.ThePit.getInstance(), 20L, 20L); // 每秒一次
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        if (hand == null) return;

        ItemStack item = player.getInventory().getItem(hand);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            var target = player.getTargetEntity(4);
            if (target instanceof Player) {
                return;
            }
            if ("yummy_bread".equals(ItemUtil.getInternalName(item))) {
                PlayerUtil.takeOneItem(player, hand);
                PlayerUtil.heal(player, 1.0);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f);
            }
        }
    }

    @Override
    public String getInternalName() {
        return "yummy_bread";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&e美味的面包";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.BREAD;
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
                .lore("&7死亡时保留")
                .build();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        // 保持空实现
    }
}
