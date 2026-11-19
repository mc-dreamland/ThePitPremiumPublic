package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.item.ItemUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoCursor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class RareEnchantScanTask {

    private static final int MAIN_INVENTORY_SIZE = 36;
    private static final String[] ARMOR_SLOT_NAMES = {"boots", "leggings", "chestplate", "helmet"};
    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger scannedPlayers = new AtomicInteger();
    private final AtomicInteger scannedItemStacks = new AtomicInteger();
    private final AtomicInteger scanErrors = new AtomicInteger();
    private final AtomicInteger matchedEquipment = new AtomicInteger();

    private volatile long startedAt = -1L;
    private volatile String lastReportPath;
    private volatile String lastError;
    private volatile int lastMatchedPlayers;
    private volatile int lastRareEnchantments;

    void start(CommandSender sender) {
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage("已有稀有附魔扫描任务正在运行，请使用 /scan rareenchant status 查看进度。");
            return;
        }

        startedAt = System.currentTimeMillis();
        scannedPlayers.set(0);
        scannedItemStacks.set(0);
        scanErrors.set(0);
        matchedEquipment.set(0);
        lastError = null;

        sender.sendMessage("已开始后台扫描数据库中的玩家背包和仓库，完成后会在插件目录 logs/scan/rareenchant 下输出 JSON 报告。");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> executeScan(sender));
    }

    void status(CommandSender sender) {
        if (running.get()) {
            sender.sendMessage("稀有附魔扫描任务运行中。");
            sender.sendMessage("已扫描玩家: " + scannedPlayers.get());
            sender.sendMessage("已扫描物品堆: " + scannedItemStacks.get());
            sender.sendMessage("已发现装备: " + matchedEquipment.get());
            sender.sendMessage("异常数据数: " + scanErrors.get());
            sender.sendMessage("运行时长: " + formatDuration(System.currentTimeMillis() - startedAt));
            return;
        }

        sender.sendMessage("当前没有运行中的稀有附魔扫描任务。");
        if (lastReportPath != null) {
            sender.sendMessage("最近一次报告: " + lastReportPath);
            sender.sendMessage("命中玩家数: " + lastMatchedPlayers
                    + "，装备数: " + matchedEquipment.get()
                    + "，稀有附魔数: " + lastRareEnchantments);
        }
        if (lastError != null) {
            sender.sendMessage("最近一次错误: " + lastError);
        }
    }

    private void executeScan(CommandSender sender) {
        List<PlayerResult> players = new ArrayList<>();
        try (MongoCursor<PlayerProfile> cursor = ThePit.getInstance()
                .getMongoDB()
                .getProfileCollection()
                .find()
                .iterator()) {
            while (cursor.hasNext()) {
                PlayerProfile profile = cursor.next();
                scannedPlayers.incrementAndGet();
                if (profile == null || profile.getUuid() == null) {
                    continue;
                }

                PlayerResult result = scanProfile(profile);
                if (!result.equipment.isEmpty()) {
                    result.calculateCounts();
                    result.equipment.sort(Comparator
                            .comparing((EquipmentResult item) -> item.container)
                            .thenComparing(item -> item.warehouse == null ? 0 : item.warehouse)
                            .thenComparingInt(item -> item.slot));
                    players.add(result);
                }

                if (scannedPlayers.get() % 1000 == 0) {
                    ThePit.getInstance().info("scan rareenchant progress: scannedPlayers=" + scannedPlayers.get()
                            + ", scannedItemStacks=" + scannedItemStacks.get()
                            + ", matchedEquipment=" + matchedEquipment.get());
                }
            }

            players.sort(Comparator
                    .comparingInt((PlayerResult player) -> player.equipmentCount).reversed()
                    .thenComparing(player -> player.playerName == null ? "" : player.playerName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(player -> player.playerUuid));

            lastMatchedPlayers = players.size();
            lastRareEnchantments = players.stream()
                    .mapToInt(player -> player.rareEnchantments)
                    .sum();
            Path reportPath = writeReport(players);
            lastReportPath = reportPath.toAbsolutePath().toString();

            ThePit.getInstance().info("scan rareenchant finished: scannedPlayers=" + scannedPlayers.get()
                    + ", scannedItemStacks=" + scannedItemStacks.get()
                    + ", scanErrors=" + scanErrors.get()
                    + ", matchedPlayers=" + lastMatchedPlayers
                    + ", matchedEquipment=" + matchedEquipment.get()
                    + ", report=" + lastReportPath);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage(
                    "扫描完成。命中玩家数: " + lastMatchedPlayers
                            + "，装备数: " + matchedEquipment.get()
                            + "，稀有附魔数: " + lastRareEnchantments
                            + "，异常数据数: " + scanErrors.get()
                            + "，日志: " + lastReportPath));
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            ThePit.getInstance().getSLF4JLogger().error("scan rareenchant failed", exception);
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> sender.sendMessage("扫描失败: " + lastError));
        } finally {
            running.set(false);
        }
    }

    private PlayerResult scanProfile(PlayerProfile profile) {
        PlayerResult result = new PlayerResult(profile.getUuid(), profile.getPlayerName());
        try {
            PlayerInv inventory = profile.getInventory();
            if (inventory != null) {
                scanItems(result, "inventory", null, inventory.getContents(), MAIN_INVENTORY_SIZE, null);
                scanItems(result, "inventory_armor", null, inventory.getArmorContents(), -1, ARMOR_SLOT_NAMES);
            }

            PlayerEnderChest[] warehouses = profile.getEnderChests();
            if (warehouses != null) {
                for (int index = 0; index < warehouses.length; index++) {
                    PlayerEnderChest warehouse = warehouses[index];
                    if (warehouse == null || warehouse.getInventory() == null) {
                        continue;
                    }
                    scanItems(result, "warehouse", index + 1,
                            warehouse.getInventory().getContents(), -1, null);
                }
            }
        } catch (Exception exception) {
            scanErrors.incrementAndGet();
            ThePit.getInstance().getSLF4JLogger().warn(
                    "scan rareenchant failed to scan player {}", profile.getUuid(), exception);
        }
        return result;
    }

    private void scanItems(PlayerResult player,
                           String container,
                           Integer warehouse,
                           ItemStack[] items,
                           int maxSlots,
                           String[] slotNames) {
        if (items == null) {
            return;
        }

        int length = maxSlots < 0 ? items.length : Math.min(items.length, maxSlots);
        for (int slot = 0; slot < length; slot++) {
            ItemStack item = items[slot];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            scannedItemStacks.incrementAndGet();

            try {
                EquipmentResult equipment = createEquipmentResult(
                        item,
                        container,
                        warehouse,
                        slot,
                        slotNames != null && slot < slotNames.length ? slotNames[slot] : null
                );
                if (equipment != null) {
                    player.equipment.add(equipment);
                    matchedEquipment.incrementAndGet();
                }
            } catch (Exception exception) {
                scanErrors.incrementAndGet();
                ThePit.getInstance().getSLF4JLogger().debug(
                        "scan rareenchant failed to read item for player {} at {}:{}",
                        player.playerUuid, container, slot, exception);
            }
        }
    }

    private EquipmentResult createEquipmentResult(ItemStack item,
                                                   String container,
                                                   Integer warehouse,
                                                   int slot,
                                                   String slotName) {
        IMythicItem mythicItem = MythicUtil.getMythicItem(item);
        if (mythicItem == null || mythicItem.getEnchantments() == null) {
            return null;
        }

        List<RareEnchantResult> rareEnchants = mythicItem.getEnchantments().entrySet().stream()
                .filter(entry -> entry.getKey() != null
                        && entry.getValue() != null
                        && entry.getValue() > 0
                        && entry.getKey().getRarity().getParentType() == EnchantmentRarity.RarityType.RARE)
                .map(entry -> new RareEnchantResult(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(enchant -> enchant.nbtName))
                .toList();
        if (rareEnchants.isEmpty()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        String displayName = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : null;
        return new EquipmentResult(
                container,
                warehouse,
                slot,
                slot + 1,
                slotName,
                item.getType().name(),
                ItemUtil.getInternalName(item),
                displayName,
                item.getAmount(),
                mythicItem.getUuid() == null ? null : mythicItem.getUuid().toString(),
                mythicItem.getTier(),
                mythicItem.getLive(),
                mythicItem.getMaxLive(),
                mythicItem.getCreator(),
                mythicItem.getCreatorName(),
                mythicItem.getOwner(),
                mythicItem.getOwnerName(),
                mythicItem.isTraded(),
                mythicItem.getTradeTime(),
                rareEnchants
        );
    }

    private Path writeReport(List<PlayerResult> players) throws IOException {
        Path logsDirectory = ScanReportPath.directory("rareenchant");
        Path reportPath = logsDirectory.resolve(
                "rare-enchant-equipment-" + LocalDateTime.now().format(FILE_NAME_TIME) + ".json");
        Report report = new Report(
                scannedPlayers.get(),
                scannedItemStacks.get(),
                scanErrors.get(),
                players.size(),
                matchedEquipment.get(),
                lastRareEnchantments,
                players
        );
        try (Writer writer = Files.newBufferedWriter(
                reportPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW)) {
            GSON.toJson(report, writer);
        }
        return reportPath;
    }

    private String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(Math.max(0L, millis));
        long seconds = duration.getSeconds();
        return String.format("%02d:%02d:%02d", seconds / 3600, seconds % 3600 / 60, seconds % 60);
    }

    private static final class Report {
        private final int scannedPlayers;
        private final int scannedItemStacks;
        private final int scanErrors;
        private final int matchedPlayers;
        private final int matchedEquipment;
        private final int rareEnchantments;
        private final List<PlayerResult> players;

        private Report(int scannedPlayers,
                       int scannedItemStacks,
                       int scanErrors,
                       int matchedPlayers,
                       int matchedEquipment,
                       int rareEnchantments,
                       List<PlayerResult> players) {
            this.scannedPlayers = scannedPlayers;
            this.scannedItemStacks = scannedItemStacks;
            this.scanErrors = scanErrors;
            this.matchedPlayers = matchedPlayers;
            this.matchedEquipment = matchedEquipment;
            this.rareEnchantments = rareEnchantments;
            this.players = new ArrayList<>(players);
        }
    }

    private static final class PlayerResult {
        private final String playerUuid;
        private final String playerName;
        private final List<EquipmentResult> equipment = new ArrayList<>();
        private int equipmentCount;
        private int rareEnchantments;

        private PlayerResult(String playerUuid, String playerName) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
        }

        private void calculateCounts() {
            equipmentCount = equipment.size();
            rareEnchantments = equipment.stream().mapToInt(item -> item.rareEnchants.size()).sum();
        }
    }

    private static final class EquipmentResult {
        private final String container;
        private final Integer warehouse;
        private final int slot;
        private final int displaySlot;
        private final String slotName;
        private final String material;
        private final String internalName;
        private final String displayName;
        private final int amount;
        private final String itemUuid;
        private final int tier;
        private final int lives;
        private final int maxLives;
        private final String creator;
        private final String creatorName;
        private final String owner;
        private final String ownerName;
        private final boolean traded;
        private final long tradeTime;
        private final List<RareEnchantResult> rareEnchants;

        private EquipmentResult(String container,
                                Integer warehouse,
                                int slot,
                                int displaySlot,
                                String slotName,
                                String material,
                                String internalName,
                                String displayName,
                                int amount,
                                String itemUuid,
                                int tier,
                                int lives,
                                int maxLives,
                                String creator,
                                String creatorName,
                                String owner,
                                String ownerName,
                                boolean traded,
                                long tradeTime,
                                List<RareEnchantResult> rareEnchants) {
            this.container = container;
            this.warehouse = warehouse;
            this.slot = slot;
            this.displaySlot = displaySlot;
            this.slotName = slotName;
            this.material = material;
            this.internalName = internalName;
            this.displayName = displayName;
            this.amount = amount;
            this.itemUuid = itemUuid;
            this.tier = tier;
            this.lives = lives;
            this.maxLives = maxLives;
            this.creator = creator;
            this.creatorName = creatorName;
            this.owner = owner;
            this.ownerName = ownerName;
            this.traded = traded;
            this.tradeTime = tradeTime;
            this.rareEnchants = new ArrayList<>(rareEnchants);
        }
    }

    private static final class RareEnchantResult {
        private final String nbtName;
        private final String name;
        private final String rarity;
        private final int level;

        private RareEnchantResult(AbstractEnchantment enchantment, int level) {
            this.nbtName = enchantment.getNbtName();
            this.name = enchantment.getEnchantName();
            this.rarity = enchantment.getRarity().name();
            this.level = level;
        }
    }
}
