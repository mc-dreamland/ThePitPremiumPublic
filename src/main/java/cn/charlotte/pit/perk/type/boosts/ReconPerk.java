package cn.charlotte.pit.perk.type.boosts;

import cn.charlotte.pit.bot.BotReward;
import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.BotCombatEffect;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.level.LevelUtil;
import com.google.common.util.concurrent.AtomicDouble;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReconPerk extends TypedPerk<PerksBoostsEnum> {


    private final Map<UUID, ShooterData> bowHitsMap = new HashMap<>();

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "侦察兵";
    }

    @Override
    public Material getIcon() {
        return Material.ENDER_EYE;
    }

    @Override
    public double requireCoins() {
        return 6000;
    }

    @Override
    public double requireRenown(int level) {
        return 20;
    }

    @Override
    public int requirePrestige() {
        return 7;
    }

    @Override
    public int requireLevel() {
        return 60;
    }

    @Override
    public List<String> getDescription(Player player) {
        return List.of(
                "&7每第 &e5 &7次箭矢命中敌人造成的伤害 &c+50% &7并额外获得 &b40 经验值 &7.",
                "&c只有在射击不同的敌人时才会命中次数,每2次触发重置次数"
        );
    }

    @Override
    public int getMaxLevel() {
        return 1;
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
    public void onPerkActive(Player player) {
    }

    @Override
    public void onPerkInactive(Player player) {
    }

    @Override
    @PlayerOnly
    @BotCombatEffect
    public void handleShootEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (CombatListener.isNight()) {
            return;
        }

        boolean b = onBowHit(attacker.getUniqueId(), target.getUniqueId());
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId());
        if (b) {
            boostDamage.getAndAdd(0.5);
            double experience = PitBotManager.isBot(target)
                    ? BotReward.experienceWithinLevelCap(40,
                    LevelUtil.getLevelTotalExperience(profile.getPrestige(), 120) - profile.getExperience()) : 40;
            profile.setExperience(profile.getExperience() + experience);
        }
    }

    @Override
    public void handleQuit(int enchantLevel, Player myself) {
        UUID uniqueId = myself.getUniqueId();
        bowHitsMap.remove(uniqueId);
    }


    public boolean onBowHit(UUID shooter, UUID target) {
        ShooterData data = this.bowHitsMap.get(shooter);

        if (data == null) {
            this.bowHitsMap.put(shooter, new ShooterData(1, target));
            return false;
        }

        return data.addAmount(target);

    }


    // ====== 新的射手数据类 ======
    @Getter
    private static class ShooterData {

        int hitCount = 0;
        Set<UUID> uuidSet;

        public ShooterData() {
            this.hitCount = 0;
            this.uuidSet = new HashSet<>(10);
        }

        public ShooterData(int hitCount, UUID uuid) {
            this.hitCount = hitCount;
            this.uuidSet = new HashSet<>(List.of(uuid));
        }

        public boolean addAmount(UUID uuid) {
            if (this.uuidSet.add(uuid)) {
                this.hitCount += 1;
                if (this.hitCount >= 5) {
                    this.clear(this.uuidSet.size() >= 10);
                    return true;
                }
            }
            return false;
        }

        private void clear(boolean b) {
            this.hitCount = 0;
            if (b) this.uuidSet.clear();
        }
    }
}
