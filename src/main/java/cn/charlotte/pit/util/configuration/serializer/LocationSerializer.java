package cn.charlotte.pit.util.configuration.serializer;

import cn.charlotte.pit.util.configuration.AbstractSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LocationSerializer extends AbstractSerializer<Location> {

    @Override
    public String serializer(Location data) {
        if (data == null) {
            return "";
        }
        return data.getWorld().getName() + "|" + data.getX() + "|" + data.getY() + "|" + data.getZ() + "|" + data.getYaw() + "|" + data.getPitch();
    }

    @Override
    public Location deserializer(Object data) {
        if (data == null) {
            return new Location(Bukkit.getWorlds().getFirst(), 0, 50, 0);
        }

        String[] parts = ((String) data).split("\\|");
        if (parts.length == 6) {
            return new Location(Bukkit.getWorld(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
        }
        if (parts.length == 4) {
            return new Location(Bukkit.getWorld(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
        }

        return new Location(Bukkit.getWorlds().getFirst(), -1, -1, -1);
    }

}
