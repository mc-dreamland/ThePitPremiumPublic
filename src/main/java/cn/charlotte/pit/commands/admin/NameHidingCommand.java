package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.nametag.hiding.NameHidingManager;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.command.CommandSender;

@CommandAlias("namehide")
@CommandPermission("pit.admin")
public class NameHidingCommand extends BaseCommand {
    @Subcommand("status")
    public void status(CommandSender sender) {
        var manager = NameHidingManager.getInstance();
        if (manager == null) { sender.sendMessage("§c玩家名隐藏服务尚未启动。"); return; }
        var config = manager.config();
        var rules = config.rules();
        sender.sendMessage("§e玩家名隐藏: " + rules.enabled() + "，P 区间: " + rules.minPrestige() + "~" + rules.maxPrestige());
        sender.sendMessage("§e区域: " + rules.areas());
        sender.sendMessage("§e显示豁免: 连杀 >= " + rules.showAboveStreak() + " 或赏金 >= " + rules.showAboveBounty() + "（-1 关闭对应豁免）");
        sender.sendMessage("§e纯数据包模式，检查 " + config.refreshTicks() + " tick，合并延迟 " + config.changeDelayTicks()
                + " tick；每 tick 最多处理 " + config.packetUpdatesPerTick() + " 对玩家，其中身份刷新最多 "
                + config.profileRefreshesPerTick() + " 对。待处理: " + manager.pendingUpdates()
                + (manager.faulted() ? " §c（异常保护：已暂停隐藏）" : ""));
        sender.sendMessage("§e识名道具: " + config.revealItemEnabled() + "，价格 " + config.price() + "，持续 " + config.durationSeconds() + " 秒。");
    }

    @Default
    public void help(CommandSender sender) { status(sender); }

    @Subcommand("reload")
    public void reload(CommandSender sender) {
        var manager = NameHidingManager.getInstance();
        if (manager == null) { sender.sendMessage("§c服务未启动，请检查启动日志并重启。"); return; }
        try {
            manager.reload();
            sender.sendMessage("§aname-hiding.yml 已重载，关闭时会恢复本功能修改的名牌。");
        } catch (Exception exception) {
            sender.sendMessage("§c重载失败，保留原配置: " + exception.getMessage());
        }
    }
}
