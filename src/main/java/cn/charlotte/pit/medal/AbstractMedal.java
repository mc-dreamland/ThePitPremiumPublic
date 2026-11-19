package cn.charlotte.pit.medal;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.MedalData;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.ChatComponentBuilder;
import cn.charlotte.pit.util.chat.RomanUtil;
import cn.charlotte.pit.util.menu.builder.ButtonBuilder;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * @Creator Misoryan
 * @Date 2021/6/8 20:14
 */
public abstract class AbstractMedal {

    public abstract String getInternalName();

    public abstract String getDisplayName(int level);

    public abstract String getRequirementDescription(int level);

    public abstract int getMaxLevel();

    public abstract int getProgressRequirement(int level);

    public abstract int getRarity(int level);

    public abstract boolean isHidden();

    public void setProgress(PlayerProfile profile, int progress) {
        MedalData data = profile.getMedalData();

        for (int i = 1; i <= getMaxLevel(); i++) {
            MedalData.MedalStatus s = data.ensure(getInternalName(), i);
            int req = getProgressRequirement(i);
            int capped = Math.min(Math.max(progress, 0), req);

            if (s.getProgress() != capped) s.setProgress(capped);

            if (!s.isUnlocked() && capped >= req) {
                s.setUnlocked(true);
                s.setFinishedTime(System.currentTimeMillis());

                Player player = Bukkit.getPlayer(profile.getPlayerUUID());
                if (player != null && player.isOnline()) {
                    ChatComponentBuilder message = new ChatComponentBuilder(CC.translate("&e&l成就解锁! &7你解锁了成就 &e"));
                    ChatComponentBuilder namePart = new ChatComponentBuilder(CC.translate(getDisplayName(i)));
                    namePart.setCurrentHoverEvent(HoverEvent.showText(new ChatComponentBuilder("")
                            .append(CC.translate(getDisplayName(i)))
                            .append("\n\n")
                            .append(CC.translate(getRequirementDescription(i)))
                            .build()));
                    message.append(namePart.build());
                    if (getMaxLevel() > 1)
                        message.append(CC.translate(String.format(" %s", RomanUtil.convert(i))));

                    message.append(CC.translate(" &7."));
                    player.sendMessage(message.build());
                }
            }
        }
    }

    public void addProgress(PlayerProfile profile, int progress) {
        MedalData data = profile.getMedalData();
        int currentLevel = data.getMedalLevel(getInternalName());

        setProgress(profile, data.view(getInternalName(), currentLevel).getProgress() + progress);
    }

    public abstract void handleProfileLoaded(PlayerProfile profile);

    public ButtonBuilder getButton(PlayerProfile profile, int level , int progress, boolean locked,int click) {
        return null;
    }
}