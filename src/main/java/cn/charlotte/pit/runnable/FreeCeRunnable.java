package cn.charlotte.pit.runnable;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.util.chat.CC;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

public class FreeCeRunnable extends BukkitRunnable implements Listener {


    @Getter
    @Setter
    static boolean B = true;

    public FreeCeRunnable() {

    }

    public static void FreeCeQuiet() {
        if (CombatListener.isNight()) {
            CC.boardCast("&f夜已深! &7已取消在线福利以及事件等等 祝你有个好梦!");
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            //我将AFK机制搬到了这个里面了,能省就省
            PlayerProfile profile = Util.getPitProfile(player);
//            if(profile.getStreakKills() < 50.0 && profile.countdamage(player.getUniqueId(), 60 * 1000 * 10) < 1){
//                continue;
//            }
            int Ce = 10;
            String message = "&a&l在线福利 ";
            if (profile.isSupporter()) {
                Ce = Ce * 5;
            }
            profile.setCoins(profile.getCoins() + Ce);
            profile.grindCoins(Ce);
            message += "&6" + Ce + "金币 ";
            if (!(profile.getLevel() >= 120)) {
                profile.setExperience(profile.getExperience() + Ce);
                profile.applyExperienceToPlayer(player);
                message += "&b" + Ce + "经验";
            }
            player.sendMessage(CC.translate(message));
        }
    }

    @Override
    public void run() {
        if (B) {
            FreeCeQuiet();
        }
    }
}