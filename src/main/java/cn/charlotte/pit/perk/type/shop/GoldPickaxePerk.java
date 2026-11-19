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
 * @Created_In: 2021/2/5 0:06
 */
public class GoldPickaxePerk extends TypedPerk<PerksShopEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "商店升级: 金稿";
    }

    @Override
    public Material getIcon() {
        return Material.GOLDEN_PICKAXE;
    }

    @Override
    public double requireCoins() {
        return 0;
    }

    @Override
    public double requireRenown(int level) {
        return 10;
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
    public PerkType getPerkType() {
        return PerkType.PERK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.SHOP;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7允许你在商店中购买金稿 (效率 VII).");
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
