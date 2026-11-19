package cn.charlotte.pit.entity;

import cn.charlotte.pit.entity.ai.FollowOwnerGoal;
import cn.charlotte.pit.entity.ai.GoalOwnerHurt;
import cn.charlotte.pit.entity.ai.GoalTargetOwner;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;

import java.lang.reflect.Field;

/**
 * @Author: EmptyIrony
 * @Date: 2021/4/11 15:53
 */
public class EntityUtil {
    public static Object getPrivateField(String fieldName, Class clazz, Object object) {
        Field field;
        Object o = null;

        try {
            field = clazz.getDeclaredField(fieldName);

            field.setAccessible(true);

            o = field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return o;
    }

    public static void insertAi(CustomOwnableEntity entity, ServerPlayer player) {

        entity.getCreature().goalSelector.getAvailableGoals().clear();
        entity.getCreature().targetSelector.getAvailableGoals().clear();

//        entity.getCreature().getNavigation().a(true);
        entity.getCreature().goalSelector.addGoal(1, new FloatGoal(entity.getCreature()));
        entity.getCreature().goalSelector.addGoal(3, new LeapAtTargetGoal(entity.getCreature(), 0.4F));
        entity.getCreature().goalSelector.addGoal(5, new FollowOwnerGoal(entity, 1.0D, 10.0F, 2.0F));
        entity.getCreature().goalSelector.addGoal(9, new LookAtPlayerGoal(entity.getCreature(), LivingEntity.class, 8.0F));
        entity.getCreature().goalSelector.addGoal(9, new RandomLookAroundGoal(entity.getCreature()));
        entity.getCreature().targetSelector.addGoal(1, new GoalTargetOwner(entity));
        entity.getCreature().targetSelector.addGoal(2, new GoalOwnerHurt(entity));

        entity.setOwner(player);
    }

}
