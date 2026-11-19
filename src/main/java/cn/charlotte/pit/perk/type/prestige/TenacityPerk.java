package cn.charlotte.pit.perk.type.prestige;

import cn.charlotte.pit.perk.BotKillEffect;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.perk.*;
import cn.charlotte.pit.util.PlayerUtil;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/10 15:13
 */
@Passive
@BotKillEffect(BotKillEffect.Kind.KILL_EFFECT)
public class TenacityPerk extends TypedPerk<PerksPrestigeEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "坚韧";
    }

    @Override
    public Material getIcon() {
        return Material.SPIDER_EYE;
    }

    @Override
    public double requireCoins() {
        return 0;
    }

    @Override
    public double requireRenown(int level) {
        return switch (level) {
            case 1 -> 10;
            case 2 -> 50;
            default -> 999;
        };
    }

    @Override
    public int requirePrestige() {
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
    public int requireLevel() {
        return 0;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        lines.add("&7每次升级:");
        lines.add("  &7击杀恢复自身 &c+0.5❤ &7生命.");
        int data = profile.getLevel(getType());
        if (data > 0) {
            lines.add("");
            lines.add("&7当前加成: &6" + data);
        }
        return lines;
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }

    @Override
    public void onPerkActive(Player player) {

    }

    @Override
    public void onPerkInactive(Player player) {

    }

    @Override
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        int data = profile.getLevel(getType());
        if (data > 0) {
            PlayerUtil.heal(myself, data);
        }
    }
}
