package cn.charlotte.pit.fishing;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.random.RandomUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

/**
 * @Author: EmptyIrony
 * @Date: 2021/2/6 20:12
 */
@AutoRegister
public class FishingHandler implements Listener {

    @EventHandler
    public void onFishing(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            event.setExpToDrop(0);
            event.getCaught().remove();

            Location location = event.getCaught().getLocation();

            ItemStack itemStack = this.generateItem(event.getPlayer(), location);
            ItemEntity entityItem = this.generateEntity(event.getPlayer(), location, itemStack);
            CraftEntity item = entityItem.getBukkitEntity();
            item.setMetadata("gold", new FixedMetadataValue(ThePit.getInstance(), RandomUtil.random.nextInt(5) + 5));
        } else if (event.getState() == PlayerFishEvent.State.FISHING) {
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

        }
    }


    private ItemEntity generateEntity(Player player, Location dropLocation, ItemStack item) {
        ServerLevel worldServer = ((CraftWorld) dropLocation.getWorld()).getHandle();
        ItemEntity entityItem = new ItemEntity(worldServer, dropLocation.getX(), dropLocation.getY(), dropLocation.getZ(), CraftItemStack.asNMSCopy(item));
        worldServer.addFreshEntity(entityItem);

        double d5 = player.getLocation().getX() - dropLocation.getX();
        double d6 = player.getLocation().getY() - dropLocation.getY();
        double d7 = player.getLocation().getZ() - dropLocation.getZ();
        double d8 = Math.sqrt(d5 * d5 + d6 * d6 + d7 * d7);
        double d9 = 0.1D;

        double motX = d5 * d9;
        double motY = d6 * d9 + Math.sqrt(d8) * 0.08D;
        double motZ = d7 * d9;

        entityItem.getBukkitEntity().setVelocity(new Vector(motX, motY, motZ));

        return entityItem;
    }

    private ItemStack generateItem(Player player, Location location) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        return new ItemBuilder(Material.GOLD_INGOT)
                .build();
    }

}
