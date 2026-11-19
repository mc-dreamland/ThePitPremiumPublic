package cn.charlotte.pit.enchantment.type.rage;

import cn.charlotte.pit.bot.PitBotManager;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

@AutoRegister
@ArmorOnly
public class AceOfSpadesEnchant extends AbstractEnchantment {

    @Override
    public String getEnchantName() {
        return "战斗铲之王";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "ace_of_spades";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RAGE;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        double reduce = switch (enchantLevel) {
            case 1 -> 0.5;
            case 2 -> 1.0;
            default -> 1.5;
        };
        return "&7来自 &e洛阳铲 &7伤害减少 &9" + reduce + "&c❤";
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (PitBotManager.skipCustomCombat(event)) return;
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player victim)) {
            return;
        }

        ItemStack item = damager.getItemInHand();
        if (!"combat_spade".equals(ItemUtil.getInternalName(item))) {
            return;
        }

        ItemStack leggings = victim.getInventory().getLeggings();
        int enchantLevel = ThePit.api.getItemEnchantLevel(leggings, getNbtName());
        if (enchantLevel <= 0) {
            return;
        }

        if (PlayerUtil.shouldIgnoreEnchant(victim, damager)) {
            return;
        }

        double reduceValue = switch (enchantLevel) {
            case 1 -> 0.5;
            case 2 -> 1.0;
            default -> 1.5;
        } * 2;

        event.setDamage(event.getDamage() - reduceValue);
    }
}
