package cn.charlotte.pit.enchantment.type.rare;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.WeaponOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.enchantment.type.ragerare.ThinkOfThePeopleEnchant;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/29 21:23
 */
@WeaponOnly
public class ExecutionerEnchant extends AbstractEnchantment implements IAttackEntity {

    private static final ThinkOfThePeopleEnchant thinkOfThePeople = new ThinkOfThePeopleEnchant();
    private final Cache<UUID, Long> cooldown = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.SECONDS).build();

    @Override
    public String getEnchantName() {
        return "处决";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "executioner";
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
        return "&7攻击玩家时若当次攻击使玩家的生命值低于 &c"
                + (0.5 * enchantLevel + 0.5)
                + "❤ &7,/s&7则该次攻击直接致死";
    }

    @Override
    @PlayerOnly
    public void handleAttackEntity(
            int enchantLevel,
            Player attacker,
            Entity target,
            double damage,
            AtomicDouble finalDamage,
            AtomicDouble boostDamage,
            AtomicBoolean cancel
    ) {
        if (!(target instanceof Player targetPlayer)) {
            return;
        }
        if (target.hasMetadata("executioner_damage")) {
            return;
        }

        // 附近玩家检测 - ThinkOfThePeople 附魔减伤
        for (Player nearbyPlayers : PlayerUtil.getNearbyPlayers(targetPlayer.getLocation(), 10.0)) {
            if (nearbyPlayers.getInventory().getLeggings() != null
                    && thinkOfThePeople.isItemHasEnchant(nearbyPlayers.getInventory().getLeggings())) {
                int level = thinkOfThePeople.getItemEnchantLevel(nearbyPlayers.getInventory().getLeggings());
                if (level > 1) {
                    boostDamage.getAndAdd(level * -0.1 + 0.1);
                }
                return;
            }
        }

        // 判定：若目标在此次攻击后血量低于 (enchantLevel + 1)，直接处决
        if ((targetPlayer.getHealth() - damage * boostDamage.get()) < (0.5 * enchantLevel + 0.5)) {
            Long lastAttack = cooldown.getIfPresent(attacker.getUniqueId());
            if (lastAttack != null && System.currentTimeMillis() < lastAttack) {
                return;
            }
            cooldown.put(attacker.getUniqueId(), System.currentTimeMillis() + 1000);
            finalDamage.getAndAdd(2000.0);
//            cancel.set(true);
//            CombatListener.handlePlayerDeath(targetPlayer, attacker, true);
            target.setMetadata("executioner_damage", new FixedMetadataValue(ThePit.getInstance(), true));
            // 播放村民死亡声音
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_VILLAGER_DEATH, 1f, 0.5f);
            List<Player> nearbyPlayers = PlayerUtil.getNearbyPlayers(attacker.getLocation(), 8.0, 3.0, 8.0);
            // 播放红石矿破碎效果（类似 NMS: PacketPlayOutWorldEvent(2001, ..., 152)）
            target.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
            target.getWorld().playEffect(target.getLocation().clone().add(0.0, -1.0, 0.0), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
            Bukkit.getScheduler().runTaskLaterAsynchronously(ThePit.getInstance(), () -> {
                for (Player player : nearbyPlayers) {
                    player.removeMetadata("executioner_damage", ThePit.getInstance());
                }
            }, 2L);
        }
    }
}
