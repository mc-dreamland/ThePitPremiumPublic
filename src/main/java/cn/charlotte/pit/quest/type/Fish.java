package cn.charlotte.pit.quest.type;

import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.quest.AbstractQuest;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.List;


@AutoRegister
public class Fish extends AbstractQuest implements Listener {
    @Override
    public String getQuestInternalName() {
        return "Fish";
    }

    @Override
    public QuestType getQuestType() {
        return QuestType.FISHING;
    }

    @Override
    public String getQuestDisplayName() {
        return "钓鱼";
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
        return Collections.singletonList("&f钓到" + level * 3 + "条任意物品");
    }

    @Override
    public long getDuration(int level) {
        return (level == 3 ? 15 * 60 * 1000 : 10 * 60 * 1000);
    }

    @Override
    public int getTotal(int level) {
        return level * 3;
    }

    @Override
    public void onActive(Player player, int level) {
        super.onActive(player, level);
    }

    @Override
    public void onInactive(Player player, int level) {
        super.onInactive(player, level);
    }
}
