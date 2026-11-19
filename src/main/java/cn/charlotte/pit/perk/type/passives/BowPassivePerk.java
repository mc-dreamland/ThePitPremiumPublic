package cn.charlotte.pit.perk.type.passives;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.BotCombatEffect;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksPassivesEnum;
import cn.charlotte.pit.perk.TypedPerk;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/7 18:11
 */
public class BowPassivePerk extends TypedPerk<PerksPassivesEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "远程伤害提升";
    }

    @Override
    public Material getIcon() {
        return Material.BOW;
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
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        lines.add("&7每次升级:");
        lines.add("  &7弓箭攻击造成的伤害 &c+3% &7.");
        int passiveLevel = profile.getLevel(this.getType());
        lines.add("  &7当前: &c+" + 3 * passiveLevel + "% 弓箭伤害");
        return lines;
    }

    @Override
    public int getMaxLevel() {
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
    public void onPerkActive(Player player) {

    }

    @Override
    public void onPerkInactive(Player player) {

    }

    @Override
    @PlayerOnly
    @BotCombatEffect
    public void handleShootEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId());
        int passiveLevel = profile.getLevel(this.getType());
        if (passiveLevel != 0) {
            boostDamage.set(boostDamage.get() + 0.03 * passiveLevel);
        }
    }
}
