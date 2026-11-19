package cn.charlotte.pit;

import cn.charlotte.pit.impl.PitInternalImpl;
import org.bukkit.Bukkit;

public class PitMain {
    public static void start() {
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            ThePit.getInstance().info("Init Pit Hook!");
            ThePit.setApi(PitInternalImpl.INSTANCE);
            PitHook.init();
            PitInternalImpl.setLoaded(true);
        });
    }

}
