package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("extrabonus")
@CommandPermission("pit.admin")
public class ExtraBonus extends BaseCommand {
    private final PitCommands pitCommands;

    public ExtraBonus(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Default
    @Syntax("unlockgold/unlockexp <player>")
    @CommandCompletion("unlockgold|unlockexp @players")
    public void extraBonus(CommandSender sender, String type, OnlinePlayer onlinePlayer) {

        if (onlinePlayer == null || onlinePlayer.player == null || !onlinePlayer.player.isOnline()) {
            sender.sendMessage("目标玩家不存在或不在线!");
            return;
        }

        Player player = onlinePlayer.player;           // 方便后续引用
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        switch (type.toLowerCase()) {
            case "unlockgold":
                profile.unlockExtraGoldBonus();
                sender.sendMessage("已为 " + player.getName() + " 解锁金币加成 30 天！");
                break;

            case "unlockexp":
                profile.unlockExtraExpBonus();
                sender.sendMessage("已为 " + player.getName() + " 解锁经验加成 30 天！");
                break;

            default:
                sender.sendMessage("未知类型: " + type);
                break;
        }
    }
}
