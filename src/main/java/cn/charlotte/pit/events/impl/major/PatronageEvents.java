package cn.charlotte.pit.events.impl.major;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitItemLiveDropEvent;
import cn.charlotte.pit.events.IEpicEvent;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.events.IScoreBoardInsert;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.time.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Araykal
 * @since 2025/2/26
 */
public class PatronageEvents implements INormalEvent, Listener, IScoreBoardInsert {
    private Cooldown timer;
    private BukkitRunnable runnable;

    @Override
    public String getEventInternalName() {
        return "patronage";
    }

    @Override
    public String getEventName() {
        return "庇护";
    }

    @Override
    public List<String> getLore() {
        return Arrays.asList(
                "&a&l庇护事件",
                "",
                "&e事件机制：",
                "&7 事件期间你的神话物品不会掉命"
        );
    }

    @Override
    public int requireOnline() {
        return CombatListener.isBedrockRoom() ? 20 : 50;
    }

    @EventHandler
    public void onItemLiveDrop(PitItemLiveDropEvent e) {
        e.setCancelled(true);
    }

    @Override
    public void onActive() {
        if (runnable != null) runnable.cancel();
        timer = new Cooldown(5, TimeUnit.MINUTES);
        runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (timer.hasExpired()) {
                    cancel();
                    if (PatronageEvents.this == ThePit.getInstance().getEventFactory().getActiveNormalEvent()) {
                        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> ThePit.getInstance()
                                .getEventFactory()
                                .inactiveEvent(PatronageEvents.this));
                    }
                }
            }
        };
        runnable.runTaskTimer(ThePit.getInstance(), 20, 10);

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = Util.getPitProfile(player);
            Integer current = profile.getEventData().getHistoricalevents().get(getEventName());
            if (current == null) {
                current = 0;
            }
            profile.getEventData().getHistoricalevents().put(getEventName(), current + 1);
        }

        CC.boardCast(MessageType.EVENT, "&a&l大型事件! &e&l庇护 &7事件开始!");
        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());
    }

    @Override
    public void onInactive() {
        HandlerList.unregisterAll(this);
        if (runnable != null) { runnable.cancel(); runnable = null; }
    }

    @Override
    public List<String> insert(Player player) {
        return Arrays.asList(
                "&f剩余时间: &a" + TimeUtil.millisToTimer(timer.getRemaining()),
                "&a不掉生命",
                "&3不掉羽毛"
        );
    }
}
