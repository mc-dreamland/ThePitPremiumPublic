package cn.charlotte.pit.parm.listener;

import cn.charlotte.pit.enchantment.param.listen.UnknowEntityType;
import cn.charlotte.pit.manager.ResourceManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * 2 * @Author: EmptyIrony
 * 3 * @Date: 2020/12/29 22:42
 * 4
 */
@UnknowEntityType
public interface IPlayerBeKilledByEntity {
    /**
     * @param enchantLevel    附魔等级
     * @param myself          玩家对象（被攻击者，附魔物品持有者）
     * @param target          击杀者
     * @param resourceManager 结算器用于结算奖励
     */
    void handlePlayerBeKilledByEntity(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager);
}
