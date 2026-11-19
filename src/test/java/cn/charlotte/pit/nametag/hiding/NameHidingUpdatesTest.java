package cn.charlotte.pit.nametag.hiding;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class NameHidingUpdatesTest {
    @Test void repeatedChangesStayOneEntryAndDoNotPostponeItForever() {
        var work = new NameHidingUpdates();
        UUID viewer = UUID.randomUUID(), target = UUID.randomUUID();
        work.mark(viewer, target, 10);
        for (int tick = 0; tick < 10; tick++) {
            assertNull(work.poll());
            for (int i = 0; i < 1000; i++) work.mark(viewer, target, 10);
            assertEquals(1, work.size());
            work.advance();
        }
        assertEquals(new NameHidingUpdates.Pair(viewer, target), work.poll());
        assertEquals(0, work.size());
        assertNull(work.poll());
    }
    @Test void delayedWorkDoesNotBlockOtherObserversAndSelfIsNeverQueued() {
        var work = new NameHidingUpdates();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), target = UUID.randomUUID();
        work.mark(first, target, 20);
        work.mark(second, target, 0);
        work.mark(first, first, 0);
        assertEquals(new NameHidingUpdates.Pair(second, target), work.poll());
        assertEquals(1, work.size());
        assertNull(work.poll());
        work.mark(first, target, 0);
        assertEquals(new NameHidingUpdates.Pair(first, target), work.poll());
    }
    @Test void disconnectClearsBothViewerAndTargetPairsAndLeavesOthersAlone() {
        var work = new NameHidingUpdates();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), third = UUID.randomUUID();
        work.mark(first, second, 0);
        work.mark(second, third, 0);
        work.mark(third, first, 0);
        work.remove(first);
        assertEquals(1, work.size());
        assertEquals(new NameHidingUpdates.Pair(second, third), work.poll());
        work.clear();
        assertEquals(0, work.size());
    }
    @Test void concurrentNetworkWritersNeverLoseQueueEntriesOrCreateDuplicates() throws Exception {
        var work = new NameHidingUpdates();
        UUID viewer = UUID.randomUUID();
        UUID[] targets = new UUID[150];
        for (int i = 0; i < targets.length; i++) targets[i] = UUID.randomUUID();
        try (var pool = Executors.newFixedThreadPool(4)) {
            var jobs = new java.util.ArrayList<Future<?>>();
            for (int thread = 0; thread < 4; thread++) jobs.add(pool.submit(() -> {
                for (int i = 0; i < 10000; i++) work.mark(viewer, targets[i % targets.length], 0);
            }));
            for (var job : jobs) job.get(10, TimeUnit.SECONDS);
        }
        assertEquals(150, work.size());
        var seen = new java.util.HashSet<NameHidingUpdates.Pair>();
        NameHidingUpdates.Pair pair;
        while ((pair = work.poll()) != null) assertTrue(seen.add(pair));
        assertEquals(150, seen.size());
        assertEquals(0, work.size());
    }
    @Test void aFull150PlayerBurstStaysBoundedAndCanBeDrainedInConfiguredBatches() {
        var work = new NameHidingUpdates();
        var players = java.util.stream.IntStream.range(0, 150).mapToObj(i -> UUID.randomUUID()).toList();
        for (UUID viewer : players) for (UUID target : players) work.mark(viewer, target, 0);
        assertEquals(150 * 149, work.size());
        for (int i = 0; i < 256; i++) assertNotNull(work.poll());
        assertEquals(150 * 149 - 256, work.size());
    }
}
