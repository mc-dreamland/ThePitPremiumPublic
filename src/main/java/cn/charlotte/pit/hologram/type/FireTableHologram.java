package cn.charlotte.pit.hologram.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.hologram.AbstractHologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class FireTableHologram extends AbstractHologram {
    @Override
    public String getInternalName() {
        return "FireTable";
    }

    @Override
    public List<String> getText(Player player) {
        return Arrays.asList("&e&l熔炉", "&7烘焙出&e美味的面包");
    }

    @Override
    public boolean shouldLoop() {
        return true;
    }

    @Override
    public int loopTicks() {
        return 20;
    }

    @Override
    public double getHologramHighInterval() {
        return 0.40;
    }

    @Override
    public Location getLocation() {
        return ThePit.getInstance().getPitConfig().getFireTableLocation();
    }
}
