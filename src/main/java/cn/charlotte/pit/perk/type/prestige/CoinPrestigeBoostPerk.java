package cn.charlotte.pit.perk.type.prestige;

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
 * @Created_In: 2021/1/12 21:36
 */
@Passive
@BotKillEffect(BotKillEffect.Kind.REWARD)
public class CoinPrestigeBoostPerk extends TypedPerk<PerksPrestigeEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "硬币获取提升";
    }

    @Override
    public Material getIcon() {
        return Material.GOLD_NUGGET;
    }

    @Override
    public double requireCoins() {
        return 0;
    }

    @Override
    public double requireRenown(int level) {
        if (level == 1) {
            return 5;
        }
        return 10;
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
    public int requirePrestige() {
        return 1;
    }

    @Override
    public int requireLevel() {
        return 0;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7每次升级:");
        lines.add("  &7击杀获得 &6+2.5% 硬币 &7.");
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        int data = profile.getLevel(PerksPrestigeEnum.RENOWN_GOLD_BOOST);
        if (data > 0) {
            lines.add("");
            lines.add("&7当前加成: &6" + data * 2.5 + "%");
        }
        return lines;
    }

    @Override
    public int getMaxLevel() {
        return 5;
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
        int data = profile.getLevel(PerksPrestigeEnum.RENOWN_GOLD_BOOST);
        if (data > 0) {
//            coins.set((1 + 0.025 * data.getLevel()) * coins.get());
            resourceManager.addGoldModifier((1 + 0.025 * data), getDisplayName());
        }
    }

    @Override
    @PlayerOnly
    public void handlePlayerAssist(int enchantLevel, Player myself, Entity target, double damage, double finalDamage, ResourceManager resourceManager) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        int passiveLevel = profile.getLevel(this.getType());
        if (passiveLevel != 0) {
            resourceManager.addExpModifier((1 + 0.015 * passiveLevel), getDisplayName());
        }
    }
}
