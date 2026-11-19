package cn.charlotte.pit.events.impl;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.config.PitConfig;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.medal.impl.challenge.CakeEventMedal;
import cn.charlotte.pit.util.FaweUtils;
import cn.charlotte.pit.util.LocationUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.configuration.serializer.ConfigRegion;
import cn.charlotte.pit.util.random.RandomUtil;
import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 巨型蛋糕 - 小型事件（2分钟）
 * 规则要点：
 * 1) 普通蛋糕每口基础 1 金币；每吃满 50 口，普通每口 +0.25 金币
 * 2) 吃掉整块蛋糕的最后一口额外 +5 金币；每吃满 50 口，此额外奖励 +1.25 金币
 * 3) 樱桃（MAGENTA_TERRACOTTA）：基础 150 金币；每 50 口 +37.5 金币
 * 4) 巧克力碎片（BLACK_TERRACOTTA）：100 XP（不受 XP Boost 影响）
 * 5) 吃掉“整块蛋糕”的最后一口时，有机会掉落：
 * - 美味牛奶（自定义药水）：给予 2 心吸收（可叠加，无上限），持续 60s
 * - 曲奇饼干：速度 II，持续 25s
 * <p>
 * 附加：
 * - 修复了原代码中 regions 初始化使用同一个区域的逻辑问题
 * - 使用 BlockData 的 Cake bites 判定，避免 legacy getData()
 * - 点击频率限制逻辑沿用，并在事件激活/结束时重置
 */
public class CakeEvents implements INormalEvent, Listener {

    /* ---------------------- 配置与常量 ---------------------- */

    // 事件时长：2 分钟（原来是 5 分钟）
    private static final int EVENT_DURATION_TICKS = 20 * 60 * 2;
    // 掉落概率与效果时长（可按需调整）
    private static final double DROP_MILK_CHANCE = 0.15;          // 15% 概率掉落“美味牛奶”
    private static final double DROP_COOKIE_CHANCE = 0.20;         // 20% 概率掉落“曲奇饼干”
    private static final int MILK_ABSORB_TICKS = 20 * 60;        // 60 秒
    private static final int COOKIE_SPEED_TICKS = 20 * 25;        // 25 秒，速度 II
    // 点击频率限制参数（沿用）
    private static final long CLICK_INTERVAL_MS = 100;   // 正常间隔 ≥100 ms
    private static final long FAST_CLICK_MS = 50;    // “超速”判定 <50 ms
    private static final int FAST_CLICK_LIMIT = 5;     // 连续超速 5 次
    private static final long COOLDOWN_MS = 5_000; // 冷却 5 s
    private static CuboidRegion[] regions;
    // 数字格式
    private final DecimalFormat numFormatTwo = new DecimalFormat("0.00");
    private final DecimalFormat df = new DecimalFormat(",###,###,###,###");
    // 名称键用于标识自定义掉落物
    private final NamespacedKey MILK_KEY = new NamespacedKey(ThePit.getInstance(), "cake_milk");
    private final NamespacedKey COOKIE_KEY = new NamespacedKey(ThePit.getInstance(), "cake_cookie");

    /* ---------------------- 运行期字段 ---------------------- */
    /* ---------- 每名玩家的节奏状态 ---------- */
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Map<UUID, Integer> fastClicks = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private BukkitRunnable runnable;
    private EditSession session;
    private AxisAlignedBB alignedBB;
    private Map<UUID, CakePlayerData> dataCache;

    /* ---------------------- 构造与基础信息 ---------------------- */

    public CakeEvents() {
        if (regions == null) {
            PitConfig config = ThePit.getInstance().getPitConfig();
            List<ConfigRegion> cakeZones = config.getCakeZones();

            // 初始化时构建全部可用区域（修复原先把同一个区域塞满的 bug）
            regions = new CuboidRegion[cakeZones.size()];
            for (int i = 0; i < cakeZones.size(); i++) {
                ConfigRegion r = cakeZones.get(i);
                regions[i] = new CuboidRegion(
                        FaweUtils.toBlockVector3(r.posA),
                        FaweUtils.toBlockVector3(r.posB)
                );
            }
        }
    }

    @Override
    public String getEventInternalName() {
        return "cake";
    }

    /**
     * 蛋糕事件只持续2分钟
     */
    @Override
    public int duration() {
        return 2;
    }

    @Override
    public List<String> getLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&6&l>> &d蛋糕争夺战 &6&l<<");
        lore.add("");
        lore.add("&6&l事件机制 &7(持续 2 分钟)");
        lore.add("&7- 巨型蛋糕随机刷新指定区域");
        lore.add("&7- 左/右键点击蛋糕方块“吃掉”蛋糕");
        lore.add("&7- 从外围向中心逐步吃掉");
        lore.add("&7- 三种蛋糕：普通 / 樱桃 / 巧克力");
        lore.add("&6奖励：");
        lore.add("&f▪ 普通: &6+1 金币 &7(每 50 口 +0.25)");
        lore.add("&f▪ 最后一口额外: &6+5 金币 &7(每 50 口 +1.25)");
        lore.add("&c▪ 樱桃: &6+150 金币 &7(每 50 口 +37.5)");
        lore.add("&4▪ 巧克力: &b+100 经验 &7(不受 XP Boost 影响)");
        lore.add("&f▪ 掉落: &e美味牛奶(2心吸收,可无限叠加,60s) / 曲奇(速度 II,25s)");
        lore.add("");
        return lore;
    }

    @Override
    public String getEventName() {
        return "&d蛋糕争夺战";
    }

    @Override
    public int requireOnline() {
        return NewConfiguration.getEventOnlineRequired().get(getEventInternalName());
    }

    /* ---------------------- 生命周期 ---------------------- */

    @Override
    public void onActive() {
        lastClickTime.clear();
        fastClicks.clear();
        cooldownUntil.clear();

        CC.boardCast(MessageType.EVENT, "&d蛋糕! &7巨型蛋糕已生成在地图上，吃掉蛋糕获得大量金币和经验! (持续 2 分钟)");

        this.dataCache = new HashMap<>();

        final CuboidRegion region = regions[RandomUtil.random.nextInt(regions.length)];
        final BlockVector3 pos1 = region.getPos1();
        final BlockVector3 pos2 = region.getPos2();
        this.alignedBB = new AxisAlignedBB(pos1.x(), pos1.y(), pos1.z(), pos2.x(), pos2.y(), pos2.z());

        // 异步生成巨型蛋糕
        FaweAPI.getTaskManager().async(() -> {
            BukkitWorld world = new BukkitWorld(Bukkit.getWorlds().getFirst());
            this.session = WorldEdit.getInstance().newEditSession(world);

            final RandomPattern pattern = new RandomPattern();
            pattern.add(new BaseBlock(BlockTypes.CAKE), 0.94);
            pattern.add(new BaseBlock(BlockTypes.MAGENTA_TERRACOTTA), 0.03); // 樱桃
            pattern.add(new BaseBlock(BlockTypes.BLACK_TERRACOTTA), 0.03);   // 巧克力

            session.setBlocks((Region) region, pattern);

            session.flushQueue();
        });

        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());
        Bukkit.getOnlinePlayers().forEach(player -> {
            PlayerProfile profile = Util.getPitProfile(player);
            profile.getEventData().callHistoricalEvents(getEventName(), 1);
        });
        // 事件 2 分钟后结束（原先是 5 分钟）
        this.runnable = new BukkitRunnable() {
            @Override
            public void run() {
                ThePit.getInstance().getEventFactory().inactiveEvent(CakeEvents.this);
            }
        };
        this.runnable.runTaskLater(ThePit.getInstance(), EVENT_DURATION_TICKS);
    }

    @Override
    public void onInactive() {
        // 撤销生成的结构
        FaweAPI.getTaskManager().async(() -> {
            if (this.session != null) {
                this.session.undo(session);
                this.session.flushQueue();
            }
        });

        HandlerList.unregisterAll(this);
        if (this.runnable != null) this.runnable.cancel();

        // 勋章统计：只统计金币（不含 XP）
        for (UUID uuid : dataCache.keySet()) {
            CakePlayerData d = dataCache.get(uuid);
            if (d != null && d.coins >= 5000 && Bukkit.getPlayer(uuid) != null) {
                new CakeEventMedal().addProgress(PlayerProfile.getPlayerProfileByUuid(uuid), 1);
            }
        }

        lastClickTime.clear();
        fastClicks.clear();
        cooldownUntil.clear();
    }

    /* ---------------------- 交互处理 ---------------------- */

    @EventHandler
    public void onEatCake(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) return;

        // 区域判定
        Location loc = block.getLocation();
        AxisAlignedBB blockAABB = new AxisAlignedBB(
                loc.getX() - 1, loc.getY() - 1, loc.getZ() - 1,
                loc.getX() + 1, loc.getY() + 1, loc.getZ() + 1);

        if (!alignedBB.intersectsWith(blockAABB)) return;

        Player player = event.getPlayer();
        UUID uid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // 冷却与频率限制
        Long until = cooldownUntil.get(uid);
        if (until != null && now < until) {
            player.sendActionBar(CC.translate("&c手速太快了，休息一下吧！"));
            return;
        }

        Long last = lastClickTime.get(uid);
        long delta = (last == null) ? CLICK_INTERVAL_MS : now - last;

        if (delta < CLICK_INTERVAL_MS) {                 // <100 ms：本次点击无效
            if (delta < FAST_CLICK_MS) {                 // <50 ms：计为超速
                int cnt = fastClicks.getOrDefault(uid, 0) + 1;
                fastClicks.put(uid, cnt);

                if (cnt >= FAST_CLICK_LIMIT) {           // 连续 5 次超速 → 冷却
                    cooldownUntil.put(uid, now + COOLDOWN_MS);
                    fastClicks.put(uid, 0);
                    player.sendActionBar(CC.translate("&c手速太快了，休息一下吧！"));
                }
            }
            return;
        }

        lastClickTime.put(uid, now);
        fastClicks.put(uid, 0);

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(uid);

        CakePlayerData cakeData = dataCache.computeIfAbsent(uid, u -> {
            CakePlayerData d = new CakePlayerData();
            d.uuid = u;
            d.name = player.getDisplayName();
            return d;
        });

        // 防止从内部吃
        if (!canEat(block.getLocation())) {
            CC.send(MessageType.EVENT, player,
                    "&d&l蛋糕! &c你不能吃里面的蛋糕! 请先吃掉周围的蛋糕!");
            return;
        }

        Material type = block.getType();

        // 普通蛋糕（含“最后一口”判定）
        if (type.toString().endsWith("CAKE")) {
            BlockData bd = block.getBlockData();
            if (bd instanceof org.bukkit.block.data.type.Cake cakeDataBlock) {
                // 计算步进（每 50 口）
                double step = cakeData.clicked / 50.0d;

                double perBite = 1.0 + 0.25 * step;      // 普通每口
                double lastBonus = 5.0 + 1.25 * step;    // 最后一口额外

                boolean lastBite = (cakeDataBlock.getBites() >= cakeDataBlock.getMaximumBites());

                double totalCoins = perBite + (lastBite ? lastBonus : 0.0);

                if (lastBite) {
                    block.setType(Material.AIR); // 整块吃完
                    // 可能掉落特殊物品
                    maybeDropSpecials(player);
                } else {
                    int next = Math.min(cakeDataBlock.getMaximumBites(), cakeDataBlock.getBites() + 1);
                    cakeDataBlock.setBites(next);
                    block.setBlockData(cakeDataBlock, true);
                    block.getState().update();
                }

                cakeData.clicked++;
                cakeData.coins += totalCoins;

                profile.grindCoins(totalCoins);
                profile.setCoins(profile.getCoins() + totalCoins);

                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1, 1);

                CC.send(MessageType.EVENT, player,
                        "&d&l蛋糕! &6+" + numFormatTwo.format(totalCoins) + " 金币 &7| &d吃了" +
                                cakeData.clicked + " 口蛋糕 &7| 本局累计 &6" +
                                numFormatTwo.format(cakeData.coins) + " 金币");
            }
        }
        // 特殊：樱桃 & 巧克力
        else if (type == Material.MAGENTA_TERRACOTTA || type == Material.BLACK_TERRACOTTA) {

            // 计算步进（每 50 口）
            int step = cakeData.clicked / 50;

            if (type == Material.MAGENTA_TERRACOTTA) { // 樱桃：金币
                block.setType(Material.AIR);

                double coins = 150.0 + 37.5 * step;

                cakeData.clicked++;
                cakeData.coins += coins;

                profile.grindCoins(coins);
                profile.setCoins(profile.getCoins() + coins);

                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1, 1);

                CC.send(MessageType.EVENT, player, "&d&l蛋糕! &7吃下了 &c樱桃 &6+" +
                        numFormatTwo.format(coins) + " 金币&7!");
            } else { // 黑色陶瓦：巧克力 → 经验（不受 XP Boost）
                block.setType(Material.AIR);

                double xp = 100.0;

                cakeData.clicked++;               // 只计点击数，不计入 coins
                profile.setExperience(profile.getExperience() + xp);
                profile.applyExperienceToPlayer(player); // 同步到玩家

                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1, 1);

                CC.send(MessageType.EVENT, player, "&d&l蛋糕! &7吃下了 &4巧克力 &b+" +
                        numFormatTwo.format(xp) + " 经验&7!");
            }
        }
    }

    /**
     * 最后一口掉落特殊物品
     */
    private void maybeDropSpecials(Player player) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        if (rnd.nextDouble() < DROP_MILK_CHANCE) {
            player.getInventory().addItem(PitItems.CAKE_CHERRY.toItemStack());
        }
        if (rnd.nextDouble() < DROP_COOKIE_CHANCE) {
            player.getInventory().addItem(PitItems.COOKIES.toItemStack());
        }
    }


    /* ---------------------- 辅助逻辑 ---------------------- */

    private boolean canEat(Location location) {
        for (Location face : LocationUtil.getFaces(location)) {
            Material m = face.getBlock().getType();
            if (m != Material.CAKE && m != Material.BLACK_TERRACOTTA && m != Material.MAGENTA_TERRACOTTA) {
                return true;
            }
        }
        return false;
    }

    /* ---------------------- 数据结构 ---------------------- */

    public static class CakePlayerData {
        private UUID uuid;
        private String name;
        private double coins;     // 仅统计金币，不含经验（用于勋章判定）
        private int clicked;   // 累计“吃”的次数（用于“每 50 口”步进）
        private Cooldown cooldown;
    }

    /* ---------------------- 自定义 AABB（占位） ---------------------- */
    // 如果你已有自己的 AxisAlignedBB，请使用原实现
    public static class AxisAlignedBB {
        private final double minX, minY, minZ;
        private final double maxX, maxY, maxZ;

        public AxisAlignedBB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
        }

        public boolean intersectsWith(AxisAlignedBB other) {
            return this.maxX >= other.minX && this.minX <= other.maxX
                    && this.maxY >= other.minY && this.minY <= other.maxY
                    && this.maxZ >= other.minZ && this.minZ <= other.maxZ;
        }
    }

    // 占位的勋章类，按你项目里的实际类名/包名替换
//    public static class CakeEventMedal {
//        public void addProgress(PlayerProfile profile, int v) {
//            // your logic
//        }
//    }

    // 占位的 Cooldown 类型（若项目已有请删除本占位）
    public static class Cooldown {
    }
}
