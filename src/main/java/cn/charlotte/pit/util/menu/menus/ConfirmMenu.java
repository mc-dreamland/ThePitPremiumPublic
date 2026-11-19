package cn.charlotte.pit.util.menu.menus;

import cn.charlotte.pit.util.callback.Callback;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import cn.charlotte.pit.util.menu.buttons.ConfirmationButton;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ConfirmMenu extends Menu {

    private final String title;
    private final Button yes;
    private final Button no;
    private final Button middle;

    public ConfirmMenu(String title, Callback<Boolean> response, boolean closeAfter, int delaySec, Button middle) {
        this.title = title;
        this.yes = new ConfirmationButton(true, response, closeAfter, delaySec);
        this.no = new ConfirmationButton(false, response, closeAfter, delaySec);
        this.middle = middle;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        HashMap<Integer, Button> buttons = new HashMap<>();

        buttons.put(11, yes);
        buttons.put(15, no);

        if (middle != null) {
            buttons.put(4, middle);
        }

        return buttons;
    }

    @Override
    public boolean isAutoUpdate() {
        return true;
    }

    @Override
    public int getSize() {
        return 3 * 9; // 3行菜单
    }

    @Override
    public String getTitle(Player player) {
        return title;
    }
}