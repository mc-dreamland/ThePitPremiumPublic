package cn.charlotte.pit.api;

import org.bukkit.entity.Player;

public interface PointsAPI {

    boolean hasPoints(Player player, int points);

    int getPoints(Player player);

    void costPoints(Player player, int points, String reason);
}
