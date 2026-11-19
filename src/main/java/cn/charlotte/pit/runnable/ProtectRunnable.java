package cn.charlotte.pit.runnable;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.config.PitConfig;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.util.aabb.AxisAlignedBB;
import cn.charlotte.pit.util.item.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;


/**
 * @Author: Misoryan
 * @Created_In: 2021/2/27 15:16
 */
public class ProtectRunnable extends BukkitRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            if (!profile.isEditingMode() /*|| !PlayerUtil.isStaffSpectating(player)*/) {
                if (!profile.isInArena()) {
                    PitConfig config = ThePit.getInstance().getPitConfig();
                    final AxisAlignedBB aabb = new AxisAlignedBB(config.getPitLocA().getX(), config.getPitLocA().getY(), config.getPitLocA().getZ(), config.getPitLocB().getX(), config.getPitLocB().getY(), config.getPitLocB().getZ());

                    Location location = player.getLocation();
                    final AxisAlignedBB playerAABB = new AxisAlignedBB(location.getX() - 0.4, location.getY(), location.getZ() - 0.4, location.getX() + 0.4, location.getY() + 2, location.getZ() + 0.4);
                    final boolean inArena = !aabb.intersectsWith(playerAABB);
                    if (inArena) {
                        profile.setInArena(true);
                        continue;
                    }
                    boolean training = PitBotManager.activeTraining(player);
                    for (ItemStack item : player.getInventory()) {
                        if (ItemUtil.isRemovedOnJoin(item)
                                && !(training && ItemUtil.isHealingItem(item))) {
                            player.getInventory().remove(item);
                        }
                    }
                    if (ItemUtil.isRemovedOnJoin(player.getInventory().getHelmet())) {
                        player.getInventory().setHelmet(new ItemStack(Material.AIR));
                    }
                    if (ItemUtil.isRemovedOnJoin(player.getInventory().getChestplate())) {
                        player.getInventory().setChestplate(new ItemStack(Material.AIR));
                    }
                    if (ItemUtil.isRemovedOnJoin(player.getInventory().getLeggings())) {
                        player.getInventory().setLeggings(new ItemStack(Material.AIR));
                    }
                    if (ItemUtil.isRemovedOnJoin(player.getInventory().getBoots())) {
                        player.getInventory().setBoots(new ItemStack(Material.AIR));
                    }
                    profile.setBounty(0);
                    if (!PitBotManager.keepTrainingBotStreak(player, profile)) profile.setStreakKills(0);
                    if (player.getGameMode() == GameMode.SURVIVAL) {
                        player.setGameMode(GameMode.ADVENTURE);
                    }
                } else {
                    if (player.getGameMode() == GameMode.ADVENTURE) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                    //TODO 未来移除
                    for (ItemStack item : player.getInventory()) {
                        String internalName = ItemUtil.getInternalName(item);
                        if (internalName != null && internalName.equals("packaged_bale") && !ItemUtil.isDeathDrop(item)) {
                            player.getInventory().remove(item);
                        }
                    }
                }
            }
        }
    }

}
