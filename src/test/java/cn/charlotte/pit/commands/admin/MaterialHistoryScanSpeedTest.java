package cn.charlotte.pit.commands.admin;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class MaterialHistoryScanSpeedTest {
    @Test
    void unlimitedDefaultAddsNoSleepAndDefaultDatabaseBatchIs500() throws Exception {
        var speed = new MaterialHistoryScanSpeed(() -> { fail("unlimited mode does not need a clock"); return 0; },
                nanos -> fail("unlimited mode must not sleep"));
        assertEquals(0, speed.perSecond());
        assertEquals(500, speed.batchSize());
        for (int i = 0; i < 10_000; i++) speed.awaitPermit();
    }

    @Test
    void pacesOnePermitAtATimeWithoutBusyWaiting() throws Exception {
        AtomicLong now = new AtomicLong();
        List<Long> sleeps = new ArrayList<>();
        var speed = new MaterialHistoryScanSpeed(now::get, nanos -> { sleeps.add(nanos); now.addAndGet(nanos); });
        speed.perSecond(100);
        speed.awaitPermit();
        assertTrue(sleeps.isEmpty());
        speed.awaitPermit();
        speed.awaitPermit();
        assertEquals(20_000_000, now.get());
        assertEquals(List.of(10_000_000L, 10_000_000L), sleeps);
    }

    @Test
    void disablingLimitDuringSleepTakesEffectWithin50ms() throws Exception {
        AtomicLong now = new AtomicLong();
        MaterialHistoryScanSpeed[] holder = new MaterialHistoryScanSpeed[1];
        holder[0] = new MaterialHistoryScanSpeed(now::get, nanos -> { now.addAndGet(nanos); holder[0].perSecond(0); });
        holder[0].perSecond(1);
        holder[0].awaitPermit();
        holder[0].awaitPermit();
        assertEquals(50_000_000, now.get());
        assertEquals(0, holder[0].perSecond());
    }

    @Test
    void changingFromSlowToFastIsLiveAndDoesNotRetainTheOldDeadline() throws Exception {
        AtomicLong now = new AtomicLong();
        var speed = new MaterialHistoryScanSpeed(now::get, nanos -> now.addAndGet(nanos));
        speed.perSecond(1);
        speed.awaitPermit();
        speed.perSecond(500);
        speed.awaitPermit();
        assertEquals(0, now.get());
        speed.awaitPermit();
        assertEquals(2_000_000, now.get());
    }

    @Test
    void slowDatabaseWorkNeverCreatesCatchUpBursts() throws Exception {
        AtomicLong now = new AtomicLong();
        var speed = new MaterialHistoryScanSpeed(now::get, nanos -> now.addAndGet(nanos));
        speed.perSecond(10);
        speed.awaitPermit();
        now.set(5_000_000_000L);
        speed.awaitPermit();
        speed.awaitPermit();
        assertEquals(5_100_000_000L, now.get());
    }

    @Test
    void resetStartsANewRunWithoutOldPacingDebtAndRetainsSettings() throws Exception {
        AtomicLong now = new AtomicLong();
        var speed = new MaterialHistoryScanSpeed(now::get, nanos -> now.addAndGet(nanos));
        speed.perSecond(1); speed.batchSize(100);
        speed.awaitPermit();
        speed.reset();
        speed.awaitPermit();
        assertEquals(0, now.get());
        assertEquals(1, speed.perSecond());
        assertEquals(100, speed.batchSize());
    }

    @Test
    void invalidSettingsFailWithoutChangingExistingValues() {
        var speed = new MaterialHistoryScanSpeed();
        assertThrows(IllegalArgumentException.class, () -> speed.perSecond(-1));
        assertThrows(IllegalArgumentException.class, () -> speed.perSecond(1_000_001));
        assertThrows(IllegalArgumentException.class, () -> speed.batchSize(0));
        assertThrows(IllegalArgumentException.class, () -> speed.batchSize(1001));
        assertEquals(0, speed.perSecond());
        assertEquals(500, speed.batchSize());
    }

    @Test
    void interruptIsNotSwallowedEvenInUnlimitedMode() {
        var speed = new MaterialHistoryScanSpeed();
        Thread.currentThread().interrupt();
        try { assertThrows(InterruptedException.class, speed::awaitPermit); }
        finally { Thread.interrupted(); }
    }
}
