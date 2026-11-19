package cn.charlotte.pit.commands.admin;

import java.util.*;

/** Shared across the duplicate-report menus. A pending or unconfirmed write must never look successful. */
final class UuidReportBlacklistState {
    enum State { UNKNOWN, CLEAR, PENDING, BLACKLISTED, INVALID }
    enum Decision { STARTED, UNKNOWN, PENDING, ALREADY_BLACKLISTED, INVALID }
    record Write(Map<String, Object> setOnInsert, Map<String, Object> set) { }
    private record Cached(State state, long revision) { }
    private final LinkedHashMap<String, Cached> states = new LinkedHashMap<>(128, 0.75f, true);
    private final int capacity;
    private long revision;

    UuidReportBlacklistState() { this(4096); }
    UuidReportBlacklistState(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Blacklist state capacity must be positive");
        this.capacity = capacity;
    }

    static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        try { return UUID.fromString(trimmed).toString().equals(trimmed) ? trimmed : null; }
        catch (IllegalArgumentException exception) { return null; }
    }

    static Set<String> candidates(Collection<String> values) {
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String uuid = normalize(value);
            if (uuid != null) result.add(uuid);
        }
        return result;
    }

    synchronized State state(String value) {
        String uuid = normalize(value);
        Cached cached = uuid == null ? null : states.get(uuid);
        return uuid == null ? State.INVALID : cached == null ? State.UNKNOWN : cached.state();
    }

    synchronized long revision() { return revision; }

    synchronized void loaded(Collection<String> requested, Collection<String> active) {
        loaded(requested, active, revision);
    }

    synchronized void loaded(Collection<String> requested, Collection<String> active, long readRevision) {
        Set<String> blacklisted = candidates(active);
        for (String uuid : candidates(requested)) if (mayRefresh(uuid, readRevision))
            put(uuid, blacklisted.contains(uuid) ? State.BLACKLISTED : State.CLEAR, false);
    }

    synchronized void failed(Collection<String> requested) {
        failed(requested, revision);
    }

    synchronized void failed(Collection<String> requested, long readRevision) {
        for (String uuid : candidates(requested)) if (mayRefresh(uuid, readRevision)) put(uuid, State.UNKNOWN, false);
    }

    private boolean mayRefresh(String uuid, long readRevision) {
        Cached cached = states.get(uuid);
        return cached == null || (cached.state() != State.PENDING && cached.revision() <= readRevision);
    }

    synchronized Decision begin(String value) {
        String uuid = normalize(value);
        State state = state(value);
        return switch (state) {
            case INVALID -> Decision.INVALID;
            case UNKNOWN -> Decision.UNKNOWN;
            case PENDING -> Decision.PENDING;
            case BLACKLISTED -> Decision.ALREADY_BLACKLISTED;
            case CLEAR -> { put(uuid, State.PENDING, true); yield Decision.STARTED; }
        };
    }

    synchronized void finish(String value, boolean confirmed) {
        String uuid = normalize(value);
        if (uuid != null && state(uuid) == State.PENDING) put(uuid, confirmed ? State.BLACKLISTED : State.UNKNOWN, true);
    }

    private void put(String uuid, State state, boolean mutation) {
        Cached previous = states.get(uuid);
        long version = mutation ? ++revision : previous == null ? 0 : previous.revision();
        states.put(uuid, new Cached(state, version));
        var iterator = states.entrySet().iterator();
        while (states.size() > capacity && iterator.hasNext())
            if (iterator.next().getValue().state() != State.PENDING) iterator.remove();
    }

    String name(String uuid, String original) {
        return switch (state(uuid)) {
            case BLACKLISTED -> "&4[已拉黑] &c" + original;
            case PENDING -> "&6[写入中] &e" + original;
            case UNKNOWN -> "&e[状态未知] &f" + original;
            default -> original;
        };
    }

    List<String> lore(String uuid, List<String> original) {
        List<String> result = new ArrayList<>();
        switch (state(uuid)) {
            case BLACKLISTED -> result.add("&4&l⚠ 已加入 UUID 回收黑名单");
            case PENDING -> result.add("&6正在写入 UUID 黑名单，请勿重复操作。");
            case UNKNOWN -> result.add("&e黑名单状态未知，请先点击下方刷新按钮后再操作。");
            case CLEAR -> result.add("&cShift + 右键：将该物品 UUID 加入回收黑名单");
            case INVALID -> result.add("&e该记录的 UUID 无效，不能加入黑名单。");
        }
        result.add("&c拉黑按 UUID 全局生效，影响所有持有者的同 UUID 副本。");
        result.add("&7加入后于持有者下次登录时按现有规则审计回收。");
        result.add("");
        result.addAll(original);
        return result;
    }

    static Map<String, Object> audit(String requestedUuid, String operatorUuid, String operatorName, long timestamp,
                                     String creationMode, String sourceReport, long generatedAt, Map<String, Object> context) {
        String uuid = normalize(requestedUuid);
        if (uuid == null) throw new IllegalArgumentException("无效的物品 UUID");
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("_id", uuid); entry.put("itemUuid", uuid); entry.put("active", true);
        entry.put("createdAt", timestamp); entry.put("createdByUuid", operatorUuid); entry.put("createdByName", operatorName);
        entry.put("creationMode", creationMode); entry.put("sourceReport", sourceReport);
        entry.put("sourceReportGeneratedAt", generatedAt);
        entry.put("sourceContext", new LinkedHashMap<>(context));
        return entry;
    }

    static Write write(Map<String, Object> audit) {
        Map<String, Object> insert = new LinkedHashMap<>(audit);
        insert.remove("active"); insert.remove("itemUuid");
        Map<String, Object> set = new LinkedHashMap<>();
        set.put("active", true); set.put("itemUuid", audit.get("itemUuid"));
        set.put("lastBlacklistedAt", audit.get("createdAt"));
        set.put("lastBlacklistedByUuid", audit.get("createdByUuid"));
        set.put("lastBlacklistedByName", audit.get("createdByName"));
        set.put("lastBlacklistSource", new LinkedHashMap<>(audit));
        return new Write(insert, set);
    }
}
