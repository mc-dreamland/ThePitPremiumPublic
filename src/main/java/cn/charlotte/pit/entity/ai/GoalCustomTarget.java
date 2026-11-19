package cn.charlotte.pit.entity.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.scores.Team;

import java.util.EnumSet;

/**
 * 适配 1.20.6 的自定义目标选择器，基于原 1.8.8 版本重写。
 */
public abstract class GoalCustomTarget extends Goal {
    /**
     * 执行 AI 的实体
     */
    protected final Mob mob;

    /**
     * 是否需要进行路径可达性检测
     */
    private final boolean checkPath;

    /**
     * 是否必须保持视线可见
     */
    protected boolean mustSee;

    /**
     * 0 = 未检查；1 = 可到达；2 = 不可到达
     */
    private int pathStatus;
    /**
     * 路径检查冷却（tick）
     */
    private int pathCooldown;
    /**
     * 已经失去视线的 tick 数
     */
    private int unseenTicks;

    public GoalCustomTarget(Mob mob, boolean mustSee) {
        this(mob, mustSee, false);
    }

    public GoalCustomTarget(Mob mob, boolean mustSee, boolean checkPath) {
        this.mob = mob;
        this.mustSee = mustSee;
        this.checkPath = checkPath;
        // 该 Goal 只占用 TARGET 标志位
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    /**
     * 判断目标是否可被攻击。
     */
    public static boolean isValidTarget(Mob attacker, LivingEntity target,
                                        boolean includeInvulnerablePlayers, boolean mustSee) {
        if (target == null || target == attacker) return false;
        if (!target.isAlive()) return false;
        if (!attacker.canAttack(target)) return false;

        Team teamA = attacker.getTeam();
        Team teamB = target.getTeam();
        if (teamA != null && teamA == teamB) return false;

        // 驯服相关检查
        if (attacker instanceof TamableAnimal tameA && tameA.isTame()) {
            if (target instanceof TamableAnimal tameB && tameB.isTame() &&
                    tameA.getOwnerUUID() != null &&
                    tameA.getOwnerUUID().equals(tameB.getOwnerUUID())) {
                return false;
            }
            if (target == tameA.getOwner()) {
                return false;
            }
        } else if (target instanceof Player player && !includeInvulnerablePlayers && player.getAbilities().invulnerable) {
            return false;
        }

        return !mustSee || attacker.getSensing().hasLineOfSight(target);
    }

    /**
     * 对应旧版 a() -> canUse()
     */
    @Override
    public abstract boolean canUse();

    /**
     * 对应旧版 b() -> canContinueToUse()
     */
    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        Team myTeam = this.mob.getTeam();
        Team targetTeam = target.getTeam();
        if (myTeam != null && myTeam == targetTeam) {
            return false;
        }

        double followRange = this.getFollowRange();
        if (this.mob.distanceToSqr(target) > followRange * followRange) {
            return false;
        }

        // 视线检测
        if (this.mustSee) {
            if (this.mob.getSensing().hasLineOfSight(target)) {
                this.unseenTicks = 0;
            } else if (++this.unseenTicks > 60) {
                return false;
            }
        }

        return !(target instanceof Player player && player.getAbilities().invulnerable);
    }

    /**
     * 获取随从范围（FOLLOW_RANGE 属性）
     */
    protected double getFollowRange() {
        AttributeInstance inst = this.mob.getAttribute(Attributes.FOLLOW_RANGE);
        return inst == null ? 16.0D : inst.getValue();
    }

    /**
     * 对应旧版 c() -> start()
     */
    @Override
    public void start() {
        this.pathStatus = 0;
        this.pathCooldown = 0;
        this.unseenTicks = 0;
    }

    /**
     * 对应旧版 d() -> stop()
     */
    @Override
    public void stop() {
        this.mob.setTarget(null);
    }

    /**
     * 包装旧版 a(EntityLiving, boolean)
     */
    protected boolean isSuitableTarget(LivingEntity target, boolean includeInvulnerablePlayers) {
        if (!isValidTarget(this.mob, target, includeInvulnerablePlayers, this.mustSee)) {
            return false;
        }

        if (this.checkPath) {
            // 每 10~14 tick 检查一次可达性
            if (--this.pathCooldown <= 0) {
                this.pathStatus = 0;
            }
            if (this.pathStatus == 0) {
                this.pathStatus = this.canReach(target) ? 1 : 2;
            }
            return this.pathStatus != 2;
        }
        return true;
    }

    /**
     * 检查是否能够寻路至目标（仿造旧版逻辑）
     */
    private boolean canReach(LivingEntity target) {
        this.pathCooldown = 10 + this.mob.getRandom().nextInt(5);
        Path path = this.mob.getNavigation().createPath(target, 0);
        if (path == null) return false;

        Node end = path.getEndNode();
        if (end == null) return false;

        int dx = end.x - Mth.floor(target.getX());
        int dz = end.z - Mth.floor(target.getZ());
        return (double) (dx * dx + dz * dz) <= 2.25D;
    }
}
