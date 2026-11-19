package cn.charlotte.pit.nametag.hiding;

import java.util.Set;

/** Pure policy. Event/mega-streak exemptions always override every hiding condition. */
public final class NameHidingRules {
    private NameHidingRules() { }

    public enum Area { SPAWN, MIDDLE, WILD, NONE }

    public record Settings(boolean enabled, int minPrestige, int maxPrestige, double showAboveStreak,
                           double showAboveBounty, Set<Area> areas) {
        public Settings {
            if (minPrestige < 0 || maxPrestige < minPrestige) throw new IllegalArgumentException("P 区间必须满足 0 <= min <= max");
            if (!Double.isFinite(showAboveStreak) || showAboveStreak < -1 || !Double.isFinite(showAboveBounty) || showAboveBounty < -1)
                throw new IllegalArgumentException("连杀/赏金阈值必须为非负数，或 -1（关闭）");
            areas = Set.copyOf(areas);
        }

        public boolean hides(Area area, int prestige, double streak, double bounty, boolean eventActive,
                             boolean megaStreakActive, boolean revealedToViewer, boolean self) {
            if (!enabled || self || revealedToViewer || eventActive || megaStreakActive
                    || area == Area.NONE || !areas.contains(area) || showsByStats(streak, bounty)) return false;
            return prestige >= minPrestige && prestige <= maxPrestige;
        }

        /** Reaching either threshold reveals the name, even inside the configured P range. */
        public boolean showsByStats(double streak, double bounty) {
            return (showAboveStreak >= 0 && streak >= showAboveStreak)
                    || (showAboveBounty >= 0 && bounty >= showAboveBounty);
        }
    }

    public record Box(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(String atWorld, double x, double y, double z) {
            return world.equals(atWorld) && x >= minX && x < (double) maxX + 1
                    && y >= minY && y < (double) maxY + 1 && z >= minZ && z < (double) maxZ + 1;
        }
    }

    public record Regions(String world, Box spawn, int middleX, int middleY, int middleZ,
                          int middleRadius, int middleHeight) {
        public Area area(String atWorld, double x, double y, double z) {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return Area.NONE;
            if (spawn != null && spawn.contains(atWorld, x, y, z)) return Area.SPAWN;
            if (world == null || !world.equals(atWorld)) return Area.NONE;
            // Same block center/radius/height as RagePitEvents.makeCylinder.
            double dx = Math.floor(x) - middleX, dz = Math.floor(z) - middleZ;
            if (middleRadius > 0 && middleHeight > 0 && y >= middleY && y < (double) middleY + middleHeight
                    && dx * dx + dz * dz <= (double) middleRadius * middleRadius) return Area.MIDDLE;
            return Area.WILD;
        }
    }

    public static long revealExpiry(long now, long seconds) {
        if (seconds <= 0) throw new IllegalArgumentException("识名时间必须大于 0");
        try {
            return Math.addExact(now, Math.multiplyExact(seconds, 1000));
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    public static boolean megaThresholdReached(double streak, int threshold) {
        return threshold > 0 && streak >= threshold;
    }
}
