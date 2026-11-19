package cn.charlotte.pit.perk.type.boosts.based;

import cn.charlotte.pit.perk.BotCombatEffect;
import cn.charlotte.pit.perk.BotKillEffect;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.TypedPerk;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 疑似已移除
 */
@BotKillEffect(BotKillEffect.Kind.REWARD)
public class GoldMinerPerk extends TypedPerk<PerksBoostsEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "灵活战术";
    }

    @Override
    public Material getIcon() {
        return Material.STRING;
    }

    @Override
    public double requireCoins() {
        return 4000;
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
        return 45;
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
        lines.add("&7助攻获得的硬币 &6+2 &7.");
        lines.add("&7箭矢命中其他玩家后,该玩家死亡时");
        lines.add("&7你参与击杀/助攻获得的硬币 &6+50% &7.");
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
    @PlayerOnly
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        if (myself.hasMetadata("mixed_combat_" + myself.getUniqueId())) {
            resourceManager.addGoldModifier(1.5, getDisplayName());
            myself.removeMetadata("mixed_combat_" + myself.getUniqueId(), ThePit.getInstance());
        }
    }

    @Override
    @PlayerOnly
    public void handlePlayerAssist(int enchantLevel, Player myself, Entity target, double damage, double finalDamage, ResourceManager resourceManager) {
        if (myself.hasMetadata("mixed_combat_" + myself.getUniqueId())) {
            resourceManager.addGoldModifier(1.5, getDisplayName());
            myself.removeMetadata("mixed_combat_" + myself.getUniqueId(), ThePit.getInstance());
        }
        resourceManager.addGold(2, getDisplayName());
    }

    @Override
    @PlayerOnly
    @BotCombatEffect
    public void handleShootEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        target.setMetadata("mixed_combat_" + attacker.getUniqueId(), new FixedMetadataValue(ThePit.getInstance(), true));
    }
}
