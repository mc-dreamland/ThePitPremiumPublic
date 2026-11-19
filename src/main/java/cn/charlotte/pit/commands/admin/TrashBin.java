package cn.charlotte.pit.commands.admin;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@CommandAlias("trashbin")
@CommandPermission("pit.admin")
public class TrashBin extends BaseCommand {

    public static final String INVENTORY_TITLE = "垃圾桶";

    @Subcommand("open")
    @Syntax("<player>")
    @CommandCompletion("@players")
    public void onTrashBin(CommandSender sender, OnlinePlayer targetPlayer) {
        Player player = targetPlayer.player;
        Inventory inventory = Bukkit.createInventory(null, 54, INVENTORY_TITLE);
        player.openInventory(inventory);
        sender.sendMessage("§a已为玩家 " + player.getName() + " 打开垃圾桶界面。");
    }
}
