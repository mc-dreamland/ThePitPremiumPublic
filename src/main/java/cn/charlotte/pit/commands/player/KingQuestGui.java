package cn.charlotte.pit.commands.player;


import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.map.kingsquests.ui.KingQuestsUI;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import org.bukkit.entity.Player;

@CommandAlias("kingquest|kingquestgui|openKingsQuestUI")
@CommandPermission("pit.kingquest.ui")
public class KingQuestGui extends BaseCommand {
    private final PitCommands pitCommands;

    public KingQuestGui(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Default
    public void handleOpenKingQuestUI(Player player) {
        KingQuestsUI.INSTANCE.openMenu(player);
    }

}
