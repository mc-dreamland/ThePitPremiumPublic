package cn.charlotte.pit.enchantment.type.aqua;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.parm.listener.IPlayerDamaged;
import cn.charlotte.pit.util.FuncsKt;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

@ArmorOnly
public class RogueEnchant extends AbstractEnchantment implements IAttackEntity, IPlayerDamaged {

    @Override
    public String getEnchantName() {
        return "无赖";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 1;
    }

    @Override
    public String getNbtName() {
        return "rogue";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.FISH_NORMAL;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7攻击穿着青色神话之甲的玩家时造成的伤害 &c+25% &7且自身受到其的伤害 &9-10%";
    }

    @PlayerOnly
    @Override
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (target instanceof Player playerTarget) {
            IMythicItem mythicItem = FuncsKt.toMythicItem(playerTarget.getInventory().getLeggings());
            if (mythicItem != null && mythicItem.getColor() == MythicColor.AQUA) {
                boostDamage.addAndGet(0.25);
            }
        }
    }

    @PlayerOnly
    @Override
    public void handlePlayerDamaged(int enchantLevel, Player myself, Entity attacker, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (attacker instanceof Player playerAttacker) {
            IMythicItem mythicItem = FuncsKt.toMythicItem(playerAttacker.getInventory().getLeggings());
            if (mythicItem != null && mythicItem.getColor() == MythicColor.AQUA) {
                boostDamage.set(boostDamage.get() - 0.1);
            }
        }
    }
}
