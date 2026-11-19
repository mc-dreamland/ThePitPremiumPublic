package cn.charlotte.pit.enchantment.type.normal;

import cn.charlotte.pit.bot.PitBotManager;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.item.BowOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.parm.listener.IPlayerShootEntity;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.RomanUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.time.TimeUtil;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoRegister
@BowOnly
public class BowComboEnchant extends AbstractEnchantment implements IPlayerShootEntity, IActionDisplayEnchant, Listener {
    public static final ConcurrentHashMap<UUID, Long> hitCheck = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Integer> combo = new ConcurrentHashMap<>();

    @Override
    public String getEnchantName() {
        return "神射手";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "bow_combo_enchant";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.NORMAL;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7连续 &e" + (enchantLevel >= 2 ? 2 : 3) + " &7次箭矢射出并命中为自身添加 &b速度 " + RomanUtil.convert(enchantLevel + 1) + " &f(" + TimeUtil.millisToTimer(4000) + ")";
    }

    @Override
    public void handleShootEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (combo.getOrDefault(attacker.getUniqueId(), 0) >= (enchantLevel >= 2 ? 2 : 3)) {
                    combo.put(attacker.getUniqueId(), 0);

                    PlayerUtil.addEffect(attacker, new PotionEffect(PotionEffectType.SPEED, 20 * 4, enchantLevel, true));
                }
            }
        }.runTaskLater(ThePit.getInstance(), 2L);
    }

    @EventHandler
    public void onBowShot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        final org.bukkit.inventory.ItemStack itemInHand = player.getInventory().getItemInMainHand();
        final int level = this.getItemEnchantLevel(itemInHand);
        if (level == -1) {
            if (profile.isDebug()) {
                player.sendMessage("神射手|onBowShot|level == -1");
            }
            return;
        }
        if (profile.isDebug()) {
            player.sendMessage("神射手|onBowShot|True");
        }
        event.getProjectile().setMetadata("bow_combo_enchant_uuid", new FixedMetadataValue(ThePit.getInstance(), UUID.randomUUID().toString()));
    }

    @EventHandler
    public void onBowHit(ProjectileHitEvent event) {
        if (event.getEntity().hasMetadata("bow_combo_enchant_uuid") && event.getEntity().getShooter() != null) {
            if (event.getEntity().getShooter() instanceof Player player) {
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                if (profile.isDebug()) {
                    player.sendMessage("神射手|onBowHit|True");
                }
                UUID uuid = UUID.fromString(event.getEntity().getMetadata("bow_combo_enchant_uuid").getFirst().asString());
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        combo.compute(player.getUniqueId(), (id, count) -> {
                            if (hitCheck.containsKey(uuid)) {
                                hitCheck.remove(uuid);
                                return (count == null) ? 1 : count + 1;
                            }
                            return 0;
                        });
                    }
                }.runTaskLater(ThePit.getInstance(), 1L);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (PitBotManager.skipCustomCombat(event)) return;
        if (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player player) {
            if (event.getDamager().hasMetadata("bow_combo_enchant_uuid")) {
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                if (profile.isDebug()) {
                    player.sendMessage("神射手|onPlayerDamagePlayer|True");
                }
                UUID uuid = UUID.fromString(event.getDamager().getMetadata("bow_combo_enchant_uuid").getFirst().asString());
                hitCheck.put(uuid, System.currentTimeMillis());
            }
        }
    }

    @Override
    public String getText(int level, Player player) {
        return "&e&l" + combo.getOrDefault(player.getUniqueId(), 0) + "/" + (level >= 2 ? 2 : 3);
    }
}
