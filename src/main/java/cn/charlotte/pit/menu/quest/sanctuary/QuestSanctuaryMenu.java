package cn.charlotte.pit.menu.quest.sanctuary;

import cn.charlotte.pit.menu.quest.sanctuary.button.ActionGiftBox;
import cn.charlotte.pit.menu.quest.sanctuary.button.FunkyFeatherSanctuaryButton;
import cn.charlotte.pit.menu.quest.sanctuary.button.PermanentBoostButton;
import cn.charlotte.pit.perk.PerksPassivesEnum;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/31 19:25
 */
public class QuestSanctuaryMenu extends Menu {
    @Override
    public String getTitle(Player player) {
        return "兑换所";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        buttons.put(10, new PermanentBoostButton(PerksPassivesEnum.GOLD_CONTRACT.getPerk(), 200));
        buttons.put(11, new PermanentBoostButton(PerksPassivesEnum.XP_CONTRACT.getPerk(), 150));
        buttons.put(12, new ActionGiftBox());
        /*        buttons.put(12, new MusicalRuneSanctuaryButton());*/
        //buttons.put(12, new CactusQuestSanctuaryButton());
//        buttons.put(13, new FunkyFeatherSanctuaryButton());
        return buttons;
    }

    @Override
    public int getSize() {
        return 3 * 9;
    }
}
