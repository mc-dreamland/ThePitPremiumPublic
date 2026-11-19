package cn.charlotte.pit.commands.admin;

import org.bukkit.command.CommandSender;

import java.util.Map;

public class ScanLuckyStone {

    private final OwnedItemScanTask scanTask = new OwnedItemScanTask(
            "luckystone",
            "scan luckystone",
            "幸运石",
            "lucky-stone-owners",
            Map.of("lucky_stone", "luckyStone")
    );

    public void start(CommandSender sender) {
        scanTask.start(sender);
    }

    public void status(CommandSender sender) {
        scanTask.status(sender);
    }
}
