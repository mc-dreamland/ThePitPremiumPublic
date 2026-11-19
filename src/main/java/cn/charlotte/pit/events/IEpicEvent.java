package cn.charlotte.pit.events;

import java.util.concurrent.CompletableFuture;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/9 13:10
 */
public interface IEpicEvent extends IEvent {
    /** Capture Bukkit/configuration state here on the main thread, before asynchronous map preparation. */
    default CompletableFuture<Void> prepare() { return CompletableFuture.completedFuture(null); }

    /** Restore maps without touching player/reward state; completion gates the next major event. */
    default CompletableFuture<Void> restore() { return CompletableFuture.completedFuture(null); }

    /** Failure/shutdown cleanup, deliberately without awarding normal end-of-event rewards. */
    default void onAbort() {
        if (this instanceof org.bukkit.event.Listener listener) org.bukkit.event.HandlerList.unregisterAll(listener);
    }
}
