package cn.charlotte.pit.util;

import org.bukkit.Location;

/**
 * @Author: EmptyIrony
 * @Date: 2021/3/7 16:45
 */
public class LocationUtil {
    public static Location[] getFaces(Location start) {
        Location[] faces = new Location[6];
        faces[0] = new Location(start.getWorld(), start.getX() + 1, start.getY(), start.getZ());
        faces[1] = new Location(start.getWorld(), start.getX() - 1, start.getY(), start.getZ());
        faces[2] = new Location(start.getWorld(), start.getX(), start.getY() + 1, start.getZ());
        faces[3] = new Location(start.getWorld(), start.getX(), start.getY() - 1, start.getZ());
        faces[4] = new Location(start.getWorld(), start.getX(), start.getY(), start.getZ() + 1);
        faces[5] = new Location(start.getWorld(), start.getX(), start.getY(), start.getZ() - 1);
        return faces;
    }
}
