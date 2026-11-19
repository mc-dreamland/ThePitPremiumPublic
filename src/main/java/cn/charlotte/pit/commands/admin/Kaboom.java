package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.commands.PitCommands;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;


@CommandAlias("kaboom")
@CommandPermission("pit.admin")
public class Kaboom extends BaseCommand {
    private final PitCommands pitCommands;

    public Kaboom(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }


}
