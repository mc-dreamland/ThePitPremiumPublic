package cn.charlotte.pit.event;

import com.google.common.util.concurrent.AtomicDouble;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/30 1:09
 */
@Getter
@Setter
public class PitDamageEvent extends PitEvent {
    private @Nullable Player attacker;
    private @NotNull Entity target;
    private AtomicDouble finalDamage;
    private AtomicDouble boostDamage;
    private double damage;
    private boolean shoot;
    private boolean cancel;

    /**
     * @param attacker    攻击者玩家，可能为null
     * @param target      目标实体，不能为null
     * @param finalDamage 真实伤害值，使用AtomicDouble包装
     * @param boostDamage 伤害加成系数，使用AtomicDouble包装
     * @param damage      原始基础伤害值
     * @param cancel      是否取消事件
     * @throws IllegalArgumentException 如果target为null
     */
    public PitDamageEvent(@Nullable Player attacker, @NotNull Entity target, AtomicDouble finalDamage, AtomicDouble boostDamage, double damage, boolean isShoot , boolean cancel) {
        this.attacker = attacker;
        this.target = target;
        this.finalDamage = finalDamage;
        this.boostDamage = boostDamage;
        this.damage = damage;
        this.shoot = isShoot;
        this.cancel = cancel;
    }
}