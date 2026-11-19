package cn.charlotte.pit.perk.type.boosts;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.BotCombatEffect;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.PlayerUtil;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: Misoryan
 * @Created_In: 2021/2/21 19:10
 */
public class FirstStrikePerk extends TypedPerk<PerksBoostsEnum> {


    private static final HashMap<UUID, List<UUID>> firstStrikes = new HashMap<>();

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "先发制人";
    }

    @Override
    public Material getIcon() {
        return Material.COOKED_CHICKEN;
    }

    @Override
    public double requireCoins() {
        return 8000;
    }

    @Override
    public double requireRenown(int level) {
        return 25;
    }

    @Override
    public int requirePrestige() {
        return 5;
    }

    @Override
    public int requireLevel() {
        return 80;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7对其他玩家的首次攻击造成伤害 &c+35% &7,");
        lines.add("&7且额外获得 &b速度 I &f(00:05)");
        lines.add("&7自身死亡时重置首次攻击统计.");
        return lines;
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
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        applyFirstStrike(attacker, target, boostDamage);
    }

    @Override
    @PlayerOnly
    @BotCombatEffect
    public void handleShootEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        applyFirstStrike(attacker, target, boostDamage);
    }

    private void applyFirstStrike(Player attacker, Entity target, AtomicDouble boostDamage) {
        if (PitBotManager.isBot(target)) {
            // Store Bot targets on the short-lived entity, not in an ever-growing player UUID list.
            String key = "first_strike_" + attacker.getUniqueId();
            if (target.hasMetadata(key)) return;
            target.setMetadata(key, new FixedMetadataValue(ThePit.getInstance(), true));
        } else {
            firstStrikes.putIfAbsent(attacker.getUniqueId(), new ArrayList<>());
            if (firstStrikes.get(attacker.getUniqueId()).contains(target.getUniqueId())) return;
            firstStrikes.get(attacker.getUniqueId()).add(target.getUniqueId());
        }
        boostDamage.getAndAdd(0.35);
        PlayerUtil.addEffect(attacker, new PotionEffect(PotionEffectType.SPEED, 20 * 5, 0));
    }

    @Override
    public void handleTick(int enchantLevel, Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (!profile.isInArena() && firstStrikes.get(player.getUniqueId()) != null) {
            firstStrikes.get(player.getUniqueId()).clear();
        }
    }

    @Override
    public int loopTick(int enchantLevel) {
        return 20 * 3;
    }
}
