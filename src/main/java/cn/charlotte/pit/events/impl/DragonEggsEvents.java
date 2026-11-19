package cn.charlotte.pit.events.impl;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.perk.PerksPassivesEnum;
import cn.charlotte.pit.util.chat.CC;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/*
龙蛋是一个小型事件。
当事件开始时，龙蛋总是出现在深渊区域的中央。点击龙蛋会奖励玩家经验值（XP）和金币。每次点击固定获得25金币，而每次点击获得的经验值则会随着玩家的点击次数线性增加。每位玩家点击之间有1秒冷却时间。
龙蛋上方会显示一个红色数字，即经验值的倍率。该倍率决定了玩家点击龙蛋时获得的经验值数量。倍率从“x1”开始，每点击20到30次会提升一次。当龙蛋上显示“x8”并被点击30次后，龙蛋会消失，事件结束。因此，每次事件最多可以点击230次。
龙蛋上括号中的数字表示该位置的龙蛋被所有玩家点击的总次数。每被点击10次，龙蛋会移动到深渊区域的另一个位置。
每次点击获得的经验值可通过以下公式计算：
（玩家点击次数 + 当前倍率）× 2
例如，在龙蛋倍率为x7时，玩家的第22次点击将获得 (22 + 7) × 2 = 58 点经验值。该经验奖励会受到“XP加成”升级的影响。
 */
public class DragonEggsEvents implements INormalEvent, Listener {

    private static final int MIN_CLICKS = 50;
    private static final int EXTRA_CLICKS_PER_PLAYER = 2;
    private static final int CLICK_THRESHOLD = 30;
    private static final int SEARCH_RADIUS = 10;
    private static final int MAX_ATTEMPTS = 20;
    // ===== 1. 新增字段 =====
    private static final long CLICK_COOLDOWN = 500;   // 点击间隔
    private static final long CLICK_INTERVAL_MS = 100;   // 合法间隔
    private static final long FAST_CLICK_MS = 50;    // 判定为“超速”的阈值
    private static final int FAST_CLICK_LIMIT = 5;     // 超速累计上限
    private static final long COOLDOWN_MS = 5_000; // 冷却时长
    private final Map<UUID, AtomicInteger> playerClicks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Map<UUID, Integer> fastClicks = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private Location eggLocation;
    private int clicks = 0;
    private int moveTimes = 0;
    private Hologram hologram;
    private boolean isActive = false;
    private boolean isClickable = false;
    private int MAX_CLICKS = MIN_CLICKS;

    @Override
    public List<String> getLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&6&l>> " + getEventName() + " &6&l<<");
        lore.add("");
        lore.add("&6&l事件机制");
        lore.add("&7- 中心区域刷新龙蛋");
        lore.add("&7- 点击龙蛋获金币经验");
        lore.add("&7- 每30点击传送位置");
        lore.add("&7- 奖励随点击次数递增");
        lore.add("&7- 全服玩家共同点击");
        lore.add("");
        return lore;
    }

    @Override
    public String getEventInternalName() {
        return "dragon_egg";
    }

    @Override
    public String getEventName() {
        return "&5龙蛋";
    }

    /* ------------------------- 生命周期 ------------------------- */

    @Override
    public int requireOnline() {
        return NewConfiguration.getEventOnlineRequired().get(getEventInternalName());
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());
    }

    private void unregisterEvents() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void onActive() {
        MAX_CLICKS = MIN_CLICKS + Bukkit.getOnlinePlayers().size() * EXTRA_CLICKS_PER_PLAYER;
        Location preset = ThePit.getInstance().getPitConfig().getEggLoc();
        if (preset == null) {
            broadcastMessage("&5&l龙蛋！ &7活动区域未设置，请联系管理员设置！");
            deactivateEvent();
            return;
        }
        eggLocation = preset.clone();
        isActive = true;
        isClickable = true;
        registerEvents();
        Bukkit.getOnlinePlayers().forEach(player -> {
            PlayerProfile profile = Util.getPitProfile(player);
            profile.getEventData().callHistoricalEvents(getEventName(), 1);
        });
        broadcastMessage("&5&l龙蛋！ &d龙蛋已在中心点位刷新,请前往点击！");
        setEggLocation(eggLocation);
        playSoundToPlayers(Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 1.5f);
        scheduleEventDeactivation();

        lastClickTime.clear();
        fastClicks.clear();
        cooldownUntil.clear();
        playerClicks.clear();
    }

    private void scheduleEventDeactivation() {
        Bukkit.getScheduler().runTaskLater(
                ThePit.getInstance(),
                this::deactivateEvent,
                8 * 20 * 60L // 8 分钟后结束
        );
    }

    private void deactivateEvent() {
        ThePit.getInstance().getEventFactory().inactiveEvent(this);
        isActive = false;
        cleanup();
    }

    @Override
    public void onInactive() {
        unregisterEvents();
        cleanup();
        playSoundToPlayers(Sound.ENTITY_ENDER_DRAGON_DEATH, 1.5f, 1.5f);
        broadcastMessage("&5&l龙蛋！ &7活动已结束！");
    }

    private void cleanup() {
        if (eggLocation != null) {
            eggLocation.getBlock().setType(Material.AIR);
        }
        despawnHolograms();
        isActive = false;
        clicks = 0;
        moveTimes = 0;
        lastClickTime.clear();
        fastClicks.clear();
        cooldownUntil.clear();
        playerClicks.clear();
    }

    /* ------------------------- 核心逻辑 ------------------------- */

    private void setEggLocation(Location loc) {
        prepareLocation();
        eggLocation = loc.clone();
        eggLocation.getBlock().setType(Material.DRAGON_EGG);
        createHolograms(eggLocation);
    }

    private void prepareLocation() {
        if (eggLocation != null) {
            eggLocation.getBlock().setType(Material.AIR);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        /* ---------- 基本过滤 ---------- */
        if (!isActive
                || event.getClickedBlock() == null
                || event.getClickedBlock().getType() != Material.DRAGON_EGG) {
            return;
        }

        // 拦截原版交互（防止龙蛋瞬移）
        event.setCancelled(true);

        /* ---------- 是否可点击 ---------- */
        if (!isClickable) {
            despawnHolograms();      // 清理悬浮字
            return;                  // 本轮点蛋结束
        }

        /* ---------- 频率限制 & 冷却 ---------- */
        Player player = event.getPlayer();
        UUID uid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // 冷却检查
        Long until = cooldownUntil.get(uid);
        if (until != null && now < until) {
            long remain = (until - now + 999) / 1000; // 取整到秒
            player.sendActionBar(CC.translate("&c点击过快！冷却中 &e" + remain + "s"));
            return;
        }

        // 节奏与超速检测
        Long last = lastClickTime.get(uid);
        long delta = (last == null) ? CLICK_INTERVAL_MS : now - last;

        if (delta < CLICK_INTERVAL_MS) {              // <100 ms：本次点击无效
            if (delta < FAST_CLICK_MS) {              // <50 ms：判定为“超速”
                int cnt = fastClicks.getOrDefault(uid, 0) + 1;
                fastClicks.put(uid, cnt);

                if (cnt >= FAST_CLICK_LIMIT) {        // 连续 5 次超速 → 5 s 冷却
                    cooldownUntil.put(uid, now + COOLDOWN_MS);
                    fastClicks.put(uid, 0);           // 重置计数
                    player.sendActionBar(CC.translate(
                            "&c诶！朋友！休息一会儿吧！"));
                }
            }
            return;                                   // 阻断后续正常逻辑
        } else if (last != null && last <= CLICK_COOLDOWN) {
            event.setCancelled(true);
            return;
        }

        // 合法点击：刷新时间戳并清零超速计数
        lastClickTime.put(uid, now);
        fastClicks.put(uid, 0);

        /* ---------- 原本的点击逻辑 ---------- */
        PlayerProfile profile =
                PlayerProfile.getPlayerProfileByUuid(uid);
        handleClick(player, profile);   // 奖励金币/经验、更新全局 clicks
        updateClickState();             // 检查是否移动或结束活动
    }


    private void handleClick(Player player, PlayerProfile profile) {
        if (clicks > MAX_CLICKS) {
            return;
        }

        double coins = 25;
        int clickTimes = playerClicks.computeIfAbsent(player.getUniqueId(), k -> new AtomicInteger()).incrementAndGet();
        double exp = (clickTimes + moveTimes) * 2.0d;

        double extraCoinsBoost = 1.0;
        double extraExpBoost = 1.0;
        if (profile.hasExtraExpBonus()) {
            extraExpBoost += 0.35;
        }
        if (profile.hasExtraGoldBonus()) {
            extraCoinsBoost += 0.35;
        }
        int xpLevel = PerksPassivesEnum.XP_BOOST.getPerk().getLevel(profile);
        int coinLevel = PerksPassivesEnum.GOLD_BOOST.getPerk().getLevel(profile);
        extraExpBoost += xpLevel * 0.1;
        extraCoinsBoost += coinLevel * 0.1;

        coins *= extraCoinsBoost;
        exp *= extraExpBoost;

        profile.setCoins(profile.getCoins() + coins);
        profile.setExperience(profile.getExperience() + exp);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.5f, 1.5f);
        player.sendMessage(CC.translate(String.format("&5&l龙蛋！ &7点击龙蛋%s 次获得 &e%s &6金币 &e%s &b经验&7", clickTimes, coins, exp)));
        clicks++;
        updateHolograms();
    }

    private void updateClickState() {
        if (clicks >= MAX_CLICKS) {
            deactivateEvent();
        } else if (clicks % CLICK_THRESHOLD == 0) {
            moveEggToNewLocation();
        }
    }

    private void moveEggToNewLocation() {
        moveTimes++;
        prepareLocation();
        if (eggLocation != null) {
            setEggLocation(findRandomLocation(eggLocation));
        }
        broadcastMessage("&5&l龙蛋！ &7龙蛋已被移动到了新的位置！");
        playSoundToPlayers(Sound.ENTITY_ENDER_DRAGON_HURT, 1.5f, 1.5f);
        isClickable = true;
    }

    private Location findRandomLocation(Location origin) {
        Random random = new Random();
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            double x = origin.getX() + randomOffset(random);
            double z = origin.getZ() + randomOffset(random);
            Location newLoc = new Location(origin.getWorld(), x, origin.getY(), z);

            if (newLoc.getBlock().getType() == Material.AIR) {
                if (isValidArea(newLoc)) {
                    return newLoc;
                }
            }
        }
        return origin;
    }

    private boolean isValidArea(Location center) {
        int solidBlockCount = 0;
        int areaSize = 5;
        int halfSize = areaSize / 2;

        for (int xOffset = -halfSize; xOffset <= halfSize; xOffset++) {
            for (int zOffset = -halfSize; zOffset <= halfSize; zOffset++) {
                Location checkLoc = center.clone().add(xOffset, 0, zOffset);
                if (checkLoc.getBlock().getType() != Material.AIR) {
                    solidBlockCount++;
                }
            }
        }

        int maxAllowed = (areaSize * areaSize) / 2;
        return solidBlockCount <= maxAllowed;
    }

    private int randomOffset(Random random) {
        return random.nextInt(31) - SEARCH_RADIUS; // [-10, 20]
    }

    /* ------------------------- 工具方法 ------------------------- */

    private void broadcastMessage(String message) {
        Bukkit.broadcastMessage(CC.translate(message));
    }

    private void playSoundToPlayers(Sound sound, float volume, float pitch) {
        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), sound, volume, pitch));
    }

    private void createHolograms(Location location) {
        if (hologram != null) {
            DHAPI.removeHologram("dragon_egg");
        }
        hologram = DHAPI.createHologram("dragon_egg", location.clone().add(0, 2, 0), false, List.of("§a" + clicks + "§7/§e" + MAX_CLICKS + " §7| §cx" + moveTimes, "§e§l点击"));
    }

    private void updateHolograms() {
        if (hologram == null) {
            hologram = DHAPI.createHologram("dragon_egg", eggLocation.clone().add(0, 2, 0), false, List.of("§a" + clicks + "§7/§e" + MAX_CLICKS + " §7| §cx" + moveTimes, "§e§l点击"));
        } else {
            hologram.getPage(0).setLine(0, "§a" + clicks + "§7/§e" + MAX_CLICKS + " §7| §cx" + moveTimes);
        }
    }

    private void despawnHolograms() {
        DHAPI.removeHologram("dragon_egg");
        hologram = null;
    }
}
