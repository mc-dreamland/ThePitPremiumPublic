package cn.charlotte.pit.entity.ai;

import cn.charlotte.pit.entity.CustomOwnableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * @Author: EmptyIrony
 * @Date: 2021/4/11 16:08
 */
public class FollowOwnerGoal extends Goal {
    private final CustomOwnableEntity self;
    private final Mob mob;
    private final PathNavigation navigation;
    private final Level level;
    private final double speed;
    private final float startDistance;
    private final float stopDistance;

    private Player owner;
    private int updateCountdownTicks;

    public FollowOwnerGoal(CustomOwnableEntity self, double speed, float startDistance, float stopDistance) {
        this.self = self;
        this.mob = self.getCreature();
        this.level = mob.level();
        this.navigation = mob.getNavigation();
        this.speed = speed;
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /* ---------------- canStart / shouldContinue ---------------- */

    @Override
    public boolean canUse() {
        owner = self.getOwner();
        if (owner == null || owner.isSpectator()) {
            return false;
        }
        return mob.distanceToSqr(owner) > startDistance * startDistance;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null && !navigation.isDone() && mob.distanceToSqr(owner) > stopDistance * stopDistance;
    }

    /* ---------------- start / stop ---------------- */

    @Override
    public void start() {
        updateCountdownTicks = 0;
    }

    @Override
    public void stop() {
        owner = null;
        navigation.stop();
    }

    /* ---------------- tick ---------------- */

    @Override
    public void tick() {
        mob.getLookControl().setLookAt(owner, 10.0F, mob.getMaxHeadXRot());
        if (--updateCountdownTicks <= 0) {
            updateCountdownTicks = 10;
            if (!navigation.moveTo(owner, speed)) {
                // 距离过远，尝试 TP 传送到玩家附近的安全方块
                if (mob.distanceToSqr(owner) >= 144.0D) {
                    final int ownerX = owner.blockPosition().getX();
                    final int ownerZ = owner.blockPosition().getZ();
                    final int ownerY = owner.blockPosition().getY();
                    for (int dx = -2; dx <= 2; ++dx) {
                        for (int dz = -2; dz <= 2; ++dz) {
                            if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                                BlockPos pos = new BlockPos(ownerX + dx, ownerY, ownerZ + dz);
                                if (isTeleportFriendly(pos)) {
                                    mob.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                                    navigation.stop();
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isTeleportFriendly(BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }
}