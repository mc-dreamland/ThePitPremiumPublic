package cn.charlotte.pit.nametag.hiding;

import java.util.UUID;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** Deduplicated, bounded by viewer/target pairs. Newer changes overwrite state, not queue entries. */
final class NameHidingUpdates {
    record Pair(UUID viewer, UUID target) { }
    private final Map<Pair, Long> due = new HashMap<>();
    private final ArrayDeque<Pair> order = new ArrayDeque<>();
    private long tick;

    synchronized void advance() { tick++; }
    synchronized void mark(UUID viewer, UUID target, int delay) {
        if (viewer.equals(target)) return;
        Pair pair = new Pair(viewer, target);
        long deadline = tick + Math.max(0, delay);
        due.compute(pair, (key, previous) -> {
            if (previous == null) order.add(key);
            return previous == null ? deadline : Math.min(previous, deadline);
        });
    }
    synchronized Pair poll() {
        int remaining = order.size();
        while (remaining-- > 0) {
            Pair pair = order.poll();
            if (pair == null) return null;
            Long deadline = due.get(pair);
            if (deadline == null) continue;
            if (deadline > tick) { order.add(pair); continue; }
            if (due.remove(pair, deadline)) return pair;
            order.add(pair);
        }
        return null;
    }
    synchronized int size() { return due.size(); }
    synchronized void remove(UUID player) {
        due.keySet().removeIf(pair -> pair.viewer.equals(player) || pair.target.equals(player));
        order.removeIf(pair -> pair.viewer.equals(player) || pair.target.equals(player));
    }
    synchronized void clear() { due.clear(); order.clear(); }
}
