package cn.charlotte.pit.commands.player;


import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.option.PlayerOptionMenu;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.level.LevelUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import org.bukkit.entity.Player;

@CommandAlias("option|options|opt|setting|settings")
public class Setting extends BaseCommand {
    private final PitCommands pitCommands;

    public Setting(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Default
    public void openOption(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile.getPrestige() > 0 || profile.getLevel() >= 5) {
            new PlayerOptionMenu().openMenu(player);
        } else {
            player.sendMessage(CC.translate("&c&l等级不足! &7此指令在 " + LevelUtil.getLevelTag(profile.getPrestige(), 5) + " &7时解锁."));
        }
    }

}
