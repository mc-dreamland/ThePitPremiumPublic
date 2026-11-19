package cn.charlotte.pit.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Created by EmptyIrony on 2021/6/20.
 */
public interface PlayerMovementListener {

    void handleUpdateLocation(Player player, Location from, Location to);

    void handleUpdateRotation(Player player, Location from, Location to);
}
