package cn.charlotte.pit.perk.type.boosts.based;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.PlayerUtil;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/1 22:13
 */
public class BonkPerk extends TypedPerk<PerksBoostsEnum> {

    private final Map<UUID, Set<UUID>> bonkMap = new HashMap<>();
    private final Map<UUID, Set<UUID>> bonkMapB = new HashMap<>();
    private final List<UUID> selfCoolDown = new ArrayList<>();

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "耐揍";
    }

    @Override
    public Material getIcon() {
        return Material.ANVIL;
    }

    @Override
    public double requireCoins() {
        return 2000;
    }

    @Override
    public double requireRenown(int level) {
        return 0;
    }

    @Override
    public int requirePrestige() {
        return 0;
    }

    @Override
    public int requireLevel() {
        return 35;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.PERK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.BOOST;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("§7你受到的首次攻击将被阻挡");
        lines.add("§7并获得效果");
        lines.add("  §9伤害吸收 I §7(1秒).");
        return lines;
    }

    public boolean canBonk(Player target, Player attacker) {
        if (selfCoolDown.contains(target.getUniqueId())) {
            return false;
        }
        return (bonkMap.get(target.getUniqueId()) == null || !bonkMap.get(target.getUniqueId()).contains(attacker.getUniqueId()));
    }

    public boolean isInBonkCoolDown(Player target, Player attacker) {
        return bonkMapB.get(target.getUniqueId()).contains(attacker.getUniqueId());
    }

    @Override
    public int getMaxLevel() {
        return 0;
    }

    @Override
    public void onPerkActive(Player player) {
        bonkMap.put(player.getUniqueId(), new HashSet<>());
        bonkMapB.put(player.getUniqueId(), new HashSet<>());
        selfCoolDown.remove(player.getUniqueId());
    }

    @Override
    public void onPerkInactive(Player player) {
        UUID uniqueId = player.getUniqueId();
        bonkMap.remove(uniqueId);
        bonkMapB.remove(uniqueId);
        selfCoolDown.remove(uniqueId);

    }

    @Override
    public void handlePlayerDamaged(int enchantLevel, Player target, Entity attacker, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (!(attacker instanceof Player attackerPlayer)) {
            return;
        }
        if (isInBonkCoolDown(target, attackerPlayer)) {
            finalDamage.set(0);
            boostDamage.set(0);
            cancel.set(true);
            return;
        }
        if (!canBonk(target, attackerPlayer)) {
            return;
        }

        finalDamage.set(0);
        boostDamage.set(0);
        cancel.set(true);


        UUID attackerUniqueId = attackerPlayer.getUniqueId();
        UUID targetUniqueId = target.getUniqueId();

        for (Entity entity : target.getNearbyEntities(24, 16, 24)) {
            if (entity.getUniqueId().equals(targetUniqueId)) continue;
            if (entity instanceof Player player) {
                player.playSound(target.getLocation(), Sound.BLOCK_ANVIL_FALL, 1, 1);
                player.spawnParticle(Particle.EXPLOSION_EMITTER, target.getLocation(), 1, 0, 0, 0, 0);
            }
        }
        bonkMap.get(targetUniqueId).add(attackerUniqueId);
        bonkMapB.get(targetUniqueId).add(attackerUniqueId);
        PlayerUtil.addEffect(target, new PotionEffect(PotionEffectType.RESISTANCE, 20, 1, false, false, true));
        selfCoolDown.add(targetUniqueId);

        new BukkitRunnable() {
            @Override
            public void run() {
                bonkMapB.get(targetUniqueId).remove(attackerUniqueId);
            }
        }.runTaskLater(ThePit.getInstance(), 10);
    }

    @Override
    public void handleJoin(int enchantLevel, Player myself) {
        bonkMap.put(myself.getUniqueId(), new HashSet<>());
        bonkMapB.put(myself.getUniqueId(), new HashSet<>());
    }

    @Override
    public void handleQuit(int enchantLevel, Player myself) {
        UUID uniqueId = myself.getUniqueId();
        bonkMap.remove(uniqueId);
        bonkMapB.remove(uniqueId);
        selfCoolDown.remove(uniqueId);

    }

    @Override
    public void handleRespawn(int enchantLevel, Player player) {
        if (bonkMap.get(player.getUniqueId()) != null) bonkMap.get(player.getUniqueId()).clear();
        if (bonkMapB.get(player.getUniqueId()) != null) bonkMapB.get(player.getUniqueId()).clear();
        selfCoolDown.remove(player.getUniqueId());
    }
}
