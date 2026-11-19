package cn.charlotte.pit.entity.ai;

import cn.charlotte.pit.entity.CustomOwnableEntity;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;

/**
 * 当主人受到攻击时，随从反击攻击者。
 */
public class GoalTargetOwner extends GoalCustomTarget {
    private final CustomOwnableEntity self;
    private LivingEntity attacker;
    private int lastTimestamp;

    public GoalTargetOwner(CustomOwnableEntity self) {
        super(self.getCreature(), false);
        this.self = self;
    }

    @Override
    public boolean canUse() {
        LivingEntity owner = self.getOwner();
        if (owner == null) {
            return false;
        }

        this.attacker = owner.getLastHurtByMob();
        int ts = owner.getLastHurtByMobTimestamp();
        return ts != this.lastTimestamp && this.isSuitableTarget(this.attacker, false);
    }

    @Override
    public void start() {
        this.mob.setTarget(this.attacker,
                EntityTargetEvent.TargetReason.TARGET_ATTACKED_OWNER, true);

        LivingEntity owner = self.getOwner();
        if (owner != null) {
            this.lastTimestamp = owner.getLastHurtByMobTimestamp();
        }
        super.start();
    }
}
