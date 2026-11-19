package cn.charlotte.pit.util.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 13:26
 */
public class TitleUtil {


    public static void sendTitle(Player player, String titleStr, String subTitleStr, int fadeIn, int fadeOut, int duration) {
        TextComponent titleComponent = Component.empty();
        TextComponent subComponent = Component.empty();
        if (titleStr != null) {
            titleComponent = Component.text(CC.translate(titleStr));
        }
        if (subTitleStr != null) {
            subComponent = Component.text(CC.translate(subTitleStr));
        }

        Title title = Title.title(titleComponent, subComponent, Title.Times.times(Duration.ofSeconds(fadeIn / 20), Duration.ofSeconds(duration / 20), Duration.ofSeconds(fadeOut / 20)));
        player.showTitle(title);
    }
}
