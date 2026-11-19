package cn.charlotte.pit.perk.type.shop;

import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksShopEnum;
import cn.charlotte.pit.perk.TypedPerk;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/2/5 0:08
 */
public class CombatSpadePerk extends TypedPerk<PerksShopEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "商店升级: 洛阳铲";
    }

    @Override
    public Material getIcon() {
        return Material.DIAMOND_SHOVEL;
    }

    @Override
    public double requireCoins() {
        return 750;
    }

    @Override
    public double requireRenown(int level) {
        return 20;
    }

    @Override
    public int requirePrestige() {
        return 3;
    }

    @Override
    public int requireLevel() {
        return 0;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7允许你在商店中购买洛阳铲.");
        lines.add("&7洛阳铲有着+6的基础攻击力,");
        lines.add("&7且攻击目标每穿着一件钻石装备,");
        lines.add("&7洛阳铲的基础攻击力+1.");
        return lines;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.PERK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.SHOP;
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
