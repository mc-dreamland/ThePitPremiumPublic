package cn.charlotte.pit.manager;

import cn.charlotte.pit.ThePit;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BlockRestoreManager 负责管理所有临时放置方块的延迟还原任务，
 * 用于在指定时间后将方块还原为原始状态，避免资源泄漏。
 */
public class BlockRestoreManager {

    private static final Map<Location, Pair<BukkitTask, Runnable>> PLACED_BLOCKS = new ConcurrentHashMap<>();
    public static BlockRestoreManager INSTANCE;
    private final Map<UUID, Location> PLAYER_LAVA = new ConcurrentHashMap<>();
    private final ThePit plugin;

    public BlockRestoreManager(ThePit plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public void placeTemporaryLava(UUID placer, Block block, long delayTicks) {
        Location location = block.getLocation();

        BlockState origin = block.getState();

        scheduleRestore(location, origin, delayTicks);   // 延迟还原
        block.setType(Material.LAVA);
        block.setMetadata("placer", new FixedMetadataValue(plugin, placer.toString()));
        PLAYER_LAVA.put(placer, location); // 单桶索引
    }

    public boolean hasPlacedLava(UUID player) {
        Location loc = PLAYER_LAVA.get(player);
        if (loc == null) return false;

        Block b = loc.getBlock();
        if (b.getType() != Material.LAVA) {
            finishRestore(loc);
            return false;
        }
        return true;
    }

    public Location getPlacedLava(UUID player) {
        return PLAYER_LAVA.get(player);
    }

    /**
     * 强制收回指定玩家放置的岩浆
     */
    public void finishRestoreByPlayer(UUID player) {
        Location loc = PLAYER_LAVA.remove(player);
        if (loc != null) finishRestore(loc);
    }

    /**
     * 强制收回指定位置的“临时方块”
     */
    public boolean finishRestore(Location location) {
        Pair<BukkitTask, Runnable> pair = PLACED_BLOCKS.remove(location);
        if (pair != null) {
            pair.getLeft().cancel();
            pair.getRight().run();            // 立即执行还原
            // 反向删除 PLAYER_LAVA 中的 value
            PLAYER_LAVA.values().removeIf(loc -> loc.equals(location));
            return true;
        }
        return false;
    }

    /**
     * 调度一个方块的还原任务。
     *
     * @param location   要处理的方块坐标
     * @param blockState 原始方块类型（用于还原）
     * @param delayTicks 延迟多少 Tick 后还原
     */
    public void scheduleRestore(Location location, BlockState blockState, long delayTicks) {
        Block block = location.getBlock();

        boolean lavaPlaced = PLAYER_LAVA.values().removeIf(loc -> {
            block.removeMetadata("placer", plugin);
            cancelRestore(location);
            return loc.equals(location);
        });

        if (lavaPlaced) {
            blockState.setType(Material.AIR);
        }

        Runnable restore = () -> {
            block.setType(blockState.getType());
            if (blockState.getType() != Material.AIR) {
                block.setBlockData(blockState.getBlockData());
            }

            block.removeMetadata("placer", plugin);
            PLACED_BLOCKS.remove(location);
        };

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                restore.run();
            }
        }.runTaskLater(plugin, delayTicks);

        PLACED_BLOCKS.put(location, Pair.of(task, restore));
    }

    /**
     * 取消指定位置的还原任务（如果存在）。
     *
     * @param location 方块位置
     * @return 如果任务存在并被取消返回 true，否则返回 false
     */
    public boolean cancelRestore(Location location) {
        Pair<BukkitTask, Runnable> pair = PLACED_BLOCKS.remove(location);
        if (pair != null) {
            pair.getLeft().cancel();
            return true;
        }
        return false;
    }

    /**
     * 强制清除指定位置周围 radius 范围内的所有已放置方块。
     *
     * @param center 中心位置
     * @param radius 半径距离（单位：Block）
     */
    public void forceRestoreNearby(Location center, double radius) {
        for (Location loc : new ArrayList<>(PLACED_BLOCKS.keySet())) {
            if (loc.getWorld().equals(center.getWorld()) && loc.distance(center) <= radius) {
                Pair<BukkitTask, Runnable> pair = PLACED_BLOCKS.remove(loc);
                if (pair != null) {
                    pair.getRight().run();
                    pair.getLeft().cancel();
                }
            }
        }
    }

    /**
     * 强制立即执行所有已注册的还原逻辑，并取消所有任务。
     * 通常用于世界重置、玩家死亡或插件关闭等场景。
     */
    public void forceRestoreAll() {
        for (Pair<BukkitTask, Runnable> pair : PLACED_BLOCKS.values()) {
            pair.getRight().run();
            pair.getLeft().cancel();
        }
        PLACED_BLOCKS.clear();
        PLAYER_LAVA.clear();
    }

    /**
     * 取消所有还原任务但不执行还原操作。
     * 一般用于插件关闭时快速清理。
     */
    public void cancelAllAndClear() {
        for (Pair<BukkitTask, Runnable> pair : PLACED_BLOCKS.values()) {
            pair.getLeft().cancel();
        }
        PLACED_BLOCKS.clear();
        PLAYER_LAVA.clear();
    }

    /**
     * 判断某个位置是否已经被注册为待还原的方块。
     *
     * @param location 方块位置
     * @return 是否存在还原任务
     */
    public boolean isScheduled(Location location) {
        return PLACED_BLOCKS.containsKey(location);
    }
}
