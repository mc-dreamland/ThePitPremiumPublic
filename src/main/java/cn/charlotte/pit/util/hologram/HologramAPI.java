package cn.charlotte.pit.util.hologram;

import cn.charlotte.pit.ThePit;
import org.bukkit.Location;

import javax.annotation.Nonnull;

@SuppressWarnings("unchecked")
public abstract class HologramAPI {

//    protected static final List<Hologram> holograms = new ArrayList<>();

    /**
     * Creates a new {@link Hologram}
     *
     * @param loc  {@link Location} to spawn the hologram at
     * @param text Initial text content of the hologram
     * @return a new {@link Hologram}
     */
    public static Hologram createHologram(Location loc, String text) {
        Hologram hologram = ThePit.api.createHologram(loc, text);
//        holograms.add(hologram);
        return hologram;
    }

    /**
     * Removes a {@link Hologram}
     *
     * @param loc  {@link Location} of the hologram
     * @param text content of the hologram
     * @return <code>true</code> if a hologram was found and has been removed, <code>false</code> otherwise
     */
    public static boolean removeHologram(Location loc, String text) {
//        Hologram toRemove = null;
//        for (Hologram h : holograms) {
//            if (h.getLocation().equals(loc) && h.getText().equals(text)) {
//                toRemove = h;
//                break;
//            }
//        }
//        if (toRemove != null) {
//            return removeHologram(toRemove);
//        }
        return false;
    }

    /**
     * Removes a {@link Hologram}
     *
     * @param hologram {@link Hologram} to remove
     * @return <code>true</code> if the hologram has been removed
     */
    public static boolean removeHologram(@Nonnull Hologram hologram) {
//        if (hologram.isSpawned()) {
//            hologram.deSpawn();
//        }
//        return holograms.remove(hologram);
        return false;
    }

}
