package cn.charlotte.pit.bot;

/** Block-aligned cuboid; the second corner's entire block is included. */
public record BotRegion(String world, double minX, double minY, double minZ,
                        double maxX, double maxY, double maxZ) {
    public static BotRegion of(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        return new BotRegion(world, Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                (double) Math.max(x1, x2) + 1, (double) Math.max(y1, y2) + 1, (double) Math.max(z1, z2) + 1);
    }

    public boolean contains(String world, double x, double y, double z) {
        return this.world.equals(world) && x >= minX && x < maxX && y >= minY && y < maxY
                && z >= minZ && z < maxZ;
    }
}
