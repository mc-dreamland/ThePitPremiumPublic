package cn.charlotte.pit.perk.type.boosts.based;

import cn.charlotte.pit.perk.BotKillEffect;

import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.TypedPerk;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bukkit.Material.ARROW;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/1 22:13
 */
@BotKillEffect(BotKillEffect.Kind.REWARD)
public class SpammerPerk extends TypedPerk<PerksBoostsEnum> {
    private final Map<UUID, UUID> spammerShotIdentifier = new HashMap<>();

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "万箭齐发";
    }

    @Override
    public Material getIcon() {
        return Material.BOW;
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
        return 40;
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
        lines.add("§7击中目标后获得 §f3 箭矢§7.");
        lines.add("");
        lines.add("§7当你击败目标后");
        lines.add("§7获得 §63x 基础金币 §7奖励");
        lines.add("");
        lines.add("§7助攻时获得 §6+2g §7.");
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

        resourceManager.addGoldModifier(3, getDisplayName());
        spammerShotIdentifier.remove(killer.getUniqueId());
    }

    @Override
    public void handleShootEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (attacker.getInventory().getItemInMainHand().getType() != Material.FISHING_ROD) {
            attacker.getInventory().addItem(new ItemStack(ARROW, 3));
        }
        spammerShotIdentifier.put(attacker.getUniqueId(), target.getUniqueId());
    }

    @Override
    public void handlePlayerAssist(int enchantLevel, Player myself, Entity target, double damage, double finalDamage, ResourceManager resourceManager) {
        resourceManager.addGold(2, "万箭齐发");
    }
}
