package cn.charlotte.pit.commands.admin;


import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.command.CommandSender;

@CommandAlias("forceprestige")
@CommandPermission("pit.admin")
public class ForcePrestige extends BaseCommand {
    private final PitCommands pitCommands;

    public ForcePrestige(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Default
    @Syntax("<player>")
    @CommandCompletion("@players")
    public void forcePrestige(CommandSender sender, OnlinePlayer player) {
        if (player.player == null || !player.player.isOnline()) {
            sender.sendMessage("目标玩家不存在或不在线!");
            return;
        }
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.player.getUniqueId());
        if (profile.getLevel() >= 120) {
            profile.prestigeUp(player.player);
        }
        sender.sendMessage("okay");
    }
}
