package cn.charlotte.pit.quest.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.perk.AbstractPerk;
import cn.charlotte.pit.quest.AbstractQuest;
import cn.charlotte.pit.util.chat.CC;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/21 14:50
 */
public class KeepSilence extends AbstractQuest {
    @Override
    public String getQuestInternalName() {
        return "KeepSilence";
    }

    @Override
    public String getQuestDisplayName() {
        return "缄默不言";
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }

    @Override
    public QuestType getQuestType() {
        return QuestType.KILL;
    }

    @Override
    public int getMinLevel() {
        return 2;
    }

    @Override
    public List<String> getQuestDescription(int level, boolean isNightQuest) {
        return List.of(
                "&f禁止在天赋栏装备&c天赋&f与&c连杀天赋");
    }

    @Override
    public long getDuration(int level) {
        return (level == 8 ? 6 * 60 * 1000 : 5 * 60 * 1000);
    }

    @Override
    public int getTotal(int level) {
        return 10;
    }

    @Override
    public void onActive(Player player, int level) {
        super.onActive(player, level);
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        player.sendMessage(CC.translate("&c已自动为你重置所有天赋栏."));
        for (String perkName : profile.getChosePerk()) {
            if (perkName == null) {
                continue;
            }
            AbstractPerk abstractPerk = ThePit.getInstance().getPerkFactory().getPerkMap().get(perkName);
            if (abstractPerk != null) {
                abstractPerk.onPerkInactive(player);
            }
        }
        profile.setChosePerk(new String[8]);
    }

}
