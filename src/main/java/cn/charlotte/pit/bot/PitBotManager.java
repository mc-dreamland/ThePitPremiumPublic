package cn.charlotte.pit.bot;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.perk.MegaStreak;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.PerksBundlesEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.perk.type.boosts.KungFuKnowledgePerk;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.level.LevelUtil;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.scheduler.BukkitTask;
import com.google.common.util.concurrent.AtomicDouble;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/** Main-thread-only service. Bots never enter CombatListener.handleKill/PitKillEvent. */
public final class PitBotManager implements Listener {
    public static final String BOT_TAG = "pit-practice-bot";
    private static PitBotManager instance;
    public enum Kind {
        TRAINING("training", "PitTrainingBot"), WILD("wild", "PitWildBot");
        final String path;
        final String type;
        Kind(String path, String type) { this.path = path; this.type = type; }
        public static Kind parse(String value) {
            for (Kind kind : values()) if (kind.path.equalsIgnoreCase(value)) return kind;
            throw new IllegalArgumentException("区域类型只能是 training 或 wild");
        }
    }

    private record Group(boolean enabled, BotRegion region, int cap, int minimum, double botsPerPlayer, int spawnMaxPrestige, long respawnMillis,
                         double xp, double coins, double radius, double leash,
                         String displayName, String bonusDisplayName) { }
    private record WildSpawnPoint(Location location, BotSpawnPoint coordinates) {
        WildSpawnPoint(Location location) {
            this(location.clone(), new BotSpawnPoint(location.getWorld().getName(), location.getX(), location.getY(), location.getZ()));
        }

        boolean near(Location at, double radius) {
            return at.getWorld() != null && coordinates.near(at.getWorld().getName(), at.getX(), at.getY(), at.getZ(), radius);
        }

        boolean sameBlock(Location at) {
            return at.getWorld() != null && coordinates.sameBlock(at.getWorld().getName(), at.getX(), at.getY(), at.getZ());
        }
    }
    private static final class State {
        final Kind kind;
        final ActiveMob active;
        final Mob entity;
        final Location home;
        final double health;
        final BotReward reward;
        final long forcedUntil;
        final BotDamageLedger ledger = new BotDamageLedger();
        UUID lastAttacker;
        long lastHit;
        State(Kind kind, ActiveMob active, Mob entity, double health, BotReward reward, long forcedUntil) {
            this.kind = kind;
            this.active = active;
            this.entity = entity;
            this.home = entity.getLocation();
            this.health = health;
            this.reward = reward;
            this.forcedUntil = forcedUntil;
        }
    }

    private static final class ForcedSpawnBatch {
        final Kind kind;
        final CommandSender sender;
        final int requested;
        int remaining;
        int spawned;
        int failures;

        ForcedSpawnBatch(Kind kind, CommandSender sender, int requested) {
            this.kind = kind;
            this.sender = sender;
            this.requested = requested;
            this.remaining = requested;
        }
    }

    private final ThePit plugin;
    private final File file;
    private final Map<UUID, State> bots = new HashMap<>();
    private final EnumMap<Kind, Group> groups = new EnumMap<>(Kind.class);
    private final EnumMap<Kind, Long> nextSpawn = new EnumMap<>(Kind.class);
    private final EnumMap<Kind, Integer> pendingSpawns = new EnumMap<>(Kind.class);
    private final EnumMap<Kind, Integer> targetCounts = new EnumMap<>(Kind.class);
    private final Deque<ForcedSpawnBatch> forcedSpawns = new ArrayDeque<>();
    // Successful spawns only. Configuration reloads must not reset session counters.
    private final EnumMap<Kind, Long> spawnedTotals = new EnumMap<>(Kind.class);
    private final EnumMap<Kind, Long> bonusSpawnedTotals = new EnumMap<>(Kind.class);
    private YamlConfiguration config;
    private BotPrestigeRewards prestigeRewards;
    private BotStreakRules streakRules;
    private int firstMegaThreshold = 50;
    private final Set<UUID> trainingStreakOwners = new HashSet<>();
    private List<WildSpawnPoint> wildSpawnPoints = List.of();
    private BukkitTask task;
    private BukkitTask refillTask;
    private int refillCursor;
    private TrainingProtection protection;
    private boolean running;
    private boolean spawning;
    private boolean pausedForMajorEvent;
    private long nextErrorLog;

    public PitBotManager(ThePit plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bots.yml");
        instance = this;
    }

    public static PitBotManager getInstance() { return instance; }

    public void start() throws IOException {
        String version = MythicBukkit.inst().getDescription().getVersion();
        if (!MythicMobsVersion.supported(version)) throw new IOException("Bot 功能需要 MythicMobs " + MythicMobsVersion.REQUIRED + "，当前为 " + version);
        if (MythicBukkit.inst().getVolatileCodeHandler().getAIHandler() == null) {
            plugin.getLogger().warning("MythicMobs 缺少当前服务端原生 AI 适配；Bot 使用 Paper 寻路/近战，不使用 MM 自定义 AI。请在测试服验证 MM 基础刷怪兼容性。");
        }
        if (!file.exists()) plugin.saveResource("bots.yml", false);
        // Use MM's lowercase directory. Preserve customized definitions from the old uppercase path.
        Path mmFolder = MythicBukkit.inst().getDataFolder().toPath();
        Path target = mmFolder.resolve("mobs/PitTrainingBots.yml");
        Path legacy = mmFolder.resolve("Mobs/PitTrainingBots.yml");
        boolean definitionsChanged = false;
        if (!Files.exists(target) && Files.isRegularFile(legacy)) {
            Files.createDirectories(target.getParent());
            Files.move(legacy, target); // No replacement: never overwrite an administrator's lowercase definition.
            definitionsChanged = true;
            plugin.getLogger().info("已将 Bot 定义迁移到 MythicMobs/mobs/PitTrainingBots.yml，保留原有配置。");
        }
        if (!Files.exists(target)) {
            Files.createDirectories(target.getParent());
            try (InputStream source = plugin.getResource("mythicmobs/PitTrainingBots.yml")) {
                if (source == null) throw new IOException("Missing bundled Bot definitions");
                Files.copy(source, target);
            }
            definitionsChanged = true;
        }
        // On Windows these may be the same file; only warn about distinct files on case-sensitive systems.
        if (Files.exists(legacy) && !Files.isSameFile(target, legacy)) {
            plugin.getLogger().warning("MythicMobs/mobs 和 Mobs 下均存在 PitTrainingBots.yml，未覆盖任何文件；请处理旧 Mobs 文件，避免 Bot 类型重复定义。");
        }
        if (definitionsChanged) MythicBukkit.inst().getMobManager().loadMobs();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        protection = new TrainingProtection(this, plugin);
        Bukkit.getPluginManager().registerEvents(protection, plugin);
        reload();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> runSafely(this::tick), 20L, 20L);
        // Global budget: at most one bounded spawn attempt per five ticks, alternating regions.
        refillTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> runSafely(this::refillOne), 21L, 5L);
    }

    public void reload() {
        cancelForcedSpawns("配置重载");
        removeAll();
        config = YamlConfiguration.loadConfiguration(file);
        upgradeConfig();
        loadWildSpawnPoints();
        prestigeRewards = new BotPrestigeRewards(config.getBoolean("rewards.prestige-decay.enabled", true),
                config.getInt("rewards.prestige-decay.start-prestige", 10),
                number("rewards.prestige-decay.reduction-per-prestige", .05, 0, 1),
                number("rewards.prestige-decay.minimum-multiplier", .10, 0, 1));
        streakRules = new BotStreakRules(config.getBoolean("kill-streak.enabled", true),
                config.getInt("kill-streak.max-prestige", 10), config.getInt("kill-streak.max-streak", 30));
        // Enum entries also cover startup before the perk factory has finished registering its list.
        firstMegaThreshold = Stream.concat(plugin.getPerkFactory().getPerks().stream(),
                        Arrays.stream(PerksBundlesEnum.values()).map(PerksBundlesEnum::getPerk))
                .filter(perk -> perk instanceof MegaStreak).mapToInt(perk -> ((MegaStreak) perk).getStreakNeed())
                .filter(threshold -> threshold > 0).min().orElse(50);
        if (streakRules.maxStreak() > streakRules.effectiveCap(firstMegaThreshold)) {
            plugin.getLogger().info("Bot 连杀配置上限 " + streakRules.maxStreak() + "，为避免超级连杀，实际上限为 "
                    + streakRules.effectiveCap(firstMegaThreshold));
        }
        groups.clear();
        for (Kind kind : Kind.values()) {
            String p = kind.path + ".";
            int defaultCap = kind == Kind.TRAINING ? 15 : 60;
            groups.put(kind, new Group(config.getBoolean(p + "enabled", true), readRegion(kind),
                    Math.max(0, config.getInt(p + "max-bots", defaultCap)),
                    Math.max(0, config.getInt(p + "min-bots", kind == Kind.TRAINING ? 4 : 15)),
                    number(p + "bots-per-player", kind == Kind.TRAINING ? 1 : .75, 0, Double.MAX_VALUE),
                    Math.max(0, config.getInt(p + "spawn-max-prestige", kind == Kind.TRAINING ? 15 : 10)),
                    (long) (number(p + "respawn-seconds", 2, 1, 3600) * 1000),
                    number(p + "experience", kind == Kind.TRAINING ? BotDefaults.TRAINING_EXPERIENCE : BotDefaults.WILD_EXPERIENCE, 0, 10000),
                    number(p + "coins", kind == Kind.TRAINING ? BotDefaults.TRAINING_COINS : BotDefaults.WILD_COINS, 0, 10000),
                    number(p + "target-radius", kind == Kind.TRAINING ? 14 : 16, 2, 48),
                    number(p + "leash-radius", kind == Kind.TRAINING ? 24 : 28, 4, 64),
                    displayName(p + "display-name", kind == Kind.TRAINING ? "&a[Bot] 训练陪练" : "&e[Bot] 荒野陪练"),
                    displayName(p + "bonus-display-name", "&6[Bot] " + (kind == Kind.TRAINING ? "训练陪练" : "荒野陪练"))));
            if (groups.get(kind).enabled && !hasSpawnArea(kind, groups.get(kind))) {
                plugin.getLogger().info(kind == Kind.TRAINING
                        ? "训练场尚未设置有效区域，不会刷新。使用 /pitbot pos1/pos2 training"
                        : "野外尚未添加刷新点，不会刷新。站在刷新位置使用 /pitbot add");
            }
        }
        running = config.getBoolean("enabled", true);
        nextSpawn.clear();
        pendingSpawns.clear();
        targetCounts.clear();
        // Remove loaded survivors from a crash/reload; unloaded chunks are cleaned by EntitiesLoadEvent.
        removeLoadedSurvivors();
        if (running) {
            for (Kind kind : Kind.values()) {
                if (MythicBukkit.inst().getMobManager().getMythicMob(kind.type).isEmpty()) {
                    plugin.getLogger().severe("缺少 MythicMobs 类型 " + kind.type + "，请检查 mobs/PitTrainingBots.yml 并 /mm reload");
                    running = false;
                }
            }
        }
    }

    public void close() {
        running = false;
        if (task != null) task.cancel();
        if (refillTask != null) refillTask.cancel();
        pendingSpawns.clear();
        cancelForcedSpawns("Bot 功能停止");
        removeAll();
        HandlerList.unregisterAll(this);
        if (protection != null) HandlerList.unregisterAll(protection);
        if (instance == this) instance = null;
    }

    private double number(String path, double fallback, double min, double max) {
        double value = config.getDouble(path, fallback);
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }

    private String displayName(String path, String fallback) {
        String value = config.getString(path);
        return value == null || value.isBlank() ? fallback : value;
    }

    private void upgradeConfig() {
        int oldVersion = config.getInt("config-version", 0);
        Map<String, Object> updates = BotConfigUpgrade.updates(oldVersion, config::get);
        if (updates.isEmpty()) return;
        updates.forEach(config::set);
        try {
            Path backup = file.toPath().resolveSibling("bots.yml.v" + Math.max(1, oldVersion) + ".bak");
            if (!Files.exists(backup)) Files.copy(file.toPath(), backup);
            config.save(file);
            plugin.getLogger().info("已升级 Bot 配置，原配置备份为 " + backup.getFileName() + "；保留已有坐标、数量和其他自定义设置。");
            if (oldVersion < 7) plugin.getLogger().info("Bot 数量上限现完全由 max-bots 配置；实体属性和穿戴装备改由 MythicMobs/mobs/PitTrainingBots.yml 管理，bots.yml 中旧属性/装备字段保留但不再读取，请按需迁移。");
            if (oldVersion < 8) plugin.getLogger().info("Bot 已知旧奖励预设已下调：训练场 2经验/2金币，野外 2.5经验/4金币，奖励 Bot 2倍；其他自定义奖励数值不变，请检查 bots.yml。");
            if (oldVersion < 9) plugin.getLogger().info("Bot 新增 kill-streak 配置：默认 ≤10P、当前连杀上限30；大型事件期间全部 Bot 清场并停止刷新。");
        } catch (IOException ex) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Bot 新配置已在内存应用，但备份/保存失败", ex);
        }
    }

    private BotRegion readRegion(Kind kind) {
        if (kind == Kind.WILD) return null; // Wild bots use explicit add/list/remove spawn points.
        Location a = config.getLocation(kind.path + ".pos1");
        Location b = config.getLocation(kind.path + ".pos2");
        if (a == null || b == null || a.getWorld() == null || !a.getWorld().equals(b.getWorld())) return null;
        return BotRegion.of(a.getWorld().getName(), a.getBlockX(), a.getBlockY(), a.getBlockZ(),
                b.getBlockX(), b.getBlockY(), b.getBlockZ());
    }

    private void loadWildSpawnPoints() {
        List<WildSpawnPoint> points = new ArrayList<>();
        for (Object value : config.getList("wild.spawn-points", List.of())) {
            if (!(value instanceof Location at) || at.getWorld() == null) continue;
            try {
                if (points.stream().noneMatch(point -> point.sameBlock(at))) points.add(new WildSpawnPoint(at));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("忽略无效野外 Bot 刷新点: " + ex.getMessage());
            }
        }
        wildSpawnPoints = List.copyOf(points);
    }

    private boolean hasSpawnArea(Kind kind, Group group) {
        return kind == Kind.WILD ? !wildSpawnPoints.isEmpty() : group.region != null;
    }

    public int addWildSpawnPoint(Location at) throws IOException {
        if (!safe(at)) throw new IllegalArgumentException("刷新点需要实体地面及两格空气，并位于已加载区块内");
        if (inTraining(at) || inLobby(at)) throw new IllegalArgumentException("刷新点必须位于训练场和出生保护区外");
        if (wildSpawnPoints.stream().anyMatch(point -> point.sameBlock(at))) throw new IllegalArgumentException("该方块已有刷新点");
        List<WildSpawnPoint> points = new ArrayList<>(wildSpawnPoints);
        points.add(new WildSpawnPoint(at));
        saveWildSpawnPoints(points);
        return points.size();
    }

    public void removeWildSpawnPoint(int id) throws IOException {
        if (id < 1 || id > wildSpawnPoints.size()) throw new IllegalArgumentException("刷新点编号不存在，使用 /pitbot list 查看");
        List<WildSpawnPoint> points = new ArrayList<>(wildSpawnPoints);
        WildSpawnPoint removed = points.remove(id - 1);
        saveWildSpawnPoints(points);
        for (State state : new ArrayList<>(bots.values())) {
            if (state.kind == Kind.WILD && removed.sameBlock(state.home)) remove(state);
        }
    }

    private void saveWildSpawnPoints(List<WildSpawnPoint> points) throws IOException {
        config.set("wild.spawn-points", points.stream().map(WildSpawnPoint::location).toList());
        config.save(file);
        wildSpawnPoints = List.copyOf(points);
        pendingSpawns.put(Kind.WILD, 0);
        nextSpawn.remove(Kind.WILD);
    }

    public List<String> wildSpawnPointDescriptions() {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < wildSpawnPoints.size(); i++) {
            Location at = wildSpawnPoints.get(i).location;
            lines.add(String.format(Locale.ROOT, "#%d %s (%.2f, %.2f, %.2f)", i + 1, at.getWorld().getName(), at.getX(), at.getY(), at.getZ()));
        }
        return lines;
    }

    private static String kindName(Kind kind) {
        return kind == Kind.TRAINING ? "训练场" : "野外";
    }

    private int forcedPending(Kind kind) {
        return forcedSpawns.stream().filter(batch -> batch.kind == kind).mapToInt(batch -> batch.remaining).sum();
    }

    public int enqueueForcedSpawn(Kind kind, int requested, CommandSender sender) {
        if (!running) throw new IllegalArgumentException("Bot 功能未开启，请检查配置及启动日志");
        if (majorEventActive()) throw new IllegalArgumentException("大型事件期间禁止生成 Bot（含强制刷新）");
        if (requested <= 0) throw new IllegalArgumentException("数量必须大于 0");
        Group group = groups.get(kind);
        if (!group.enabled) throw new IllegalArgumentException(kindName(kind) + " Bot 已禁用");
        if (!hasSpawnArea(kind, group)) throw new IllegalArgumentException(kind == Kind.TRAINING
                ? "请先用 /pitbot pos1/pos2 training 设置训练场区域" : "请先用 /pitbot add 添加野外刷新点");
        int accepted = BotForceRules.accepted(requested, group.cap, alive(kind), forcedPending(kind));
        if (accepted == 0) throw new IllegalArgumentException(kindName(kind) + "已达到数量上限，或剩余名额已被强制刷新队列占用");
        forcedSpawns.addLast(new ForcedSpawnBatch(kind, sender, accepted));
        return accepted;
    }

    private void refillForced() {
        ForcedSpawnBatch batch = forcedSpawns.peekFirst();
        Group group = groups.get(batch.kind);
        // A larger configured cap can require more than 60 seconds of paced spawning.
        // Stop on repeated failures instead of truncating healthy, progressing large batches.
        if (!group.enabled || !hasSpawnArea(batch.kind, group) || alive(batch.kind) >= group.cap) {
            finishForced(batch, "区域不可用或已达上限");
            return;
        }
        try {
            Location location = batch.kind == Kind.WILD ? wildSpawnLocation(group, List.of(), true)
                    : forcedTrainingLocation(group);
            if (location != null && spawn(batch.kind, group, location, true)) {
                batch.spawned++;
                batch.remaining--;
                batch.failures = 0;
                if (batch.remaining == 0) finishForced(batch, "完成");
            } else if (++batch.failures >= BotForceRules.MAX_PLACEMENT_FAILURES) {
                finishForced(batch, "连续刷新失败，请检查区块是否加载、空间是否安全、刷新点是否被占用");
            }
        } catch (RuntimeException ex) {
            finishForced(batch, "生成异常，已停止；详情见控制台");
            throw ex;
        }
    }

    private void finishForced(ForcedSpawnBatch batch, String reason) {
        forcedSpawns.remove(batch);
        String message = kindName(batch.kind) + "强制刷新" + reason + ": 成功 " + batch.spawned + "/" + batch.requested + " 只";
        if (!(batch.sender instanceof Player player) || player.isOnline()) batch.sender.sendMessage("§e" + message);
        plugin.getLogger().info(message + "（操作人: " + batch.sender.getName() + "）");
    }

    private void cancelForcedSpawns(String reason) {
        while (!forcedSpawns.isEmpty()) finishForced(forcedSpawns.peekFirst(), reason);
    }

    public List<String> countDescriptions() {
        List<String> lines = new ArrayList<>();
        if (majorEventActive()) lines.add("大型事件进行中：Bot 已清理，自动与强制刷新均暂停");
        lines.add("当前存活 Bot: " + (alive(Kind.TRAINING) + alive(Kind.WILD)) + " 只；累计成功刷新: "
                + spawnedTotals.values().stream().mapToLong(Long::longValue).sum() + " 只（本次插件启动，重载不清零）");
        for (Kind kind : Kind.values()) {
            Group group = groups.get(kind);
            List<State> living = bots.values().stream().filter(s -> s.kind == kind && s.entity.isValid() && !s.entity.isDead()).toList();
            long bonus = living.stream().filter(s -> s.reward.bonus()).count();
            long forced = living.stream().filter(s -> s.forcedUntil > 0).count();
            lines.add(String.format(Locale.ROOT, "%s: 存活 %d/%d，普通 %d，奖励 %d（其中强制 %d）；自动目标 %d，待补 %d，强制排队 %d",
                    kindName(kind), living.size(), group == null ? 0 : group.cap, living.size() - bonus, bonus, forced,
                    targetCounts.getOrDefault(kind, 0), pendingSpawns.getOrDefault(kind, 0), forcedPending(kind)));
            lines.add(kindName(kind) + "累计成功刷新 " + spawnedTotals.getOrDefault(kind, 0L)
                    + " 只，其中累计奖励 Bot " + bonusSpawnedTotals.getOrDefault(kind, 0L) + " 只");
        }
        return lines;
    }

    public static boolean isBot(Entity entity) {
        return entity != null && entity.getScoreboardTags().contains(BOT_TAG);
    }

    public static boolean isBotType(String type) {
        return Kind.TRAINING.type.equals(type) || Kind.WILD.type.equals(type);
    }

    static boolean contains(BotRegion region, Location location) {
        return region != null && location != null && location.getWorld() != null
                && region.contains(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    public static boolean inTraining(Location location) {
        PitBotManager manager = instance;
        if (manager == null || !manager.running) return false;
        Group group = manager.groups.get(Kind.TRAINING);
        return group != null && group.enabled && contains(group.region, location);
    }

    public static boolean inTraining(Entity entity) { return entity != null && inTraining(entity.getLocation()); }

    public static Entity source(Entity entity) {
        if (entity instanceof Projectile p && p.getShooter() instanceof Entity shooter) return shooter;
        if (entity instanceof TNTPrimed tnt && tnt.getSource() != null) return tnt.getSource();
        if (entity instanceof AreaEffectCloud cloud && cloud.getSource() instanceof Entity owner) return owner;
        if (entity instanceof Tameable pet && pet.getOwner() instanceof Player owner) return owner;
        return entity;
    }

    public static boolean blockedPvp(Player attacker, Player victim) {
        return inTraining(attacker) != inTraining(victim);
    }

    /** Delayed effects must retain the side on which the original hit occurred. */
    public static boolean blockedPvp(Player attacker, Player victim, boolean originInside) {
        return blockedPvp(attacker, victim) || originInside != inTraining(victim);
    }

    public static boolean isTrainingPvp(Entity attacker, Entity victim) {
        return attacker instanceof Player && victim instanceof Player
                && !attacker.getUniqueId().equals(victim.getUniqueId()) && inTraining(attacker) && inTraining(victim);
    }

    /** Applied once to each ordinary/true/direct damage component, never to Bot combat. */
    public static double scalePvpDamage(Entity attacker, Entity victim, double damage) {
        if (!(attacker instanceof Player) || !(victim instanceof Player)) return damage;
        if (attacker.getUniqueId().equals(victim.getUniqueId())) return damage;
        return damage * TrainingCombatRules.playerDamageMultiplier(inTraining(attacker), inTraining(victim));
    }

    /** Narrow exception to lobby attack restrictions; never grants permission to damage a player. */
    public static boolean canAttackTrainingBot(Player player, Entity target) {
        PitBotManager manager = instance;
        if (manager == null || !manager.running || target == null) return false;
        State state = manager.bots.get(target.getUniqueId());
        return state != null && state.kind == Kind.TRAINING && target.isValid() && !target.isDead() && inTraining(target)
                && manager.eligibleContributor(state.kind, player, state.home, manager.groups.get(state.kind).leash);
    }

    /** Used before ANY enchant/perk listener, including listeners which ignore cancellation. */
    public static boolean skipCustomCombat(EntityDamageByEntityEvent event) {
        Entity attacker = source(event.getDamager());
        PitBotManager manager = instance;
        if (attacker instanceof Player player && event.getEntity() instanceof Player victim
                && (blockedPvp(player, victim)
                || (manager != null && manager.protection != null && manager.protection.forbidden(event)))) {
            event.setCancelled(true);
            return true;
        }
        return isBot(attacker) || isBot(event.getEntity()) || event.isCancelled();
    }

    /** A narrow exception for ordinary perk hooks; the complete enchant/quest chain stays isolated. */
    public static boolean supportsBotCombat(EntityDamageByEntityEvent event) {
        PitBotManager manager = instance;
        if (manager == null || !manager.running || event.isCancelled() || manager.majorEventActive()
                || event.getFinalDamage() <= 0) return false;
        Entity attacker = source(event.getDamager());
        State target = manager.bots.get(event.getEntity().getUniqueId());
        if (target != null) {
            return attacker instanceof Player player
                    && (event.getDamager() instanceof Player || event.getDamager() instanceof Projectile)
                    && manager.eligibleContributor(target.kind, player, target.home, manager.groups.get(target.kind).leash);
        }
        State source = manager.bots.get(attacker.getUniqueId());
        return source != null && event.getEntity() instanceof Player player
                && manager.eligibleTarget(source.kind, player, source.home, manager.groups.get(source.kind).leash);
    }

    static PlayerProfile profile(Player player) { return PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()); }

    private boolean participant(Player player) {
        PlayerProfile profile = profile(player);
        return player.isOnline() && player.isValid() && !player.isDead() && !player.hasMetadata("NPC")
                && profile.isLoaded() && !profile.isEditingMode() && !profile.isTempInvUsing()
                && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE);
    }

    boolean eligibleTarget(Kind kind, Player player, Location origin, double radius) {
        if (!eligibleContributor(kind, player, origin, radius)) return false;
        return kind != Kind.TRAINING || inTraining(player);
    }

    /** Damage and payout eligibility are separate from the bot's strictly local AI targets. */
    private boolean eligibleContributor(Kind kind, Player player, Location origin, double radius) {
        Group group = groups.get(kind);
        if (!participant(player)
                || !origin.getWorld().equals(player.getWorld()) || origin.distanceSquared(player.getLocation()) > radius * radius) return false;
        if (kind == Kind.TRAINING) return group.enabled && contains(group.region, origin);
        return !inTraining(player) && profile(player).isInArena() && !inLobby(player.getLocation());
    }

    private boolean spawnEligiblePrestige(Kind kind, Player player) {
        return profile(player).getPrestige() <= groups.get(kind).spawnMaxPrestige;
    }

    public void setPosition(Kind kind, int corner, Location location) throws IOException {
        if (kind != Kind.TRAINING) throw new IllegalArgumentException("野外请使用 /pitbot add 添加刷新点，无需设置区域");
        config.set(kind.path + ".pos" + corner, location.clone());
        config.save(file);
        reload();
    }

    private void runSafely(Runnable action) {
        if (!running) return;
        try {
            if (majorEventActive()) {
                onMajorEventStarted();
                return;
            }
            if (pausedForMajorEvent) {
                pausedForMajorEvent = false;
                nextSpawn.clear();
                plugin.getLogger().info("大型事件结束，恢复 Bot 自动刷新。");
            }
            action.run();
        }
        catch (Exception ex) {
            if (System.currentTimeMillis() >= nextErrorLog) {
                nextErrorLog = System.currentTimeMillis() + 60_000;
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Bot 更新失败（相同错误最多每分钟输出一次）", ex);
            }
        }
    }

    private boolean majorEventActive() {
        return plugin.getEventFactory() != null && plugin.getEventFactory().isMajorEventBusy();
    }

    /** Invoked synchronously before the major event's onActive(), with periodic guards as a backstop. */
    public void onMajorEventStarted() {
        if (!running) return;
        boolean firstPause = !pausedForMajorEvent;
        pendingSpawns.clear();
        targetCounts.clear();
        nextSpawn.clear();
        trainingStreakOwners.clear();
        cancelForcedSpawns("大型事件开始，已取消");
        removeAll(); // Remove, never kill: no reward/kill-streak settlement.
        if (firstPause) removeLoadedSurvivors();
        pausedForMajorEvent = true;
        if (firstPause) plugin.getLogger().info("大型事件开始：已清理训练场/野外全部 Bot，并暂停自动与强制刷新。");
    }

    public static boolean activeTraining(Player player) {
        PitBotManager manager = instance;
        if (manager == null || !manager.running || manager.majorEventActive()) return false;
        Group group = manager.groups.get(Kind.TRAINING);
        return group != null && group.enabled && contains(group.region, player.getLocation());
    }

    /** Only protect actually earned, bounded Bot streaks; never preserve a spawn-area mega streak. */
    public static boolean keepTrainingBotStreak(Player player, PlayerProfile profile) {
        PitBotManager manager = instance;
        return manager != null && activeTraining(player) && manager.streakRules.preserveTraining(profile.getPrestige(),
                profile.getStreakKills(), manager.firstMegaThreshold, manager.trainingStreakOwners.contains(player.getUniqueId()));
    }

    private boolean advanceBotStreak(Player player, PlayerProfile profile) {
        if (majorEventActive() || CombatListener.isNight() || profile.isMaybeBot()) return false;
        double before = profile.getStreakKills();
        double next = streakRules.next(profile.getPrestige(), before, firstMegaThreshold);
        if (!(next > before)) return false;
        profile.setBotStreakKills(next);
        if (!(profile.getStreakKills() > before)) return false;
        if (activeTraining(player)) trainingStreakOwners.add(player.getUniqueId());
        if (profile.getHighestStreaks() < profile.getStreakKills()) profile.setHighestStreaks((int) profile.getStreakKills());
        return true;
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        trainingStreakOwners.remove(event.getPlayer().getUniqueId());
    }

    private void tick() {
        trainingStreakOwners.removeIf(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            return player == null || !activeTraining(player);
        });
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        long now = System.currentTimeMillis();
        for (State state : new ArrayList<>(bots.values())) {
            Group group = groups.get(state.kind);
            if (!state.entity.isValid() || state.entity.isDead()) {
                // A death is settled synchronously by onDeath, never by this periodic cleanup.
                bots.remove(state.entity.getUniqueId());
                continue;
            }
            if ((state.kind == Kind.TRAINING && !contains(group.region, state.entity.getLocation()))
                    || (state.kind == Kind.WILD && (inTraining(state.entity) || inLobby(state.entity.getLocation())))
                    || !state.home.getWorld().equals(state.entity.getWorld())
                    || state.home.distanceSquared(state.entity.getLocation()) > group.leash * group.leash) {
                remove(state);
                continue;
            }
            if (BotForceRules.expired(state.forcedUntil, state.lastHit, now)) {
                remove(state);
                continue;
            }
            Player target = state.lastAttacker == null || now - state.lastHit > 10_000 ? null : Bukkit.getPlayer(state.lastAttacker);
            if (target == null || !eligibleTarget(state.kind, target, state.home, group.leash)) {
                target = players.stream().filter(p -> spawnEligiblePrestige(state.kind, p) && eligibleTarget(state.kind, p, state.entity.getLocation(), group.radius))
                        .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(state.entity.getLocation()))).orElse(null);
            }
            state.entity.setTarget(target);
            // Keep targeting, Paper pathfinding and melee independent of MM's custom AI.
            if (target == null) {
                state.entity.getPathfinder().stopPathfinding();
            } else if (state.entity.getLocation().distanceSquared(target.getLocation()) <= 4) {
                state.entity.getPathfinder().stopPathfinding();
                if (state.entity.hasLineOfSight(target)) {
                    state.entity.swingMainHand();
                    state.entity.attack(target);
                }
            } else {
                state.entity.getPathfinder().moveTo(target, 1.0);
            }
            if (state.forcedUntil == 0 && target == null && (state.lastHit == 0 || now - state.lastHit > 10_000)
                    && players.stream().noneMatch(p -> spawnEligiblePrestige(state.kind, p) && eligibleTarget(state.kind, p, state.home, group.leash))) remove(state);
        }
        for (Kind kind : Kind.values()) {
            Group group = groups.get(kind);
            List<Player> eligible = participants(kind, players);
            int target = desired(kind, group, eligible.size());
            targetCounts.put(kind, target);
            trimExcess(kind, target, now);
            if (now < nextSpawn.getOrDefault(kind, 0L)) continue;
            nextSpawn.put(kind, now + group.respawnMillis);
            pendingSpawns.put(kind, BotPopulation.missing(target, alive(kind)));
        }
    }

    private List<Player> participants(Kind kind, Collection<? extends Player> players) {
        return players.stream().filter(p -> spawnEligiblePrestige(kind, p) && eligibleTarget(kind, p, p.getLocation(), 1))
                .filter(p -> kind != Kind.WILD || nearWildSpawnPoint(p.getLocation(), groups.get(kind).radius))
                .map(p -> (Player) p).toList();
    }

    private boolean nearWildSpawnPoint(Location at, double radius) {
        return wildSpawnPoints.stream().anyMatch(point -> point.near(at, radius));
    }

    private int desired(Kind kind, Group group, int players) {
        return !majorEventActive() && group.enabled && hasSpawnArea(kind, group)
                ? BotPopulation.target(players, group.minimum, group.cap, group.botsPerPlayer) : 0;
    }

    private int alive(Kind kind) {
        return (int) bots.values().stream().filter(s -> s.kind == kind && s.entity.isValid() && !s.entity.isDead()).count();
    }

    private void trimExcess(Kind kind, int target, long now) {
        int excess = BotPopulation.missing(alive(kind), target);
        if (excess == 0) return;
        List<State> idle = bots.values().stream().filter(s -> s.kind == kind && s.forcedUntil == 0 && (s.lastHit == 0 || now - s.lastHit > 10_000))
                .sorted(Comparator.<State, Boolean>comparing(s -> s.reward.bonus()).thenComparingLong(s -> s.lastHit)).toList();
        for (State state : idle) {
            if (excess-- <= 0) break;
            remove(state);
        }
    }

    private void refillOne() {
        if (!forcedSpawns.isEmpty()) {
            refillForced();
            return;
        }
        Kind[] kinds = Kind.values();
        for (int attempt = 0; attempt < kinds.length; attempt++) {
            Kind kind = kinds[refillCursor];
            refillCursor = (refillCursor + 1) % kinds.length;
            int pending = pendingSpawns.getOrDefault(kind, 0);
            if (pending <= 0) continue;
            Group group = groups.get(kind);
            // Recheck at execution time: queued work cannot overfill after players leave or die.
            List<Player> eligible = participants(kind, Bukkit.getOnlinePlayers());
            int target = desired(kind, group, eligible.size());
            targetCounts.put(kind, target);
            if (BotPopulation.missing(target, alive(kind)) == 0) {
                pendingSpawns.put(kind, 0);
                continue;
            }
            pendingSpawns.put(kind, pending - 1);
            Location location = kind == Kind.WILD ? wildSpawnLocation(group, eligible)
                    : spawnLocation(kind, group, eligible.get(ThreadLocalRandom.current().nextInt(eligible.size())).getLocation());
            if (location != null) spawn(kind, group, location, false);
            return;
        }
    }

    private Location wildSpawnLocation(Group group, List<Player> eligible) {
        return wildSpawnLocation(group, eligible, false);
    }

    private Location wildSpawnLocation(Group group, List<Player> eligible, boolean forced) {
        List<Location> players = eligible.stream().map(Player::getLocation).toList();
        List<WildSpawnPoint> candidates = new ArrayList<>(wildSpawnPoints.stream()
                .filter(point -> forced || players.stream().anyMatch(at -> point.near(at, group.radius))).toList());
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 16 && !candidates.isEmpty(); attempt++) {
            Location at = candidates.remove(random.nextInt(candidates.size())).location;
            if (inTraining(at) || inLobby(at) || !safe(at) || crowded(at)) continue;
            return at.clone();
        }
        return null;
    }

    private boolean crowded(Location at) {
        return bots.values().stream().anyMatch(s -> s.entity.isValid() && s.entity.getWorld().equals(at.getWorld())
                && s.entity.getLocation().distanceSquared(at) < 9);
    }

    private Location spawnLocation(Kind kind, Group group, Location anchor) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 16; attempt++) {
            int x = anchor.getBlockX() + random.nextInt(-10, 11);
            int z = anchor.getBlockZ() + random.nextInt(-10, 11);
            World world = anchor.getWorld();
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            for (int y = Math.min(world.getMaxHeight() - 2, anchor.getBlockY() + 4); y >= Math.max(world.getMinHeight() + 1, anchor.getBlockY() - 6); y--) {
                Location at = new Location(world, x + 0.5, y, z + 0.5);
                if (!contains(group.region, at) || !contains(group.region, at.clone().add(0, 1.8, 0))
                        || (kind == Kind.WILD && (inTraining(at) || inLobby(at))) || !safe(at)) continue;
                if (at.distanceSquared(anchor) < 9) continue;
                if (!crowded(at)) return at;
            }
        }
        return null;
    }

    /** Bounded search in the configured cuboid, without requiring a nearby player or loading chunks. */
    private Location forcedTrainingLocation(Group group) {
        BotRegion region = group.region;
        World world = Bukkit.getWorld(region.world());
        if (world == null) return null;
        int bottom = (int) Math.max(world.getMinHeight() + 1, region.minY());
        int top = (int) Math.min(world.getMaxHeight() - 3, Math.ceil(region.maxY() - 1.8) - 1);
        if (top < bottom) return null;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 16; attempt++) {
            int x = (int) Math.floor(random.nextDouble(region.minX(), region.maxX()));
            int z = (int) Math.floor(random.nextDouble(region.minZ(), region.maxZ()));
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            int scanBottom = bottom + random.nextInt(Math.max(1, top - bottom - 62));
            int scanTop = Math.min(top, scanBottom + 63);
            for (int y = scanTop; y >= scanBottom; y--) {
                Location at = new Location(world, x + .5, y, z + .5);
                if (contains(region, at) && contains(region, at.clone().add(0, 1.8, 0)) && safe(at) && !crowded(at)) return at;
            }
        }
        return null;
    }

    private static boolean inLobby(Location location) {
        Location a = ThePit.getInstance().getPitConfig().getPitLocA();
        Location b = ThePit.getInstance().getPitConfig().getPitLocB();
        if (a == null || b == null || a.getWorld() == null || !a.getWorld().equals(b.getWorld())) return false;
        return contains(BotRegion.of(a.getWorld().getName(), a.getBlockX(), a.getBlockY(), a.getBlockZ(),
                b.getBlockX(), b.getBlockY(), b.getBlockZ()), location);
    }

    static boolean safe(Location location) {
        World world = location.getWorld();
        if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)
                || location.getY() <= world.getMinHeight() || location.getY() + 2 >= world.getMaxHeight()
                || !world.getWorldBorder().isInside(location)) return false;
        Material floor = location.clone().subtract(0, 1, 0).getBlock().getType();
        return floor.isSolid() && floor != Material.MAGMA_BLOCK && floor != Material.CACTUS
                && floor != Material.CAMPFIRE && floor != Material.SOUL_CAMPFIRE
                && location.getBlock().getType().isAir() && location.clone().add(0, 1, 0).getBlock().getType().isAir();
    }

    private boolean spawn(Kind kind, Group group, Location location, boolean forced) {
        if (!running || majorEventActive()) return false;
        ActiveMob mob;
        spawning = true;
        try { mob = MythicBukkit.inst().getMobManager().spawnMob(kind.type, location, 1); }
        finally { spawning = false; }
        if (mob == null) return false;
        // MM spawn callbacks may synchronously start a major event after our first guard.
        if (!running || majorEventActive()) {
            mob.remove();
            return false;
        }
        Entity entity = mob.getEntity().getBukkitEntity();
        if (!(entity instanceof Mob living) || entity instanceof Player || !entity.isValid()) {
            mob.remove();
            return false;
        }
        try {
            living.addScoreboardTag(BOT_TAG);
            living.addScoreboardTag(BOT_TAG + "-" + kind.path);
            living.setPersistent(false);
            living.setRemoveWhenFarAway(!forced);
            living.setCanPickupItems(false);
            // Paper API only. Retaliation/nearest-target goals must not choose protected players.
            Bukkit.getMobGoals().removeAllGoals(living);
            // MM owns health, damage, movement, armor and equipment. Never replace them with ThePit defaults.
            double startingHealth = living.getHealth();
            if (!Double.isFinite(startingHealth) || startingHealth <= 0 || living.isDead()) {
                throw new IllegalStateException("MM Bot 生成后的实际生命值必须大于 0");
            }
            EntityEquipment equipment = living.getEquipment();
            if (equipment != null) disableEquipmentDrops(equipment);
            BotReward reward = BotReward.roll(config.getBoolean("bonus-bot.enabled", true),
                    number("bonus-bot.chance", .05, 0, 1), ThreadLocalRandom.current().nextDouble(),
                    number("bonus-bot.experience-multiplier", BotDefaults.BONUS_EXPERIENCE_MULTIPLIER, 1, 100),
                    number("bonus-bot.coins-multiplier", BotDefaults.BONUS_COINS_MULTIPLIER, 1, 100));
            String name = CC.translate((reward.bonus() ? group.bonusDisplayName : group.displayName)
                    .replace("{experience-multiplier}", multiplierText(reward.experienceMultiplier()))
                    .replace("{coins-multiplier}", multiplierText(reward.coinsMultiplier())));
            mob.setDisplayName(name);
            living.setCustomName(name);
            // Ordinary bots must also show their name without looking directly at them.
            living.setCustomNameVisible(true);
            if (reward.bonus()) {
                living.addScoreboardTag(BOT_TAG + "-bonus");
            }
            long forcedUntil = forced ? System.currentTimeMillis()
                    + (long) (number("admin.forced-bot-lifetime-seconds", 300, 10, 3600) * 1000) : 0;
            bots.put(living.getUniqueId(), new State(kind, mob, living, startingHealth, reward, forcedUntil));
            spawnedTotals.merge(kind, 1L, Long::sum);
            if (reward.bonus()) bonusSpawnedTotals.merge(kind, 1L, Long::sum);
            return true;
        } catch (RuntimeException ex) {
            // A partially initialized entity must not survive outside the count/reward ledger.
            mob.remove();
            throw ex;
        }
    }

    private static String multiplierText(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    private static void disableEquipmentDrops(EntityEquipment equipment) {
        // Preserve MM's items and metadata, but never release bot equipment into the economy.
        equipment.setHelmetDropChance(0);
        equipment.setChestplateDropChance(0);
        equipment.setLeggingsDropChance(0);
        equipment.setBootsDropChance(0);
        equipment.setItemInMainHandDropChance(0);
        equipment.setItemInOffHandDropChance(0);
    }

    private void remove(State state) {
        bots.remove(state.entity.getUniqueId());
        state.active.remove();
    }

    private void removeAll() {
        for (State state : new ArrayList<>(bots.values())) remove(state);
        bots.clear();
    }

    private void removeLoadedSurvivors() {
        for (ActiveMob mob : new ArrayList<>(MythicBukkit.inst().getMobManager().getActiveMobs())) {
            if (isBotType(mob.getType().getInternalName())) mob.remove();
        }
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) if (isBot(entity)) removeEntity(entity);
        }
    }

    private void removeEntity(Entity entity) {
        MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId()).ifPresentOrElse(ActiveMob::remove, entity::remove);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(MythicMobSpawnEvent event) {
        if (!isBotType(event.getMobType().getInternalName())) return;
        // These reserved types cannot be spawned via eggs, MM spawners or random spawns.
        if (!running || !spawning || majorEventActive()) event.setCancelled(true);
        else event.getEntity().addScoreboardTag(BOT_TAG);
    }

    @EventHandler
    public void onLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) if (isBot(entity) && !bots.containsKey(entity.getUniqueId())) removeEntity(entity);
    }

    /** Called at LOWEST by protection, before ordinary perk combat modifiers. Equipment enchants stay isolated. */
    void scaleDamage(EntityDamageByEntityEvent event) {
        Entity attacker = source(event.getDamager());
        State targetBot = bots.get(event.getEntity().getUniqueId());
        State attackerBot = bots.get(attacker.getUniqueId());
        if (targetBot != null) {
            // Pets, TNT, other mobs and environmental damage cannot AFK-farm the reward pool.
            if (!(attacker instanceof Player player)
                    || !(event.getDamager() instanceof Player || event.getDamager() instanceof Projectile)
                    || !eligibleContributor(targetBot.kind, player, targetBot.home, groups.get(targetBot.kind).leash)) {
                event.setCancelled(true);
                return;
            }
            double base = event.getDamage();
            if (profile(player).hasEquipPerk(PerksBoostsEnum.KUNG_FU_KNOWLEDGE.name())) {
                base = KungFuKnowledgePerk.attackDamage(player);
            }
            double raw = Math.min(base, number("max-player-hit-damage", 10, 0.1, 100));
            event.setDamage(targetBot.kind == Kind.WILD ? BotBalance.outgoing(raw, profile(player).getPrestige(),
                    number("max-player-hit-damage", 10, 0.1, 100), number("wild.outgoing-per-prestige", .15, 0, 10),
                    number("wild.min-outgoing-multiplier", .12, .01, 1)) : raw);
        } else if (attackerBot != null) {
            if (!(event.getEntity() instanceof Player player) || !eligibleTarget(attackerBot.kind, player, attackerBot.home, groups.get(attackerBot.kind).leash)) {
                event.setCancelled(true);
                return;
            }
            double base = event.getDamage(); // MM/vanilla attack attributes and equipment have already contributed.
            event.setDamage(attackerBot.kind == Kind.WILD ? BotBalance.incoming(base, profile(player).getPrestige(),
                    number("wild.incoming-per-prestige", .12, 0, 10), number("wild.max-incoming-multiplier", 5, 1, 50)) : base);
        } else if (isBot(attacker) || isBot(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCombatEffects(EntityDamageByEntityEvent event) {
        if (!supportsBotCombat(event)) return;
        Entity attacker = source(event.getDamager());
        boolean attackingBot = bots.containsKey(event.getEntity().getUniqueId());
        Player player = attackingBot ? (Player) attacker : (Player) event.getEntity();
        Entity bot = attackingBot ? event.getEntity() : attacker;
        PlayerProfile profile = profile(player);
        AtomicDouble extraDamage = new AtomicDouble(0);
        AtomicDouble multiplier = new AtomicDouble(1);
        AtomicBoolean cancel = new AtomicBoolean(false);
        Set<String> applied = new HashSet<>();
        for (TypedPerk<?> perk : profile.getActivePerks()) {
            if (!applied.add(perk.getInternalPerkName())) continue;
            try {
                if (attackingBot) BotCombatEffects.attack(perk, perk.getLevel(profile), player, bot,
                        event.getDamager() instanceof Projectile, event.getDamager() instanceof Arrow,
                        event.getFinalDamage(), extraDamage, multiplier, cancel);
                else BotCombatEffects.damaged(perk, perk.getLevel(profile), player, bot,
                        event.getFinalDamage(), extraDamage, multiplier, cancel);
            } catch (Exception ex) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Bot 战斗天赋效果执行失败: " + perk.getInternalPerkName(), ex);
            }
            if (cancel.get()) break;
        }
        if (cancel.get()) event.setCancelled(true);
        else event.setDamage(BotCombatEffects.damage(event.getDamage(), multiplier.get(), extraDamage.get()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordDamage(EntityDamageByEntityEvent event) {
        State state = bots.get(event.getEntity().getUniqueId());
        if (state == null || !(source(event.getDamager()) instanceof Player player)) return;
        if (event.getFinalDamage() <= 0) return;
        long now = System.currentTimeMillis();
        state.ledger.record(player.getUniqueId(), event.getFinalDamage(), state.entity.getHealth(), now);
        state.lastAttacker = player.getUniqueId();
        state.lastHit = now;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMythicDeath(MythicMobDeathEvent event) {
        if (isBotType(event.getMobType().getInternalName()) || isBot(event.getEntity())) event.setDrops(new ArrayList<>());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        if (!isBot(event.getEntity())) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        State state = bots.remove(event.getEntity().getUniqueId());
        if (!running || state == null || majorEventActive()) return;
        Group group = groups.get(state.kind);
        state.ledger.shares(state.health, System.currentTimeMillis(),
                (long) number("rewards.contribution-timeout-seconds", 30, 1, 300) * 1000).forEach((uuid, share) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !eligibleContributor(state.kind, player, state.home, group.leash)
                    || share < number("rewards.minimum-damage-share", .05, 0, 1)) return;
            PlayerProfile profile = profile(player);
            double prestigeMultiplier = prestigeRewards.multiplier(profile.getPrestige());
            // Only the eligible final attacker gets kill effects and bounded streak progression.
            boolean killingBlow = uuid.equals(state.lastAttacker);
            boolean streakAdvanced = killingBlow && advanceBotStreak(player, profile);
            ResourceManager resources = CombatListener.createBotKillReward(player, profile, event.getEntity(),
                    group.xp, group.coins, killingBlow, streakAdvanced);
            BotReward.Payout payout = state.reward.payout(resources, share,
                    LevelUtil.getLevelTotalExperience(profile.getPrestige(), 120) - profile.getExperience(), prestigeMultiplier);
            double coins = payout.coins();
            double xp = payout.experience();
            // No generic PitKillEvent, kill/quest increments or mythic/material drop rolls.
            profile.setCoins(profile.getCoins() + coins);
            if (xp > 0) {
                profile.setExperience(profile.getExperience() + xp);
                profile.applyExperienceToPlayer(player);
            }
            String prestigeNote = prestigeMultiplier < 1
                    ? String.format(Locale.ROOT, " &7(转生奖励 %.1f%%)", prestigeMultiplier * 100) : "";
            player.sendMessage(CC.translate(String.format(Locale.ROOT, "%s &b+%.1f 经验 &6+%.1f 金币%s",
                    state.reward.bonus() ? "&6&l奖励陪练！" : "&a陪练奖励", xp, coins, prestigeNote)));
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHeal(EntityRegainHealthEvent event) {
        if (isBot(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        State state = bots.get(event.getEntity().getUniqueId());
        if (state == null || event.getTarget() == null) return;
        if (!(event.getTarget() instanceof Player player) || !eligibleTarget(state.kind, player, state.home, groups.get(state.kind).leash)) event.setCancelled(true);
    }

    public String status() {
        StringBuilder text = new StringBuilder("Bot 功能: " + (running ? "已开启" : "已关闭"));
        if (majorEventActive()) text.append("（大型事件期间已清场并暂停刷新）");
        if (streakRules != null) text.append(" | Bot连杀: ").append(streakRules.enabled() ? "≤" + streakRules.maxPrestige()
                + "P，有效上限" + streakRules.effectiveCap(firstMegaThreshold) + "（配置" + streakRules.maxStreak() + "）" : "已关闭");
        for (Kind kind : Kind.values()) {
            Group g = groups.get(kind);
            text.append(" | ").append(kind.path).append(": ")
                    .append(g == null || !hasSpawnArea(kind, g) ? "未配置" : (g.enabled ? "已启用" : "已禁用"))
                    .append(" ").append(alive(kind))
                    .append("/").append(g == null ? 0 : g.cap);
            text.append(" (目标 ").append(targetCounts.getOrDefault(kind, 0)).append("，奖励Bot ")
                    .append(bots.values().stream().filter(s -> s.kind == kind && s.entity.isValid() && !s.entity.isDead() && s.reward.bonus()).count()).append(")");
            if (kind == Kind.WILD) text.append(" 刷新点 ").append(wildSpawnPoints.size());
        }
        return text.toString();
    }
}
