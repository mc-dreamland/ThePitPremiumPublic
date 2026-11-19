package cn.charlotte.pit.entity.custom;

import cn.charlotte.pit.entity.CustomOwnableEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * @Author: EmptyIrony
 * @Date: 2021/4/11 16:12
 */
public class CustomSlime extends Wolf implements CustomOwnableEntity {

    private ServerPlayer owner;

    public CustomSlime(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    public CustomSlime(Level level, ServerPlayer owner) {
        this(EntityType.WOLF, level);
        this.owner = owner;
    }

    /* ---------------- CustomOwnableEntity ---------------- */

    @Override
    public ServerPlayer getOwner() {
        return owner;
    }

    @Override
    public void setOwner(ServerPlayer player) {
        this.owner = player;
    }

    @Override
    public Mob getCreature() {
        return this;
    }

    /* ---------------- AI Goal 注册 ---------------- */

    @Override
    protected void registerGoals() {
        // 通用 Goal
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0D, 2.0F, 10.0F, false));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        // Target 相关 Goal（注意添加到 targetSelector）
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
    }
}
