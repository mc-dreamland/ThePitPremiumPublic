package cn.charlotte.pit.perk;

import cn.charlotte.pit.manager.ResourceManager;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractHandle {
    /**
     * @param enchantLevel 物品附魔等级
     * @param attacker     攻击者（物品持有者）
     * @param target       目标玩家对象
     * @param damage       物品初始伤害（未经过护甲处理）
     * @param finalDamage  最终伤害，即target受到的最终伤害
     * @param boostDamage  伤害倍率，将在所有附魔结算后统一计算，基础值为1
     * @param cancel       取消事件
     */
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
    }


    /**
     * @param enchantLevel 物品附魔等级
     * @param itemStack    物品
     * @param myself       玩家本身（物品持有者）
     * @param cancel       取消事件
     */
    public void handleItemDamaged(int enchantLevel, ItemStack itemStack, Player myself, AtomicBoolean cancel) {
    }

    /**
     * @param enchantLevel    附魔等级
     * @param myself          玩家对象（被攻击者，附魔物品持有者）
     * @param target          死亡生物
     * @param damage          物品初始伤害（未经过护甲处理）
     * @param finalDamage     最终伤害，即target受到的最终伤害
     * @param resourceManager 结算管理，控制额外获取或加成
     */
    public void handlePlayerAssist(int enchantLevel, Player myself, Entity target, double damage, double finalDamage, ResourceManager resourceManager) {
    }

    /**
     * @param enchantLevel    附魔等级
     * @param myself          玩家对象（被攻击者，附魔物品持有者）
     * @param target          击杀者
     * @param resourceManager 结算管理，控制额外获取或加成
     */
    public void handlePlayerBeKilledByEntity(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
    }

    /**
     * @param enchantLevel 附魔等级
     * @param myself       玩家对象（被攻击者，附魔物品持有者）
     * @param attacker     （攻击者）
     * @param damage       物品初始伤害（未经过护甲处理）
     * @param finalDamage  最终伤害，即target受到的最终伤害
     * @param boostDamage  伤害倍率，将在所有附魔结算后统一计算，基础值为1
     * @param cancel       取消事件
     *                                                             Todo: @param projectile 攻击事件的投掷物(没有则为null)
     */
    public void handlePlayerDamaged(int enchantLevel, Player myself, Entity attacker, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
    }

    /**
     * @param enchantLevel 附魔等级
     * @param myself       玩家对象（被攻击者，附魔物品持有者）
     */
    public void handleJoin(int enchantLevel, Player myself) {
    }

    /**
     * @param enchantLevel    附魔等级
     * @param myself          玩家对象（被攻击者，附魔物品持有者）
     * @param target          死亡生物
     * @param resourceManager 结算管理，控制额外获取或加成
     */
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
    }

    /** Called only for explicitly reviewed @BotKillEffect perks; override to omit progression side effects. */
    public void handleBotKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        handlePlayerKilled(enchantLevel, myself, target, resourceManager);
    }

    /**
     * @param enchantLevel 附魔等级
     * @param myself       玩家对象（被攻击者，附魔物品持有者）
     */
    public void handleQuit(int enchantLevel, Player myself) {
    }


    /**
     * @param enchantLevel 附魔等级
     * @param myself       玩家对象（被攻击者，附魔物品持有者）
     */
    public void handleRespawn(int enchantLevel, Player myself) {
    }

    /**
     * @param enchantLevel 物品附魔等级
     * @param attacker     攻击者（物品持有者）
     * @param target       目标玩家对象
     * @param damage       物品初始伤害（未经过护甲处理）
     * @param finalDamage  最终伤害，即target受到的最终伤害
     * @param boostDamage  伤害倍率，将在所有附魔结算后统一计算，基础值为1
     * @param cancel       取消事件
     */
    public void handleShootEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
    }


    /**
     * @param enchantLevel 持有玩家物品等级
     * @param player       持有物品玩家
     */
    public void handleTick(int enchantLevel, Player player) {
    }

    /**
     * @return 每多少tick执行一次，20tick为1秒
     */
    public int loopTick(int enchantLevel) {
        return -1;
    }
}
