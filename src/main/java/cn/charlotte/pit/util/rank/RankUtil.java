package cn.charlotte.pit.util.rank;

import cn.charlotte.pit.ThePit;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * @author Yurinan, Misoryan
 * @since 2021/1/5 14:55
 */
public class RankUtil {

    private static Boolean mmEnable = null;

    public static String getPlayerRank(String name) {
        return LuckPermsUtil.getAllPrefix(Bukkit.getOfflinePlayer(name).getUniqueId()); //default player rank
    }

    public static boolean isPlayerNicked(UUID uuid) {
        return getPlayerRealColoredName(uuid).equalsIgnoreCase(getPlayerColoredName(uuid));
    }

    public static boolean isPlayerNicked(String name) {

        return getPlayerRealColoredName(name).equalsIgnoreCase(getPlayerColoredName(name));
    }

    public static boolean isPlayerNicked(Player player) {

        return isPlayerNicked(player.getUniqueId());
    }

    public static String getPlayerRankColor(String name) {

        return getNameColor(Bukkit.getOfflinePlayer(name).getUniqueId()).toString(); //default rank color

    }

    public static String getPlayerRankColor(UUID uuid) {

        return getNameColor(uuid).toString(); //default rank color
    }

    public static String getPlayerColoredName(String name) {

        return getNameFormatWithPrefix(Bukkit.getOfflinePlayer(name).getUniqueId()); //[NickRank] NickName
    }

    public static String getPlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            User user = LuckPermsUtil.getUser(uuid);
            if (user != null) {
                return user.getUsername();
            }

            if (mmEnable == null) {
                mmEnable = Bukkit.getPluginManager().getPlugin("MythicMobs") != null;
            }

            if (mmEnable) {
                Optional<ActiveMob> mob = MythicBukkit.inst().getMobManager().getActiveMob(uuid);
                if (mob.isPresent()) {
                    return mob.get().getType().getDisplayName().get();
                }
            }

            return "Bot";
        }

        return player.getDisplayName();
    }

    public static String getPlayerColoredName(UUID uuid) {
        return getNameFormatWithPrefix(uuid); //[NickRank] NickName
    }

    public static String getPlayerRealColoredName(String name) {

        try {
            UUID uuid = LuckPermsUtil.userManager.lookupUniqueId(name).get();
            if (uuid == null) {
                return "未知玩家";
            }
            return getNameFormat(uuid);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

    }

    public static String getPlayerRealColoredName(UUID uuid) {

        return getNameFormat(uuid); //fixme
    }

    public static String getNameFormat(UUID uuid) {
        return getNameColor(uuid) + getPlayerName(uuid);
    }


    public static String getNameFormatWithPrefix(UUID uuid) {
        String playerName = getPlayerName(uuid);

        if (playerName == null || "null".equals(playerName)) {
            return "boss".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName()) ?
                    "&5BOSS" : ThePit.getApi().getBotName();
        }
        return LuckPermsUtil.getVipPrefix(uuid) + getPlayerName(uuid);
    }

    // 获取名字颜色，Adventure 方式实现
    public static ChatColor getNameColor(UUID uuid) {
        String prefix = LuckPermsUtil.getVipPrefix(uuid);
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix);

        TextColor color = findLastColor(component);

        if (color == null || color.equals(NamedTextColor.WHITE)) {
            return ChatColor.GRAY;
        }

        // 尝试转为 Bukkit 的 ChatColor（支持 RGB）
        try {
            return ChatColor.of("#" + String.format("%06X", color.value()));
        } catch (IllegalArgumentException e) {
            return ChatColor.GRAY; // 保险处理
        }
    }

    // 递归查找最后一个有效颜色
    private static TextColor findLastColor(Component component) {
        TextColor currentColor = component.color();

        List<Component> children = component.children();
        for (Component child : children) {
            TextColor childColor = findLastColor(child);
            if (childColor != null) {
                currentColor = childColor;
            }
        }

        return currentColor;
    }

}
