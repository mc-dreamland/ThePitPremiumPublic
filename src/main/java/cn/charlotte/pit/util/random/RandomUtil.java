package cn.charlotte.pit.util.random;

import cn.charlotte.pit.ThePit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 2 * @Author: EmptyIrony
 * 3 * @Date: 2020/12/28 23:47
 * 4
 */
public class RandomUtil {
    public static final Random random;

    static {
        random = new Random();
    }

    /**
     * 参数范围为0-1
     *
     * @param chance 百分数概率，大于等于1永远返回true
     * @return 是否成功
     */
    public static boolean hasSuccessfullyByChance(double chance) {
        if (chance >= 1.0) return true;
        if (chance <= 0.0) return false;

        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    @SafeVarargs
    public static <T> T helpMeToChooseOne(T... entries) {
        if (entries == null || entries.length == 0) {
            throw new IllegalArgumentException("entries cannot be null or empty");
        }
        return entries[random.nextInt(entries.length)];
    }

    public static Location generateRandomLocation() {
        Location ragePitMiddle = ThePit.getInstance().getPitConfig().getRagePitMiddle().clone();
        int x = RandomUtil.random.nextInt(180) - 90;
        int z = RandomUtil.random.nextInt(180) - 90;
        World world = Bukkit.getWorlds().getFirst();
        ragePitMiddle.add(x, 0, z);
        return world.getHighestBlockAt(ragePitMiddle).getLocation().clone().add(0, 1, 0);
    }

    public static int randomInRange(int max, int min) {
        return Math.max(random.nextInt(max), min);
    }
}
