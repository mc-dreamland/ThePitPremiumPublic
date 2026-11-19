package cn.charlotte.pit.activity;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.event.PitProfileLoadedEvent;
import cn.charlotte.pit.menu.Help.button.UpdateMenu;
import cn.charlotte.pit.parm.AutoRegister;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
@Getter
@Slf4j
@AutoRegister
public class ActivityFactory implements Listener {

    private ActivityData activityData = null;
    private ActivityType factoryType = null;
    private final BukkitRunnable endRunnable = new BukkitRunnable() {
        @Override
        public void run() {
            cancel();
            if (factoryType == null || factoryType == ActivityType.NULL) return;
            String typeName = factoryType.getClazz().getName();
            try {
                ((IActivity) factoryType.getClazz().getDeclaredConstructor().newInstance()).end(activityData);
                factoryType = ActivityType.NULL;
                if (activityData != null) activityData.clear();
                log.info("end {}", typeName);
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException |
                     NoSuchMethodException e) {
                log.error("", e);
            }
        }
    };

    public void load() {
        ThePit.getInstance().getLogger().info("load activityFactory");
        for (ActivityType type : ActivityType.values()) {
            if (type.getDecimal() == UpdateMenu.getVer()) {
                try {
                    IActivity event = (IActivity) type.getClazz().getDeclaredConstructor().newInstance();
                    event.load();
                    this.activityData = ActivityData.load(UpdateMenu.getVer());
                    this.factoryType = type;
                    if (System.currentTimeMillis() < this.factoryType.getEndTime()) {
                        log.info("Run {}", this.factoryType.getClass().getName());
                        long delay = this.factoryType.getEndTime() - System.currentTimeMillis();
                        endRunnable.runTaskLater(ThePit.getInstance(), delay / 50);
                        break;
                    }
                    String typeName = type.getClazz().getName();
                    event.end(this.activityData);
                    this.factoryType = null;
                    log.info("end {}", typeName);
                    break;
                } catch (Exception e) {
                    log.error("", e);
                }
            }
        }
        if (this.factoryType == null) {
            ThePit.getInstance().getLogger().info("no run activityFactory");
            this.factoryType = ActivityType.NULL;
        }
    }

    @EventHandler
    public void onLoad(PitProfileLoadedEvent event) {
        if (this.factoryType == null || this.factoryType == ActivityType.NULL) return;
        event.getPlayerProfile().getActivityPlayerData().purposeCleaning();
    }
}