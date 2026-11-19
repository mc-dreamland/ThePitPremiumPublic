package cn.charlotte.pit.runnable;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.EventFactory;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.TitleUtil;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.time.TimeUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

/**
 * @Author: EmptyIrony
 * @Date: 2021/2/24 13:04
 */
@Getter
public class RebootRunnable extends BukkitRunnable {
    private final long serverStartTime;
    private RebootTask currentTask;
    private boolean eventSettled;
    private boolean shutdownStarted;

    public RebootRunnable() {
        this.serverStartTime = System.currentTimeMillis();
    }

    @Override
    public void run() {

        if (currentTask != null) {
            if (!shutdownStarted && currentTask.endTime <= System.currentTimeMillis()) {
                CC.boardCast("&4&l注意! &7服务器即将重启！");
                shutdownStarted = true;
                Bukkit.getScheduler().runTask(ThePit.getInstance(), this::startGradualShutdown);
                return;
            }

            if (!eventSettled && currentTask.endTime <= System.currentTimeMillis() + 7 * 1000) {
                eventSettled = true;
                Bukkit.getScheduler().runTask(ThePit.getInstance(), this::settleEvents);
            }
        }
    }

    public void addRebootTask(RebootTask task) {
        if (this.currentTask != null) {
            return;
        }
        this.currentTask = task;
        this.eventSettled = false;
        this.shutdownStarted = false;
        Bukkit.setWhitelist(true);
        Bukkit.setWhitelistEnforced(true);
        long l = task.endTime - System.currentTimeMillis();
        String time = TimeUtil.millisToRoundedTime(l);
        CC.boardCast("&4&l注意! &7一个计划中的重启即将在 &c" + time + "&7 后执行&8[#" + task.reason + "]");
        for (Player player : Bukkit.getOnlinePlayers()) {
            TitleUtil.sendTitle(player, CC.translate("&c服务器即将在 &b" + time + " &c后重启"), "", 20, 100, 100);
        }
    }

    private void settleEvents() {
        final EventFactory factory = ThePit.getInstance().getEventFactory();
        if (factory.getActiveEpicEvent() != null) {
            factory.inactiveEvent(factory.getActiveEpicEvent());
        }
        if (factory.getActiveNormalEvent() != null) {
            factory.inactiveEvent(factory.getActiveNormalEvent());
        }

        CC.boardCast("&4&l注意! &7因存在一个即将执行的重启任务,已&c强制结算&7事件.");
    }

    private void startGradualShutdown() {
        Bukkit.setWhitelist(true);
        Bukkit.setWhitelistEnforced(true);

        Queue<UUID> kickQueue = new ArrayDeque<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            kickQueue.add(player.getUniqueId());
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                Player player = pollOnlinePlayer(kickQueue);
                if (player == null) {
                    cancel();
                    Bukkit.shutdown();
                    return;
                }

                savePlayerBeforeKick(player);
                player.kick(Component.text("§c" + currentTask.reason));
            }
        }.runTaskTimer(ThePit.getInstance(), 0L, 2L);
    }

    private Player pollOnlinePlayer(Queue<UUID> kickQueue) {
        while (!kickQueue.isEmpty()) {
            Player player = Bukkit.getPlayer(kickQueue.poll());
            if (player != null && player.isOnline()) {
                return player;
            }
        }
        return null;
    }

    private void savePlayerBeforeKick(Player player) {
        try {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            if (profile.isLoaded()) {
                profile.setInventory(InventoryUtil.playerInventoryFromPlayer(player));
                profile.save(null);
                CC.boardCast("&6&l公告! &7正在保存 " + player.getDisplayName() + " 的数据...");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public record RebootTask(String reason, long endTime) {
    }
}
