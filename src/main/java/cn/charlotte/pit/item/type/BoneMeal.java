package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BoneMeal extends AbstractPitItem implements IPlayerInteract {

    private final HashMap<UUID, Cooldown> cooldownHashMap = new HashMap<>(8);

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.BONE_MEAL)
                .canDrop(false)
                .canTrade(true)
                .canSaveToEnderChest(true)
                .deathDrop(false)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .removeOnJoin(false)
                .lore(
                        "",
                        "&f肥料",
                        " &7手持对准小麦进行右键使用,将以你为中心的 4×4×4 区域内的小麦催熟",
                        ""
                )
                .build();
    }

    @Override
    public String getInternalName() {
        return "bone_meal";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&f肥料";
    }

    @Override
    public void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || !block.getType().equals(Material.WHEAT)) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);

        if (cooldownHashMap.get(player.getUniqueId()) != null && !cooldownHashMap.get(player.getUniqueId()).hasExpired()) {
            CC.send(player, "&c请你再等等!");
            return;
        }

        Location loc = player.getLocation();

        int px = loc.getBlockX();
        int py = loc.getBlockY();
        int pz = loc.getBlockZ();

        for (int x = px - 1; x <= px + 2; x++) {
            for (int y = py - 1; y <= py + 2; y++) {
                for (int z = pz - 1; z <= pz + 2; z++) {
                    Block target = player.getWorld().getBlockAt(x, y, z);
                    if (target.getType() == Material.WHEAT) {
                        Ageable ageable = (Ageable) target.getBlockData();
                        if (ageable.getAge() < ageable.getMaximumAge()) {
                            ageable.setAge(ageable.getMaximumAge());
                            target.setBlockData(ageable);
                        }
                    }
                }
            }
        }

        PlayerUtil.takeOneItem(player, hand);

        cooldownHashMap.put(player.getUniqueId(), new Cooldown(3, TimeUnit.SECONDS));
    }
}