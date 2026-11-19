package cn.charlotte.pit.enchantment.type.ragerare;

import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.event.PitDamageEvent;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

@AutoRegister
@ArmorOnly
public class RegularityEnchant extends AbstractEnchantment implements Listener {

    @Override
    public String getEnchantName() {
        return "狂暴连击";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "regularity";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RAGE_RARE;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7当近战伤害低于&c" + a(enchantLevel) +
                "❤ &7时/s&7, 将会自动再次攻击./s&7第二次攻击的伤害为第一次攻击的&c" +
                b(enchantLevel) + "%&7.";
    }

    private double a(int enchantLevel) {
        switch (enchantLevel) {
            case 1:
                return 0.7;
            case 2:
                return 1.7;
            default:
                return 1.9;
        }
    }

    private int b(int enchantLevel) {
        switch (enchantLevel) {
            case 1:
                return 40;
            case 2:
                return 45;
            default:
                return 60;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void damage(PitDamageEvent event) {
        if (event.getAttacker() == null) return;
        if (event.getAttacker() instanceof Arrow) return;
        if (!(event.getTarget() instanceof Player victim)) return;

        Player attacker = event.getAttacker();

        if (PlayerUtil.shouldIgnoreEnchant(attacker, victim)) {
            return;
        }

        int level = ThePit.api.getItemEnchantLevel(
                attacker.getInventory().getLeggings(),
                "regularity"
        );
        if (level < 1) return;

        double finalDamage = event.getDamage() * event.getBoostDamage().get();
        if (finalDamage >= a(level)) return;

        // 冷却检测
        List<MetadataValue> metadata = victim.getMetadata("regularity_cooldown");
        if (!metadata.isEmpty()) {
            long time = metadata.getFirst().asLong();
            if (System.currentTimeMillis() < time) return;
        }

        if (victim.isDead()) return;

        double boost = b(level) * 0.01;
        boolean originInside = PitBotManager.inTraining(attacker);

        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
            if (!victim.isOnline() || victim.isDead() || PitBotManager.blockedPvp(attacker, victim, originInside)) return;
            victim.setNoDamageTicks(0);
            victim.damage(event.getDamage() * boost, attacker);
            victim.setMetadata(
                    "regularity_cooldown",
                    new FixedMetadataValue(
                            ThePit.getInstance(),
                            System.currentTimeMillis() + 1000L + 2
                    )
            );
        }, 5L);
    }
}
