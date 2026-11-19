package cn.charlotte.pit.events.impl.major;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.events.IEpicEvent;
import cn.charlotte.pit.events.MajorEventTasks;
import cn.charlotte.pit.events.IScoreBoardInsert;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
import cn.charlotte.pit.lambda.playerLambda;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.util.DirectionUtil;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.RandomList;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.hologram.Hologram;
import cn.charlotte.pit.util.hologram.HologramAPI;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.log.ItemOperationJson;
import cn.charlotte.pit.util.random.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.*;

@Slf4j
public class LuckyWheel implements IEpicEvent, IScoreBoardInsert, Listener {

    private static final Random random = new Random();
    private static final HashMap<UUID, Set<UUID>> killData = new HashMap<>(16);
    private static final HashMap<UUID, Integer> rankMap = new HashMap<>(16);
    private static final RandomList<playerLambda> rewardActions = new RandomList<>(
            Map.entry((player) -> {
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                if (profile != null) {
                    Integer amount = 400 + random.nextInt(600);
                    profile.setCoins(profile.getCoins() + amount);
                    profile.grindCoins(amount + 0.0);
                    player.sendMessage(CC.translate("&a幸运转盘 &7获得 &e" + amount + " 金币"));
                }
            }, 200),
            Map.entry((player) -> {
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                if (profile != null) {
                    Integer amount = 400 + random.nextInt(600);
                    profile.setExperience(profile.getExperience() + amount);
                    profile.applyExperienceToPlayer(player);
                    player.sendMessage(CC.translate("&a幸运转盘 &7获得 &b" + amount + " 经验"));
                }
            }, 200),
            Map.entry((player) -> {
                org.bukkit.inventory.ItemStack mythic = RandomUtil.helpMeToChooseOne(
                        new MythicSwordItem().toItemStack(),
                        new MythicBowItem().toItemStack(),
                        new MythicLeggingsItem().toItemStack()
                );
                player.getInventory().addItem(mythic);
                log.info("{}", ItemOperationJson.toJson(
                        ItemOperationJson.mythicGranted(player, "LUCKY_WHEEL", mythic)
                ));
                player.sendMessage(CC.translate("&a幸运转盘 &7获得 &d随机神话装备"));
            }, 50),
            Map.entry((player) -> {
                player.getInventory().addItem(PitItems.ACTION_GIFT_BOX.toItemStack());
                player.sendMessage(CC.translate("&a幸运转盘 &7获得 &e行动礼盒"));
            }, 200),
            Map.entry((player) -> {
                player.getInventory().addItem(PitItems.CHUNK_OF_VILE.toItemStack());
                player.sendMessage(CC.translate("&a幸运转盘 &7获得 &5暗聚块"));
            }, 50),
            Map.entry((player) -> {
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                if (profile != null) {
                    Integer amount = random.nextInt(1) + 1;
                    profile.getEventData().addEventrenown(amount);
                    profile.setRenown(profile.getRenown() + amount);
                    player.sendMessage(CC.translate("&a幸运转盘 &7获得 &b声望 &7*" + amount));
                }
            }, 50),
            Map.entry((player) -> {
                PitItems crop = PitItems.randomOf(PitItems.WHEAT, PitItems.PACKAGED_BALE, PitItems.SUGAR);
                int amount = crop == PitItems.PACKAGED_BALE ? 1 : random.nextInt(63) + 1;
                Util.giveItems(player, crop.toItemStacks(amount));
                player.sendMessage(CC.translate("&a幸运转盘 &7获得 &a随机农作物"));
            }, 100),
            Map.entry((player) -> player.sendMessage(CC.translate("&7Thanks♪(･ω･)ﾉ 感谢你的参与!")), 100),
            Map.entry((player) -> {
                Util.giveItems(player, PitItems.LUCKY_LOTTERY.toItemStacks(2));
                player.sendMessage(CC.translate("&a幸运转盘 &7获得 &a幸运转盘抽奖券 &f * 2"));
            }, 50)
    );
    private static Location currentBlock = null;
    private static int lotteryDraws = 0;
    private static int times = 0;
    private static long eventStartTime = 0L;
    private static Hologram hologram = null;
    private org.bukkit.block.BlockState originalBlock;

    @Override
    public String getEventInternalName() {
        return "lucky_wheel";
    }

    @Override
    public String getEventName() {
        return "幸运转盘";
    }

    @Override
    public List<String> getLore() {
        return Arrays.asList(
                "&6&l>> &e幸运转盘事件 &6&l<<",
                "",
                "&6&l事件机制",
                "&7- 连续击杀5名不同玩家获得幸运转盘抽奖券",
                "&7- 右键唱片机使用幸运转盘抽奖券进行抽奖",
                "&7- 每抽奖5次，唱片机会随机传送到新位置",
                "",
                "&6&l奖励机制",
                "&7- 全服抽奖达到100次: 所有玩家获得1声望",
                "&7- 个人排名奖励:",
                "&f第1-3名: &62000金币 &f+ &b2声望",
                "&f第4-20名: &61000金币 &f+ &b1声望",
                "&f其他玩家: &6200金币",
                ""
        );
    }

    @Override
    public int requireOnline() {
        return CombatListener.isBedrockRoom() ? 20 : 50;
    }

    @Override
    public java.util.concurrent.CompletableFuture<Void> prepare() {
        var locations = ThePit.getInstance().getPitConfig().getLuckyWheelLocation();
        if (locations == null || locations.stream().noneMatch(at -> at != null && at.getWorld() != null))
            throw new IllegalStateException("未配置有效幸运转盘位置");
        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }

    private void stopRuntime() {
        currentBlock = null;
        if (hologram != null) { hologram.deSpawn(); hologram = null; }
        HandlerList.unregisterAll(this);
        Bukkit.getOnlinePlayers().forEach(player -> InventoryUtil.removeItemWithInternalName(player, PitItems.LUCKY_LOTTERY.getInternalName()));
    }

    @Override public void onAbort() {
        stopRuntime();
        killData.clear(); rankMap.clear(); times = lotteryDraws = 0;
    }

    @Override public java.util.concurrent.CompletableFuture<Void> restore() {
        if (originalBlock != null) {
            if (!originalBlock.update(true, false)) return java.util.concurrent.CompletableFuture.failedFuture(
                    new IllegalStateException("无法恢复幸运转盘位置: " + originalBlock.getLocation()));
            originalBlock = null;
        }
        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }

    @Override
    public void onActive() {
        lotteryDraws = 0;
        times = 0;
        this.setupblock();
        killData.clear();
        rankMap.clear();
        eventStartTime = System.currentTimeMillis();
        hologram = HologramAPI.createHologram(currentBlock.clone().add(0, 2.4, 0), CC.translate(String.format("&a%s/5", times)));
        hologram.spawn();
        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());
        MajorEventTasks.later(this, () -> ThePit.getInstance().getEventFactory().inactiveEvent(this), 10 * 20L * 60L);
    }

    @Override
    public void onInactive() {
        stopRuntime();
        rankMap.forEach(
                (k, v) -> {
                    Player player = Bukkit.getPlayer(k);
                    if (player == null || !player.isOnline()) return;
                    PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                    if (profile != null) {
                        int rank = getPlayerRank(k);
                        double coins = 0.0;
                        int renown = 0;

                        if (rank <= 3) {
                            coins = 2000.0;
                            renown = 2;
                        } else if (rank <= 20) {
                            coins = 1000.0;
                            renown = 1;
                        } else {
                            coins = 200.0;
                        }

                        if (lotteryDraws >= 100) {
                            renown += 1;
                        }

                        profile.setCoins(profile.getCoins() + coins);
                        profile.changeRenown(profile.getRenown() + renown);

                        CC.send(MessageType.EVENT, player, String.format("&6排名奖励: &e+%s金币 &b+%s声望 &7(排名#%s)%s", coins, renown, rank, lotteryDraws >= 100 ? " &a(包含全服奖励)" : ""));
                    }
                }
        );
        killData.clear(); rankMap.clear();
    }

    @EventHandler()
    public void onPlayerKill(PitKillEvent event) {
        Player killer = event.getKiller();

        Set<UUID> uuidSet = killData.computeIfAbsent(killer.getUniqueId(), k -> new HashSet<>(3));
        if (!uuidSet.add(event.getTarget().getUniqueId())) {
            CC.send(MessageType.EVENT, killer, "&c已重复击杀该玩家!");
            return;
        }

        if (uuidSet.size() != 3) return;
        uuidSet.clear();
        killer.getInventory().addItem(PitItems.LUCKY_LOTTERY.toItemStack());
        CC.send(MessageType.EVENT, killer, "&7获得幸运转盘抽奖券");

    }

    @EventHandler
    public void onJukeboxInteract(PlayerInteractEvent event) {
        if (!isActive()) return;
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        if (block == null ||
                !block.getType().equals(Material.JUKEBOX) ||
                !PitItems.LUCKY_LOTTERY.getInternalName().equals(ItemUtil.getInternalName(player.getInventory().getItemInMainHand())))
            return;
        event.setCancelled(true);
        rankMap.merge(player.getUniqueId(), 1, Integer::sum);
        lotteryDraws++;
        PlayerUtil.takeOneItem(player, EquipmentSlot.HAND);
        rewardActions.random().execute(player);
        times++;
        if (hologram != null && hologram.isSpawned()) {
            hologram.deSpawn();
            hologram = HologramAPI.createHologram(currentBlock.clone().add(0, 2.4, 0), CC.translate(String.format("&a%s/5", times)));
            hologram.spawn();
        }
        if (times < 5) return;
        times = 0;
        setupblock();
    }

    @Override
    public List<String> insert(Player player) {
        List<String> lines = new ArrayList<>();
        String remainingTime = "0:00";

        long remainingTime1 = 10L * 60 * 1000 - (System.currentTimeMillis() - eventStartTime);

        if (remainingTime1 > 0) {
            remainingTime = String.format("%d:%02d",
                    remainingTime1 / (60 * 1000),
                    (remainingTime1 % (60 * 1000)) / 1000);
        }

        lines.add(String.format("&f剩余时间: &e%s", remainingTime));
        lines.add(String.format("&f全服抽奖: &e%d/100", lotteryDraws));
        lines.add(String.format("&f个人抽奖次数: &b%d  &f排名:#%d",
                rankMap.getOrDefault(player.getUniqueId(), 0),
                getPlayerRank(player.getUniqueId())));

        if (currentBlock != null) {
            String direction = DirectionUtil.getTargetDirection(player, currentBlock);
            lines.add(String.format("&a抽奖机: &f%d : %d : %d&a追踪: %s",
                    currentBlock.getBlockX(),
                    currentBlock.getBlockY(),
                    currentBlock.getBlockZ(),
                    direction));
        } else {
            lines.add("&a抽奖机: &c未放置");
        }

        return lines;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isActive() || !isWithinRadius(event.getBlockPlaced().getLocation(), currentBlock)) return;
        event.setCancelled(true);
        CC.send(MessageType.EVENT, event.getPlayer(), "&c你不能在抽奖箱附近4格内放置方块!");
    }

    private Boolean isWithinRadius(Location location, Location target) {
        return location != null && target != null && location.getWorld().equals(target.getWorld()) &&
                Math.abs(location.getBlockX() - target.getBlockX()) <= 4 &&
                Math.abs(location.getBlockY() - target.getBlockY()) <= 4 &&
                Math.abs(location.getBlockZ() - target.getBlockZ()) <= 4;
    }

    private Location randomLoc() {
        List<Location> locationList = ThePit.getInstance().getPitConfig().getLuckyWheelLocation().stream()
                .filter(at -> at != null && at.getWorld() != null).toList();
        if (locationList.isEmpty()) throw new IllegalStateException("未配置唱片机位置");
        return locationList.get(random.nextInt(locationList.size())).clone();
    }

    private void setupblock() {
        if (originalBlock != null && !originalBlock.update(true, false)) throw new IllegalStateException("无法恢复原唱片机位置");
        currentBlock = randomLoc();
        originalBlock = currentBlock.getBlock().getState();
        CC.boardCast(String.format("&e幸运转盘已传送到新位置! &6X: &e%d &6Y: &e%d &6Z: &e%d",
                currentBlock.getBlockX(),
                currentBlock.getBlockY(),
                currentBlock.getBlockZ()));
        currentBlock.getBlock().setType(Material.JUKEBOX);
        if (hologram != null && hologram.isSpawned()) {
            hologram.deSpawn();
            hologram = HologramAPI.createHologram(currentBlock.clone().add(0, 2.4, 0), CC.translate(String.format("&a%s/5", times)));
            hologram.spawn();
        }
    }

    private Integer getPlayerRank(UUID playerId) {
        List<Map.Entry<UUID, Integer>> sortedPlayers = rankMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .toList();

        for (int i = 0; i < sortedPlayers.size(); i++) {
            if (sortedPlayers.get(i).getKey().equals(playerId)) {
                return i + 1;
            }
        }
        return 0;
    }
}
