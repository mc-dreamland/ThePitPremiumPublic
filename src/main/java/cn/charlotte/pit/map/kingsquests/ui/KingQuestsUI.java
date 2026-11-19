package cn.charlotte.pit.map.kingsquests.ui;

import cn.charlotte.pit.map.kingsquests.ui.button.KingsQuestsButton;
import cn.charlotte.pit.map.kingsquests.ui.button.KingsQuestsPaper;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public final class KingQuestsUI extends Menu {

    public static final KingQuestsUI INSTANCE = new KingQuestsUI();

    private KingQuestsUI() {
    }

    @Override
    public String getTitle(Player player) {
        return "国王任务";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> map = new HashMap<>();
        map.put(13, KingsQuestsButton.INSTANCE);
        map.put(26, KingsQuestsPaper.INSTANCE);
        return map;
    }

    @Override
    public int size(Map<Integer, Button> buttons) {
        return 3 * 9;
    }
}
