package cn.charlotte.pit.data.sub;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class MedalData {

    // tier medal -> medalName#medalLevel
    private Map<String, MedalStatus> medalStatus;

    public MedalData() {
        this.medalStatus = new HashMap<>();
    }

    @JsonIgnore
    private static String keyOf(String internal, int level) {
        return internal + "#" + level;
    }

    @JsonIgnore
    private static boolean isDefault(MedalStatus s) {
        return !s.isUnlocked() && s.getProgress() == 0 && s.getFinishedTime() == 0L;
    }

    /**
     * 只读视图：不存在则返回“默认快照”，不入 Map
     */
    @JsonIgnore
    public MedalStatus view(String medalInternal, int level) {
        MedalStatus s = medalStatus.get(keyOf(medalInternal, level));
        if (s != null) return s;
        MedalStatus snapshot = new MedalStatus();
        snapshot.setUnlocked(false);
        snapshot.setProgress(0);
        snapshot.setFinishedTime(0L);
        return snapshot;
    }

    /**
     * 写前保证存在：不存在则放入 Map 并返回
     */
    @JsonIgnore
    public MedalStatus ensure(String medalInternal, int level) {
        return medalStatus.computeIfAbsent(keyOf(medalInternal, level), __ -> {
            MedalStatus m = new MedalStatus();
            m.setUnlocked(false);
            m.setProgress(0);
            m.setFinishedTime(0L);
            return m;
        });
    }

    /**
     * 保存前清理仍为默认值的条目
     */
    @JsonIgnore
    public void compact() {
        medalStatus.entrySet().removeIf(e -> isDefault(e.getValue()));
    }

    /* ===== 原有 API 的“只读”版本，兼容旧调用 ===== */
    @JsonIgnore
    @Deprecated // 标记为废弃：请用 view/ensure 区分读写
    public MedalStatus getMedalStatus(String medalInternal, int level) {
        return view(medalInternal, level);
    }

    @JsonIgnore
    public Map<String, MedalStatus> getUnlockedMedals() {
        Map<String, MedalStatus> unlocked = new HashMap<>();
        for (Map.Entry<String, MedalStatus> e : medalStatus.entrySet()) {
            if (e.getValue().isUnlocked()) unlocked.put(e.getKey(), e.getValue());
        }
        return unlocked;
    }

    @JsonIgnore
    public int getMedalLevel(String medalInternal) {
        int level = 1;
        for (String key : medalStatus.keySet()) {
            if (key.startsWith(medalInternal + "#")) {
                MedalStatus s = medalStatus.get(key);
                if (s != null && s.isUnlocked()) {
                    int lv = Integer.parseInt(key.substring((medalInternal + "#").length()));
                    level = Math.max(level, lv);
                }
            }
        }
        return level;
    }

    @Data
    public static class MedalStatus {
        private boolean unlocked;
        private int progress;
        private long finishedTime;
        private int click;
    }
}
