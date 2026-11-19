package cn.charlotte.pit.events;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Serialized lifecycle; the supplied executor owns all state and gameplay callbacks. */
public final class MajorEventLifecycle<E> {
    public enum Phase { IDLE, PREPARING, RUNNING, RESTORING, FAILED }
    public interface Hooks<E> {
        CompletableFuture<Void> prepare(E event);
        void start(E event);
        void stop(E event, boolean wasStarted, boolean shutdown);
        CompletableFuture<Void> restore(E event);
        void error(E event, String phase, Throwable error);
    }
    private final Executor main;
    private final Hooks<E> hooks;
    private Phase phase = Phase.IDLE;
    private E event;
    private long generation;
    private boolean cancelled, started, closed, cleanupFailed;

    public MajorEventLifecycle(Executor main, Hooks<E> hooks) {
        this.main = Objects.requireNonNull(main);
        this.hooks = Objects.requireNonNull(hooks);
    }
    public Phase phase() { return phase; }
    public E event() { return event; }
    public long generation() { return generation; }
    public boolean busy() { return phase != Phase.IDLE; }
    public boolean running(E owner, long epoch) {
        return !closed && phase == Phase.RUNNING && event == owner && generation == epoch;
    }
    public boolean begin(E next) {
        if (closed || busy()) return false;
        event = Objects.requireNonNull(next);
        phase = Phase.PREPARING;
        cancelled = started = false;
        cleanupFailed = false;
        long epoch = ++generation;
        CompletableFuture<Void> preparation;
        try { preparation = Objects.requireNonNull(hooks.prepare(next)); }
        catch (Throwable ex) { preparation = CompletableFuture.failedFuture(ex); }
        preparation.whenComplete((ignored, failure) -> main.execute(() -> {
            if (event != next || epoch != generation) return;
            if (failure != null) hooks.error(next, "prepare", failure);
            if (failure != null || cancelled || closed) { stopAndRestore(false); return; }
            phase = Phase.RUNNING;
            started = true; // A partially failing start still needs runtime cleanup, never normal rewards.
            try { hooks.start(next); }
            catch (Throwable ex) { hooks.error(next, "start", ex); stopAndRestore(false); }
        }));
        return true;
    }
    public void cancel() {
        if (!busy()) return;
        cancelled = true;
        if (phase == Phase.PREPARING || phase == Phase.RESTORING) return;
        if (phase == Phase.RUNNING) stopAndRestore(!closed);
        else if (phase == Phase.FAILED) {
            if (cleanupFailed) stopAndRestore(false);
            else restoreOnly(); // Never award rewards twice when retrying restoration.
        }
    }
    private void stopAndRestore(boolean normalEnd) {
        phase = Phase.RESTORING;
        cleanupFailed = false;
        try { hooks.stop(event, normalEnd && started, closed); }
        catch (Throwable ex) { cleanupFailed = true; hooks.error(event, "stop", ex); }
        finally { restoreOnly(); }
    }
    private void restoreOnly() {
        phase = Phase.RESTORING;
        E owner = event;
        long epoch = generation;
        CompletableFuture<Void> restoration;
        try { restoration = Objects.requireNonNull(hooks.restore(owner)); }
        catch (Throwable ex) { restoration = CompletableFuture.failedFuture(ex); }
        restoration.whenComplete((ignored, failure) -> main.execute(() -> {
            if (event != owner || epoch != generation) return;
            if (failure != null || cleanupFailed) {
                phase = Phase.FAILED; // Fail closed: do not start another event over a partially restored map.
                if (failure != null) hooks.error(owner, "restore", failure);
            } else {
                phase = Phase.IDLE;
                event = null;
                started = false;
            }
        }));
    }
    public void close() { closed = true; cancel(); }
}
