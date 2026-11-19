package cn.charlotte.pit.perk.type.boosts;

import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.TypedPerk;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@AutoRegister
public class ConglomeratePerk extends TypedPerk<PerksBoostsEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "财团分红";
    }

    @Override
    public Material getIcon() {
        return Material.GOLD_NUGGET;
    }

    @Override
    public double requireCoins() {
        return 20000;
    }

    @Override
    public double requireRenown(int level) {
        return 40;
    }

    @Override
    public int requirePrestige() {
        return 8;
    }

    @Override
    public int requireLevel() {
        return 100;
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
        lines.add("§7击杀和助攻不再获得 §bXP §7.");
        lines.add("§7你获得的 §e20％ §bXP §7将转换为 §6金币 ");
        return lines;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public void onPerkActive(Player player) {

    }

    @Override
    public void onPerkInactive(Player player) {

    }
}
