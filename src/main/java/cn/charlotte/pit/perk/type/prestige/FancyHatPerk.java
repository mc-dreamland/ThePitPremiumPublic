package cn.charlotte.pit.perk.type.prestige;

import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.perk.TypedPerk;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/22 18:13
 */
public class FancyHatPerk extends TypedPerk<PerksPrestigeEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "好看的头盔";
    }

    @Override
    public Material getIcon() {
        return Material.LEATHER_HELMET;
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
        return 5;
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
        return List.of("§7解锁后可获得 永久 皮革帽子&7(护甲值=锁链头盔)", "§7可在商店 消耗 §62000金币 §7自定义染色§c(需要天坑乱斗增值包)");
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public void onPerkActive(Player player) {
        player.getInventory().addItem(PitItems.FANCY_HAT_HELMET.toItemStack());
    }

    @Override
    public void onPerkInactive(Player player) {

    }
}
