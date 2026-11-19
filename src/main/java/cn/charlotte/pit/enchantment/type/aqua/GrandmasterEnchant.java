package cn.charlotte.pit.enchantment.type.aqua;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.FuncsKt;
import cn.charlotte.pit.util.cooldown.Cooldown;
import org.bukkit.Bukkit;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@ArmorOnly
@AutoRegister
public class GrandmasterEnchant extends AbstractEnchantment implements Listener {

    @EventHandler
    private void onDamage(EntityDamageByEntityEvent event) {
        if (PitBotManager.skipCustomCombat(event)) return;
        if (!(event.getDamager() instanceof FishHook damager) || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(damager.getShooter() instanceof Player shooter)) {
            return;
        }
        IMythicItem mythicItem = FuncsKt.toMythicItem(shooter.getInventory().getLeggings());
        if (mythicItem == null || mythicItem.getEnchantments().keySet().stream().noneMatch(enchant -> "rod_back".equals(enchant.getNbtName()))) {
            return;
        }
        boolean originInside = PitBotManager.inTraining(shooter);
        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
            if (!player.isOnline() || player.isDead() || PitBotManager.blockedPvp(shooter, player, originInside)) return;
            player.damage(0.0, shooter);
            player.setHealth(Math.max(0.0, player.getHealth() - PitBotManager.scalePvpDamage(shooter, player, 1.0)));
        }, 1L);
    }

    @Override
    public String getEnchantName() {
        return "特级渔夫";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 1;
    }

    @Override
    public String getNbtName() {
        return "grandmaster";
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
        return "&7使用鱼竿命中玩家时额外造成 &c0.5❤ &7的伤害";
    }
}
