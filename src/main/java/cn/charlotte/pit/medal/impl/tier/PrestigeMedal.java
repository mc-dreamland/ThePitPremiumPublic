package cn.charlotte.pit.medal.impl.tier;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.medal.AbstractMedal;
import cn.charlotte.pit.util.chat.RomanUtil;
import cn.charlotte.pit.util.menu.builder.ButtonBuilder;

/**
 * @Creator Misoryan
 * @Date 2021/6/8 20:21
 */
public class PrestigeMedal extends AbstractMedal {

    @Override
    public String getInternalName() {
        return "STATUS_PRESTIGE";
    }

    @Override
    public String getDisplayName(int level) {
        return "千锤百炼";
    }

    @Override
    public String getRequirementDescription(int level) {
        return "达到精通" + RomanUtil.convert(getProgressRequirement(level));
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public int getProgressRequirement(int level) {
        return switch (level) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 5;
            case 4 -> 7;
            case 5 -> 12;
            case 6 -> 15;
            case 7 -> 20;
            case 8 -> 25;
            case 9 -> 30;
            case 10 -> 35;
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
        setProgress(profile, profile.getPrestige());
    }

    @Override
    public ButtonBuilder getButton(PlayerProfile profile, int level, int progress, boolean locked, int click) {
        return super.getButton(profile, level, progress, locked, click);
    }
    //    @Override
//    public Button getButton(PlayerProfile profile, int progress, boolean locked) {
//        return super.getButton(profile, progress, locked);
//    }
}
