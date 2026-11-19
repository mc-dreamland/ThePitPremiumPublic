package cn.charlotte.pit.enchantment.type.rage;

import cn.charlotte.pit.bot.PitBotManager;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.cooldown.Cooldown;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@AutoRegister
public class SingularityEnchant extends AbstractEnchantment implements Listener {

    @Override
    public String getEnchantName() {
        return "奇异";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "singularity";
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
        double value = switch (enchantLevel) {
            case 1 -> 2.5;
            case 2 -> 2.0;
            default -> 1.5;
        };
        return "&7单次最多受到&c " + value + "❤ &7伤害";
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (PitBotManager.skipCustomCombat(event)) return;
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        int level = ThePit.api.getItemEnchantLevel(victim.getInventory().getLeggings(), "singularity");
        if (level <= 0) {
            return;
        }

        double maxDamage = switch (level) {
            case 1 -> 2.5;
            case 2 -> 2.0;
            default -> 1.5;
        } * 2.0;

        double reduce = Math.min(maxDamage, event.getFinalDamage()) / event.getFinalDamage();
        event.setDamage(reduce * event.getDamage());
    }
}
