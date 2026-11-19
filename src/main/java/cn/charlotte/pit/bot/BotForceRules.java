package cn.charlotte.pit.bot;

/** Pure limits shared by command admission and forced-bot cleanup. */
final class BotForceRules {
    static final int MAX_PLACEMENT_FAILURES = 16;
    private static final long COMBAT_GRACE_MILLIS = 10_000;

    private BotForceRules() { }

    static int accepted(int requested, int cap, int alive, int queued) {
        long available = (long) Math.max(0, cap) - Math.max(0, alive) - Math.max(0, queued);
        return (int) Math.max(0L, Math.min(Math.max(0, requested), available));
    }

    static boolean expired(long deadline, long lastHit, long now) {
        return deadline > 0 && now >= deadline && (lastHit == 0 || now - lastHit > COMBAT_GRACE_MILLIS);
    }
}
