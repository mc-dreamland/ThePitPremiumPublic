package cn.charlotte.pit.entity;


/**
 * @Author: EmptyIrony
 * @Date: 2021/4/11 15:58
 */

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

public interface CustomOwnableEntity {
    @Nullable
    ServerPlayer getOwner();

    void setOwner(ServerPlayer player);

    /**
     * 返回真正的 Mob 对象（等价于旧版 EntityInsentient）
     */
    Mob getCreature();
}