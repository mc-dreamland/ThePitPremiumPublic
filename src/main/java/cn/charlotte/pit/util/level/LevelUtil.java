package cn.charlotte.pit.util.level;

import cn.charlotte.pit.util.chat.RomanUtil;

public class LevelUtil {
    // ===== 可调参数 =====
    private static final int MAX_LEVEL = 120;
    private static final int MAX_PRESTIGE = 100;

    // ===== 经验段表（按十级分段）=====
    // idx: 0:[0-9], 1:[10-19], ..., 10:[100-109], 11:[110+]
    private static final double[] EXP_TABLE = {
            15, 30, 50, 75, 125, 250, 600, 800, 900, 1000, 1200, 1500
    };

    // ===== 转生倍率表（0..100；>50 统一 1_000_000）=====
    private static final double[] BOOST = {
            // 0..50（与你原逻辑一致）
            1, 1.1, 1.2, 1.3, 1.4, 1.5, 1.75, 2, 2.5, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16,
            18, 20, 24, 28, 32, 36, 40, 45, 50, 75, 100, 101, 101, 101, 101, 101,
            200, 300, 400, 500, 750, 1000, 1250, 1500, 1750, 2000, 3000, 5000, 10000, 50000, 100000
    };

    // ===== 预计算缓存 =====
    // REQ_EXP[p][l]：在 prestige=p 下，从 l 升到 l+1 所需的经验（l ∈ [0..119]）
    private static final double[][] REQ_EXP = new double[MAX_PRESTIGE + 1][MAX_LEVEL];
    // CUM_EXP[p][l]：在 prestige=p 下，到达等级 l 所需的总经验（l ∈ [0..120]，CUM_EXP[p][0] = 0）
    private static final double[][] CUM_EXP = new double[MAX_PRESTIGE + 1][MAX_LEVEL + 1];
    private static final RangeColor[] PRESTIGE_COLORS = {
            new RangeColor(1, 4, "&9"),
            new RangeColor(5, 9, "&e"),
            new RangeColor(10, 14, "&6"),
            new RangeColor(15, 19, "&c"),
            new RangeColor(20, 24, "&5"),
            new RangeColor(25, 29, "&d"),
            new RangeColor(30, 34, "&f"),
            new RangeColor(35, 39, "&b"),
            new RangeColor(40, 47, "&0"),
            new RangeColor(48, 50, "&4"),
            new RangeColor(51, 65, "&1"),
            new RangeColor(66, 70, "&3"),
            new RangeColor(71, 90, "&2"),
            new RangeColor(91, Integer.MAX_VALUE, "&8")
    };
    // 0:<10, 1:10-19, ..., 11:110-119, 12:>=120
    private static final String[] LEVEL_TIER_COLOR = {
            "&7",
            "&9", "&3", "&2", "&a", "&e", "&6&l",
            "&c&l", "&4&l", "&5&l", "&d&l", "&f&l",
            "&b&l"
    };

    static {
        rebuildTables();
    }

    /**
     * 若公式/倍率改为可配置，修改后调用一次即可重建表
     */
    public static void rebuildTables() {
        for (int p = 0; p <= MAX_PRESTIGE; p++) {
            double bf = getBoostFactor(p);
            double sum = 0.0;
            CUM_EXP[p][0] = 0.0;
            for (int lvl = 0; lvl < MAX_LEVEL; lvl++) {
                double req = getLevelExpRequired(bf, lvl); // 使用表驱动 & Math.round
                REQ_EXP[p][lvl] = req;
                sum += req;
                CUM_EXP[p][lvl + 1] = sum;
            }
        }
    }

    // ================== 颜色 & 展示 ==================
    public static String getPrestigeColor(int prestige) {
        for (RangeColor r : PRESTIGE_COLORS) {
            if (prestige >= r.lo && prestige <= r.hi) return r.c;
        }
        return "&7";
    }

    public static String getLevelColor(int level) {
        if (level < 10) return LEVEL_TIER_COLOR[0];
        int idx = level / 10;
        if (idx >= 12) return LEVEL_TIER_COLOR[12];
        return LEVEL_TIER_COLOR[idx];
    }

    /**
     * 某一等级（level）升到下一级所需经验（表驱动；与原逻辑数值一致）
     */
    public static double getLevelExpRequired(double boostFactor, int level) {
        int idx = level / 10;
        if (idx > 11) idx = 11; // 110+
        return Math.round(boostFactor * (EXP_TABLE[idx] + level));
    }

    // ================== 经验计算（表驱动） ==================

    /**
     * 转生经验倍率：0..100；>50 已在表中为 1_000_000
     */
    public static double getBoostFactor(int prestige) {
        if (prestige <= 0) return BOOST[0];
        if (prestige >= BOOST.length) return 1_000_000;
        return BOOST[prestige];
    }

    /**
     * 通过经验值获取等级（使用累计经验表 + 二分）
     */
    public static int getLevelByExp(int prestige, double exp) {
        int p = clampPrestige(prestige);
        if (exp <= 0) return 0;

        double[] cum = CUM_EXP[p];
        if (exp >= cum[MAX_LEVEL]) return MAX_LEVEL;

        // upperBound：第一个 cum[i] > exp 的下标
        int lo = 0, hi = MAX_LEVEL;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (cum[mid] <= exp) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo - 1; // 最大的 cum[level] <= exp
    }

    // ================== 对外 API（预计算 + 二分） ==================

    /**
     * 获取升级到某一级所需总经验（0..level 的总和）
     */
    public static double getLevelTotalExperience(int prestige, int level) {
        int p = clampPrestige(prestige);
        int l = clampLevel(level);
        return CUM_EXP[p][l];
    }

    /**
     * 当前等级进度 [0,1]
     */
    public static float getLevelProgress(int prestige, double experience) {
        int level = getLevelByExp(prestige, experience);
        if (level >= MAX_LEVEL) {
            return 1.0f;
        }
        int p = clampPrestige(prestige);
        double expThisLevel = REQ_EXP[p][level];
        double expTotalThisLevel = CUM_EXP[p][level];
        double progress = (experience - expTotalThisLevel) / expThisLevel;
        if (progress < 0) return 0.0f;
        if (progress > 1) return 1.0f;
        return (float) progress;
    }

    public static String getLevelTag(int prestige, double experience) {
        int level = getLevelByExp(prestige, experience);
        String pc = getPrestigeColor(prestige), lc = getLevelColor(level);
        return pc + '[' +
                lc + level +
                pc + ']';
    }

    public static String getLevelTag(int prestige, int level) {
        String pc = getPrestigeColor(prestige), lc = getLevelColor(level);
        return pc + '[' +
                lc + level +
                pc + ']';
    }

    public static String getLevelTagWithRoman(int prestige, double experience) {
        if (prestige <= 0) return getLevelTag(prestige, experience);
        int level = getLevelByExp(prestige, experience);
        String pc = getPrestigeColor(prestige), lc = getLevelColor(level);
        return pc + "[&e" +
                RomanUtil.convert(prestige) +
                pc + '-' +
                lc + level +
                pc + ']';
    }

    public static String getLevelTagWithRoman(int prestige, int level) {
        if (prestige <= 0) return getLevelTag(prestige, level);
        String pc = getPrestigeColor(prestige), lc = getLevelColor(level);
        return pc + "[&e" +
                RomanUtil.convert(prestige) +
                pc + '-' +
                lc + level +
                pc + ']';
    }

    // ================== 工具方法 ==================
    private static int clampPrestige(int prestige) {
        if (prestige < 0) return 0;
        return Math.min(prestige, MAX_PRESTIGE);
    }

    private static int clampLevel(int level) {
        if (level < 0) return 0;
        return Math.min(level, MAX_LEVEL);
    }

    // ===== 颜色映射（更易维护）=====
    private record RangeColor(int lo, int hi, String c) {
    }
}
