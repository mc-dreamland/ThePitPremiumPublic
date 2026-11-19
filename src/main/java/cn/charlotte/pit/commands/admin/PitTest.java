package cn.charlotte.pit.commands.admin;


import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.util.PlayerUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.entity.Player;


@CommandAlias("test")
@CommandPermission("pit.admin")
public class PitTest extends BaseCommand {
    private final PitCommands pitCommands;

    public PitTest(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Subcommand("maxhealth")
    @CommandCompletion("@players")
    public void checkMaxHealth(Player player, Player tester) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(tester.getUniqueId());
        player.sendMessage("额外血量: " + profile.getExtraMaxHealth());
    }

    @Subcommand("goldhealth")
    @CommandCompletion("增加的金心数值|2代表一颗心|1")
    public void checkMaxHealth(Player player, int amount) {
        PlayerUtil.setAbsorption(player, amount);
    }


}