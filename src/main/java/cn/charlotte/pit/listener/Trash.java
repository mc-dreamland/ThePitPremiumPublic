package cn.charlotte.pit.listener;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.parm.AutoRegister;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

@AutoRegister
public class Trash implements Listener {

    @EventHandler
    public void onBarrelInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null ||
                clickedBlock.getType() != Material.BARREL) {
            return;
        }
        Player player = event.getPlayer();
        if (PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).isInArena()) {
            return;
        }
        // 关闭已打开的界面
        if (player.getOpenInventory() != null) {
            player.closeInventory();
        }

        player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 0.5f, 1.0f);
        event.setCancelled(true);

        Inventory trashInventory = Bukkit.createInventory(null, 54, "垃圾桶");
        player.openInventory(trashInventory);
    }

}