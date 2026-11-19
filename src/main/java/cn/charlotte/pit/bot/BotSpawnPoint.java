package cn.charlotte.pit.bot;

import java.util.Objects;

/** World-aware point geometry, independent of loaded chunks or Bukkit entity state. */
public record BotSpawnPoint(String world, double x, double y, double z) {
    public BotSpawnPoint {
        Objects.requireNonNull(world, "world");
        if (world.isBlank() || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Invalid bot spawn point");
        }
    }

    public boolean near(String world, double x, double y, double z, double radius) {
        if (!this.world.equals(world) || !Double.isFinite(radius) || radius < 0) return false;
        double dx = this.x - x, dy = this.y - y, dz = this.z - z;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    public boolean sameBlock(String world, double x, double y, double z) {
        return this.world.equals(world) && Math.floor(this.x) == Math.floor(x)
                && Math.floor(this.y) == Math.floor(y) && Math.floor(this.z) == Math.floor(z);
    }
}
