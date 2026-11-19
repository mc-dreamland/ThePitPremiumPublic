package cn.charlotte.pit.hologram.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.hologram.AbstractHologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class EnchantForgeHologram extends AbstractHologram {
    @Override
    public String getInternalName() {
        return "EnchantForgeHologram";
    }

    @Override
    public List<String> getText(Player player) {
        return List.of(
                "&d&l神话锻造台", "&e&o神话物品再度复苏"
        );
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
        return ThePit.getInstance().getPitConfig().getEnchantForgeTableLocation();
    }
}
