package cn.charlotte.pit.hologram.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.hologram.AbstractHologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/4 13:39
 */
public class JumpAndFightHologram extends AbstractHologram {
    private static final Random random = new Random();
    private static final List<String> str = Arrays.asList(
            "&e你知道吗? /trade 玩家名字,不仅可以发送交易请求也可以接受请求!",
            "&a&l跳下去! &c&l战斗!",
            "&e你知道吗? 在拍卖时使用/trade gui 可以打开拍卖页面!"
    );
    private final int animationTick = 0;
    private final long lastTickTime = 0;

    @Override
    public String getInternalName() {
        return "jump_and_fight";
    }

    @Override
    public List<String> getText(Player player) {
        return Arrays.asList("&e&l天坑乱斗", str.get(random.nextInt(str.size())));
    }

    @Override
    public boolean shouldLoop() {
        return true;
    }

    @Override
    public int loopTicks() {
        return 3600;
    }

    @Override
    public double getHologramHighInterval() {
        return 0.35;
    }

    @Override
    public Location getLocation() {
        return ThePit.getInstance().getPitConfig().getHologramLocation();
    }
}
