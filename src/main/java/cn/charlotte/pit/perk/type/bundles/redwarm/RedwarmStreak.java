package cn.charlotte.pit.perk.type.bundles.redwarm;

import cn.charlotte.pit.perk.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RedwarmStreak extends TypedPerk<PerksBundlesEnum> implements MegaStreak {
    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "&c红温";
    }

    @Override
    public Material getIcon() {
        return Material.REDSTONE;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.MEGA_STREAK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.STREAK;
    }

    @Override
    public double requireCoins() {
        return 20000;
    }

    @Override
    public double requireRenown(int level) {
        return 100;
    }

    @Override
    public int requirePrestige() {
        return 12;
    }

    @Override
    public int requireLevel() {
        return 100;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> list = new ArrayList<>();
        list.add("&7激活要求连杀数: &c50 连杀");
        list.add(" ");
        list.add("&7激活后:");
        list.add("  &a▶ &7获得永久&b速度&7I.");
        list.add("  &a▶ &7攻击造成的伤害 &c+10%.");
        list.add("  &a▶ &7当血量低于 &c3❤ &&时攻击百分百造成暴击.");
        list.add("  &a▶ &7&d神话装备&7掉落倍率增加 50%.");
        list.add("  &c▶ &7激活后每击杀5名玩家,受到的伤害 &c+0.1颗心&7.");
        list.add(" ");
        return list;
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
    public int getStreakNeed() {
        return 50;
    }

    @Override
    public List<String> getDescriptionInRenownShop(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7包括以下连杀天赋:");
        lines.add("&8- &a十字弩");
        lines.add("&8- &a");
        lines.add(" ");
        lines.add("&7超级连杀天赋: &c红温");
        return lines;
    }
}
