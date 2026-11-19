package cn.charlotte.pit.perk.type.boosts.based;

import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.TypedPerk;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/1 22:13
 */
public class StreakerPerk extends TypedPerk<PerksBoostsEnum> {
    private final Map<UUID, UUID> spammerShotIdentifier = new HashMap<>();

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "嗜血者";
    }

    @Override
    public Material getIcon() {
        return Material.WHEAT;
    }

    @Override
    public double requireCoins() {
        return 8000;
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
        return 50;
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
        lines.add("§7获得 §e3x §b连杀经验 §7加成.");
        lines.add("§720连杀后每10连杀增加 §b1点连杀基础经验 §7加成.");
        return lines;
    }

    @Override
    public int getMaxLevel() {
        return 0;
    }

    @Override
    public void onPerkActive(Player player) {
    }

    @Override
    public void onPerkInactive(Player player) {

    }

    @Override
    public void handlePlayerKilled(int enchantLevel, Player killer, Entity target, ResourceManager resourceManager) {
        if (!spammerShotIdentifier.containsKey(killer.getUniqueId())) return;
        if (spammerShotIdentifier.get(killer.getUniqueId()) != target.getUniqueId()) return;


        spammerShotIdentifier.remove(killer.getUniqueId());
    }

    @Override
    public void handleShootEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        spammerShotIdentifier.put(attacker.getUniqueId(), target.getUniqueId());
    }

    @Override
    public void handlePlayerAssist(int enchantLevel, Player myself, Entity target, double damage, double finalDamage, ResourceManager resourceManager) {

    }
}
