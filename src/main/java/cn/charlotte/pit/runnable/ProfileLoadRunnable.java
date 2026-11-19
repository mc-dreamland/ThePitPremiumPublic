package cn.charlotte.pit.runnable;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author: EmptyIrony
 * @Date: 2021/4/2 23:40
 */
public class ProfileLoadRunnable extends BukkitRunnable {
    @Getter
    private static ProfileLoadRunnable instance;
    @Getter
    private final Map<UUID, Cooldown> cooldownMap = new HashMap<>();

    public ProfileLoadRunnable(ThePit plugin) {
        instance = this;
        this.runTaskTimerAsynchronously(plugin, 20, 20);
    }


    @Override
    public void run() {
        final Map<UUID, Cooldown> map = new HashMap<>(cooldownMap);
        for (Map.Entry<UUID, Cooldown> entry : map.entrySet()) {
            final Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                cooldownMap.remove(entry.getKey());
                continue;
            }

            final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            if (profile.isLoaded()) {
                cooldownMap.remove(entry.getKey());

                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    player.removePotionEffect(PotionEffectType.WEAKNESS);
                    player.removePotionEffect(PotionEffectType.SPEED);
                    player.removePotionEffect(PotionEffectType.JUMP_BOOST);
                });
                continue;
            }

            if (entry.getValue().hasExpired()) {
                player.sendMessage(CC.translate("&c档案加载异常,请尝试重新进入!"));
                Util.backToLobby(player);
                cooldownMap.remove(entry.getKey());
            }
        }
    }

    public void handleJoin(Player player) {
        cooldownMap.put(player.getUniqueId(), new Cooldown(1, TimeUnit.MINUTES));
    }

    public void handleQuit(Player player) {
        cooldownMap.remove(player.getUniqueId());
    }
}
