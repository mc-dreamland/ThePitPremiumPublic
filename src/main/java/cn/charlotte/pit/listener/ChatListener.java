package cn.charlotte.pit.listener;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.activity.ActivityType;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.genesis.team.GenesisTeam;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.rank.RankUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 11:31
 */
@AutoRegister
public class ChatListener implements Listener {

    public static final HashMap<UUID, Cooldown> cooldown = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        String message = event.getMessage();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        if (!PlayerUtil.isStaff(player)) {
            if (profile.getChat() == null) {
                profile.setChat(new java.util.ArrayList<>());
            }
            int repeatCount = profile.countChatMessage(message);
            int cooldownTime = 0;
            if (repeatCount >= 3) {
                event.setCancelled(true);
                player.sendMessage(CC.CHAT_BAR);
                player.sendMessage(CC.translate("&c检测到重复消息! 请勿在短时间内重复发送相同内容!"));
                player.sendMessage(CC.translate("&c您已发送此消息 &e" + (repeatCount + 1) + " &c次，请等待一段时间后再试。"));
                player.sendMessage(CC.CHAT_BAR);
                cooldown.put(player.getUniqueId(), new Cooldown(cooldownTime, TimeUnit.SECONDS));
                return;
            }

            profile.addChatMessage(message);
            cooldown.put(player.getUniqueId(), new Cooldown(cooldownTime, TimeUnit.SECONDS));
        } else {
            cooldown.put(player.getUniqueId(), new Cooldown(0, TimeUnit.SECONDS));
        }

        String tag = profile.getFormattedLevelTagWithRoman();
        if (ActivityType.GENES.isActive()) {
            if (profile.getActivityPlayerData().getGenesisData().getTeam() == GenesisTeam.ANGEL) {
                tag = "&b♆ " + tag;
            }
            if (profile.getActivityPlayerData().getGenesisData().getTeam() == GenesisTeam.DEMON) {
                tag = "&c♨ " + tag;
            }
        }

        String rank = RankUtil.getPlayerRank(player.getName());


        profile.setLastActionTimestamp(System.currentTimeMillis());

        if (NewConfiguration.isCustomChatFormatEnable()) {
            event.setFormat(CC.translate(PlaceholderAPI
                    .setPlaceholders(player, NewConfiguration.getCustomChatFormat()
                            .replace("%s", "$s"))
                    .replace("$s", "%s")
            ));
        } else {
            if ("&7".equalsIgnoreCase(rank)) {
                event.setFormat(CC.translate(tag
                        + (profile.isSupporter() && profile.getPlayerOption().isSupporterStarDisplay() && !profile.isNicked() ? " &e✬ " : " ")
                        + rank
                        + RankUtil.getPlayerRankColor(player.getName()) + "%s: "
                        + (RankUtil.getPlayerRankColor(player.getUniqueId()).equalsIgnoreCase(CC.translate("&7")) ? "&7" : "&f")
                        + (player.hasPermission("thepit.admin") ? CC.translate("%s") : "%s")));
            } else {
                String translate = CC.translate(tag
                        + (profile.isSupporter() && profile.getPlayerOption().isSupporterStarDisplay() && !profile.isNicked() ? " &e✬ " : " ")
                        + rank
                        + " "
                        + RankUtil.getPlayerRankColor(player.getName()) + "%s: "
                        + (RankUtil.getPlayerRankColor(player.getUniqueId()).equalsIgnoreCase(CC.translate("&7")) ? "&7" : "&f")
                        + (player.hasPermission("thepit.admin") ? CC.translate("%s") : "%s"));

                event.setFormat(PlaceholderAPI.setPlaceholders(player, translate));
            }
        }
    }
}