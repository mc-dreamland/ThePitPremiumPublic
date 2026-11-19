package cn.charlotte.pit.commands.admin;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;

import static cn.charlotte.pit.commands.admin.UuidReportBlacklistState.Decision.*;
import static cn.charlotte.pit.commands.admin.UuidReportBlacklistState.State;
import static org.junit.jupiter.api.Assertions.*;

class UuidReportBlacklistStateTest {
    private static final String A = "bb7fbf1d-c915-493c-abc7-297e478b47ed";
    private static final String B = "5804d211-9d3d-4e19-8fe0-452d5a85bb0e";
    private static final String C = "038beb72-8a50-493a-aa8e-dea40ed00e73";

    @Test void validUuidKeysAreCanonicalAndInvalidOrAbbreviatedUuidsCannotBeBlacklisted() {
        assertEquals(A, UuidReportBlacklistState.normalize(" " + A.toUpperCase(Locale.ROOT) + " "));
        for (String value : Arrays.asList(null, "", "not-a-uuid", "1-1-1-1-1", "../../report.json", A.replace("-", "")))
            assertNull(UuidReportBlacklistState.normalize(value));
        assertEquals(Set.of(A, B), UuidReportBlacklistState.candidates(Arrays.asList(null, A, A.toUpperCase(Locale.ROOT), B, "bad")));
        var state = new UuidReportBlacklistState();
        assertEquals(INVALID, state.begin("not-a-uuid"));
        assertEquals(State.INVALID, state.state("not-a-uuid"));
    }

    @Test void onlyAConfirmedWriteShowsBlacklistedAndAnUnloadedStateCannotStartAWrite() {
        var state = new UuidReportBlacklistState();
        assertEquals(UNKNOWN, state.begin(A));
        state.loaded(List.of(A), List.of());
        assertEquals(State.CLEAR, state.state(A));
        assertEquals(STARTED, state.begin(A));
        assertEquals(State.PENDING, state.state(A));
        assertFalse(state.name(A, "装备").contains("已拉黑"));
        state.finish(A, true);
        assertEquals(State.BLACKLISTED, state.state(A));
        assertTrue(state.name(A, "装备").contains("已拉黑"));
        assertEquals(ALREADY_BLACKLISTED, state.begin(A));
    }

    @Test void caseInsensitiveKeySharesPendingAndConfirmedStateAcrossAllReportMenus() {
        var state = new UuidReportBlacklistState();
        state.loaded(List.of(A), List.of());
        assertEquals(STARTED, state.begin(A.toUpperCase(Locale.ROOT)));
        assertEquals(PENDING, state.begin(A));
        state.finish(" " + A + " ", true);
        assertEquals(State.BLACKLISTED, state.state(A.toUpperCase(Locale.ROOT)));
        assertEquals(ALREADY_BLACKLISTED, state.begin(A));
    }

    @Test void failedOrUnconfirmedWritesBecomeUnknownNotSuccessfulAndNeedAFreshDatabaseRead() {
        var state = new UuidReportBlacklistState();
        state.loaded(List.of(A), List.of());
        state.begin(A);
        state.finish(A, false);
        assertEquals(State.UNKNOWN, state.state(A));
        assertFalse(state.name(A, "装备").contains("已拉黑"));
        assertEquals(UNKNOWN, state.begin(A));
        // An uncertain network failure may have committed: use actual state on the next read.
        state.loaded(List.of(A), List.of(A));
        assertEquals(State.BLACKLISTED, state.state(A));
    }

    @Test void refreshCannotOverwriteAnInFlightWriteOrAConfirmedWriteWithAnOlderRead() {
        var state = new UuidReportBlacklistState();
        state.loaded(List.of(A), List.of());
        long oldRead = state.revision();
        state.begin(A);
        state.loaded(List.of(A), List.of(), oldRead);
        state.failed(List.of(A), oldRead);
        assertEquals(State.PENDING, state.state(A));
        state.finish(A, true);
        state.loaded(List.of(A), List.of(), oldRead);
        state.failed(List.of(A), oldRead);
        assertEquals(State.BLACKLISTED, state.state(A));
        // A genuinely newer read can reflect an administrator disabling the rule elsewhere.
        state.loaded(List.of(A), List.of(), state.revision());
        assertEquals(State.CLEAR, state.state(A));
    }

    @Test void blacklistQueriesUpdateOnlyRequestedCandidatesAndFailedReadsAreNotTreatedAsClear() {
        var state = new UuidReportBlacklistState();
        state.loaded(List.of(A, B), List.of(A));
        assertEquals(State.BLACKLISTED, state.state(A));
        assertEquals(State.CLEAR, state.state(B));
        state.failed(List.of(B));
        assertEquals(State.UNKNOWN, state.state(B));
        assertEquals(State.BLACKLISTED, state.state(A));
        assertEquals(UNKNOWN, state.begin(B));
        state.loaded(List.of(B), List.of());
        assertEquals(STARTED, state.begin(B));
    }

    @Test void concurrentShiftRightRequestsForTheSameUuidOnlyStartOneWrite() throws Exception {
        var state = new UuidReportBlacklistState();
        state.loaded(List.of(A), List.of());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<UuidReportBlacklistState.Decision>> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
            for (int i = 0; i < 12; i++) results.add(executor.submit(() -> { start.await(); return state.begin(A); }));
            start.countDown();
            List<UuidReportBlacklistState.Decision> decisions = new ArrayList<>();
            for (var result : results) decisions.add(result.get(5, TimeUnit.SECONDS));
            assertEquals(1, decisions.stream().filter(value -> value == STARTED).count());
            assertEquals(11, decisions.stream().filter(value -> value == PENDING).count());
        }
        state.finish(A, true);
        assertEquals(ALREADY_BLACKLISTED, state.begin(A));
    }

    @Test void cacheIsBoundedButPendingRequestsAreNeverEvicted() {
        var state = new UuidReportBlacklistState(2);
        state.loaded(List.of(A), List.of());
        state.begin(A);
        state.loaded(List.of(B), List.of(B));
        state.loaded(List.of(C), List.of());
        assertEquals(State.PENDING, state.state(A));
        assertEquals(State.UNKNOWN, state.state(B));
        assertEquals(State.CLEAR, state.state(C));
        state.finish(A, true);
        assertEquals(State.BLACKLISTED, state.state(A));
        assertThrows(IllegalArgumentException.class, () -> new UuidReportBlacklistState(0));
    }

    @Test void markersAndInstructionsPreserveEveryOriginalLoreLineWithoutMutatingTheSource() {
        var state = new UuidReportBlacklistState();
        List<String> original = List.of("玩家: Ledigrant", "", "§b灵魂汲取 III", "生命: 19/19");
        state.loaded(List.of(A), List.of());
        assertTrue(state.lore(A, original).getFirst().contains("Shift + 右键"));
        state.begin(A);
        assertTrue(state.lore(A, original).getFirst().contains("正在写入"));
        state.finish(A, true);
        List<String> decorated = state.lore(A, original);
        assertTrue(decorated.getFirst().contains("已加入 UUID 回收黑名单"));
        assertTrue(decorated.stream().anyMatch(value -> value.contains("所有持有者")));
        assertEquals(original, decorated.subList(decorated.size() - original.size(), decorated.size()));
        assertEquals(4, original.size());
    }

    @Test void auditCapturesOperatorReportAndClickedItemEvidenceForBothReportTypes() {
        Map<String, Object> context = new LinkedHashMap<>(Map.of("backupDatabaseId", "6a1c06cd6609e3695970158d",
                "playerName", "Ledigrant", "location", "warehouse[2][7]", "lore", List.of("生命: 19/19", "完整 Lore")));
        for (String mode : List.of("UUID_DUPLICATE_VIEW", "UUID_DUPLICATE_HISTORY_VIEW")) {
            Map<String, Object> audit = UuidReportBlacklistState.audit(A.toUpperCase(Locale.ROOT), "operator-uuid", "管理员", 123,
                    mode, "logs/scan/uuid/report.json", 100, context);
            assertEquals(A, audit.get("_id")); assertEquals(A, audit.get("itemUuid"));
            assertEquals(true, audit.get("active")); assertEquals(123L, audit.get("createdAt"));
            assertEquals("operator-uuid", audit.get("createdByUuid")); assertEquals("管理员", audit.get("createdByName"));
            assertEquals(mode, audit.get("creationMode")); assertEquals(100L, audit.get("sourceReportGeneratedAt"));
            assertEquals(context, audit.get("sourceContext"));
            assertNotSame(context, audit.get("sourceContext"));
        }
        assertThrows(IllegalArgumentException.class, () -> UuidReportBlacklistState.audit("bad", "operator", "管理员", 1,
                "UUID_DUPLICATE_VIEW", "report", 1, context));
    }

    @Test void writesAreIdempotentUpsertsReactivatingInactiveRulesWithoutOverwritingTheirOriginalCreationAudit() {
        Map<String, Object> audit = UuidReportBlacklistState.audit(A, "operator", "管理员", 123,
                "UUID_DUPLICATE_HISTORY_VIEW", "report.json", 100, Map.of("historyStatus", "CROSS_PLAYER_CANDIDATE_ONLY"));
        var write = UuidReportBlacklistState.write(audit);
        assertEquals(A, write.setOnInsert().get("_id"));
        assertEquals(123L, write.setOnInsert().get("createdAt"));
        assertFalse(write.setOnInsert().containsKey("active"));
        assertFalse(write.setOnInsert().containsKey("itemUuid"));
        assertEquals(true, write.set().get("active"));
        assertEquals(A, write.set().get("itemUuid"));
        assertFalse(write.set().containsKey("createdAt"));
        assertFalse(write.set().containsKey("createdByUuid"));
        assertEquals(audit, write.set().get("lastBlacklistSource"));
        assertTrue(Collections.disjoint(write.set().keySet(), write.setOnInsert().keySet()));
        assertEquals(true, audit.get("active")); // Building the update did not mutate the audit.
    }

    @Test void repeatedCompletionCallbacksCannotUndoASuccessfulWrite() {
        var state = new UuidReportBlacklistState();
        state.loaded(List.of(A), List.of());
        state.begin(A);
        state.finish(A, true);
        state.finish(A, false);
        assertEquals(State.BLACKLISTED, state.state(A));
        state.finish("bad", true);
        assertEquals(State.INVALID, state.state("bad"));
    }
}
