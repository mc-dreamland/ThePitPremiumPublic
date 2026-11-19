package cn.charlotte.pit.data;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.mail.Mail;
import cn.charlotte.pit.util.chat.CC;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class FixedRewardData {

    private static final Map<UUID, FixedRewardData> CACHE = new HashMap<>();
    private static volatile boolean LOADING = false;
    // ===== Getter / Setter（便于 Jackson 反序列化）=====
    // ===== 原 Kotlin 成员变量 =====
    private long sendTime = -1L;
    private String mailId = "";
    private long startTime = -1L;
    private long endTime = -1L;
    private String title = "-1";
    private String content = "-1";
    private CDKData data;
    public FixedRewardData() {
    }

    public static void refreshAll() {
        LOADING = true;
        CACHE.clear();
        // 若你的项目里是公开字段而非 getter，请改为：ThePit.getInstance().mongoDB.rewardCollection.find()
        for (FixedRewardData data : ThePit.getInstance()
                .getMongoDB()
                .getRewardCollection()
                .find()) {
            CACHE.put(UUID.fromString(data.mailId), data);
        }
        LOADING = false;
    }

    public static void sendMail(PlayerProfile profile, Player player) {
        if (LOADING) return;

        long now = System.currentTimeMillis();

        for (FixedRewardData data : CACHE.values()) {
            // 根据你 PlayerProfile 的实现选择 getClaimedMail() 或字段访问
            if (profile.getClaimedMail().contains(data.mailId)) continue;

            CDKData info = data.data;
            if (info == null) continue;

            long start = data.startTime + data.sendTime;
            long end = data.endTime + data.sendTime;

            if (now >= start && now <= end) {
                // 这里同样根据 CDKData 的实现选择使用字段或 getter
                String perm = info.getLimitPermission();
                if (perm == null || perm.isEmpty() || player.hasPermission(perm)) {

                    // Kotlin 里用了 "data.data?.limitLevel!!" 的非空断言，这里显式做一次判空与读取
                    int limitLevel = info.getLimitLevel();

                    if (profile.getPrestige() > 0 || profile.getLevel() > limitLevel) {
                        Mail mail = new Mail();
                        // 以下字段赋值请与 Mail/CDKData 的真实定义保持一致（字段或 setter）
                        mail.setCoins(info.getCoins());
                        mail.setExp(info.getExp());
                        mail.setRenown(info.getRenown());
                        mail.setItem(info.getItem());
                        mail.setTitle(data.title);
                        mail.setContent(data.content);
                        mail.setSendTime(now);
                        mail.setExpireTime(now + 30L * 24 * 60 * 60 * 1000); // 30 天

                        mail.build();

                        profile.getClaimedMail().add(data.mailId);
                        profile.getMailData().sendMail(mail);
                        player.sendMessage(CC.translate("&6&l邮件! &7你收到了一封邮件,请从邮件NPC处查看具体内容."));
                    }
                }
            }
        }
    }

}
