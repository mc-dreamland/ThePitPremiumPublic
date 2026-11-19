package cn.charlotte.pit.nametag.hiding;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.*;

/** Main-thread, one-time adoption/restoration only. Never calls hidePlayer/showPlayer or changes real tracking. */
final class NameHidingRefresh {
    private final Map<UUID, Set<Player>> tracked = new HashMap<>();
    private final Map<UUID, List<Packet<? super ClientGamePacketListener>>> spawn = new HashMap<>();

    void run(Player viewer, Player target, Runnable profilePackets) {
        boolean visible = target != null && !viewer.equals(target) && viewer.getWorld().equals(target.getWorld())
                && viewer.canSee(target) && !target.isDead()
                && tracked.computeIfAbsent(target.getUniqueId(), ignored -> target.getTrackedBy()).contains(viewer);
        var connection = ((CraftPlayer) viewer).getHandle().connection;
        if (visible) connection.send(new ClientboundRemoveEntitiesPacket(target.getEntityId()));
        profilePackets.run();
        if (visible) {
            List<Packet<? super ClientGamePacketListener>> packets = spawn.computeIfAbsent(target.getUniqueId(), ignored -> {
                var handle = ((CraftPlayer) target).getHandle();
                List<Packet<? super ClientGamePacketListener>> result = new ArrayList<>();
                // A temporary packet snapshot, NOT the real tracker: never dirties its movement/metadata caches.
                var snapshot = new ServerEntity(handle.serverLevel(), handle, 1, false, unused -> { }, Set.of());
                snapshot.sendPairingData(((CraftPlayer) viewer).getHandle(), result::add);
                return List.copyOf(result);
            });
            connection.send(new ClientboundBundlePacket(packets));
        }
    }
}
