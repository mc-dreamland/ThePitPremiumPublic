package cn.charlotte.pit.movement;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.config.PitConfig;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.runnable.ProfileLoadRunnable;
import cn.charlotte.pit.util.VectorUtil;
import cn.charlotte.pit.util.aabb.AxisAlignedBB;
import cn.charlotte.pit.util.chat.ActionBarUtil;
import cn.charlotte.pit.util.chat.CC;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/16 23:49
 */
public class PlayerMoveHandler implements PlayerMovementListener, Listener {
    private static final Set<Player> cantMoveList = Collections.synchronizedSet(new HashSet<>());

    public PlayerMoveHandler() {
        Bukkit.getServer().getPluginManager().registerEvents(this, ThePit.getInstance());
    }

    public static Set<Player> getCantMoveList() {
        return PlayerMoveHandler.cantMoveList;
    }

    public static void checkMove(Location to, Location from, Player player) {

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        //when X/Z Loc change
        if (to.getBlockX() != from.getBlockX() || to.getBlockZ() != from.getBlockZ()) {
            if (!profile.isLoaded() && ProfileLoadRunnable.getInstance() != null && ProfileLoadRunnable.getInstance().getCooldownMap() != null && ProfileLoadRunnable.getInstance().getCooldownMap().containsKey(player.getUniqueId())) {
                ActionBarUtil.sendActionBar(player, "&c正在加载您的游戏数据,如长时间等待请尝试重新进入...");

                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 9999999, 1, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 9999999, -100, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 9999999, -100, false));
                return;
            }
        }

        if (!profile.isEditingMode()) {
            if (profile.isInArena()) {
                PitConfig config = ThePit.getInstance().getPitConfig();
                final AxisAlignedBB aabb = new AxisAlignedBB(config.getPitLocA().getX(), config.getPitLocA().getY(), config.getPitLocA().getZ(), config.getPitLocB().getX(), config.getPitLocB().getY(), config.getPitLocB().getZ());

                final AxisAlignedBB playerAABB = new AxisAlignedBB(to.getX(), to.getY(), to.getZ(), to.getX() + 0.8, to.getY() + 2, to.getZ() + 0.8);

                profile.setInArena(!aabb.intersectsWith(playerAABB));
            }

            if (profile.isInArena()) {
                if (player.getGameMode() == GameMode.ADVENTURE) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
            } else {
                if (player.getGameMode() == GameMode.SURVIVAL) {
                    player.setGameMode(GameMode.ADVENTURE);
                }
            }
        }
//        if (PlayerUtil.isStaffSpectating(player)) {
//            player.setAllowFlight(true);
//        }

        boolean backing = player.hasMetadata("backing");
        if (backing) {
            if (to.getBlockX() != from.getBlockX() ||
                    to.getBlockY() != from.getBlockY() ||
                    to.getBlockZ() != from.getBlockZ()) {
                player.removeMetadata("backing", ThePit.getInstance());
                player.sendMessage(CC.translate("&c回城被取消."));
            }
        }


        if (to.getBlock().getRelative(BlockFace.DOWN).getType() == Material.SLIME_BLOCK) {
            BlockIterator blockIterator = new BlockIterator(player.getLocation());
            for (int i = 0; i < 30; i++) {
                blockIterator.next();
            }
            to.setPitch(-30);
            player.getLocation().setPitch(-30);
            VectorUtil.entityPush(player, blockIterator.next().getLocation(), 110);

            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
            ClientboundLevelParticlesPacket particlesPacket = new ClientboundLevelParticlesPacket(ParticleTypes.EXPLOSION_EMITTER,
                    true, to.getX(), to.getY(), to.getZ(), 0, 0, 0, 1, 1);
            ((CraftPlayer) player).getHandle().connection.sendPacket(particlesPacket);
        }

    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (cantMoveList.contains(event.getPlayer())) {
            final Location to = event.getTo();
            final Location from = event.getFrom();

            if (to.getBlockX() != from.getBlockX() ||
                    to.getBlockY() != from.getBlockY() ||
                    to.getBlockZ() != from.getBlockZ()) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void handleUpdateLocation(Player player, Location location, Location location1) {
        checkMove(location, location1, player.getPlayer());
    }

    @Override
    public void handleUpdateRotation(Player player, Location location, Location location1) {
        PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).setLastActionTimestamp(System.currentTimeMillis());
    }
}
