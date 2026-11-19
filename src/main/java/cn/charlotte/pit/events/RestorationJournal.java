package cn.charlotte.pit.events;

import java.util.ArrayList;
import java.util.List;

/** Single-owner reverse-order undo. A failed newer edit must not be overtaken by older overlapping edits. */
public final class RestorationJournal<T> {
    @FunctionalInterface public interface Restore<T> { void apply(T entry) throws Exception; }
    private final List<T> entries = new ArrayList<>();
    public void add(T entry) { entries.add(entry); }
    public boolean isEmpty() { return entries.isEmpty(); }
    public int size() { return entries.size(); }
    public void restore(Restore<T> restore) throws Exception {
        while (!entries.isEmpty()) {
            int last = entries.size() - 1;
            restore.apply(entries.get(last));
            entries.remove(last); // Failure retains this entry AND all older entries for an ordered retry.
        }
    }
}
