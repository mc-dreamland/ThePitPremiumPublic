package cn.charlotte.pit.perk.type.prestige;

import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.perk.TypedPerk;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/30 18:13
 */
public class ExtraEnderchestPerk extends TypedPerk<PerksPrestigeEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "末影箱扩容";
    }

    @Override
    public Material getIcon() {
        return Material.ENDER_CHEST;
    }

    @Override
    public double requireCoins() {
        return 0;
    }

    @Override
    public double requireRenown(int level) {
        return switch (level) {
            case 1 -> 10;
            case 2 -> 20;
            default -> 50;
        };
    }

    @Override
    public int requirePrestige() {
        return 4;
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
    public int requireLevel() {
        return 0;
    }

    @Override
    public List<String> getDescription(Player player) {
        return Arrays.asList("&7每次升级:", "  &7为末影箱扩容 &d9 &7个槽位.");
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public void onPerkActive(Player player) {
    }

    @Override
    public void onPerkInactive(Player player) {

    }
}
