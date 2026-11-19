package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.bot.MythicMobsVersion;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.List;

@CommandAlias("pitbot")
@CommandPermission("pit.admin")
public final class PitBotCommand extends BaseCommand {
    private PitBotManager manager(CommandSender sender) {
        PitBotManager manager = PitBotManager.getInstance();
        if (manager == null) sender.sendMessage("§cBot 功能未加载，请检查启动日志和 MythicMobs " + MythicMobsVersion.REQUIRED + "。");
        return manager;
    }

    @Default
    @Subcommand("status")
    public void status(CommandSender sender) {
        PitBotManager manager = manager(sender);
        if (manager != null) sender.sendMessage("§a" + manager.status());
        sender.sendMessage("§e/pitbot pos1|pos2 training §7设置训练场区域，玩家直接步行进出");
        sender.sendMessage("§e/pitbot add §7将当前位置添加为野外刷新点");
        sender.sendMessage("§e/pitbot list [页码] | remove <编号> §7查看/删除野外刷新点");
        sender.sendMessage("§e/pitbot count §7查看存活、排队和本次启动累计刷新数量");
        sender.sendMessage("§e/pitbot spawn <training|wild> <数量> §7强制分批刷新，仍遵守数量上限");
        sender.sendMessage("§e/pitbot reload §7重载 bots.yml");
    }

    @Subcommand("pos1")
    @CommandCompletion("training")
    public void pos1(Player player, String kind) { position(player, kind, 1); }

    @Subcommand("pos2")
    @CommandCompletion("training")
    public void pos2(Player player, String kind) { position(player, kind, 2); }

    private void position(Player player, String value, int corner) {
        PitBotManager manager = manager(player);
        if (manager == null) return;
        try {
            manager.setPosition(PitBotManager.Kind.parse(value), corner, player.getLocation());
            player.sendMessage("§a已保存 " + value + " 的角点 " + corner + "。区域需涵盖 Bot 的两格身高。设置坐标会清理现有 Bot。");
        } catch (IllegalArgumentException | IOException ex) { player.sendMessage("§c保存失败: " + ex.getMessage()); }
    }

    @Subcommand("add")
    public void add(Player player) {
        PitBotManager manager = manager(player);
        if (manager == null) return;
        try {
            int id = manager.addWildSpawnPoint(player.getLocation());
            player.sendMessage("§a已添加野外刷新点 #" + id + "，自动保存并生效。");
        } catch (IllegalArgumentException | IOException ex) { player.sendMessage("§c添加失败: " + ex.getMessage()); }
    }

    @Subcommand("list")
    @Syntax("[页码]")
    public void list(CommandSender sender, @Optional Integer page) {
        PitBotManager manager = manager(sender);
        if (manager == null) return;
        List<String> points = manager.wildSpawnPointDescriptions();
        int pages = Math.max(1, (points.size() + 9) / 10);
        int selected = page == null ? 1 : page;
        if (selected < 1 || selected > pages) {
            sender.sendMessage("§c页码范围 1～" + pages);
            return;
        }
        sender.sendMessage("§a野外刷新点共 " + points.size() + " 个，第 " + selected + "/" + pages + " 页");
        for (int i = (selected - 1) * 10; i < Math.min(selected * 10, points.size()); i++) sender.sendMessage("§e" + points.get(i));
    }

    @Subcommand("remove")
    @Syntax("<刷新点编号>")
    public void remove(CommandSender sender, int id) {
        PitBotManager manager = manager(sender);
        if (manager == null) return;
        try {
            manager.removeWildSpawnPoint(id);
            sender.sendMessage("§a已删除刷新点及该点刷出的 Bot；后续编号已顺移，请用 /pitbot list 查看。");
        } catch (IllegalArgumentException | IOException ex) { sender.sendMessage("§c删除失败: " + ex.getMessage()); }
    }

    @Subcommand("reload")
    public void reload(CommandSender sender) {
        PitBotManager manager = manager(sender);
        if (manager == null) return;
        manager.reload();
        sender.sendMessage("§a" + manager.status());
    }

    @Subcommand("count")
    public void count(CommandSender sender) {
        PitBotManager manager = manager(sender);
        if (manager != null) manager.countDescriptions().forEach(line -> sender.sendMessage("§a" + line));
    }

    @Subcommand("spawn")
    @CommandCompletion("training|wild")
    @Syntax("<training|wild> <数量>")
    public void spawn(CommandSender sender, String area, int amount) {
        PitBotManager manager = manager(sender);
        if (manager == null) return;
        try {
            PitBotManager.Kind kind = PitBotManager.Kind.parse(area);
            int accepted = manager.enqueueForcedSpawn(kind, amount, sender);
            sender.sendMessage("§a已排队强制刷新 " + area + " " + accepted + " 只，每 5 tick 最多尝试生成 1 只，完成后反馈实际数量。"
                    + (accepted < amount ? "§e 请求 " + amount + " 只，按剩余名额限制为 " + accepted + " 只。" : ""));
            sender.sendMessage("§7无需附近有低 P 玩家，但要求配置有效、区块已加载且空间安全；强制 Bot 默认保留 5 分钟。");
        } catch (IllegalArgumentException ex) { sender.sendMessage("§c强制刷新失败: " + ex.getMessage()); }
    }
}
