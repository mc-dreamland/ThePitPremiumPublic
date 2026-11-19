package cn.charlotte.pit.perk.type.prestige;

import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.PlayerUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public class OccultDoctrine extends TypedPerk<PerksPrestigeEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "神秘学说";
    }

    @Override
    public Material getIcon() {
        return Material.ENCHANTED_BOOK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.PRESTIGE;
    }

    @Override
    public double requireCoins() {
        return 0;
    }

    @Override
    public double requireRenown(int level) {
        switch (level) {
            case 1, 2 -> {
                return 10;
            }

            case 3 -> {
                return 15;
            }

            case 4 -> {
                return 20;
            }

            case 5 -> {
                return 25;
            }

            default -> {
                return 5;
            }
        }
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
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        int nextLevel = PlayerUtil.getPlayerUnlockedPerkLevel(player, this.getType()) + 1;
        HashMap<Integer, List<String>> effects = new HashMap<>();

        Arrays.asList(1, 2, 3, 4, 5).forEach(i -> {
            List<String> levelEffects = new ArrayList<>();
            levelEffects.add(String.format("  &7击杀玩家获得神秘碎片的概率: &e%.4f%%", i * 0.002D));
            levelEffects.add(String.format("  &7击杀玩家获得盒子残片的概率: &e%.4f%%", i * 0.0005D));
            //            if (i == 5) {
//                levelEffects.add("  &7击杀玩家获得神话之铲的概率: &e0.0005%");
//            }
            effects.put(i, levelEffects);
        });

        if (nextLevel > 1) {
            lines.add("&7当前:");
            lines.addAll(effects.get(nextLevel - 1));
            lines.add("&7下一升级:");
        } else {
            lines.add("&7解锁后:");
        }

        lines.addAll(nextLevel > 5 ? Collections.singletonList("&d没有下一等级了,恭喜!") : effects.get(nextLevel));
        return lines;
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.PERK;
    }

    @Override
    public void onPerkActive(Player player) {

    }

    @Override
    public void onPerkInactive(Player player) {

    }
}