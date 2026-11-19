package cn.charlotte.pit.parm.listener;

import cn.charlotte.pit.manager.ResourceManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/2 23:21
 */
public interface IPlayerAssist {
    /**
     * @param enchantLevel    附魔等级
     * @param myself          玩家对象（被攻击者，附魔物品持有者）
     * @param target          死亡生物
     * @param damage          物品初始伤害（未经过护甲处理）
     * @param finalDamage     最终伤害，即target受到的最终伤害
     * @param resourceManager 结算管理，控制额外获取或加成
     */
    void handlePlayerAssist(int enchantLevel, Player myself, Entity target, double damage, double finalDamage, ResourceManager resourceManager);

}
