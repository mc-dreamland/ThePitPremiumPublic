package cn.charlotte.pit.menu.admin.mail;

import cn.charlotte.pit.menu.cdk.generate.CDKMenu;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.entity.Player;

import java.util.Map;

public class MailGenerator extends CDKMenu {

    @Override
    public String getTitle(Player player) {
        return "Mail Generator";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = super.getButtons(player);
        buttons.remove(34);
        buttons.remove(35);
        return buttons;
    }
}
