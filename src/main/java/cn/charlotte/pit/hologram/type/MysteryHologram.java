package cn.charlotte.pit.hologram.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.hologram.AbstractHologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class MysteryHologram extends AbstractHologram {

    @Override
    public String getInternalName() {
        return "mystery";
    }

    @Override
    public List<String> getText(Player player) {
        return List.of(
                "&5神秘造物台",
                "&7古怪的台子"
        );
    }

    @Override
    public boolean shouldLoop() {
        return false;
    }

    @Override
    public int loopTicks() {
        return 0;
    }

    @Override
    public double getHologramHighInterval() {
        return 0.4;
    }

    @Override
    public Location getLocation() {
        return ThePit.getInstance().getPitConfig().getHologramMystery();
    }
}
