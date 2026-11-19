package cn.charlotte.pit.medal.impl.challenge;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.medal.AbstractMedal;

public class goldenFishMedal extends AbstractMedal {

    @Override
    public String getInternalName() {
        return "golden_fish_medal";
    }

    @Override
    public String getDisplayName(int level) {
        return "黄金鱼";
    }

    @Override
    public String getRequirementDescription(int level) {
        return "从水池中吊出一条&e[黄金鱼]";
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public int getProgressRequirement(int level) {
        return 1;
    }

    @Override
    public int getRarity(int level) {
        return 5;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public void handleProfileLoaded(PlayerProfile profile) {

    }
}
