package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.commands.PitCommands;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;

@CommandAlias("supporter")
@CommandPermission("pit.admin")
public class Supporter extends BaseCommand {

    private final PitCommands pitCommands;

    public Supporter(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    //GIVE

    //TAKE
}
