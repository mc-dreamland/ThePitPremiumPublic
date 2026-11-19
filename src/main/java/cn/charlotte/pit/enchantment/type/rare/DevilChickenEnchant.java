package cn.charlotte.pit.enchantment.type.rare;

import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.item.BowOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.parm.listener.IPlayerShootEntity;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoRegister
@BowOnly
public final class DevilChickenEnchant extends AbstractEnchantment implements IPlayerShootEntity, IActionDisplayEnchant, Listener {

    private final Map<UUID, Long> cooldownMap = new ConcurrentHashMap<>();

    @Override
    public String getEnchantName() {
        return "恶魔鸡";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "devil_chicken";
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
        return "&7射出的箭矢命中玩家时将召唤 &e" + enchantLevel + " &7只 &4恶魔鸡 &7并在 &e1s &7后爆炸&7, /s&7每只鸡将对周围 &e2格 &7内的玩家造成最多 &f" + ((double) enchantLevel / 2) + "❤ &7的&f真实&7伤害 &7(" + (6 - enchantLevel) + "s冷却)";
    }

    @Override
    public void handleShootEntity(int enchantLevel, @NotNull Player attacker, @NotNull Entity entity, double damage, @NotNull AtomicDouble finalDamage, @NotNull AtomicDouble boostDamage, @NotNull AtomicBoolean cancel) {
        if (!(entity instanceof Player victim)) {
            return;
        }

        long cooldownSeconds = 6 - enchantLevel;
        long cooldownMillis = cooldownSeconds * 1000;

        UUID attackerId = attacker.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (cooldownMap.containsKey(attackerId)) {
            long remaining = cooldownMap.get(attackerId) - currentTime;
            if (remaining > 0) {
                return;
            }
        }

        cooldownMap.put(attackerId, currentTime + cooldownMillis);

        boolean originInside = PitBotManager.inTraining(attacker);
        Location loc = victim.getLocation();
        for (int i = 0; i < enchantLevel; i++) {
            double offsetX = (Math.random() - 0.5) * 1.5;
            double offsetZ = (Math.random() - 0.5) * 1.5;
            Location spawnLoc = loc.clone().add(offsetX, 0.5, offsetZ);

            Chicken chicken = loc.getWorld().spawn(spawnLoc, Chicken.class);
            chicken.setCustomName(CC.DARK_RED + "恶魔鸡");
            chicken.setCustomNameVisible(true);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (chicken.isDead()) return;

                    Location explodeLoc = chicken.getLocation();
                    explodeLoc.getWorld().playEffect(explodeLoc, Effect.EXTINGUISH, 1);
                    explodeLoc.getWorld().playSound(explodeLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);

                    for (Entity nearby : chicken.getNearbyEntities(2, 2, 2)) {
                        if (nearby instanceof Player target && !PitBotManager.blockedPvp(attacker, target, originInside)) {
                            PlayerUtil.damage(attacker, target, PlayerUtil.DamageType.TRUE, enchantLevel * 1D, true);
                        }
                    }

                    chicken.remove();
                }
            }.runTaskLater(ThePit.getInstance(), 20);
        }
    }

    @Override
    public String getText(int level, @NotNull Player attacker) {
        UUID attackerId = attacker.getUniqueId();
        if (cooldownMap.containsKey(attackerId)) {
            long remaining = cooldownMap.get(attackerId) - System.currentTimeMillis();
            if (remaining > 0) {
                return CC.RED + "冷却中 (" + String.format("%.1f", remaining / 1000.0) + "s)";
            }
        }
        return CC.GREEN + "✔";
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity().getType().equals(EntityType.CHICKEN)) event.setCancelled(true);
    }
}
