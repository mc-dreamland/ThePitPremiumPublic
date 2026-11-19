package cn.charlotte.pit.perk.type.prestige;

import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PureRagePerk extends TypedPerk<PerksPrestigeEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "极度愤怒";
    }

    @Override
    public Material getIcon() {
        return Material.LEATHER_LEGGINGS;
    }

    @Override
    public ItemStack getIconWithNameAndLore(String name,
                                            List<String> lore,
                                            int durability,
                                            int amount) {
        return new ItemBuilder(Material.LEATHER_LEGGINGS)
                .name(name)
                .lore(lore)
                .durability(durability)
                .amount(1)
                .setLetherColor(MythicColor.RAGE.getLeatherColor())  // 若枚举字段是 public，可改为 .leatherColor
                .build();
    }

    @Override
    public double requireCoins() {
        return 0.0;
    }

    @Override
    public double requireRenown(int level) {
        return 40.0;
    }

    @Override
    public int requirePrestige() {
        return 13;
    }

    @Override
    public int requireLevel() {
        return 0;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7解锁合成&4 暴怒之甲&7.");
        lines.add("&7对抗特定附魔的好助手");
        return lines;
    }

    @Override
    public int getMaxLevel() {
        return 1;
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
        // 无额外逻辑
    }

    @Override
    public void onPerkInactive(Player player) {
        // 无额外逻辑
    }
}
