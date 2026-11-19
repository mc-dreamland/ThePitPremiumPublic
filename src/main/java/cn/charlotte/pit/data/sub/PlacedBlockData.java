package cn.charlotte.pit.data.sub;


import cn.charlotte.pit.util.cooldown.Cooldown;
import org.bukkit.Location;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 22:15
 */
public record PlacedBlockData(Location location, Cooldown cooldown) {
}
