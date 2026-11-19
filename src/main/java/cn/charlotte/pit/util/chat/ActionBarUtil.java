package cn.charlotte.pit.util.chat;

import cn.charlotte.pit.ThePit;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 12:37
 */
public class ActionBarUtil {

    public static void sendActionBar(Player player, String message) {
        ThePit.getInstance().getActionBarManager().addToQueue(player, message);
    }

    public static void sendActionBar0(Player player, String message) {
        player.sendActionBar(Component.text(CC.translate(message)));
    }

}
