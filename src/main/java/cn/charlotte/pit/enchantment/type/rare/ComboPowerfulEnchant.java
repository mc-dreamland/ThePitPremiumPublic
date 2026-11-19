package cn.charlotte.pit.enchantment.type.rare;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

@ArmorOnly
public class ComboPowerfulEnchant extends AbstractEnchantment implements IAttackEntity {

    @Override
    public String getEnchantName() {
        return "强力击: 蓄力";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "combo_powerful";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RARE;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        String bonus;
        switch (enchantLevel) {
            case 2:
                bonus = "95";
                break;
            case 3:
                bonus = "110";
                break;
            default:
                bonus = "75";
                break;
        }
        return "&7在&a 不在战斗&7 状态中持续15秒未受到伤害/s" +
                "&7则你下次攻击伤害额外增加&c +" + bonus + "%/s/s" +
                "&7在&c 战斗中&7 状态持续超过25秒未收到伤害, /s" +
                "则你下次攻击变为&f 真实伤害";
    }

    @Override
    public void handleAttackEntity(
            int enchantLevel,
            Player attacker,
            Entity target,
            double damage,
            AtomicDouble finalDamage,
            AtomicDouble boostDamage,
            AtomicBoolean cancel
    ) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId());
        boolean inFight = profile.getCombatTimer().hasExpired();

        long now = System.currentTimeMillis();
        long lastDamageAt = profile.getLastDamageAt();

        if (inFight) {
            // 战斗中：超过25秒未受伤害 → 下一次攻击转真实伤害
            if (lastDamageAt == -1L || now - lastDamageAt >= 25 * 1000L) {
                double boost = boostDamage.getAndSet(0.0);
                finalDamage.addAndGet(damage * boost);
            }
        } else {
            // 非战斗中：超过15秒未受伤害 → 下一次攻击增加百分比伤害
            if (lastDamageAt == -1L || now - lastDamageAt >= 15 * 1000L) {
                double boost = boostDamage.getAndSet(0.0);
                finalDamage.addAndGet(damage * boost);
            }
        }
    }
}
