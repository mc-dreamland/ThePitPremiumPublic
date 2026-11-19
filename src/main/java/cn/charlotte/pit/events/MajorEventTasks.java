package cn.charlotte.pit.events;

import cn.charlotte.pit.ThePit;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/** Tasks belong to one run, not to a reusable event instance. */
public final class MajorEventTasks {
    private MajorEventTasks() { }
    public static BukkitTask track(IEpicEvent event, BukkitTask task) {
        ThePit.getInstance().getEventFactory().trackMajorTask(event, task);
        return task;
    }
    public static BukkitTask later(IEpicEvent event, Runnable action, long ticks) {
        EventFactory factory = ThePit.getInstance().getEventFactory();
        long generation = factory.majorGeneration();
        return track(event, Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
            if (factory.isMajorRun(event, generation)) action.run();
        }, ticks));
    }
}
