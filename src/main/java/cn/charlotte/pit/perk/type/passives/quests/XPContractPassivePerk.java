package cn.charlotte.pit.perk.type.passives.quests;

import cn.charlotte.pit.perk.BotKillEffect;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.perk.*;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/31 20:14
 */
@Passive
@BotKillEffect(BotKillEffect.Kind.REWARD)
public class XPContractPassivePerk extends TypedPerk<PerksPassivesEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "经验加成(行动任务)";
    }

    @Override
    public Material getIcon() {
        return Material.EXPERIENCE_BOTTLE;
    }

    @Override
    public double requireCoins() {
        return 0;
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
        return 0;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.PERK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.PRESTIGE;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7每次升级:");
        lines.add("  &7击杀或助攻获得 &b+1.5% 经验值 &7.");
        lines.add("&7此增益会永久保留且升级进度与其他天赋互相独立.");
        return lines;
    }

    @Override
    public int getMaxLevel() {
        return 10;
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
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        int passiveLevel = profile.getLevel(this.getType());
        if (passiveLevel != 0) {
            resourceManager.addExpModifier((1 + 0.015 * passiveLevel), getDisplayName());
        }
    }

    @Override
    public void handlePlayerAssist(int enchantLevel, Player myself, Entity target, double damage, double finalDamage, ResourceManager resourceManager) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        int passiveLevel = profile.getLevel(this.getType());
        if (passiveLevel != 0) {
            resourceManager.addExpModifier((1 + 0.015 * passiveLevel), getDisplayName());
        }
    }
}
