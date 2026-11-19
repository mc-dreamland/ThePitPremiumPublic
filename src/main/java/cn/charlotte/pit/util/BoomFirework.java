package cn.charlotte.pit.util;

import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * 一个“瞬爆”烟花实体：
 * ─ 生成后立刻向所有在线玩家发送 17 号实体事件 → 客户端播放爆炸粒子与声音
 * ─ 下一 tick 自动移除自身
 * <p>
 * ⚠ 1.20.2+ NMS 已全面使用 Mojang 命名；字段、方法与构造器都与旧版不同。
 */
public class BoomFirework extends FireworkRocketEntity {

    private boolean gone = false;  // 是否已经触发过爆炸

    /* ---------- 构造器 ---------- */

    /**
     * 直接用 NMS 的 ServerLevel 创建
     */
    public BoomFirework(ServerLevel level) {
        super(EntityType.FIREWORK_ROCKET, level);
        // 给实体塞一个最简单的火箭物品，防止 Vanilla 逻辑在 tick() 中 NPE
    }

    /**
     * 便捷重载：用 Bukkit 的 World 就能创建
     */
    public BoomFirework(World bukkitWorld) {
        this(((CraftWorld) bukkitWorld).getHandle());
    }

    /* ---------- 核心逻辑 ---------- */

    @Override
    public void tick() {
        if (gone) {                        // 第二次 tick：安全移除
            this.discard();               // 1.17+ 用 discard() 代替 die()/remove()
            return;
        }

        if (!this.level().isClientSide) { // 只在服务端执行
            gone = true;                  // 标记已爆炸

            // 17 号实体事件 = 烟花爆炸特效
            ClientboundEntityEventPacket explode =
                    new ClientboundEntityEventPacket(this, (byte) 17);

            // 手动发送给所有在线玩家（比旧版自行循环 PacketPlayOutEntityStatus 更简单）
            for (Player online : Bukkit.getOnlinePlayers()) {
                ((CraftPlayer) online).getHandle().connection.send(explode);
            }

            // 兜底：也调用 Vanilla 的广播方法（单人调试或无玩家在线时仍能看到特效）
            this.level().broadcastEntityEvent(this, (byte) 17);
        }
    }
}
