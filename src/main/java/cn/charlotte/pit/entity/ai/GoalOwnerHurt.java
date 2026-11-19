package cn.charlotte.pit.entity.ai;

import cn.charlotte.pit.entity.CustomOwnableEntity;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;

/**
 * 当主人攻击其他实体时，随从自动攻击同一目标。
 */
public class GoalOwnerHurt extends GoalCustomTarget {
    private final CustomOwnableEntity self;
    private LivingEntity target;
    private int lastTimestamp;

    public GoalOwnerHurt(CustomOwnableEntity self) {
        // 不强制视线、不检查寻路
        super(self.getCreature(), false);
        this.self = self;
    }

    /**
     * 对应旧版 a()
     */
    @Override
    public boolean canUse() {
        LivingEntity owner = self.getOwner();
        if (owner == null) {
            return false;
        }

        // 1.20+：主人最近的攻击目标及时间戳
        this.target = owner.getLastHurtMob();
        int ts = owner.getLastHurtMobTimestamp();
        return ts != this.lastTimestamp && this.isSuitableTarget(this.target, false);
    }

    /**
     * 对应旧版 c()
     */
    @Override
    public void start() {
        // 触发 CraftBukkit 事件，保持旧版行为
        this.mob.setTarget(this.target,
                EntityTargetEvent.TargetReason.OWNER_ATTACKED_TARGET, true);

        LivingEntity owner = self.getOwner();
        if (owner != null) {
            this.lastTimestamp = owner.getLastHurtMobTimestamp();
        }
        super.start();
    }
}
