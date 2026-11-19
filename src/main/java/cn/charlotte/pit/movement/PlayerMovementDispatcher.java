package cn.charlotte.pit.movement;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by EmptyIrony on 2021/6/20.
 */
public final class PlayerMovementDispatcher implements Listener {
    private final List<PlayerMovementListener> listeners = new ArrayList<>();

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        dispatch(event);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        dispatch(event);
    }

    private void dispatch(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        for (PlayerMovementListener listener : listeners) {
            if (from.getPitch() != to.getPitch() || from.getYaw() != to.getYaw()) {
                listener.handleUpdateRotation(event.getPlayer(), from, to);
            }
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                listener.handleUpdateLocation(event.getPlayer(), from, to);
            }
        }
    }

    public void addListener(PlayerMovementListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PlayerMovementListener listener) {
        listeners.remove(listener);
    }
}
