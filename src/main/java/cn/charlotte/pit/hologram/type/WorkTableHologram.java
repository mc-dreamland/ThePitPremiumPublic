package cn.charlotte.pit.hologram.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.hologram.AbstractHologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class WorkTableHologram extends AbstractHologram {
    @Override
    public String getInternalName() {
        return "WorkTable";
    }

    @Override
    public List<String> getText(Player player) {
        return Arrays.asList("&e&l工作台", "&7用于制作&e干草块");
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
        return ThePit.getInstance().getPitConfig().getWorkTableLocation();
    }
}
