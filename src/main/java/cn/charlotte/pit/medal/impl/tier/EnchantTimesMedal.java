package cn.charlotte.pit.medal.impl.tier;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitPlayerEnchantEvent;
import cn.charlotte.pit.medal.AbstractMedal;
import cn.charlotte.pit.parm.AutoRegister;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.text.DecimalFormat;

/**
 * @Creator Misoryan
 * @Date 2021/6/10 19:04
 */
@AutoRegister
public class EnchantTimesMedal extends AbstractMedal implements Listener {
    private final DecimalFormat df = new DecimalFormat(",###,###,###,###");

    @Override
    public String getInternalName() {
        return "STATUS_ENCHANT_TIMES";
    }

    @Override
    public String getDisplayName(int level) {
        return "神话附魔师";
    }

    @Override
    public String getRequirementDescription(int level) {
        return "附魔" + df.format(getProgressRequirement(level)) + " 次神话物品";
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public int getProgressRequirement(int level) {
        return switch (level) {
            case 1 -> 10;
            case 2 -> 50;
            case 3 -> 100;
            case 4 -> 250;
            case 5 -> 1000;
            default -> -1;
        };
    }

    @Override
    public int getRarity(int level) {
        return level;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public void handleProfileLoaded(PlayerProfile profile) {
        setProgress(profile, profile.getEnchantment().values().stream().mapToInt(Integer::intValue).sum());
    }

}
