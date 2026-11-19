package cn.charlotte.pit.commands.admin;

import org.bukkit.command.CommandSender;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScanMysteryBox {

    private final OwnedItemScanTask scanTask = new OwnedItemScanTask(
            "mysterybox",
            "scan mysterybox",
            "神秘之盒",
            "mystery-box-owners",
            trackedBoxes()
    );

    public void start(CommandSender sender) {
        scanTask.start(sender);
    }

    public void status(CommandSender sender) {
        scanTask.status(sender);
    }

    private static Map<String, String> trackedBoxes() {
        Map<String, String> boxes = new LinkedHashMap<>();
        boxes.put("mystery_box_one", "boxI");
        boxes.put("mystery_box_two", "boxII");
        boxes.put("mystery_box_three", "boxIII");
        return boxes;
    }
}
