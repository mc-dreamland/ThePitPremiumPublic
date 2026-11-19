package cn.charlotte.pit.events;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.nametag.hiding.NameHidingManager;
import cn.charlotte.pit.util.chat.CC;
import org.jetbrains.annotations.NotNull;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.time.TimeUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/9 12:59
 */
@Slf4j
public class EventFactory {
    public final HashMap<Class<?>, IEpicEvent> allEpicEvent;
    public final HashMap<Class<?>, INormalEvent> allNormalEvent;
    @Getter
    private final List<INormalEvent> normalEvents;
    @Getter
    private final List<IEpicEvent> epicEvents;
    @Getter
    private INormalEvent activeNormalEvent;
    @Getter
    private IEpicEvent activeEpicEvent;
    @Setter
    @Getter
    private Cooldown normalEnd;
    
    @Getter
    private long lastNormalEvent;
    
    @Getter
    private IEpicEvent nextEpicEvent;
    @Getter
    private Cooldown nextEpicEventTimer;

    private final MajorEventLifecycle<IEpicEvent> major;
    private final List<BukkitTask> majorTasks = new ArrayList<>();
    private BukkitTask majorPreview;
    private boolean closed;
    
    public EventFactory() {
        this.normalEvents = new ArrayList<>();
        this.epicEvents = new ArrayList<>();
        this.allEpicEvent = new HashMap<>();
        this.allNormalEvent = new HashMap<>();
        this.normalEnd = new Cooldown(0);
        this.major = new MajorEventLifecycle<>(action -> {
            if (Bukkit.isPrimaryThread() || !ThePit.getInstance().isEnabled()) action.run();
            else Bukkit.getScheduler().runTask(ThePit.getInstance(), action);
        }, new MajorEventLifecycle.Hooks<>() {
            @Override public java.util.concurrent.CompletableFuture<Void> prepare(IEpicEvent event) {
                NameHidingManager.suspendForEvent();
                PitBotManager bots = PitBotManager.getInstance();
                if (bots != null) bots.onMajorEventStarted();
                log.info("大型事件 {}：开始准备地图", event.getEventInternalName());
                return event.prepare();
            }
            @Override public void start(IEpicEvent event) {
                activeEpicEvent = event;
                normalEnd = new Cooldown(5, TimeUnit.MINUTES);
                event.onActive();
                log.info("大型事件 {}：准备完成，正式开始", event.getEventInternalName());
            }
            @Override public void stop(IEpicEvent event, boolean wasStarted, boolean shutdown) {
                activeEpicEvent = null;
                majorTasks.forEach(BukkitTask::cancel);
                majorTasks.clear();
                // A cancelled preparation has never mutated player state. Shutdown completion may run off-thread.
                if (shutdown && !Bukkit.isPrimaryThread()) return;
                try {
                    if (wasStarted) {
                        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1, .72F));
                        event.onInactive();
                    } else event.onAbort();
                } catch (Throwable ex) {
                    try { event.onAbort(); } catch (Throwable cleanup) { ex.addSuppressed(cleanup); }
                    throw ex;
                } finally {
                    ThePit.getInstance().getBossBar().getBossBar().setTitle("");
                }
            }
            @Override public java.util.concurrent.CompletableFuture<Void> restore(IEpicEvent event) {
                log.info("大型事件 {}：开始恢复地图，完成前禁止下一场大型事件", event.getEventInternalName());
                return event.restore().whenComplete((ignored, error) -> {
                    if (error == null) log.info("大型事件 {}：地图恢复完成", event.getEventInternalName());
                });
            }
            @Override public void error(IEpicEvent event, String phase, Throwable error) {
                log.error("大型事件 " + event.getEventInternalName() + " 阶段 " + phase + " 失败"
                        + (("restore".equals(phase) || "stop".equals(phase)) ? "；已阻止后续大型事件，使用 /startevent cancel 重试清理/恢复" : ""), error);
            }
        });
    }
    
    public void pushEvent(IEpicEvent event) {
        pushEvent(event, false);
    }
    
    public void pushEvent(IEpicEvent event, boolean force) {
        pushEvent(event, force, -1);
    }
    
    public void pushEvent(IEpicEvent event, boolean force, int waitToStart) {
        if (force) {
            readyEpicEvent(event, waitToStart);
        } else if (ThePit.isDEBUG_SERVER() || Bukkit.getOnlinePlayers().size() >= event.requireOnline() && !CombatListener.isNight()) {
            readyEpicEvent(event, waitToStart);
        }
    }
    
    public void pushEvent(INormalEvent event) {
        pushEvent(event, false);
    }
    
    /**
     *
     * @param event 欲执行事件
     * @param force 是否强制执行事件
     */
    public void pushEvent(INormalEvent event, boolean force) {
        if (force) {
            activeEvent(event);
        } else if (ThePit.isDEBUG_SERVER() || Bukkit.getOnlinePlayers().size() >= event.requireOnline() && !CombatListener.isNight()) {
            activeEvent(event);
        }
    }
    
    @SneakyThrows
    public void init(List<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            if (IEpicEvent.class.isAssignableFrom(clazz)) {
                try {
                    IEpicEvent e = (IEpicEvent) clazz.newInstance();
                    this.epicEvents.add(e);
                    this.allEpicEvent.put(clazz, e);
                } catch (Throwable e) {
                    log.error("", e);
                }
            }
            if (INormalEvent.class.isAssignableFrom(clazz)) {
                try {
                    INormalEvent e = (INormalEvent) clazz.newInstance();
                    this.normalEvents.add(e);
                    this.allNormalEvent.put(clazz, e);
                } catch (Throwable e) {
                    log.error("", e);
                }
            }
        }
        
        Bukkit.getScheduler()
                .runTaskTimer(ThePit.getInstance(), new EventTimer(), 20, 20);
        
    }
    
    public String getActiveEpicEventName() {
        if (activeEpicEvent == null) return null;
        return activeEpicEvent.getEventInternalName();
    }
    
    public String getActiveNormalEventName() {
        if (activeNormalEvent == null) return null;
        return activeNormalEvent.getEventInternalName();
    }
    
    /**
     * 启用事件
     */
    public void activeEvent(INormalEvent event) {
        this.lastNormalEvent = System.currentTimeMillis();
        this.activeNormalEvent = event;
        NameHidingManager.suspendForEvent();
        this.normalEnd = new Cooldown(event.duration(), TimeUnit.MINUTES);
        event.onActive();
        
        new BukkitRunnable() {
            int tick = 0;
            
            @Override
            public void run() {
                if (!event.equals(activeNormalEvent)) {
                    ThePit.getInstance()
                            .getBossBar()
                            .getBossBar()
                            .setTitle("");
                    cancel();
                    return;
                }
                
                if (normalEnd.hasExpired()) {
                    ThePit.getInstance()
                            .getBossBar()
                            .getBossBar()
                            .setTitle("");
                    cancel();
                    return;
                }
                
                if (tick >= 15) tick = 0;
                
                String start = tick % 2 == 0 ? "&a&l小型事件! " : "&2&l小型事件! ";
                
                String title = CC.translate(String.format("%s&6&l%s &7将在 &e%s &7后结束!",
                        start, event.getEventName(), TimeUtil.millisToTimer(normalEnd.getRemaining())));
                
                ThePit.getInstance()
                        .getBossBar()
                        .getBossBar()
                        .setTitle(title);
                ThePit.getInstance()
                        .getBossBar()
                        .getBossBar()
                        .setProgress(normalEnd.getRemaining() / (5 * 1000 * 60d));
                
                tick++;
            }
        }.runTaskTimer(ThePit.getInstance(), 10, 10);
    }
    
    public void inactiveEvent(@NotNull INormalEvent event) {
        if (activeNormalEvent != event) return;

        lastNormalEvent = System.currentTimeMillis();
        this.activeNormalEvent = null;
        event.onInactive();
    }
    
    public void readyEpicEvent(IEpicEvent event, int waitSecondsToStart) {
        if (!Bukkit.isPrimaryThread()) { Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> readyEpicEvent(event, waitSecondsToStart)); return; }
        if (closed || major.busy()) {
            log.warn("大型事件正在准备/运行/恢复，拒绝覆盖为 {}", event.getEventInternalName());
            return;
        }
        if (majorPreview != null) majorPreview.cancel();
        this.nextEpicEvent = event;
        this.nextEpicEventTimer = new Cooldown(waitSecondsToStart == -1 ? 5 * 60 : waitSecondsToStart, TimeUnit.SECONDS);
        final Cooldown announcedTimer = this.nextEpicEventTimer;
        
        CC.boardCast(MessageType.EVENT, Component.text()
                .append(Component.text(String.format("§e大型天坑事件 §5%s §7即将开始 §e[查看简介]", event.getEventName().replace("&", "§")))
                        .hoverEvent(HoverEvent.showText(Component.text(CC.translate(String.join("\n", event.getLore()))))))
                .build());
        try { event.onPreActive(); }
        catch (RuntimeException ex) {
            nextEpicEvent = null; nextEpicEventTimer = null;
            log.error("大型事件预告准备失败: " + event.getEventInternalName(), ex);
            return;
        }
        
        majorPreview = new BukkitRunnable() {
            int tick = 0;
            
            @Override
            public void run() {
                if (closed || nextEpicEvent != event || nextEpicEventTimer != announcedTimer) { cancel(); return; }
                if (nextEpicEventTimer.hasExpired()) {
                    ThePit.getInstance()
                            .getBossBar()
                            .getBossBar()
                            .setTitle("");
                    cancel();
                    activeEvent(event);
                    return;
                }
                
                if (tick >= 15) tick = 0;
                
                String start = tick % 2 == 0 ? "&5&l大型事件! " : "&d&l大型事件! ";
                
                String title = CC.translate(String.format("%s&6&l%s &7将在 &e%s &7后开始!",
                        start, event.getEventName(), TimeUtil.millisToTimer(nextEpicEventTimer.getRemaining())));
                
                ThePit.getInstance()
                        .getBossBar()
                        .getBossBar()
                        .setTitle(title);
                ThePit.getInstance()
                        .getBossBar()
                        .getBossBar()
                        .setProgress(nextEpicEventTimer.getRemaining() / (5 * 1000 * 60d));
                
                tick++;
            }
        }.runTaskTimer(ThePit.getInstance(), 10, 10);
    }
    
    public void activeEvent(IEpicEvent event) {
        if (!Bukkit.isPrimaryThread()) { Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> activeEvent(event)); return; }
        if (closed || major.busy()) {
            log.warn("大型事件仍处于 {}，不能启动 {}", major.phase(), event.getEventInternalName());
            return;
        }
        if (majorPreview != null) majorPreview.cancel();
        majorPreview = null;
        this.nextEpicEvent = null;
        major.begin(event);
    }
    
    public void inactiveEvent(@NotNull IEpicEvent event) {
        if (!Bukkit.isPrimaryThread()) { Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> inactiveEvent(event)); return; }
        if (major.event() == event) major.cancel();
    }

    public boolean isMajorEventBusy() { return major.busy(); }
    public long majorGeneration() { return major.generation(); }
    public boolean isMajorRun(IEpicEvent event, long generation) { return major.running(event, generation); }
    public void trackMajorTask(IEpicEvent event, BukkitTask task) {
        if (major.running(event, major.generation())) majorTasks.add(task);
        else task.cancel();
    }
    public void cancelMajorEvent() {
        if (!Bukkit.isPrimaryThread()) { Bukkit.getScheduler().runTask(ThePit.getInstance(), this::cancelMajorEvent); return; }
        if (majorPreview != null) majorPreview.cancel();
        majorPreview = null;
        nextEpicEvent = null;
        nextEpicEventTimer = null;
        major.cancel();
    }
    public void close() {
        closed = true;
        if (majorPreview != null) majorPreview.cancel();
        majorPreview = null;
        nextEpicEvent = null;
        nextEpicEventTimer = null;
        major.close();
    }
    
}
