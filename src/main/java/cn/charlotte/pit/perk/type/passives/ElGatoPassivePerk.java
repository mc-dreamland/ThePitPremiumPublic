package cn.charlotte.pit.perk.type.passives;

import cn.charlotte.pit.perk.BotKillEffect;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksPassivesEnum;
import cn.charlotte.pit.perk.TypedPerk;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/8 10:15
 */
@BotKillEffect(BotKillEffect.Kind.REWARD)
public class ElGatoPassivePerk extends TypedPerk<PerksPassivesEnum> {

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
        return Material.CAKE;
    }

    @Override
    public double requireCoins() {
        return 0;
    }

    @Override
    //renown = durability of icon when prestige = 0
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
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        lines.add("&7下一升级:");
        int passiveLevel = profile.getLevel(this.getType());
        lines.add("  &d前 " + (passiveLevel == 0 ? 1 : Math.min(5, passiveLevel + 1)) + " 个击杀 &7额外 &6+5硬币 &b+5经验值 &7.");
        lines.add("  &7当前: &d前 " + passiveLevel + " 个击杀");
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
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        int equippedLevel = getEquippedLevel(profile);
        if (equippedLevel > 0) {
            if (profile.getStreakKills() <= equippedLevel) {
                resourceManager.addGold(5, "先发制人");
                resourceManager.addExp(5, "先发制人");
            }
        }
    }
}
