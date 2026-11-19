package cn.charlotte.pit.listener;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.FixedRewardData;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.time.TimeUtil;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AutoRegister
public class MailSendListener implements Listener {

    public static final Map<UUID, FixedRewardData> editing = new HashMap<>();

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();

        FixedRewardData data = editing.get(player.getUniqueId());
        if (data == null) {
            return;
        }

        // 取消编辑
        if ("cancel".equalsIgnoreCase(event.getMessage())) {
            editing.remove(player.getUniqueId());
            return;
        }

        event.setCancelled(true);

        // 输入标题
        if ("-1".equals(data.getTitle())) {
            data.setTitle(CC.translate(event.getMessage()));
            player.sendMessage(CC.translate("&a现在，你可以输入邮件内容了，输入 cancel 以取消"));
            return;
        }

        // 输入内容
        if ("-1".equals(data.getContent())) {
            data.setContent(CC.translate(event.getMessage()));
            player.sendMessage(CC.translate("&a现在，你可以输入发送时间了，输入的时间将在从现在开始计算起之后的时间生效，输入 cancel 以取消"));
            return;
        }

        // 输入开始时间
        if (data.getStartTime() == -1L) {
            long parseTime = TimeUtil.parseTime(event.getMessage());
            if (parseTime == -1L) {
                player.sendMessage(CC.translate("&c你输入的时间不合法"));
                return;
            }
            data.setStartTime(parseTime);
            player.sendMessage(CC.translate("&a现在，你可以输入结束时间了，输入的时间将在从现在开始计算起之后的时间生效，输入 cancel 以取消"));
            return;
        }

        // 输入结束时间
        if (data.getEndTime() == -1L) {
            long parseTime = TimeUtil.parseTime(event.getMessage());
            if (parseTime == -1L) {
                player.sendMessage(CC.translate("&c你输入的时间不合法"));
                return;
            }

            data.setEndTime(parseTime);
            data.setMailId(UUID.randomUUID().toString());
            data.setSendTime(System.currentTimeMillis());

            player.sendMessage(data.toString());
            player.sendMessage(CC.translate("&a现在，输入 confirm 以发送，输入 cancel 取消发送"));
            return;
        }

        // 确认发送
        if ("confirm".equalsIgnoreCase(event.getMessage())) {
            final FixedRewardData dataRef = data;
            final UUID playerId = player.getUniqueId();

            new BukkitRunnable() {
                @Override
                public void run() {
                    // 如为字段访问，请改为：ThePit.getInstance().mongoDB.rewardCollection
                    ThePit.getInstance()
                            .getMongoDB()
                            .getRewardCollection()
                            .replaceOne(
                                    Filters.eq("mailId", dataRef.getMailId()),
                                    dataRef,
                                    new ReplaceOptions().upsert(true)
                            );

                    FixedRewardData.refreshAll();
                    player.sendMessage(CC.translate("&aOK!"));

                    editing.remove(playerId);
                }
            }.runTaskAsynchronously(ThePit.getInstance());
        }
    }
}
