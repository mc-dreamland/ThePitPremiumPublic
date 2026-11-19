package cn.charlotte.pit.util.configuration.serializer;

import cn.charlotte.pit.util.configuration.AbstractSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.HashMap;

public class ConfigRegionSerializer extends AbstractSerializer<ConfigRegion> {

    @Override
    public Object serializer(ConfigRegion data) {
        if (data == null) {
            return "";
        }
        HashMap<String, Object> out = new HashMap<>();
        out.put("posA", data.posA.getWorld().getName() + "|" + data.posA.getX() + "|" + data.posA.getY() + "|" + data.posA.getZ());
        out.put("posB", data.posB.getWorld().getName() + "|" + data.posB.getX() + "|" + data.posB.getY() + "|" + data.posB.getZ());
        return out;
    }

    @Override
    public ConfigRegion deserializer(Object data) {

        ConfigRegion cakeRegion = new ConfigRegion();
        if (data == null) {
            cakeRegion.posA = new Location(Bukkit.getWorlds().get(0), 0, 50, 0);
            cakeRegion.posB = new Location(Bukkit.getWorlds().get(0), 0, 50, 0);
            return cakeRegion;
        }
        HashMap<String, Object> info = (HashMap<String, Object>) data;
        String locA = (String) info.get("posA");
        String locB = (String) info.get("posB");
        String[] partsA = locA.split("\\|");
        String[] partsB = locB.split("\\|");
        cakeRegion.posA = new Location(Bukkit.getWorld(partsA[0]), Float.parseFloat(partsA[1]), Float.parseFloat(partsA[2]), Float.parseFloat(partsA[3]));
        cakeRegion.posB = new Location(Bukkit.getWorld(partsB[0]), Float.parseFloat(partsB[1]), Float.parseFloat(partsB[2]), Float.parseFloat(partsB[3]));
        return cakeRegion;


    }

}
