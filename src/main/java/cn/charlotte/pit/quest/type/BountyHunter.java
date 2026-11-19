package cn.charlotte.pit.quest.type;

import cn.charlotte.pit.quest.AbstractQuest;

import java.util.List;

public class BountyHunter extends AbstractQuest {
    @Override
    public String getQuestInternalName() {
        return "BountyHunter";
    }

    @Override
    public QuestType getQuestType() {
        return QuestType.BOUNTY;
    }

    @Override
    public String getQuestDisplayName() {
        return "赏金猎手";
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public List<String> getQuestDescription(int level, boolean isNightQuest) {
        return List.of(
                "&7夺取 " + level * 300 + "赏金"
        );
    }

    @Override
    public long getDuration(int level) {
        return level == 3 ? 15 * 60 * 1000 : 10 * 60 * 1000;
    }

    @Override
    public int getTotal(int level) {
        return level * 300;
    }
}
