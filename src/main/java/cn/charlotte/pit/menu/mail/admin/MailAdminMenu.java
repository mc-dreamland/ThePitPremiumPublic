package cn.charlotte.pit.menu.mail.admin;

import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * @Author: EmptyIrony
 * @Date: 2021/3/16 23:36
 */
public class MailAdminMenu extends Menu {
    @Override
    public String getTitle(Player player) {
        return "邮件管理中心";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        //管理你妈逼我怎么知道怎么写 <-前辈的话,没绷住哈
        //todo: 设计一下
        return null;
    }
}
