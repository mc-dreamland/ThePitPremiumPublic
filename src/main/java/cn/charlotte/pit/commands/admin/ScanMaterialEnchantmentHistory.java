package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static cn.charlotte.pit.commands.admin.MaterialEnchantmentHistory.*;

/** Build once from the local UUID index, then query per operator/equipment without database access. */
final class ScanMaterialEnchantmentHistory {
    private static final int PAGE_SIZE = 45;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    private final AtomicBoolean building = new AtomicBoolean();
    private final AtomicBoolean exporting = new AtomicBoolean();
    private final Set<UUID> busy = ConcurrentHashMap.newKeySet();
    private volatile Progress progress = new Progress(0, 0, 0, 0, 0, 0, 0);
    private volatile String lastReport, lastError;
    private volatile MaterialEnchantmentExport.Progress exportProgress = new MaterialEnchantmentExport.Progress(0, 0, 0, "未运行");
    private volatile String exportPlayer, lastExport, lastExportError;

    void build(CommandSender sender) {
        if (!building.compareAndSet(false, true)) { sender.sendMessage("§e附魔历史整理正在进行，请使用 /scan materialhistory enchantrecords status。"); return; }
        progress = new Progress(0, 0, 0, 0, 0, 0, 0); lastError = null;
        sender.sendMessage("§a开始异步整理本地 UUID 索引中的 records 和附魔净变化，不读取数据库。完成后按操作者、装备持久保存。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            try {
                var source = UuidDuplicateHistory.latestIndex(directory());
                if (source == null) throw new IllegalArgumentException("没有本地 UUID 索引，请先完成 /scan materialhistory uuidindex。");
                Path output = directory().resolve("enchant-record-history-" + LocalDateTime.now().format(FILE_TIME));
                Report report = MaterialEnchantmentHistory.build(source, output, update -> {
                    progress = update;
                    if (update.shards() % 32 == 0 || update.readEvents() % 10_000 == 0)
                        ThePit.getInstance().info("scan enchantrecords progress: shards=" + update.shards() + "/" + update.totalShards()
                                + ", readEvents=" + update.readEvents() + ", items=" + update.items() + ", records=" + update.records()
                                + ", changes=" + update.changes() + ", malformed=" + update.malformedItems());
                });
                lastReport = report.directory().toAbsolutePath().toString();
                Manifest manifest = report.manifest();
                ThePit.getInstance().info("scan enchantrecords finished: items=" + manifest.itemCount() + ", records="
                        + manifest.recordCount() + ", changes=" + manifest.changeCount() + ", report=" + lastReport);
                message(sender, "§a整理完成：装备 " + manifest.itemCount() + " 件，去重操作 " + manifest.recordCount()
                        + " 条，附魔净变化 " + manifest.changeCount() + " 次。使用 /scan materialhistory enchantrecords [玩家] 查看。");
                message(sender, "§7本地目录: " + lastReport + "；操作者来自 records，候选变化不能视为精确归因。");
                if (manifest.malformedItems() > 0 || manifest.sourceMetadata().scanErrors() > 0)
                    message(sender, "§e部分源记录解析异常或源索引不完整，缺失历史不代表没有操作，详情见 manifest.json。");
            } catch (Exception exception) {
                lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
                ThePit.getInstance().getSLF4JLogger().error("scan enchantrecords failed", exception);
                message(sender, "§c整理失败: " + lastError + "；未完成的目录不会作为查看结果。");
            } finally { building.set(false); }
        });
    }

    void status(CommandSender sender) {
        var snapshot = progress;
        sender.sendMessage(building.get() ? "§e本地附魔历史整理运行中。" : "§7当前没有附魔历史整理任务。");
        sender.sendMessage("§7分片: " + snapshot.shards() + "/" + snapshot.totalShards() + "，读取: " + snapshot.readEvents()
                + "，装备: " + snapshot.items() + "，记录: " + snapshot.records() + "，变化: " + snapshot.changes()
                + "，异常: " + snapshot.malformedItems());
        if (lastReport != null) sender.sendMessage("§7最近目录: " + lastReport);
        if (lastError != null) sender.sendMessage("§c最近错误: " + lastError);
        if (exporting.get()) {
            var export = exportProgress;
            sender.sendMessage("§e附魔记录导出中: " + exportPlayer + "；阶段: " + export.stage()
                    + "；装备: " + export.scannedItems() + "/" + export.totalItems() + "；读取 records: " + export.matchedRecords());
        }
        if (lastExport != null) sender.sendMessage("§7最近导出文件: " + lastExport);
        if (lastExportError != null) sender.sendMessage("§c最近导出错误: " + lastExportError);
    }

    void export(CommandSender sender, String playerName) {
        if (!exporting.compareAndSet(false, true)) {
            sender.sendMessage("§e已有附魔记录导出任务，请使用 /scan materialhistory enchantrecords status 查看进度。");
            return;
        }
        exportPlayer = playerName; lastExportError = null;
        exportProgress = new MaterialEnchantmentExport.Progress(0, 0, 0, "读取本地整理结果");
        sender.sendMessage("§a正在后台导出指定操作者的本地 records，包含附魔台、打宝石等全部操作来源，不查询数据库。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            try {
                Report report = requireReport();
                message(sender, "§7导出来源: " + report.directory().getFileName());
                var result = MaterialEnchantmentExport.export(report, playerName, directory().resolve("enchant-record-exports"),
                        ZoneId.systemDefault(), update -> {
                            exportProgress = update;
                            if (update.scannedItems() > 0 && (update.scannedItems() % 1000 == 0 || !"读取操作记录".equals(update.stage())))
                                ThePit.getInstance().info("scan enchantrecords export progress: player=" + playerName + ", stage=" + update.stage()
                                        + ", items=" + update.scannedItems() + "/" + update.totalItems() + ", readRecords=" + update.matchedRecords());
                        });
                lastExport = result.path().toAbsolutePath().toString();
                ThePit.getInstance().info("scan enchantrecords export finished: player=" + result.playerName() + ", records=" + result.records()
                        + ", items=" + result.scannedItems() + ", source=" + report.directory() + ", file=" + lastExport);
                message(sender, "§a导出完成: " + result.playerName() + "，去重记录 " + result.records() + " 条。按时间戳升序，一行一条。");
                message(sender, "§7文件: " + lastExport);
                message(sender, "§7格式: 时间 - 玩家名 - 装备UUID - 操作来源（含宝石）；时间使用服务器时区 " + ZoneId.systemDefault() + "。");
                message(sender, "§e仅包含本地已捕获的 records，并非完整累计次数；最后一列不是精确的单次附魔结果。");
                if (result.suspectRecords() > 0) message(sender, "§e部分 records 的时间晚于首次观察它的备份，已按原始时间保留，请人工核查时间可信度。");
                if (report.manifest().malformedItems() > 0 || (report.manifest().sourceMetadata() != null
                        && report.manifest().sourceMetadata().scanErrors() > 0))
                    message(sender, "§e源整理结果存在异常或缺失，导出不能补全未捕获的记录，请核对 manifest.json。");
            } catch (IllegalArgumentException exception) {
                lastExportError = exception.getMessage();
                message(sender, "§c" + lastExportError);
            } catch (Exception exception) {
                lastExportError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
                ThePit.getInstance().getSLF4JLogger().error("Local enchantment records export failed for " + playerName, exception);
                message(sender, "§c附魔记录导出失败: " + lastExportError + "；未查询数据库。");
            } finally { exporting.set(false); }
        });
    }

    void open(CommandSender sender, String playerName) {
        if (!(sender instanceof Player player)) { sender.sendMessage("请在游戏中打开界面；控制台可执行 enchantrecords build/status/export。"); return; }
        async(player, () -> {
            Report report = requireReport();
            ActorsMenu menu = new ActorsMenu(report, false);
            if (playerName == null || playerName.isBlank()) show(player, menu);
            else {
                Actor actor = report.manifest().actors().stream().filter(value -> value.playerName().equalsIgnoreCase(playerName)
                        || value.key().equalsIgnoreCase(playerName)).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("本地 records 中未找到该操作者: " + playerName + "（不是按持有人查询）。"));
                openActor(player, menu, actor, false);
            }
        });
    }

    void openItem(CommandSender sender, String uuid) {
        if (!(sender instanceof Player player)) { sender.sendMessage("请在游戏中打开装备历史界面。"); return; }
        openItem(player, uuid, null);
    }

    void openItem(Player player, String uuid, Menu backMenu) {
        if (!player.hasPermission("pit.admin")) { player.sendMessage("§c你没有查看装备附魔历史的权限。"); return; }
        if (uuid == null || uuid.isBlank()) { player.sendMessage("§c该物品没有 UUID，无法准确查询附魔历史。"); return; }
        async(player, () -> {
            Report report = requireReport();
            ItemReport equipment;
            try { equipment = item(report, uuid); }
            catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(exception.getMessage()
                        + "；如已更新 UUID 索引，请重新运行 /scan materialhistory enchantrecords build。");
            }
            Menu parent = backMenu == null ? new ActorsMenu(report, false) : backMenu;
            // Equipment lookup must include every recorded operator, not just its current inventory holder.
            show(player, new TimelineMenu(report, equipment, null, parent, true, false));
        });
    }

    private void openActor(Player player, ActorsMenu parent, Actor actor, boolean countSort) throws Exception {
        List<ActorItem> items = actorItems(parent.report, actor.key());
        if (countSort) items = items.stream().sorted(Comparator.comparingInt(ActorItem::recordCount).reversed()
                .thenComparingLong(ActorItem::firstTimestamp).thenComparing(ActorItem::itemUuid)).toList();
        show(player, new EquipmentMenu(parent, actor, items, countSort));
    }
    private void openEquipment(Player player, EquipmentMenu parent, String uuid) throws Exception {
        show(player, new TimelineMenu(parent.parent.report, item(parent.parent.report, uuid), parent,
                parent, false, false));
    }
    private static Path directory() { return ThePit.getInstance().getDataFolder().toPath().resolve("logs/scan/materialhistory"); }
    private static Report requireReport() throws Exception {
        Report report = latest(directory());
        if (report == null) throw new IllegalArgumentException("没有附魔整理结果，请先运行 /scan materialhistory enchantrecords build。");
        return report;
    }
    private interface Job { void run() throws Exception; }
    private void async(Player player, Job job) {
        if (!busy.add(player.getUniqueId())) { player.sendMessage("§e你的附魔历史查询仍在处理中。"); return; }
        player.sendMessage("§7正在后台读取本地附魔历史...");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            try { job.run(); }
            catch (IllegalArgumentException exception) { message(player, "§c" + exception.getMessage()); }
            catch (Exception exception) { ThePit.getInstance().getSLF4JLogger().error("Local enchantment history query failed", exception);
                message(player, "§c读取本地附魔记录失败，详情见控制台。未查询数据库。"); }
            finally { busy.remove(player.getUniqueId()); }
        });
    }
    private static void message(CommandSender sender, String text) { Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(text)); }
    private static void show(Player player, Menu menu) { Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> { if (player.isOnline()) menu.openMenu(player); }); }
    private static String date(long value) { return value > 0 ? DATE.format(Instant.ofEpochMilli(value)) : "未知"; }
    private static Material material(String value) { Material found = value == null ? null : Material.matchMaterial(value); return found == null || found.isAir() ? Material.ENCHANTED_BOOK : found; }
    private static String title(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static Button button(Material icon, String name, List<String> lore, BiConsumer<Player, ClickType> action) {
        return new Button() {
            @Override public ItemStack getButtonItem(Player player) { return new ItemBuilder(icon).name(name).lore(lore).build(); }
            @Override public void clicked(Player player, int slot, ClickType click, int hotbar, ItemStack current) { if (action != null) action.accept(player, click); }
        };
    }

    private abstract static class Paged extends Menu {
        int page;
        Paged() { setUpdateAfterClick(false); }
        @Override public int getSize() { return 54; }
        void navigation(Map<Integer, Button> buttons, int size, Menu parent, Report report) {
            if (page > 0) buttons.put(45, button(Material.ARROW, "&e上一页", List.of(), (player, click) -> { page--; openMenu(player); }));
            if ((long) (page + 1) * PAGE_SIZE < size) buttons.put(53, button(Material.ARROW, "&e下一页", List.of(), (player, click) -> { page++; openMenu(player); }));
            if (parent != null) buttons.put(49, button(Material.BARRIER, "&e返回", List.of(), (player, click) -> parent.openMenu(player)));
            buttons.put(50, button(Material.BOOK, "&b本地附魔历史", List.of("&7整理时间: &f" + date(report.manifest().generatedAt()),
                    "&7源索引时间: &f" + date(report.manifest().sourceMetadata().generatedAt()),
                    "&e按 records 操作者归类，不是备份持有人。", "&c只保留备份捕获的记录，可能不完整。",
                    "&c候选操作 ≠ 附魔变化的精确归因。", "&7查看、排序、翻页均不查数据库。"), null));
        }
    }

    private final class ActorsMenu extends Paged {
        final Report report; final boolean dates; final List<Actor> actors;
        ActorsMenu(Report report, boolean dates) {
            this.report = report; this.dates = dates;
            actors = dates ? report.manifest().actors().stream().sorted(Comparator.comparingLong(Actor::firstTimestamp).thenComparing(Actor::key)).toList()
                    : report.manifest().actors();
        }
        @Override public String getTitle(Player player) { return "附魔记录-按操作者 (" + (page + 1) + ")"; }
        @Override public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            for (int i = page * PAGE_SIZE; i < Math.min((page + 1) * PAGE_SIZE, actors.size()); i++) {
                Actor actor = actors.get(i);
                buttons.put(i % PAGE_SIZE, button(UNKNOWN_ACTOR.equals(actor.key()) ? Material.BARRIER : Material.PLAYER_HEAD,
                        "&e" + actor.playerName(), List.of("&7涉及装备: &f" + actor.itemCount(), "&7去重 records: &f" + actor.recordCount(),
                                "&7候选附魔变化: &f" + actor.candidateChanges(), "&7最早记录: &f" + date(actor.firstTimestamp()),
                                "&7末次记录: &f" + date(actor.lastTimestamp()), "&e点击查看装备"),
                        (viewer, click) -> async(viewer, () -> openActor(viewer, this, actor, false))));
            }
            navigation(buttons, actors.size(), null, report);
            buttons.put(48, button(Material.HOPPER, dates ? "&e排序：日期" : "&e排序：记录数", List.of("&7点击切换"),
                    (viewer, click) -> async(viewer, () -> show(viewer, new ActorsMenu(report, !dates)))));
            return buttons;
        }
    }

    private final class EquipmentMenu extends Paged {
        final ActorsMenu parent; final Actor actor; final List<ActorItem> items; final boolean countSort;
        EquipmentMenu(ActorsMenu parent, Actor actor, List<ActorItem> items, boolean countSort) {
            this.parent = parent; this.actor = actor; this.items = items; this.countSort = countSort;
        }
        @Override public String getTitle(Player player) { return actor.playerName() + "-附魔装备 (" + (page + 1) + ")"; }
        @Override public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            for (int i = page * PAGE_SIZE; i < Math.min((page + 1) * PAGE_SIZE, items.size()); i++) {
                ActorItem item = items.get(i);
                buttons.put(i % PAGE_SIZE, button(material(item.material()), title(item.displayName(), "&e" + item.itemUuid()),
                        List.of("&7UUID: &f" + item.itemUuid(), "&7该操作者 records: &f" + item.recordCount(),
                                "&7候选变化: &f" + item.candidateChanges(), "&7同 UUID 状态冲突: &f" + item.conflicts(),
                                "&7最早记录: &f" + date(item.firstTimestamp()), "&7末次记录: &f" + date(item.lastTimestamp()),
                                "&e点击按时间查看记录；可切换全部操作者"),
                        (viewer, click) -> async(viewer, () -> openEquipment(viewer, this, item.itemUuid()))));
            }
            navigation(buttons, items.size(), parent, parent.report);
            buttons.put(48, button(Material.HOPPER, countSort ? "&e排序：记录数" : "&e排序：日期", List.of("&7点击切换"),
                    (viewer, click) -> async(viewer, () -> openActor(viewer, parent, actor, !countSort))));
            int actorIndex = parent.actors.indexOf(actor);
            for (int direction : new int[]{-1, 1}) {
                int next = actorIndex + direction;
                if (next >= 0 && next < parent.actors.size()) buttons.put(direction < 0 ? 46 : 52,
                        button(Material.ARROW, direction < 0 ? "&e上一个操作者" : "&e下一个操作者", List.of(parent.actors.get(next).playerName()),
                                (viewer, click) -> async(viewer, () -> openActor(viewer, parent, parent.actors.get(next), false))));
            }
            return buttons;
        }
    }

    private record Entry(long timestamp, Operation operation, Change change, Conflict conflict) { }
    private final class TimelineMenu extends Paged {
        final Report report; final ItemReport item; final EquipmentMenu equipment; final Menu parent;
        final boolean all, reversed; final List<Entry> entries; final int equipmentIndex;
        TimelineMenu(Report report, ItemReport item, EquipmentMenu equipment, Menu parent, boolean all, boolean reversed) {
            this.report = report; this.item = item; this.equipment = equipment; this.parent = parent; this.all = all; this.reversed = reversed;
            String actor = equipment == null || all ? null : equipment.actor.key();
            List<Entry> list = new ArrayList<>();
            for (Operation operation : item.operations()) if (actor == null || actor.equals(operation.id.actorKey()))
                list.add(new Entry(operation.id.timestamp(), operation, null, null));
            for (Change change : item.changes()) if (actor == null || (UNKNOWN_ACTOR.equals(actor)
                    && !"ONE_RECORDED_CANDIDATE".equals(change.attribution()))
                    || change.recordedCandidates().stream().anyMatch(candidate -> actor.equals(candidate.actorKey())))
                list.add(new Entry(change.afterEvidence().timestamp(), null, change, null));
            item.conflicts().forEach(conflict -> list.add(new Entry(conflict.evidence().timestamp(), null, null, conflict)));
            Comparator<Entry> order = Comparator.comparingLong(Entry::timestamp);
            entries = list.stream().sorted(reversed ? order.reversed() : order).toList();
            equipmentIndex = equipment == null ? -1 : java.util.stream.IntStream.range(0, equipment.items.size())
                    .filter(index -> equipment.items.get(index).itemUuid().equals(item.itemUuid())).findFirst().orElse(-1);
        }
        @Override public String getTitle(Player player) { return "装备附魔时间线 (" + (page + 1) + ")"; }
        @Override public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            for (int i = page * PAGE_SIZE; i < Math.min((page + 1) * PAGE_SIZE, entries.size()); i++) {
                Entry entry = entries.get(i);
                List<String> lore = new ArrayList<>(); lore.add("&7装备 UUID: &f" + item.itemUuid());
                if (entry.operation != null) {
                    Operation operation = entry.operation;
                    lore.add("&7记录中的操作者: &a" + operation.playerName);
                    lore.add("&7操作来源: &f" + operation.id.source());
                    lore.add("&7操作时间: &f" + date(operation.id.timestamp()));
                    if (operation.timestampAfterFirstEvidence) lore.add("&c记录时间晚于首次备份，时间可信度异常。");
                    evidence(lore, "首次观察该记录", operation.firstSeen);
                    lore.add("&7末次观察: &f" + date(operation.lastSeen.timestamp()));
                    lore.add("&7重复出现于备份: &f" + operation.observedBackups + "（只计一次操作）");
                    enchantments(lore, operation.firstObservedEnchantments);
                    original(lore, "首次观察时的 Lore（不是精确操作结果）", operation.firstObservedLore);
                    buttons.put(i % PAGE_SIZE, button(Material.ENCHANTED_BOOK, "&a操作记录 " + date(entry.timestamp), lore, null));
                } else if (entry.change != null) {
                    Change change = entry.change;
                    lore.add("&e备份间观察到的附魔净变化，不是单次操作日志。");
                    evidence(lore, "之前", change.beforeEvidence()); evidence(lore, "之后", change.afterEvidence());
                    lore.add("&7阶数: &f" + change.before().tier() + " → " + change.after().tier());
                    for (Delta delta : change.differences()) lore.add("&b" + enchantName(delta.enchantment()) + ": &f" + delta.before() + " → " + delta.after());
                    lore.add("&7关联情况: &e" + attribution(change.attribution()));
                    for (OperationId candidate : change.recordedCandidates()) lore.add("&7候选: &f" + candidate.actorKey().replaceFirst("^player:", "")
                            + " / " + candidate.source() + " / " + date(candidate.timestamp()));
                    lore.add("&e点击分别查看前后状态及完整 Lore");
                    buttons.put(i % PAGE_SIZE, button(Material.BOOK, "&e附魔变化 " + date(entry.timestamp), lore,
                            (viewer, click) -> new ComparisonMenu(change, this).openMenu(viewer)));
                } else {
                    lore.add("&c同一份备份中，同 UUID 存在不同附魔状态，不能按单件装备推断。");
                    evidence(lore, "冲突备份", entry.conflict.evidence());
                    for (State state : entry.conflict.states()) enchantments(lore, state.enchantments());
                    buttons.put(i % PAGE_SIZE, button(Material.BARRIER, "&c状态冲突 " + date(entry.timestamp), lore, null));
                }
            }
            navigation(buttons, entries.size(), parent, report);
            buttons.put(48, button(Material.HOPPER, reversed ? "&e日期：新 → 旧" : "&e日期：旧 → 新", List.of("&7点击切换"),
                    (viewer, click) -> async(viewer, () -> show(viewer, new TimelineMenu(report, item, equipment, parent, all, !reversed)))));
            if (equipment != null) buttons.put(47, button(Material.PLAYER_HEAD, all ? "&e当前：全部操作者" : "&e当前：" + equipment.actor.playerName(),
                    List.of("&7点击切换全部/指定操作者"), (viewer, click) -> async(viewer,
                            () -> show(viewer, new TimelineMenu(report, item, equipment, parent, !all, reversed)))));
            if (equipmentIndex >= 0) for (int direction : new int[]{-1, 1}) {
                int next = equipmentIndex + direction;
                if (next >= 0 && next < equipment.items.size()) buttons.put(direction < 0 ? 46 : 52,
                        button(Material.ARROW, direction < 0 ? "&e上一件装备" : "&e下一件装备", List.of(),
                                (viewer, click) -> async(viewer, () -> openEquipment(viewer, equipment, equipment.items.get(next).itemUuid()))));
            }
            return buttons;
        }
    }

    private static final class ComparisonMenu extends Menu {
        final Change change; final Menu parent;
        ComparisonMenu(Change change, Menu parent) { this.change = change; this.parent = parent; setUpdateAfterClick(false); }
        @Override public int getSize() { return 27; }
        @Override public String getTitle(Player player) { return "附魔变动-前后状态"; }
        @Override public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            for (boolean after : new boolean[]{false, true}) {
                State state = after ? change.after() : change.before();
                List<String> lore = new ArrayList<>(); evidence(lore, after ? "之后" : "之前", after ? change.afterEvidence() : change.beforeEvidence());
                lore.add("&7阶数: &f" + state.tier()); enchantments(lore, state.enchantments()); original(lore, "完整原始 Lore", state.lore());
                buttons.put(after ? 15 : 11, button(material(state.material()), (after ? "&a之后 " : "&e之前 ") + title(state.displayName(), state.internalName()), lore, null));
            }
            buttons.put(22, button(Material.BARRIER, "&e返回时间线", List.of(), (viewer, click) -> parent.openMenu(viewer)));
            return buttons;
        }
    }
    private static void evidence(List<String> lore, String label, Evidence evidence) {
        lore.add("&7" + label + ": &f" + date(evidence.timestamp()));
        lore.add("&7备份持有人: &f" + title(evidence.playerName(), evidence.playerUuid()) + "（非操作者认定）");
        lore.add("&7Mongo _id: &f" + title(evidence.backupDatabaseId(), "未知"));
        lore.add("&7备份 UUID: &f" + title(evidence.backupUuid(), "未知"));
        lore.add("&7位置: &f" + String.join(", ", evidence.locations()));
    }
    private static String enchantName(String id) {
        var factor = ThePit.getInstance().getEnchantmentFactor();
        var enchantment = factor == null ? null : factor.getEnchantmentMap().get(id);
        return enchantment == null ? id : enchantment.getEnchantName() + " &8(" + id + ")";
    }
    private static void enchantments(List<String> lore, Map<String, Integer> values) {
        if (values.isEmpty()) lore.add("&7附魔: &f无");
        new TreeMap<>(values).forEach((name, level) -> lore.add("&7附魔: &f" + enchantName(name) + " &f" + level));
    }
    private static void original(List<String> lore, String label, List<String> original) {
        lore.add(""); lore.add("&8" + label + ":"); lore.addAll(original); lore.add("");
    }
    private static String attribution(String value) {
        return switch (value) {
            case "ONE_RECORDED_CANDIDATE" -> "一条已记录候选，不能精确归因";
            case "MULTIPLE_RECORDED_CANDIDATES" -> "多条操作，无法逐次还原";
            case "AMBIGUOUS_COPY_OR_ORDER" -> "同 UUID 多副本或时间顺序不明确";
            default -> "缺少时间区间内的操作记录，操作者未知";
        };
    }
}
