package cn.charlotte.pit.commands.player;


import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.viewer.StatusViewerMenu;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.level.LevelUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("view|look")
public class View extends BaseCommand {
    private final PitCommands pitCommands;

    public View(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Default
    @CommandCompletion("@players")
    public void viewProfile(Player player, @Optional String playerName) {
        final Long present = this.pitCommands.viewCooldown.getIfPresent(player.getUniqueId());
        if (present != null && !player.isOp()) {
            player.sendMessage(CC.translate("&c冷却中..."));
            return;
        }

        this.pitCommands.viewCooldown.put(player.getUniqueId(), System.currentTimeMillis());

        try {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            if (profile.getLevel() < 70 && profile.getPrestige() < 1) {
                player.sendMessage(CC.translate("&c&l等级不足! &7此指令在 " + LevelUtil.getLevelTag(profile.getPrestige(), 70) + " &7时解锁."));
                return;
            }

            if (playerName == null || playerName.trim().isEmpty()) {
                player.sendMessage(CC.translate("&c用法: /view <玩家名>"));
                return;
            }

            String targetName = playerName.trim();

            Player onlineTarget = Bukkit.getPlayer(targetName);
            if (onlineTarget != null && onlineTarget.isOnline()) {
                handleOnlinePlayerView(player, onlineTarget);
            } else {
                if (player.isOp()) {
                    handleOfflinePlayerView(player, targetName);
                } else {
                    player.sendMessage("§c该玩家不存在或不在线!");
                }
            }

        } catch (Exception e) {
            if (player.hasPermission("pit.admin")) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理在线玩家查看
     */
    private void handleOnlinePlayerView(Player player, Player target) {
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            PlayerProfile targetProfile = Util.getPitProfile(target);
            if (targetProfile == null) {
                player.sendMessage(CC.translate("&c此玩家的档案不存在,请检查输入是否有误."));
                return;
            }
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> new StatusViewerMenu(targetProfile).openMenu(player));
        });
    }

    /**
     * 处理离线玩家查看
     */
    private void handleOfflinePlayerView(Player player, String playerName) {
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            try {
                PlayerProfile targetProfile = PlayerProfile.getOrLoadPlayerProfileByName(playerName);
                if (targetProfile == null) {
                    player.sendMessage(CC.translate("&c未找到玩家 " + playerName + " 的档案."));
                    return;
                }
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> new StatusViewerMenu(targetProfile).openMenu(player));
            } catch (Exception e) {
                if (player.hasPermission("pit.admin")) {
                    e.printStackTrace();
                }
            }
        });
    }
}
