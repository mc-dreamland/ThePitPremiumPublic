package cn.charlotte.pit.events;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RestorationJournalTest {
    @Test void overlappingEditsAreRestoredInReverseOrder() throws Exception {
        var journal = new RestorationJournal<String>();
        journal.add("wall"); journal.add("door"); journal.add("portal");
        var restored = new ArrayList<String>(); journal.restore(restored::add);
        assertEquals(List.of("portal", "door", "wall"), restored);
        assertTrue(journal.isEmpty());
    }

    @Test void anUndoFailureCannotBeOvertakenByAnOlderOverlappingEdit() {
        var journal = new RestorationJournal<String>();
        journal.add("wall"); journal.add("door");
        var attempted = new ArrayList<String>();
        assertThrows(Exception.class, () -> journal.restore(entry -> {
            attempted.add(entry); throw new Exception("target close failed");
        }));
        assertEquals(List.of("door"), attempted);
        assertEquals(2, journal.size());
    }

    @Test void retryRetainsFailedAndOlderEditsWithoutReplayingAlreadyCompletedOnes() throws Exception {
        var journal = new RestorationJournal<String>();
        journal.add("wall"); journal.add("door"); journal.add("portal");
        var completed = new ArrayList<String>();
        assertThrows(Exception.class, () -> journal.restore(entry -> {
            if (entry.equals("door")) throw new Exception("partial failure");
            completed.add(entry);
        }));
        assertEquals(List.of("portal"), completed);
        journal.restore(completed::add);
        assertEquals(List.of("portal", "door", "wall"), completed);
        assertTrue(journal.isEmpty());
    }

    @Test void aSecondRestoreOfAnEmptyJournalDoesNothing() throws Exception {
        var journal = new RestorationJournal<String>(); journal.add("map");
        var calls = new ArrayList<String>(); journal.restore(calls::add); journal.restore(calls::add);
        assertEquals(List.of("map"), calls);
    }
}
