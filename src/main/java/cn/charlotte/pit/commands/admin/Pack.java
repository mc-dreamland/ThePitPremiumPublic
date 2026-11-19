package cn.charlotte.pit.commands.admin;


import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.medal.impl.challenge.hidden.SupporterMedal;
import cn.charlotte.pit.util.chat.CC;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.command.CommandSender;

@CommandAlias("pitpack")
@CommandPermission("pit.admin")
public class Pack extends BaseCommand {
    private final PitCommands pitCommands;

    public Pack(PitCommands pitCommands) {
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
        profile.setBetterPlayer(true);
        CC.boardCast("&6&l惊了! " + profile.getFormattedNameWithRoman() + " &7购买了 &e天坑增值包 &7! &a十分感谢!");
        new SupporterMedal().setProgress(profile, 1);
        sender.sendMessage("okay");
    }
}
