package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.commands.PitCommands;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import org.bukkit.command.CommandSender;

@CommandAlias("scan")
@CommandPermission("pit.admin")
public class ScanCommand extends BaseCommand {

    private final ScanItemUuid itemUuid;
    private final ScanUuidDuplicateHistory duplicateUuidHistory = new ScanUuidDuplicateHistory();
    private final ScanUuidReportView uuidReportView = new ScanUuidReportView();
    private final ScanPhaseBook phaseBook = new ScanPhaseBook();
    private final ScanMythicBundle mythicBundle = new ScanMythicBundle();
    private final ScanLuckyStone luckyStone = new ScanLuckyStone();
    private final ScanMysteryBox mysteryBox = new ScanMysteryBox();
    private final ScanInventoryGrowth inventoryGrowth = new ScanInventoryGrowth();
    private final RareEnchantScanTask rareEnchant = new RareEnchantScanTask();
    private final ScanAdvancedItem advancedItem = new ScanAdvancedItem();
    private final ScanEnchantCount enchantCount = new ScanEnchantCount();
    private final ScanPlayerBackupDiff backupDiff = new ScanPlayerBackupDiff();
    private final ScanPlayerBackupPhaseBooks backupPhaseBooks = new ScanPlayerBackupPhaseBooks();
    private final ScanRareMaterialHistory rareMaterialHistory = new ScanRareMaterialHistory();
    private final ScanMaterialQuantityHistory materialQuantityHistory = new ScanMaterialQuantityHistory();
    private final ScanMaterialEnchantmentHistory materialEnchantmentHistory = new ScanMaterialEnchantmentHistory();
    private final ScanMaterialUuidHistoryIndex materialUuidHistoryIndex = new ScanMaterialUuidHistoryIndex();
    private final ScanEnchantedMythicBackupDiff enchantedMythicDiff = new ScanEnchantedMythicBackupDiff();
    private final ScanPhaseBookMarkedMythics phaseBookMarkedMythics = new ScanPhaseBookMarkedMythics();

    public ScanCommand(PitCommands pitCommands) {
        this.itemUuid = new ScanItemUuid(pitCommands);
        this.rareMaterialHistory.setUuidHistoryIndex(materialUuidHistoryIndex);
        this.rareMaterialHistory.setEnchantmentHistory(materialEnchantmentHistory);
    }

    @Default
    public void help(CommandSender sender) {
        sender.sendMessage("§6/scan uuid §7- 扫描重复装备 UUID");
        sender.sendMessage("§6/scan uuid view [玩家/物品UUID] §7- 查看本地重复物品、持有人、位置和完整 Lore");
        sender.sendMessage("§6/scan uuid history [时间窗口秒] §7- 从本地索引追查重复 UUID 最早线索（默认 60 秒）");
        sender.sendMessage("§6/scan uuid history view [玩家/物品UUID] §7- 查看本地重复历史线索及对应物品");
        sender.sendMessage("§6/scan phasebook §7- 扫描相位书");
        sender.sendMessage("§6/scan mythicbundle §7- 扫描神话装备收纳箱");
        sender.sendMessage("§6/scan luckystone §7- 扫描幸运石");
        sender.sendMessage("§6/scan mysterybox §7- 扫描神秘之盒");
        sender.sendMessage("§6/scan growth [hours] §7- 扫描背包和仓库增长");
        sender.sendMessage("§6/scan rareenchant §7- 扫描含稀有附魔的装备");
        sender.sendMessage("§6/scan advanceditem §7- 按类型统计玩家持有的高级道具");
        sender.sendMessage("§6/scan enchantcount §7- 按累计附魔次数扫描玩家（仅大于 100 次）");
        sender.sendMessage("§6/scan backupdiff <玩家> [最小新增数量] §7- 对比相邻备份中的稀有道具");
        sender.sendMessage("§6/scan phasebookhistory <玩家> [最小数量] §7- 查询历史备份中的大量相位书");
        sender.sendMessage("§6/scan materialhistory [最小数量] §7- 扫描全部高级道具及原物资（不要求 UUID，默认 10）");
        sender.sendMessage("§6/scan materialhistory all [最小数量] §7- 同样扫描全部高级道具，流式保存报告（默认 1）");
        sender.sendMessage("§6/scan materialhistory speed [每秒份数] §7- 实时调速，0 不限速");
        sender.sendMessage("§6/scan materialhistory batch [每批份数] §7- 调整数据库批量，下次扫描生效");
        sender.sendMessage("§6/scan materialhistory item [物品ID/名称] [玩家] §7- 只读本地物品数量历史，可按日期/数量排序");
        sender.sendMessage("§6/scan materialhistory max <玩家> §7- 只读本地报告，汇总该玩家每种物品的历史最高数量");
        sender.sendMessage("§6/scan materialhistory view [玩家] §7- 查看命中玩家列表或指定玩家备份");
        sender.sendMessage("§6/scan materialhistory uuidtrace <玩家> §7- 追踪当前 UUID 装备在全部玩家备份中的记录");
        sender.sendMessage("§6/scan materialhistory uuidtrace view [玩家] §7- 查看 UUID 装备历史追踪界面");
        sender.sendMessage("§6/scan currenthistory <玩家> §7- 从本地索引查询当前背包、仓库稀有物品的持有人历史");
        sender.sendMessage("§6/scan currenthistory view [玩家] §7- 查看当前物品报告；装备右键查看历史附魔/宝石记录");
        sender.sendMessage("§6/scan materialhistory uuidindex §7- 将全部 UUID 物品历史整理为本地索引");
        sender.sendMessage("§6/scan materialhistory enchantrecords build §7- 只读本地 UUID 索引，按操作者/装备整理附魔记录");
        sender.sendMessage("§6/scan materialhistory enchantrecords [玩家] §7- 查看附魔操作者及装备历史");
        sender.sendMessage("§6/scan materialhistory enchantrecords item <装备UUID> §7- 查看指定装备的全部附魔记录");
        sender.sendMessage("§6/scan materialhistory enchantrecords export <玩家> §7- 按时间导出该操作者的附魔、打宝石等 records，一行一条");
        sender.sendMessage("§6/scan materialhistory uuidtransfer <玩家> §7- 查看首次属于该玩家、最新在他人处的装备");
        sender.sendMessage("§6/scan materialhistory legitgem §7- 查看曾使用遵纪守法宝石的装备");
        sender.sendMessage("§6/scan materialhistory legitgem usage [玩家] §7- 统计玩家累计使用次数");
        sender.sendMessage("§6/scan mythicdiff [最小变动数量] §7- 扫描相邻备份中的附魔神话装备变动");
        sender.sendMessage("§6/scan phasebookmarked §7- 扫描带相位书标记的神话装备");
        sender.sendMessage("§7每个子命令后可追加 §fstatus §7查看进度。");
    }

    @Subcommand("uuid")
    public void scanItemUuid(CommandSender sender) {
        itemUuid.start(sender);
    }

    @Subcommand("uuid status")
    public void scanItemUuidStatus(CommandSender sender) {
        itemUuid.status(sender);
    }

    @Subcommand("uuid view")
    @Syntax("[playerOrItemUuid]")
    @CommandCompletion("@players")
    public void viewDuplicateUuidReport(CommandSender sender, @Optional String playerOrItemUuid) {
        uuidReportView.open(sender, false, playerOrItemUuid);
    }

    @Subcommand("uuid history")
    @Syntax("[windowSeconds]")
    @CommandCompletion("60|0|10|300")
    public void traceDuplicateUuidHistory(CommandSender sender, @Optional Integer windowSeconds) {
        duplicateUuidHistory.start(sender, windowSeconds, itemUuid);
    }

    @Subcommand("uuid history status")
    public void traceDuplicateUuidHistoryStatus(CommandSender sender) {
        duplicateUuidHistory.status(sender);
    }

    @Subcommand("uuid history view")
    @Syntax("[playerOrItemUuid]")
    @CommandCompletion("@players")
    public void viewDuplicateUuidHistoryReport(CommandSender sender, @Optional String playerOrItemUuid) {
        uuidReportView.open(sender, true, playerOrItemUuid);
    }

    @Subcommand("phasebook")
    public void scanPhaseBook(CommandSender sender) {
        phaseBook.start(sender);
    }

    @Subcommand("phasebook status")
    public void scanPhaseBookStatus(CommandSender sender) {
        phaseBook.status(sender);
    }

    @Subcommand("mythicbundle")
    public void scanMythicBundle(CommandSender sender) {
        mythicBundle.start(sender);
    }

    @Subcommand("mythicbundle status")
    public void scanMythicBundleStatus(CommandSender sender) {
        mythicBundle.status(sender);
    }

    @Subcommand("luckystone")
    public void scanLuckyStone(CommandSender sender) {
        luckyStone.start(sender);
    }

    @Subcommand("luckystone status")
    public void scanLuckyStoneStatus(CommandSender sender) {
        luckyStone.status(sender);
    }

    @Subcommand("mysterybox")
    public void scanMysteryBox(CommandSender sender) {
        mysteryBox.start(sender);
    }

    @Subcommand("mysterybox status")
    public void scanMysteryBoxStatus(CommandSender sender) {
        mysteryBox.status(sender);
    }

    @Subcommand("growth")
    @Syntax("[hours]")
    @CommandCompletion("6|12|24|48|72|168")
    public void scanInventoryGrowth(CommandSender sender, @Optional Integer hours) {
        inventoryGrowth.start(sender, hours);
    }

    @Subcommand("growth status")
    public void scanInventoryGrowthStatus(CommandSender sender) {
        inventoryGrowth.status(sender);
    }

    @Subcommand("rareenchant")
    public void scanRareEnchant(CommandSender sender) {
        rareEnchant.start(sender);
    }

    @Subcommand("rareenchant status")
    public void scanRareEnchantStatus(CommandSender sender) {
        rareEnchant.status(sender);
    }

    @Subcommand("advanceditem")
    public void scanAdvancedItem(CommandSender sender) {
        advancedItem.start(sender);
    }

    @Subcommand("advanceditem status")
    public void scanAdvancedItemStatus(CommandSender sender) {
        advancedItem.status(sender);
    }

    @Subcommand("enchantcount")
    public void scanEnchantCount(CommandSender sender) {
        enchantCount.start(sender);
    }

    @Subcommand("enchantcount status")
    public void scanEnchantCountStatus(CommandSender sender) {
        enchantCount.status(sender);
    }

    @Subcommand("backupdiff")
    @Syntax("<player> [minimumItems]")
    @CommandCompletion("@players 64|128|256|512")
    public void scanBackupDiff(CommandSender sender, String playerName, @Optional Integer minimumItems) {
        backupDiff.start(sender, playerName, minimumItems);
    }

    @Subcommand("backupdiff status")
    public void scanBackupDiffStatus(CommandSender sender) {
        backupDiff.status(sender);
    }

    @Subcommand("phasebookhistory")
    @Syntax("<player> [minimumBooks]")
    @CommandCompletion("@players 5|10|20|50")
    public void scanBackupPhaseBooks(CommandSender sender, String playerName, @Optional Integer minimumBooks) {
        backupPhaseBooks.start(sender, playerName, minimumBooks);
    }

    @Subcommand("phasebookhistory status")
    public void scanBackupPhaseBooksStatus(CommandSender sender) {
        backupPhaseBooks.status(sender);
    }

    @Subcommand("materialhistory")
    @Syntax("[minimumItems]")
    @CommandCompletion("5|10|20|50|100")
    public void scanRareMaterialHistory(CommandSender sender, @Optional Integer minimumItems) {
        rareMaterialHistory.start(sender, minimumItems);
    }

    @Subcommand("materialhistory all")
    @Syntax("[minimumItems]")
    @CommandCompletion("1|5|10|20|50|100")
    public void scanAllUuidMaterialHistory(CommandSender sender, @Optional Integer minimumItems) {
        rareMaterialHistory.startAll(sender, minimumItems);
    }

    @Subcommand("materialhistory speed")
    @Syntax("[backupsPerSecond]")
    @CommandCompletion("0|50|100|200|500")
    public void materialHistorySpeed(CommandSender sender, @Optional Integer backupsPerSecond) {
        rareMaterialHistory.speed(sender, backupsPerSecond);
    }

    @Subcommand("materialhistory batch")
    @Syntax("[batchSize]")
    @CommandCompletion("500|100|250|1000")
    public void materialHistoryBatch(CommandSender sender, @Optional Integer batchSize) {
        rareMaterialHistory.batch(sender, batchSize);
    }

    @Subcommand("materialhistory item")
    @Syntax("[item] [player]")
    public void viewMaterialQuantityHistory(CommandSender sender, @Optional String item, @Optional String playerName) {
        materialQuantityHistory.open(sender, item, playerName);
    }

    @Subcommand("materialhistory max")
    @Syntax("<player>")
    @CommandCompletion("@players")
    public void viewPlayerMaterialMaximum(CommandSender sender, String playerName) {
        materialQuantityHistory.openMaximum(sender, playerName);
    }

    @Subcommand("materialhistory status")
    public void scanRareMaterialHistoryStatus(CommandSender sender) {
        rareMaterialHistory.status(sender);
    }

    @Subcommand("materialhistory all status")
    public void scanAllUuidMaterialHistoryStatus(CommandSender sender) {
        rareMaterialHistory.status(sender);
    }

    @Subcommand("materialhistory view")
    @Syntax("[player]")
    @CommandCompletion("@players")
    public void viewRareMaterialHistory(CommandSender sender, @Optional String playerName) {
        rareMaterialHistory.open(sender, playerName);
    }

    @Subcommand("materialhistory uuidtrace")
    @Syntax("<player>")
    @CommandCompletion("@players")
    public void traceMaterialHistoryUuid(CommandSender sender, String playerName) {
        rareMaterialHistory.traceCurrentInventoryUuids(sender, playerName);
    }

    @Subcommand("materialhistory uuidtrace status")
    public void traceMaterialHistoryUuidStatus(CommandSender sender) {
        rareMaterialHistory.uuidTraceStatus(sender);
    }

    @Subcommand("materialhistory uuidtrace view")
    @Syntax("[player]")
    @CommandCompletion("@players")
    public void viewMaterialHistoryUuidTrace(CommandSender sender, @Optional String playerName) {
        rareMaterialHistory.openUuidTrace(sender, playerName);
    }

    @Subcommand("materialhistory uuidindex")
    public void scanMaterialHistoryUuidIndex(CommandSender sender) {
        materialUuidHistoryIndex.start(sender, rareMaterialHistory);
    }

    @Subcommand("materialhistory enchantrecords build")
    public void buildMaterialEnchantmentHistory(CommandSender sender) { materialEnchantmentHistory.build(sender); }

    @Subcommand("materialhistory enchantrecords status")
    public void materialEnchantmentHistoryStatus(CommandSender sender) { materialEnchantmentHistory.status(sender); }

    @Subcommand("materialhistory enchantrecords")
    @Syntax("[player]")
    @CommandCompletion("@players")
    public void viewMaterialEnchantmentHistory(CommandSender sender, @Optional String playerName) {
        materialEnchantmentHistory.open(sender, playerName);
    }

    @Subcommand("materialhistory enchantrecords item")
    @Syntax("<itemUuid>")
    public void viewItemEnchantmentHistory(CommandSender sender, String itemUuid) {
        materialEnchantmentHistory.openItem(sender, itemUuid);
    }

    @Subcommand("materialhistory enchantrecords export")
    @Syntax("<player>")
    @CommandCompletion("@players")
    public void exportMaterialEnchantmentHistory(CommandSender sender, String playerName) {
        materialEnchantmentHistory.export(sender, playerName);
    }

    @Subcommand("currenthistory")
    @Syntax("<player>")
    @CommandCompletion("@players")
    public void scanCurrentRareHistory(CommandSender sender, String playerName) {
        rareMaterialHistory.traceCurrentRareHistory(sender, playerName);
    }

    @Subcommand("currenthistory view")
    @Syntax("[player]")
    @CommandCompletion("@players")
    public void viewCurrentRareHistory(CommandSender sender, @Optional String playerName) {
        rareMaterialHistory.openCurrentRareHistory(sender, playerName);
    }

    @Subcommand("currenthistory status")
    public void currentRareHistoryStatus(CommandSender sender) {
        rareMaterialHistory.currentRareHistoryStatus(sender);
    }

    @Subcommand("materialhistory uuidindex status")
    public void scanMaterialHistoryUuidIndexStatus(CommandSender sender) {
        materialUuidHistoryIndex.status(sender);
    }

    @Subcommand("materialhistory uuidtransfer")
    @Syntax("<player>")
    @CommandCompletion("@players")
    public void viewTransferredUuidItems(CommandSender sender, String playerName) {
        rareMaterialHistory.openUuidTransfers(sender, playerName);
    }

    @Subcommand("materialhistory legitgem")
    public void viewTotallyLegitGemItems(CommandSender sender) {
        rareMaterialHistory.openTotallyLegitGemItems(sender);
    }

    @Subcommand("materialhistory legitgem usage")
    @Syntax("[player]")
    @CommandCompletion("@players")
    public void viewTotallyLegitGemUsage(CommandSender sender, @Optional String playerName) {
        rareMaterialHistory.openTotallyLegitGemUsage(sender, playerName);
    }

    @Subcommand("mythicdiff")
    @Syntax("[minimumChanges]")
    @CommandCompletion("5|10|20|50|100")
    public void scanEnchantedMythicDiff(CommandSender sender, @Optional Integer minimumChanges) {
        enchantedMythicDiff.start(sender, minimumChanges);
    }

    @Subcommand("mythicdiff status")
    public void scanEnchantedMythicDiffStatus(CommandSender sender) {
        enchantedMythicDiff.status(sender);
    }

    @Subcommand("phasebookmarked")
    public void scanPhaseBookMarkedMythics(CommandSender sender) {
        phaseBookMarkedMythics.start(sender);
    }

    @Subcommand("phasebookmarked status")
    public void scanPhaseBookMarkedMythicsStatus(CommandSender sender) {
        phaseBookMarkedMythics.status(sender);
    }
}
