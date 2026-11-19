package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import com.mongodb.WriteConcern;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static cn.charlotte.pit.commands.admin.UuidDuplicateLocalReport.*;

/** Local report GUIs with explicit UUID blacklist actions. All database access runs on workers. */
final class ScanUuidReportView {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());
    private final Set<UUID> busy = ConcurrentHashMap.newKeySet();
    private final UuidReportBlacklistState blacklist = new UuidReportBlacklistState();

    void open(CommandSender sender, boolean history, String filter) {
        if (!(sender instanceof Player player)) { sender.sendMessage("§c该界面只能由玩家打开。"); return; }
        async(player, () -> openLatest(player, history ? Mode.HISTORY : Mode.CURRENT, filter));
    }

    private void openLatest(Player player, Mode mode, String filter) throws Exception {
        Path directory = ThePit.getInstance().getDataFolder().toPath().resolve("logs/scan/uuid");
        Report report = latest(directory, mode);
        if (report == null) throw new IllegalArgumentException(mode == Mode.HISTORY
                ? "没有本地 UUID 历史报告，请先运行 /scan uuid history。" : "没有本地重复 UUID 报告，请先运行 /scan uuid。");
        openPage(player, report, filter, 0);
    }

    private void openPage(Player player, Report report, String filter, int number) throws Exception {
        Page contents = page(report, filter, number);
        loadBlacklist(player, contents.items().stream().map(Summary::itemUuid).toList());
        show(player, new GroupsMenu(report, filter, contents));
    }

    private void openGroup(Player player, GroupsMenu parent, String uuid) throws Exception {
        Group group = group(parent.report, uuid);
        loadBlacklist(player, List.of(group.itemUuid()));
        show(player, parent.report.mode() == Mode.CURRENT ? new LocationsMenu(parent.report, group, parent, 0)
                : new HistoryMenu(parent.report, group, parent));
    }

    private interface Job { void run() throws Exception; }
    private void async(Player player, Job job) {
        if (!busy.add(player.getUniqueId())) { player.sendMessage("§e你的重复 UUID 界面查询仍在处理中，请稍候。"); return; }
        player.sendMessage("§7正在后台读取本地重复 UUID 记录和黑名单状态...");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            try { job.run(); }
            catch (IllegalArgumentException exception) { message(player, "§c" + exception.getMessage()); }
            catch (Exception exception) {
                ThePit.getInstance().getSLF4JLogger().error("Local duplicate UUID report view failed", exception);
                message(player, "§c读取重复 UUID 界面数据失败，详情见控制台。");
            } finally { busy.remove(player.getUniqueId()); }
        });
    }
    private static void show(Player player, Menu menu) {
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> { if (player.isOnline()) menu.openMenu(player); });
    }
    private static void message(Player player, String message) {
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> { if (player.isOnline()) player.sendMessage(message); });
    }
    private static String date(long time) { return time > 0 ? DATE.format(Instant.ofEpochMilli(time)) : "无 / 未知"; }
    private static String text(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static Material material(String value) {
        Material found = value == null ? null : Material.matchMaterial(value);
        return found == null || found.isAir() ? Material.ENCHANTED_BOOK : found;
    }
    private static Button button(Material material, String name, List<String> lore, Consumer<Player> action) {
        return new Button() {
            @Override public ItemStack getButtonItem(Player player) { return new ItemBuilder(material).name(name).lore(lore).build(); }
            @Override public void clicked(Player player, int slot, ClickType click, int hotbar, ItemStack current) {
                if (action != null) action.accept(player);
            }
        };
    }

    private void loadBlacklist(Player player, Collection<String> requested) {
        Set<String> candidates = UuidReportBlacklistState.candidates(requested);
        if (candidates.isEmpty()) return;
        long readRevision = blacklist.revision();
        try {
            Set<String> active = new HashSet<>();
            for (Document document : ThePit.getInstance().getMongoDB().getItemUuidBlacklistCollection()
                    .find(Filters.and(Filters.or(Filters.in("itemUuid", candidates), Filters.in("_id", candidates)),
                            Filters.ne("active", false)))
                    .projection(Projections.include("itemUuid", "_id"))) {
                Object raw = document.get("itemUuid");
                if (!(raw instanceof String)) raw = document.get("_id");
                if (raw instanceof String uuid) active.add(uuid);
            }
            blacklist.loaded(candidates, active, readRevision);
        } catch (Exception exception) {
            blacklist.failed(candidates, readRevision);
            ThePit.getInstance().getSLF4JLogger().warn("Unable to load duplicate report UUID blacklist status", exception);
            message(player, "§e黑名单状态读取失败，报告仍可查看；请刷新黑名单状态后再操作。");
        }
    }

    private Button itemButton(View owner, String uuid, Material material, String name, List<String> lore,
                              Map<String, Object> context, Consumer<Player> action) {
        return new Button() {
            @Override public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(material).name(blacklist.name(uuid, name)).lore(blacklist.lore(uuid, lore)).build();
            }
            @Override public void clicked(Player player, int slot, ClickType click, int hotbar, ItemStack current) {
                if (click == ClickType.SHIFT_RIGHT) addToBlacklist(player, owner, uuid, context);
                else if (action != null) action.accept(player);
            }
        };
    }

    private void addToBlacklist(Player player, View owner, String requestedUuid, Map<String, Object> context) {
        if (!player.hasPermission("pit.admin")) { player.sendMessage("§c你没有修改物品 UUID 黑名单的权限。"); return; }
        UuidReportBlacklistState.Decision decision = blacklist.begin(requestedUuid);
        if (decision != UuidReportBlacklistState.Decision.STARTED) {
            player.sendMessage(switch (decision) {
                case INVALID -> "§c该记录没有有效的物品 UUID，不能拉黑。";
                case UNKNOWN -> "§e黑名单状态未知，请先点击下方刷新按钮再操作。";
                case PENDING -> "§e该 UUID 正在写入黑名单，请勿重复操作。";
                case ALREADY_BLACKLISTED -> "§e该物品 UUID 已在回收黑名单中。";
                default -> throw new IllegalStateException("Unexpected blacklist decision");
            });
            return;
        }
        String uuid = UuidReportBlacklistState.normalize(requestedUuid);
        Map<String, Object> source = new LinkedHashMap<>(context);
        if (owner.report.historyIndex() != null) source.put("historyIndex", owner.report.historyIndex());
        if (owner.report.duplicateReport() != null) source.put("duplicateReport", owner.report.duplicateReport());
        Map<String, Object> audit = UuidReportBlacklistState.audit(uuid, player.getUniqueId().toString(), player.getName(),
                System.currentTimeMillis(), owner.report.mode() == Mode.HISTORY ? "UUID_DUPLICATE_HISTORY_VIEW" : "UUID_DUPLICATE_VIEW",
                owner.report.path().toAbsolutePath().toString(), owner.report.generatedAt(), source);
        UuidReportBlacklistState.Write write = UuidReportBlacklistState.write(audit);
        player.sendMessage("§7正在将物品 UUID §f" + uuid + " §7加入回收黑名单，影响该 UUID 的所有副本...");
        refreshVisible(player);
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            Exception failure = null;
            try {
                var result = ThePit.getInstance().getMongoDB().getItemUuidBlacklistCollection()
                        .withWriteConcern(WriteConcern.ACKNOWLEDGED).updateOne(Filters.eq("_id", uuid),
                                new Document("$setOnInsert", new Document(write.setOnInsert()))
                                        .append("$set", new Document(write.set())), new UpdateOptions().upsert(true));
                if (!result.wasAcknowledged()) throw new IllegalStateException("UUID blacklist write was not acknowledged");
            } catch (Exception exception) { failure = exception; }
            Exception writeFailure = failure;
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                blacklist.finish(uuid, writeFailure == null);
                if (writeFailure != null) ThePit.getInstance().getSLF4JLogger().error("Unable to blacklist report item UUID " + uuid, writeFailure);
                if (!player.isOnline()) return;
                player.sendMessage(writeFailure == null ? "§a物品 UUID §f" + uuid + " §a已加入回收黑名单；持有者下次登录时按现有规则审计回收。"
                        : "§c黑名单写入未确认，请刷新状态后核查；本次未直接回收物品。");
                refreshVisible(player);
            });
        });
    }

    private void refreshVisible(Player player) {
        if (Menu.currentlyOpenedMenus.get(player.getUniqueId()) instanceof View view) view.openMenu(player);
    }

    private static Map<String, Object> context(Object... values) {
        Map<String, Object> context = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) if (values[i + 1] != null) context.put((String) values[i], values[i + 1]);
        return context;
    }
    private static String status(String value) {
        return switch (text(value, "")) {
            case "SAME_BACKUP_DUPLICATE" -> "&c同一份备份内存在多份";
            case "CROSS_PLAYER_CANDIDATE_ONLY" -> "&e仅有跨玩家时间接近候选";
            case "NO_LOCAL_HISTORY" -> "&7本地无历史记录";
            case "NO_SIMULTANEOUS_EVIDENCE" -> "&7未发现同时持有证据";
            default -> "&7" + text(value, "未说明");
        };
    }
    private static void originalLore(List<String> target, List<String> original) {
        target.add(""); target.add("&8完整原始 Lore:");
        if (original == null) target.add("&e报告未保存完整 Lore，不能还原当时状态。");
        else if (original.isEmpty()) target.add("&7（无）");
        else original.stream().filter(Objects::nonNull).forEach(target::add);
    }

    private abstract class View extends Menu {
        final Report report;
        View(Report report) { this.report = report; setUpdateAfterClick(false); }
        abstract Collection<String> visibleUuids();
        @Override public int getSize() { return 54; }
        void navigation(Map<Integer, Button> buttons, Report report, Menu parent, int page, long total, IntConsumer move) {
            if (page > 0) buttons.put(45, button(Material.ARROW, "&e上一页", List.of(), player -> move.accept(page - 1)));
            if ((long) (page + 1) * PAGE_SIZE < total)
                buttons.put(53, button(Material.ARROW, "&e下一页", List.of(), player -> move.accept(page + 1)));
            if (parent != null) buttons.put(49, button(Material.BARRIER, "&e返回", List.of(), parent::openMenu));
            List<String> info = new ArrayList<>(List.of("&7报告: &f" + report.path().getFileName(),
                    "&7生成时间: &f" + date(report.generatedAt()), "&7物品信息仅读本地报告，不重新扫描玩家存档。",
                    "&7黑名单状态和写入异步访问现有数据库。",
                    "&7结果属于报告生成时的数据，不是在线实时背包。"));
            if (report.mode() == Mode.HISTORY) {
                info.add("&7跨玩家候选时间窗口: &f" + report.windowSeconds() + " 秒");
                info.add("&c时间接近不证明同时持有，也不能认定复制者。");
                info.add("&e最早仅表示本地已收录备份中最早，记录可能不完整。");
                if (report.indexVersion() < 2) info.add("&e旧版 v1 索引可能遗漏数量变化。");
                if (report.scanErrors() > 0 || report.invalidEvents() > 0)
                    info.add("&c源索引异常: " + report.scanErrors() + "；无效记录: " + report.invalidEvents());
            }
            buttons.put(50, button(Material.BOOK, "&b本地重复 UUID 报告", info, null));
            buttons.put(52, button(Material.LIME_DYE, "&e刷新黑名单状态", List.of("&7仅批量查询当前页物品 UUID 的黑名单标识。"),
                    viewer -> async(viewer, () -> {
                        loadBlacklist(viewer, visibleUuids());
                        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                            if (viewer.isOnline() && Menu.currentlyOpenedMenus.get(viewer.getUniqueId()) == this) openMenu(viewer);
                        });
                    })));
        }
    }

    private final class GroupsMenu extends View {
        final String filter;
        final Page contents;
        GroupsMenu(Report report, String filter, Page contents) { super(report); this.filter = filter; this.contents = contents; }
        @Override Collection<String> visibleUuids() { return contents.items().stream().map(Summary::itemUuid).toList(); }
        @Override public String getTitle(Player player) {
            return (report.mode() == Mode.HISTORY ? "重复UUID历史线索" : "当前存档重复UUID") + " (" + (contents.number() + 1) + ")";
        }
        @Override public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            for (int i = 0; i < contents.items().size(); i++) {
                Summary summary = contents.items().get(i);
                List<String> lore = new ArrayList<>(List.of("&7物品 UUID: &f" + summary.itemUuid(),
                        "&7当前报告重复位置: &e" + summary.count(), "&7当前报告涉及玩家: &e" + summary.holders()));
                if (report.mode() == Mode.HISTORY) {
                    lore.add("&7历史结论: " + status(summary.status()));
                    lore.add("&7最早重复 / 候选线索: &f" + date(summary.clueTimestamp()));
                    lore.add("&c跨玩家候选不能认定复制。");
                }
                lore.add("&e点击查看" + (report.mode() == Mode.HISTORY ? "历史线索和物品位置" : "各份物品及完整 Lore"));
                buttons.put(i, itemButton(this, summary.itemUuid(), material(summary.material()), text(summary.displayName(), "&e重复 UUID 物品"), lore,
                        context("displayName", summary.displayName(), "material", summary.material(), "duplicateCount", summary.count(),
                                "historyStatus", summary.status(), "clueTimestamp", summary.clueTimestamp()),
                        viewer -> async(viewer, () -> openGroup(viewer, this, summary.itemUuid()))));
            }
            if (contents.items().isEmpty()) buttons.put(22, button(Material.BARRIER, "&e没有符合条件的重复 UUID", List.of("&7可更换筛选条件或重新运行扫描。"), null));
            navigation(buttons, report, null, contents.number(), contents.total(),
                    number -> async(player, () -> openPage(player, report, filter, number)));
            buttons.put(48, button(Material.PAPER, "&e共 " + contents.total() + " 个重复 UUID", List.of(
                    "&7筛选: &f" + text(filter, "全部"), "&7按本地报告顺序排列，历史报告按最早线索时间。"), null));
            buttons.put(47, button(Material.COMPASS, report.mode() == Mode.CURRENT ? "&e切换历史报告" : "&e切换当前存档报告", List.of(
                    "&7打开另一类最新本地报告，保留筛选条件。"), viewer -> async(viewer,
                    () -> openLatest(viewer, report.mode() == Mode.CURRENT ? Mode.HISTORY : Mode.CURRENT, filter))));
            if (filter != null && !filter.isBlank()) buttons.put(51, button(Material.MILK_BUCKET, "&e清除筛选", List.of(),
                    viewer -> async(viewer, () -> openPage(viewer, report, null, 0))));
            return buttons;
        }
    }

    private final class LocationsMenu extends View {
        final Group group; final Menu parent; final int page;
        LocationsMenu(Report report, Group group, Menu parent, int page) {
            super(report); this.group = group; this.parent = parent; this.page = page;
        }
        @Override Collection<String> visibleUuids() { return List.of(group.itemUuid()); }
        @Override public String getTitle(Player player) { return "重复物品-持有人和位置 (" + (page + 1) + ")"; }
        @Override public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            List<Location> locations = group.locations();
            int start = page * PAGE_SIZE, end = Math.min(start + PAGE_SIZE, locations.size());
            for (int i = start; i < end; i++) {
                Location location = locations.get(i);
                List<String> lore = new ArrayList<>(List.of("&7物品 UUID: &f" + group.itemUuid(),
                        "&7持有人: &e" + text(location.playerName(), "未知"), "&7玩家 UUID: &f" + text(location.playerUuid(), "未知"),
                        "&7位置: &f" + position(location), "&7原始槽位（从 0 开始）: &f" + location.slot()));
                if (location.holderItemInternalName() != null) lore.add("&7收纳物品: &f" + location.holderItemInternalName());
                if (location.nestedPath() != null) lore.add("&7嵌套位置: &f" + location.nestedPath());
                if (location.color() != null) lore.add("&7皮革颜色: &f" + location.color());
                originalLore(lore, location.lore());
                buttons.put(i - start, itemButton(this, group.itemUuid(), material(location.material()), text(location.displayName(), "&e重复 UUID 物品"), lore,
                        context("playerUuid", location.playerUuid(), "playerName", location.playerName(), "container", location.container(),
                                "warehouseIndex", location.warehouseIndex(), "slot", location.slot(), "holderItemInternalName", location.holderItemInternalName(),
                                "nestedPath", location.nestedPath(), "displayName", location.displayName(), "material", location.material(), "lore", location.lore()), null));
            }
            navigation(buttons, report, parent, page, locations.size(), number -> new LocationsMenu(report, group, parent, number).openMenu(player));
            if (report.mode() == Mode.CURRENT) buttons.put(47, button(Material.CLOCK, "&e查看此 UUID 的历史线索", List.of(
                    "&7读取最新 uuid history 报告，不自动运行历史追查。"), viewer -> async(viewer, () -> openLatest(viewer, Mode.HISTORY, group.itemUuid()))));
            return buttons;
        }
    }

    private final class HistoryMenu extends View {
        final Group group; final Menu parent;
        HistoryMenu(Report report, Group group, Menu parent) { super(report); this.group = group; this.parent = parent; }
        @Override Collection<String> visibleUuids() { return List.of(group.itemUuid()); }
        @Override public String getTitle(Player player) { return "重复UUID-历史线索"; }
        @Override public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            History history = group.history();
            List<Evidence> evidence = evidence(history);
            for (int i = 0; i < evidence.size(); i++) {
                Evidence entry = evidence.get(i);
                var observation = entry.observation();
                List<String> lore = new ArrayList<>();
                entry.labels().forEach(label -> lore.add("&e" + label));
                observationInfo(lore, group.itemUuid(), observation);
                lore.add("&c转手或时间接近本身不证明复制。");
                lore.add("&e点击查看此备份中的各份物品和完整 Lore");
                buttons.put(i, itemButton(this, group.itemUuid(), Material.CHEST, "&e" + date(observation.timestamp()) + " &f" + text(observation.playerName(), "未知"), lore,
                        context("playerUuid", observation.playerUuid(), "playerName", observation.playerName(), "backupDatabaseId", observation.backupDatabaseId(),
                                "backupUuid", observation.backupUuid(), "timestamp", observation.timestamp(), "historyStatus", history.status(), "evidenceLabels", entry.labels()),
                        viewer -> async(viewer, () -> {
                            Path materialDirectory = ThePit.getInstance().getDataFolder().toPath().resolve("logs/scan/materialhistory");
                            HistoricalDetails details = historicalDetails(report, observation, materialDirectory);
                            show(viewer, new HistoricalItemsMenu(report, group.itemUuid(), entry, details, this, 0));
                        })));
            }
            navigation(buttons, report, parent, 0, evidence.size(), ignored -> {});
            List<String> conclusion = new ArrayList<>(List.of("&7物品 UUID: &f" + group.itemUuid(),
                    "&7结论: " + status(history.status()), "&7本地记录事件数: &f" + history.recordedEvents(),
                    "&e同一份备份兼具多种线索时合并显示。", "&c最早持有人不等于复制者，时间接近不等于同时持有。"));
            if (history.firstCrossPlayerCandidate() != null) conclusion.add("&7跨玩家候选间隔: &f" + history.firstCrossPlayerCandidate().differenceMillis() + " ms");
            buttons.put(48, button(Material.PAPER, "&e历史结论", conclusion, null));
            buttons.put(47, button(Material.ENDER_CHEST, "&e查看源扫描报告中的重复位置", List.of(
                    "&7优先读取生成此历史报告时使用的 uuid 报告。", "&7不是当前在线实时背包。"), viewer -> async(viewer, () -> {
                Path source = sourceCurrent(report);
                Report sourceReport = source == null ? report : report(source, Mode.CURRENT);
                Group current = source == null ? group : group(sourceReport, group.itemUuid());
                show(viewer, new LocationsMenu(sourceReport, current, this, 0));
            })));
            return buttons;
        }
    }

    private final class HistoricalItemsMenu extends View {
        final String itemUuid; final Evidence evidence; final HistoricalDetails details; final Menu parent; final int page;
        HistoricalItemsMenu(Report report, String itemUuid, Evidence evidence, HistoricalDetails details, Menu parent, int page) {
            super(report); this.itemUuid = itemUuid; this.evidence = evidence; this.details = details; this.parent = parent; this.page = page;
        }
        @Override Collection<String> visibleUuids() { return List.of(itemUuid); }
        @Override public String getTitle(Player player) { return "历史备份内的重复物品 (" + (page + 1) + ")"; }
        @Override public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            int start = page * PAGE_SIZE, end = Math.min(start + PAGE_SIZE, details.items().size());
            for (int i = start; i < end; i++) {
                HistoricalItem item = details.items().get(i);
                List<String> lore = new ArrayList<>();
                evidence.labels().forEach(label -> lore.add("&e" + label));
                observationInfo(lore, itemUuid, evidence.observation());
                lore.add("&7位置: &f" + text(item.location(), "未知"));
                lore.add("&7该位置数量: &e" + item.amount());
                lore.add("&7物品 ID: &f" + text(item.internalName(), "未知"));
                if (details.warning() != null) lore.add("&e" + details.warning());
                originalLore(lore, item.lore());
                buttons.put(i - start, itemButton(this, itemUuid, material(item.material()), text(item.displayName(), "&e历史 UUID 物品"), lore,
                        context("playerUuid", evidence.observation().playerUuid(), "playerName", evidence.observation().playerName(),
                                "backupDatabaseId", evidence.observation().backupDatabaseId(), "backupUuid", evidence.observation().backupUuid(),
                                "timestamp", evidence.observation().timestamp(), "location", item.location(), "internalName", item.internalName(),
                                "material", item.material(), "displayName", item.displayName(), "amount", item.amount(), "lore", item.lore()), null));
            }
            navigation(buttons, report, parent, page, details.items().size(), number ->
                    new HistoricalItemsMenu(report, itemUuid, evidence, details, parent, number).openMenu(player));
            if (details.warning() != null) buttons.put(48, button(Material.BARRIER, "&e历史物品数据不完整", List.of("&e" + details.warning()), null));
            return buttons;
        }
    }

    private static void observationInfo(List<String> lore, String uuid, UuidDuplicateHistory.Observation observation) {
        lore.add("&7物品 UUID: &f" + uuid);
        lore.add("&7备份持有人: &e" + text(observation.playerName(), "未知"));
        lore.add("&7玩家 UUID: &f" + text(observation.playerUuid(), "未知"));
        lore.add("&7备份时间: &f" + date(observation.timestamp()));
        lore.add("&7转生: &f" + observation.prestige());
        lore.add("&7同份备份内数量: &e" + observation.copies());
        lore.add("&7Mongo _id: &f" + text(observation.backupDatabaseId(), "未知"));
        lore.add("&7备份 UUID: &f" + text(observation.backupUuid(), "未知"));
    }
    private static String position(Location location) {
        String container = switch (text(location.container(), "")) {
            case "inventory" -> "背包";
            case "inventory_armor" -> "装备栏";
            case "ender_chest" -> "仓库 " + text(location.warehouseIndex() == null ? null : location.warehouseIndex().toString(), "未知");
            default -> text(location.container(), "未知容器");
        };
        return container + " / 第 " + (location.displaySlot() == null
                ? location.slot() == null ? "未知" : location.slot() + 1 : location.displaySlot()) + " 格"
                + (location.slotName() == null ? "" : "（" + location.slotName() + "）");
    }
}
