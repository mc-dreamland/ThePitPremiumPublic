package cn.charlotte.pit.events;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

class MajorEventLifecycleTest {
    static class Fixture implements MajorEventLifecycle.Hooks<Object> {
        CompletableFuture<Void> preparation = new CompletableFuture<>();
        CompletableFuture<Void> restoration = new CompletableFuture<>();
        final List<String> calls = new ArrayList<>();
        boolean startFails, stopFails;
        final MajorEventLifecycle<Object> lifecycle;
        Fixture() { this(Runnable::run); }
        Fixture(Executor executor) { lifecycle = new MajorEventLifecycle<>(executor, this); }
        @Override public CompletableFuture<Void> prepare(Object event) { calls.add("prepare"); return preparation; }
        @Override public void start(Object event) {
            calls.add("start");
            if (startFails) throw new IllegalStateException("partial start");
        }
        @Override public void stop(Object event, boolean normalEnd, boolean shutdown) {
            calls.add(normalEnd ? "rewards" : "abort");
            if (stopFails) throw new IllegalStateException("runtime cleanup");
        }
        @Override public CompletableFuture<Void> restore(Object event) { calls.add("restore"); return restoration; }
        @Override public void error(Object event, String phase, Throwable error) { calls.add("error:" + phase); }
    }

    @Test void gameplayStartsOnlyAfterTheMapIsCompleteAndAnotherMajorCannotOverlap() {
        Fixture f = new Fixture(); Object event = new Object();
        assertTrue(f.lifecycle.begin(event));
        assertEquals(MajorEventLifecycle.Phase.PREPARING, f.lifecycle.phase());
        assertFalse(f.lifecycle.begin(new Object()));
        assertFalse(f.lifecycle.running(event, f.lifecycle.generation()));
        assertEquals(List.of("prepare"), f.calls);
        f.preparation.complete(null);
        assertTrue(f.lifecycle.running(event, f.lifecycle.generation()));
        assertEquals(List.of("prepare", "start"), f.calls);
    }

    @Test void cancellingDuringPreparationWaitsForTheWriteThenRollsBackWithoutStartingOrPaying() {
        Fixture f = new Fixture(); f.lifecycle.begin(new Object());
        f.lifecycle.cancel(); f.lifecycle.cancel();
        assertEquals(List.of("prepare"), f.calls);
        assertTrue(f.lifecycle.busy());
        f.preparation.complete(null);
        assertEquals(List.of("prepare", "abort", "restore"), f.calls);
        assertFalse(f.lifecycle.begin(new Object()));
        f.restoration.complete(null);
        assertFalse(f.lifecycle.busy());
    }

    @Test void aPartialPreparationFailureStillRestoresAndNeverAwardsRewards() {
        Fixture f = new Fixture(); f.lifecycle.begin(new Object());
        f.preparation.completeExceptionally(new IllegalStateException("partial paste"));
        assertEquals(List.of("prepare", "error:prepare", "abort", "restore"), f.calls);
        f.restoration.complete(null);
        assertEquals(MajorEventLifecycle.Phase.IDLE, f.lifecycle.phase());
    }

    @Test void aPartialGameplayStartUsesAbortNotNormalPayout() {
        Fixture f = new Fixture(); f.startFails = true; f.lifecycle.begin(new Object());
        f.preparation.complete(null);
        assertEquals(List.of("prepare", "start", "error:start", "abort", "restore"), f.calls);
        f.restoration.complete(null);
        assertFalse(f.lifecycle.busy());
    }

    @Test void stoppingIsIdempotentAndKeepsTheGateClosedUntilRestorationFinishes() {
        Fixture f = new Fixture(); Object event = new Object(); f.lifecycle.begin(event); f.preparation.complete(null);
        long epoch = f.lifecycle.generation();
        f.lifecycle.cancel(); f.lifecycle.cancel();
        assertEquals(MajorEventLifecycle.Phase.RESTORING, f.lifecycle.phase());
        assertFalse(f.lifecycle.running(event, epoch));
        assertEquals(List.of("prepare", "start", "rewards", "restore"), f.calls);
        assertFalse(f.lifecycle.begin(new Object()));
        f.restoration.complete(null);
        assertNull(f.lifecycle.event());
        assertFalse(f.lifecycle.busy());
    }

    @Test void evenARuntimeCleanupFailureCannotSkipMapRestoration() {
        Fixture f = new Fixture(); f.stopFails = true; f.lifecycle.begin(new Object()); f.preparation.complete(null);
        f.lifecycle.cancel();
        assertEquals(List.of("prepare", "start", "rewards", "error:stop", "restore"), f.calls);
        f.restoration.complete(null);
        assertEquals(MajorEventLifecycle.Phase.FAILED, f.lifecycle.phase());
        f.stopFails = false;
        f.lifecycle.cancel();
        assertEquals(1, f.calls.stream().filter("rewards"::equals).count());
        assertTrue(f.calls.contains("abort"));
        assertFalse(f.lifecycle.busy());
    }

    @Test void aFailedRestorationBlocksTheNextEventAndCanBeRetriedWithoutRepaying() {
        Fixture f = new Fixture(); f.lifecycle.begin(new Object()); f.preparation.complete(null); f.lifecycle.cancel();
        f.restoration.completeExceptionally(new IllegalStateException("target flush"));
        assertEquals(MajorEventLifecycle.Phase.FAILED, f.lifecycle.phase());
        assertFalse(f.lifecycle.begin(new Object()));
        f.restoration = new CompletableFuture<>();
        f.lifecycle.cancel();
        assertEquals(1, f.calls.stream().filter("rewards"::equals).count());
        assertEquals(2, f.calls.stream().filter("restore"::equals).count());
        f.restoration.complete(null);
        assertFalse(f.lifecycle.busy());
    }

    @Test void taskEpochDoesNotMatchANewRunOfTheSameReusableInstance() {
        Fixture f = new Fixture(); Object event = new Object(); f.lifecycle.begin(event); f.preparation.complete(null);
        long old = f.lifecycle.generation(); f.lifecycle.cancel(); f.restoration.complete(null);
        f.preparation = CompletableFuture.completedFuture(null); f.restoration = new CompletableFuture<>();
        assertTrue(f.lifecycle.begin(event));
        assertFalse(f.lifecycle.running(event, old));
        assertTrue(f.lifecycle.running(event, f.lifecycle.generation()));
    }

    @Test void shutdownCleansAnActiveEventWithoutPayingOrAllowingAnotherStart() {
        Fixture f = new Fixture(); f.lifecycle.begin(new Object()); f.preparation.complete(null); f.lifecycle.close();
        assertEquals(List.of("prepare", "start", "abort", "restore"), f.calls);
        f.restoration.complete(null);
        assertFalse(f.lifecycle.begin(new Object()));
    }

    @Test void shutdownDuringPreparationDoesNotRaceTheWriteWithRestoration() {
        Fixture f = new Fixture(); f.lifecycle.begin(new Object()); f.lifecycle.close();
        assertEquals(List.of("prepare"), f.calls);
        f.preparation.complete(null);
        assertEquals(List.of("prepare", "abort", "restore"), f.calls);
    }

    @Test void workerCompletionsOnlyRunGameplayOnTheSuppliedMainExecutor() {
        ArrayDeque<Runnable> main = new ArrayDeque<>(); Fixture f = new Fixture(main::add);
        f.lifecycle.begin(new Object()); f.preparation.complete(null);
        assertEquals(List.of("prepare"), f.calls);
        main.remove().run();
        assertEquals(List.of("prepare", "start"), f.calls);
        f.lifecycle.cancel(); f.restoration.complete(null);
        assertTrue(f.lifecycle.busy());
        main.remove().run();
        assertFalse(f.lifecycle.busy());
    }

    @Test void cancellationBeforeAnAlreadyCompletedPreparationCallbackCannotStartTheEvent() {
        ArrayDeque<Runnable> main = new ArrayDeque<>(); Fixture f = new Fixture(main::add);
        f.lifecycle.begin(new Object()); f.preparation.complete(null); f.lifecycle.cancel();
        main.remove().run();
        assertEquals(List.of("prepare", "abort", "restore"), f.calls);
    }
}
