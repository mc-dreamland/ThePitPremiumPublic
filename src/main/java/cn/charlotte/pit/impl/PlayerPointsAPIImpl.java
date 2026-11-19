package cn.charlotte.pit.impl;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.api.PointsAPI;
import com.heypixel.heypoints.HeyPoints;
import com.heypixel.heypoints.HeyPointsApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * PlayerPoints 的 PointsAPI 实现（单例）
 */
public final class PlayerPointsAPIImpl implements PointsAPI {

    /**
     * 全局唯一实例，对应 Kotlin 的 object
     */
    public static final PlayerPointsAPIImpl INSTANCE = new PlayerPointsAPIImpl();

    /**
     * 缓存 PlayerPoints API，懒加载
     */
    private final HeyPointsApi pointsAPI;

    /**
     * 私有构造，防止外部实例化
     */
    private PlayerPointsAPIImpl() {
        HeyPoints plugin = (HeyPoints) Bukkit.getPluginManager().getPlugin("HeyPoints");
        if (plugin == null) {
            throw new IllegalStateException("未找到 HeyPoints 插件！");
        }
        this.pointsAPI = plugin.getApi();
    }

    @Override
    public boolean hasPoints(Player player, int points) {
        return pointsAPI.getAmount(player.getUniqueId()) >= points;
    }

    @Override
    public int getPoints(Player player) {
        return pointsAPI.getAmount(player.getUniqueId());
    }

    @Override
    public void costPoints(Player player, int points, String reason) {
        pointsAPI.take(Bukkit.getServer().getConsoleSender(), player, points, false, ThePit.getInstance(), reason);
    }
}
