package cn.charlotte.pit.quest.type;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.quest.AbstractQuest;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/2/2 17:12
 */
public class LowHealth extends AbstractQuest {
    @Override
    public String getQuestInternalName() {
        return "low_health";
    }

    @Override
    public String getQuestDisplayName() {
        return "侵蚀";
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
    public QuestType getQuestType() {
        return QuestType.KILL;
    }

    @Override
    public List<String> getQuestDescription(int level, boolean isNightQuest) {
        return List.of("&f最大生命值 &c-" + (level == 3 ? level * 1.5 : level * 2) + "❤");
    }

    @Override
    public long getDuration(int level) {
        return (level == 3 ? 10 * 60 * 1000 : 5 * 60 * 1000);
    }

    @Override
    public int getTotal(int level) {
        return 10;
    }

    @Override
    public void onActive(Player player, int level) {
        super.onActive(player, level);
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        profile.getExtraMaxHealth().put(getQuestInternalName(), -(level == 3 ? level * 3.5 : level * 4.0));
        player.setMaxHealth(profile.getMaxHealth());
    }

    @Override
    public void onInactive(Player player, int level) {
        super.onInactive(player, level);
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        profile.getExtraMaxHealth().remove(getQuestInternalName());
        player.setMaxHealth(profile.getMaxHealth());
    }
}
