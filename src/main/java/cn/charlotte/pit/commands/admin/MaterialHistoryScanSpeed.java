package cn.charlotte.pit.commands.admin;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** One async worker, with a live rate ceiling. Zero never introduces an artificial delay. */
final class MaterialHistoryScanSpeed {
    interface Sleeper { void sleep(long nanos) throws InterruptedException; }
    private final LongSupplier clock;
    private final Sleeper sleeper;
    private volatile int perSecond;
    private volatile int batchSize = 500;
    private int previousRate = -1;
    private long nextPermit;

    MaterialHistoryScanSpeed() { this(System::nanoTime, TimeUnit.NANOSECONDS::sleep); }
    MaterialHistoryScanSpeed(LongSupplier clock, Sleeper sleeper) { this.clock = clock; this.sleeper = sleeper; }
    int perSecond() { return perSecond; }
    int batchSize() { return batchSize; }
    void perSecond(int value) {
        if (value < 0 || value > 1_000_000) throw new IllegalArgumentException("每秒份数需为 0～1000000；0 表示不限速。");
        perSecond = value;
    }
    void batchSize(int value) {
        if (value < 1 || value > 1000) throw new IllegalArgumentException("数据库每批份数需为 1～1000。");
        batchSize = value;
    }
    void reset() { previousRate = -1; nextPermit = 0; }

    void awaitPermit() throws InterruptedException {
        while (true) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException("materialhistory worker interrupted");
            int rate = perSecond;
            if (rate == 0) { previousRate = 0; return; }
            long now = clock.getAsLong();
            if (rate != previousRate) { previousRate = rate; nextPermit = now; }
            long remaining = nextPermit - now;
            if (remaining <= 0) {
                // Never accumulate a burst of permits while database I/O or decoding was slow.
                nextPermit = now + Math.max(1L, 1_000_000_000L / rate);
                return;
            }
            // Live changes take effect within 50 ms even when currently throttled to 1 backup/sec.
            sleeper.sleep(Math.min(remaining, 50_000_000L));
        }
    }
}
