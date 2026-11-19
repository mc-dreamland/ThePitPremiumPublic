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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static cn.charlotte.pit.commands.admin.MaterialHistoryLocalReport.*;

/** Every query/navigation/sort is file-only or in-memory, never a database lookup. */
final class ScanMaterialQuantityHistory {
    private static final int PAGE_SIZE = 45;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private final Set<UUID> busy = ConcurrentHashMap.newKeySet();

    void open(CommandSender sender, String item, String requestedPlayer) {
        if (!(sender instanceof Player player)) { sender.sendMessage("该命令只能由玩家执行。"); return; }
        async(player, () -> {
            Path file = latest(ThePit.getInstance().getDataFolder().toPath().resolve("logs/scan/materialhistory"));
            if (file == null) throw new IllegalArgumentException("没有本地物资报告，请先运行 /scan materialhistory 或 /scan materialhistory all。");
            Header header = header(file);
            if (item == null || item.isBlank()) show(player, new TypesMenu(header));
            else query(player, header, item, requestedPlayer);
        });
    }
    void openMaximum(CommandSender sender, String requestedPlayer) {
        if (!(sender instanceof Player player)) { sender.sendMessage("该命令只能由玩家执行。"); return; }
        async(player, () -> {
            Path file = latest(ThePit.getInstance().getDataFolder().toPath().resolve("logs/scan/materialhistory"));
            if (file == null) throw new IllegalArgumentException("没有本地物资报告，请先运行 /scan materialhistory 或 /scan materialhistory all。");
            Header header = header(file);
            MaterialHistoryPlayerMaximum.Result result = MaterialHistoryPlayerMaximum.query(header, requestedPlayer);
            if (result.items().isEmpty()) throw new IllegalArgumentException("该玩家的已收录备份中没有数量大于 0 的物品记录: " + result.name());
            show(player, new MaximumMenu(header, result, result.items(), Order.QUANTITY_DESC));
        });
    }
    private void query(Player player, Header header, String requestedItem, String requestedPlayer) throws Exception {
        String item = header.resolveItem(requestedItem);
        List<PlayerCounts> players = sortPlayers(players(header, item), Order.DATE_ASC);
        if (players.isEmpty()) throw new IllegalArgumentException("这份本地报告没有玩家持有 " + header.label(item) + " 的记录。");
        PlayersMenu parent = new PlayersMenu(header, item, players, Order.DATE_ASC);
        if (requestedPlayer == null || requestedPlayer.isBlank()) show(player, parent);
        else {
            PlayerCounts selected = players.stream().filter(info -> requestedPlayer.equalsIgnoreCase(info.name())
                    || requestedPlayer.equalsIgnoreCase(info.uuid())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("本地报告未找到该玩家持有此物品的记录: " + requestedPlayer));
            openRows(player, parent, selected, Order.DATE_ASC);
        }
    }
    private void openRows(Player player, PlayersMenu parent, PlayerCounts selected, Order order) throws Exception {
        show(player, new RowsMenu(parent, selected, sortRows(rows(parent.header, parent.item, selected.uuid()), order), order));
    }
    private interface Job { void run() throws Exception; }
    private void async(Player player, Job job) {
        if (!busy.add(player.getUniqueId())) { player.sendMessage("§e你的本地数量查询仍在处理中，请稍候。"); return; }
        player.sendMessage("§7正在后台读取/整理本地数量记录...");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            try { job.run(); }
            catch (IllegalArgumentException exception) { message(player, "§c" + exception.getMessage()); }
            catch (Exception exception) {
                ThePit.getInstance().getSLF4JLogger().error("Local material quantity query failed", exception);
                message(player, "§c读取本地报告失败，请查看日志。未查询数据库。");
            } finally { busy.remove(player.getUniqueId()); }
        });
    }
    private static void show(Player player, Menu menu) {
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> { if (player.isOnline()) menu.openMenu(player); });
    }
    private static void message(Player player, String text) {
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> { if (player.isOnline()) player.sendMessage(text); });
    }
    private static String time(long value) { return value > 0 ? DATE.format(Instant.ofEpochMilli(value)) : "未知"; }
    private static Button button(Material material, String name, List<String> lore, Consumer<Player> action) {
        return new Button() {
            @Override public ItemStack getButtonItem(Player player) { return new ItemBuilder(material).name(name).lore(lore).build(); }
            @Override public void clicked(Player player, int slot, ClickType click, int hotbar, ItemStack current) {
                if (action != null) action.accept(player);
            }
        };
    }

    private abstract static class Paged extends Menu {
        int page;
        Paged() { setUpdateAfterClick(false); }
        @Override public int getSize() { return 54; }
        void pages(Map<Integer, Button> buttons, int size) {
            if (page > 0) buttons.put(45, button(Material.ARROW, "&e上一页", List.of(), player -> { page--; openMenu(player); }));
            if ((long) (page + 1) * PAGE_SIZE < size)
                buttons.put(53, button(Material.ARROW, "&e下一页", List.of(), player -> { page++; openMenu(player); }));
        }
        void info(Map<Integer, Button> buttons, Header header, String text) {
            buttons.put(50, button(Material.BOOK, "&b仅读取本地报告", List.of(
                    "&7" + text, "&7报告: &f" + header.path().getFileName(),
                    "&7范围: &f" + header.scopeLabel(),
                    "&7旧报告不会自动补全缺失物品，扩大范围后需重新扫描。",
                    "&7普通入选合计阈值: &f" + header.minimumItems(), "&7UUID 异常堆叠也会收录。", "",
                    "&c仅展示报告已收录的备份。",
                    "&c未收录的备份不代表数量为 0。", "&7翻页、排序、切换玩家均不查数据库。"), null));
        }
    }
    private final class TypesMenu extends Paged {
        private final Header header;
        private final List<String> items;
        TypesMenu(Header header) { this.header = header; this.items = header.items().keySet().stream().sorted().toList(); }
        @Override public String getTitle(Player player) { return "本地物品数量-选择物品 (" + (page + 1) + ")"; }
        @Override public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            int start = page * PAGE_SIZE, end = Math.min(start + PAGE_SIZE, items.size());
            for (int i = start; i < end; i++) {
                String item = items.get(i);
                buttons.put(i - start, button(Material.PAPER, "&e" + header.label(item), List.of("&7ID: &f" + item,
                        "&e点击按玩家查询此物品的历史数量"), viewer -> async(viewer, () -> query(viewer, header, item, null))));
            }
            pages(buttons, items.size());
            info(buttons, header, "选择指定物品");
            return buttons;
        }
    }
    private final class PlayersMenu extends Paged {
        final Header header;
        final String item;
        final List<PlayerCounts> players;
        final Order order;
        PlayersMenu(Header header, String item, List<PlayerCounts> players, Order order) {
            this.header = header; this.item = item; this.players = players; this.order = order;
        }
        @Override public String getTitle(Player player) { return header.label(item) + " 的持有玩家 (" + (page + 1) + ")"; }
        @Override public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            int start = page * PAGE_SIZE, end = Math.min(start + PAGE_SIZE, players.size());
            for (int i = start; i < end; i++) {
                PlayerCounts selected = players.get(i);
                buttons.put(i - start, button(Material.PLAYER_HEAD, "&e" + selected.name(), List.of(
                        "&7UUID: &f" + selected.uuid(), "&7报告记录转生: &f" + selected.prestige(),
                        "&7首次出现: &f" + time(selected.firstTimestamp()), "&7末次出现: &f" + time(selected.lastTimestamp()),
                        "&7单次备份最高数量: &e" + selected.maximum(),
                        "&7持有记录 / 已收录备份: &f" + selected.positiveSnapshots() + " / " + selected.snapshots(),
                        "", "&e点击查看各份备份的背包 / 仓库数量"),
                        viewer -> async(viewer, () -> openRows(viewer, this, selected, Order.DATE_ASC))));
            }
            pages(buttons, players.size());
            buttons.put(48, button(Material.HOPPER, "&e排序: " + order.label,
                    List.of("&7玩家按首次/末次出现日期，或单次备份峰值排序。", "&e点击切换为: " + order.next().label),
                    viewer -> async(viewer, () -> show(viewer, new PlayersMenu(header, item, sortPlayers(players, order.next()), order.next())))));
            buttons.put(49, button(Material.ARROW, "&e返回物品选择", List.of(), viewer -> new TypesMenu(header).openMenu(viewer)));
            info(buttons, header, header.label(item) + " | " + players.size() + " 位玩家");
            return buttons;
        }
    }
    private final class MaximumMenu extends Paged {
        private final Header header;
        private final MaterialHistoryPlayerMaximum.Result result;
        private final List<MaterialHistoryPlayerMaximum.ItemMaximum> items;
        private final Order order;
        MaximumMenu(Header header, MaterialHistoryPlayerMaximum.Result result,
                    List<MaterialHistoryPlayerMaximum.ItemMaximum> items, Order order) {
            this.header = header; this.result = result; this.items = items; this.order = order;
        }
        @Override public String getTitle(Player player) { return result.name() + "-物品历史最高数量 (" + (page + 1) + ")"; }
        @Override public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            int start = page * PAGE_SIZE, end = Math.min(start + PAGE_SIZE, items.size());
            for (int i = start; i < end; i++) {
                MaterialHistoryPlayerMaximum.ItemMaximum item = items.get(i);
                Row peak = item.peak();
                buttons.put(i - start, button(Material.PAPER, "&e" + header.label(item.item()) + " &7| &6" + item.maximum(),
                        List.of("&7物品 ID: &f" + item.item(), "&7玩家: &f" + result.name(),
                                "&7历史最高合计: &6" + item.maximum(),
                                "&7峰值时背包（含装备栏）: &e" + peak.inventory(), "&7峰值时仓库: &e" + peak.warehouse(),
                                "&7首次达到峰值: &f" + time(peak.timestamp()), "&7峰值时转生: &f" + peak.prestige(),
                                "&7持有记录 / 已收录备份: &f" + item.positiveSnapshots() + " / " + result.snapshots(),
                                "&7Mongo _id: &f" + Objects.toString(peak.databaseId(), "未知"),
                                "&7备份 UUID: &f" + Objects.toString(peak.backupUuid(), "未知"), "",
                                "&7取同一份备份的背包 + 仓库合计峰值。", "&7不是累计获得量，各物品峰值可能不在同一时间。",
                                "&e点击查看此物品各份备份的数量"), viewer -> async(viewer, () -> openItem(viewer, item))));
            }
            pages(buttons, items.size());
            buttons.put(48, button(Material.HOPPER, "&e排序: " + order.label,
                    List.of("&7日期排序依据首次达到峰值的时间。", "&e点击切换为: " + order.next().label),
                    viewer -> async(viewer, () -> show(viewer, new MaximumMenu(header, result,
                            MaterialHistoryPlayerMaximum.sort(items, order.next()), order.next())))));
            buttons.put(49, button(Material.PLAYER_HEAD, "&e" + result.name(), List.of(
                    "&7UUID: &f" + result.uuid(), "&7最新记录转生: &f" + result.prestige(),
                    "&7已收录备份: &f" + result.snapshots(), "&7有持有记录的物品类型: &f" + items.size(),
                    "&7每种物品独立计算峰值，不将历史备份累加。"), null));
            info(buttons, header, result.name() + " | 各物品历史最高数量");
            return buttons;
        }
        private void openItem(Player player, MaterialHistoryPlayerMaximum.ItemMaximum item) throws Exception {
            PlayerCounts selected = new PlayerCounts(result.uuid(), result.name(), result.prestige(), result.snapshots(),
                    item.positiveSnapshots(), item.maximum(), item.firstTimestamp(), item.lastTimestamp());
            PlayersMenu parent = new PlayersMenu(header, item.item(), List.of(selected), Order.DATE_ASC);
            show(player, new RowsMenu(parent, selected, sortRows(rows(header, item.item(), result.uuid()), Order.DATE_ASC),
                    Order.DATE_ASC, this, "返回最高数量汇总"));
        }
    }
    private final class RowsMenu extends Paged {
        private final PlayersMenu parent;
        private final PlayerCounts selected;
        private final List<Row> rows;
        private final Order order;
        private final int playerIndex;
        private final Menu returnMenu;
        private final String returnLabel;
        RowsMenu(PlayersMenu parent, PlayerCounts selected, List<Row> rows, Order order) {
            this(parent, selected, rows, order, parent, "返回玩家列表");
        }
        RowsMenu(PlayersMenu parent, PlayerCounts selected, List<Row> rows, Order order, Menu returnMenu, String returnLabel) {
            this.parent = parent; this.selected = selected; this.rows = rows; this.order = order;
            this.returnMenu = returnMenu; this.returnLabel = returnLabel;
            int found = -1;
            for (int i = 0; i < parent.players.size(); i++) if (parent.players.get(i).uuid().equals(selected.uuid())) { found = i; break; }
            this.playerIndex = found;
        }
        @Override public String getTitle(Player player) {
            return selected.name() + "-" + parent.header.label(parent.item) + " (" + (page + 1) + ")";
        }
        @Override public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            int start = page * PAGE_SIZE, end = Math.min(start + PAGE_SIZE, rows.size());
            for (int i = start; i < end; i++) {
                Row row = rows.get(i);
                buttons.put(i - start, button(row.total() == 0 ? Material.GRAY_DYE : Material.CHEST,
                        "&e" + (row.time() == null ? time(row.timestamp()) : row.time()) + " &7- &6" + row.total(),
                        List.of("&7玩家: &f" + selected.name(), "&7物品: &f" + parent.header.label(parent.item),
                                "&7背包（含装备栏）: &e" + row.inventory(), "&7仓库: &e" + row.warehouse(),
                                "&7合计: &6" + row.total(), "&7报告记录转生: &f" + row.prestige(),
                                "&7Mongo _id: &f" + Objects.toString(row.databaseId(), "未知"),
                                "&7备份 UUID: &f" + Objects.toString(row.backupUuid(), "未知"),
                                "", "&7此界面只显示本地数量，不从数据库打开背包。"), null));
            }
            pages(buttons, rows.size());
            buttons.put(48, button(Material.HOPPER, "&e排序: " + order.label,
                    List.of("&e点击切换为: " + order.next().label), viewer -> async(viewer,
                            () -> show(viewer, new RowsMenu(parent, selected, sortRows(rows, order.next()), order.next(), returnMenu, returnLabel)))));
            buttons.put(49, button(Material.ARROW, "&e" + returnLabel, List.of(), returnMenu::openMenu));
            if (playerIndex > 0) adjacent(buttons, 46, playerIndex - 1, "上一个玩家");
            if (playerIndex >= 0 && playerIndex + 1 < parent.players.size()) adjacent(buttons, 52, playerIndex + 1, "下一个玩家");
            info(buttons, parent.header, parent.header.label(parent.item) + " | " + rows.size() + " 份已收录备份");
            return buttons;
        }
        private void adjacent(Map<Integer, Button> buttons, int slot, int index, String label) {
            PlayerCounts next = parent.players.get(index);
            buttons.put(slot, button(Material.ARROW, "&e" + label + ": " + next.name(), List.of(),
                    viewer -> async(viewer, () -> openRows(viewer, parent, next, order))));
        }
    }
}
