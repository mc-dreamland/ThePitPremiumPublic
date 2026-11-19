package cn.charlotte.pit.commands.player;


import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.map.kingsquests.ui.CakeBakeUI;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import org.bukkit.entity.Player;

@CommandAlias("bakemaster|BakeMaster|openBakeMaster")
@CommandPermission("pit.kingquest.ui")
public class BakeMaster extends BaseCommand {
    private final PitCommands pitCommands;

    public BakeMaster(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Default
    public void handleOpenBake(Player player) {
        CakeBakeUI.INSTANCE.openMenu(player);
    }

}
