package cn.charlotte.pit.events;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.function.operation.ChangeSetExecutor;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.world.World;

import java.util.concurrent.CompletableFuture;

/** Per-event map jobs. Immutable captured inputs only; no Bukkit/player collections in workers. */
public final class MajorEventMapEdits {
    @FunctionalInterface public interface Work { void run(Batch batch) throws Exception; }
    @FunctionalInterface public interface Edit { void run(EditSession session) throws Exception; }
    private record Backup(World world, ChangeSet history) { }
    private final RestorationJournal<Backup> backups = new RestorationJournal<>();
    private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);

    @FunctionalInterface public interface Job { void run() throws Exception; }
    public static CompletableFuture<Void> run(Job job) {
        return CompletableFuture.runAsync(() -> {
            try { job.run(); }
            catch (Exception ex) { throw new java.util.concurrent.CompletionException(ex); }
        }, task -> FaweAPI.getTaskManager().async(task));
    }

    public final class Batch {
        private Batch() { }
        public void edit(World world, Edit action) throws Exception {
            // Explicit history: temporary event structures must be restorable regardless of FAWE global defaults.
            try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(world).changeSet(false, null).build()) {
                backups.add(new Backup(world, session.getChangeSet())); // Retain even partially failed edits.
                action.run(session);
            }
        }
    }
    public CompletableFuture<Void> prepare(Work work) {
        if (!tail.isDone() || !backups.isEmpty()) return CompletableFuture.failedFuture(
                new IllegalStateException("Previous major-event map edit has not been restored"));
        tail = CompletableFuture.runAsync(() -> {
            try { work.run(new Batch()); }
            catch (Exception ex) { throw new java.util.concurrent.CompletionException(ex); }
        }, task -> FaweAPI.getTaskManager().async(task));
        return tail;
    }
    public CompletableFuture<Void> restore() {
        tail = tail.handle((ignored, failure) -> null).thenRunAsync(() -> {
            try {
                backups.restore(backup -> {
                    try (EditSession target = WorldEdit.getInstance().newEditSessionBuilder().world(backup.world()).changeSetNull().build()) {
                        // Keep the original history for ordered retries, even if the target flush fails.
                        target.setBlocks(backup.history(), ChangeSetExecutor.Type.UNDO);
                    }
                });
            } catch (Exception ex) { throw new java.util.concurrent.CompletionException(ex); }
        }, task -> FaweAPI.getTaskManager().async(task));
        return tail;
    }
}
