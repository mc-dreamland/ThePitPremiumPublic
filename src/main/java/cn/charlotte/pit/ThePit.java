package cn.charlotte.pit;

import cn.charlotte.pit.actionbar.IActionBarManager;
import cn.charlotte.pit.activity.ActivityFactory;
import cn.charlotte.pit.api.PitInternalHook;
import cn.charlotte.pit.api.PointsAPI;
import cn.charlotte.pit.buff.BuffFactory;
import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.config.ItemSettings;
import cn.charlotte.pit.config.PitConfig;
import cn.charlotte.pit.data.FixedRewardData;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.database.MongoDB;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.EnchantmentFactor;
import cn.charlotte.pit.event.PitOriginalTimeChangeEvent;
import cn.charlotte.pit.events.EventFactory;
import cn.charlotte.pit.events.EventsHandler;
import cn.charlotte.pit.game.Game;
import cn.charlotte.pit.hologram.HologramFactory;
import cn.charlotte.pit.hook.PitPapiHook;
import cn.charlotte.pit.item.ItemFactor;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.manager.BlockRestoreManager;
import cn.charlotte.pit.medal.MedalFactory;
import cn.charlotte.pit.minigame.MiniGameController;
import cn.charlotte.pit.mode.Mode;
import cn.charlotte.pit.movement.PlayerMoveHandler;
import cn.charlotte.pit.movement.PlayerMovementDispatcher;
import cn.charlotte.pit.npc.AbstractPitNPC;
import cn.charlotte.pit.npc.NpcFactory;
import cn.charlotte.pit.npc.NpcPlaceHolderExpansion;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.PerkFactory;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.pet.PetFactory;
import cn.charlotte.pit.quest.QuestFactory;
import cn.charlotte.pit.runnable.DayNightCycleRunnable;
import cn.charlotte.pit.runnable.LeaderBoardRunnable;
import cn.charlotte.pit.runnable.ProfileLoadRunnable;
import cn.charlotte.pit.runnable.RebootRunnable;
import cn.charlotte.pit.setup.LocationsSetup;
import cn.charlotte.pit.util.ClassUtil;
import cn.charlotte.pit.util.bossbar.BossBarHandler;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.dependencies.Dependency;
import cn.charlotte.pit.util.dependencies.DependencyManager;
import cn.charlotte.pit.util.dependencies.loaders.LoaderType;
import cn.charlotte.pit.util.dependencies.loaders.ReflectionClassLoader;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.menu.MenuUpdateTask;
import cn.charlotte.pit.util.nametag.NametagHandler;
import cn.charlotte.pit.nametag.hiding.NameHidingManager;
import cn.charlotte.pit.util.random.RandomStrUtil;
import cn.charlotte.pit.util.rank.RankUtil;
import cn.charlotte.pit.util.sound.SoundFactory;
import cn.charlotte.pit.util.time.TimeUtil;
import cn.charlotte.pit.webhook.CustomNameValidWebhook;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Executor;


/**
 * @author EmptyIrony, Misoryan
 */

public class ThePit extends JavaPlugin implements PluginMessageListener, Executor {


    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ThePit.class);
    public static Mode mode = Mode.Myhic;
    public static PitInternalHook api;
    private static boolean DEBUG_SERVER = false;
    private static String bungeeServerName;
    private static ThePit instance;
    private static String random;
    private final ItemFactor itemFactor = new ItemFactor();
    private IActionBarManager actionBarManager;
    private MongoDB mongoDB;
    private JedisPool jedis;
    private PitConfig pitConfig;
    private EnchantmentFactor enchantmentFactor;
    private NpcFactory npcFactory;
    private NametagHandler nametagHandler;
    private Game game;
    private MedalFactory medalFactory;
    private PerkFactory perkFactory;
    private BuffFactory buffFactory;
    private HologramFactory hologramFactory;
    private EventFactory eventFactory;
    private PlayerMoveHandler movementHandler;
    @Getter
    private PlayerMovementDispatcher movementDispatcher;
    private QuestFactory questFactory;
    private BossBarHandler bossBar;
    private RebootRunnable rebootRunnable;
    private MiniGameController miniGameController;
    private SoundFactory soundFactory;
    private PetFactory petFactory;
    @Getter
    private BlockRestoreManager blockRestoreManager;

    private PointsAPI pointsAPI;

    private NpcPlaceHolderExpansion npcPlaceHolderExpansion;
    private PitPapiHook pitPapiHook;
    @Getter
    private LocationsSetup locationsSetup;
    @Getter
    private CustomNameValidWebhook customNameValidator;
    @Getter
    private ActivityFactory activityFactory;

    public static boolean isDEBUG_SERVER() {
        return ThePit.DEBUG_SERVER;
    }

    public static void toggleDebugServer() {
        ThePit.DEBUG_SERVER = !ThePit.DEBUG_SERVER;
    }

    public static ThePit getInstance() {
        return ThePit.instance;
    }

    public static String getBungeeServerName() {
        return bungeeServerName == null ? random : bungeeServerName.toUpperCase();
    }

    private static void setBungeeServerName(String name) {
        bungeeServerName = name;
    }

    public static boolean callTimeChange(long time) {
        final PitOriginalTimeChangeEvent event = new PitOriginalTimeChangeEvent(time);
        event.callEvent();
        return event.isCancelled();
    }

    public static PitInternalHook getApi() {
        return api;
    }

    public static void setApi(PitInternalHook api) {
        ThePit.api = api;
    }

    @Override
    public void onLoad() {
        instance = this;
        this.loadDepend();
    }

    @Override
    public void onEnable() {
        random = RandomStrUtil.generateRandomString(3);
        saveDefaultConfig();
        ItemSettings.load(this);

        movementDispatcher = new PlayerMovementDispatcher();
        getServer().getPluginManager().registerEvents(movementDispatcher, this);

        this.loadConfig();
        this.loadDatabase();
        this.loadWebhooks();
        this.loadEnchantment();
        this.loadListener();
        PitMain.start();
        this.loadMenu();
        this.loadNpc();
        this.loadGame();
        this.loadMedals();
        this.loadBuffs();
        this.loadHologram();
        this.loadSound();
        this.loadPerks();
        this.loadQuest();
        this.loadEvents();
        this.loadActivity();
        try {
            TimeUtil.initHolidayData();
        } catch (Exception e) {

        }

        try {
            this.loadMoveHandler();
        } catch (Exception ignored) {
        }

        this.loadQuest();
        this.initBossBar();

        this.initPet();

        this.rebootRunnable = new RebootRunnable();
        this.rebootRunnable.runTaskTimerAsynchronously(this, 20, 20);

        this.miniGameController = new MiniGameController();
        this.miniGameController.runTaskTimerAsynchronously(this, 1, 1);
        this.blockRestoreManager = new BlockRestoreManager(this);


        new DayNightCycleRunnable().runTaskTimer(this, 20, 20);

        Bukkit.getScheduler().runTaskTimerAsynchronously(
                this,                       // 插件实例
                new LeaderBoardRunnable(this),  // 只实现 Runnable，不要再自己开线程池
                0L, 36000L                     // Run every 0.5h
        );

        try {
            EventsHandler.loadFromDatabase();
        } catch (Exception ignored) {

        }
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setGameRule(GameRule.MOB_GRIEFING, false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_WARDEN_SPAWNING, false);
            world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
            world.setGameRule(GameRule.RANDOM_TICK_SPEED, 3);
        }
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "velocity:player_info");
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "velocity:connect");
        FixedRewardData.refreshAll();
        new ProfileLoadRunnable(this);


        try {
            new PitBotManager(this).start();
        } catch (Exception ex) {
            getLogger().log(java.util.logging.Level.SEVERE, "Bot 功能启动失败", ex);
            if (PitBotManager.getInstance() != null) PitBotManager.getInstance().close();
        }
        try {
            new NameHidingManager(this).start();
        } catch (Exception exception) {
            getLogger().log(java.util.logging.Level.SEVERE, "玩家名隐藏功能启动失败", exception);
            if (NameHidingManager.getInstance() != null) NameHidingManager.getInstance().close();
        }
        new PitCommands(this);
        npcPlaceHolderExpansion = new NpcPlaceHolderExpansion();
        pitPapiHook = new PitPapiHook();
        npcPlaceHolderExpansion.register();
        pitPapiHook.register();
        locationsSetup = new LocationsSetup();
        Bukkit.getPluginManager().registerEvents(locationsSetup, this);
    }

    @Override
    public void onDisable() {
        if (NameHidingManager.getInstance() != null) NameHidingManager.getInstance().close();
        if (PitBotManager.getInstance() != null) PitBotManager.getInstance().close();
        if (eventFactory != null) eventFactory.close();
        synchronized (Bukkit.getOnlinePlayers()) {
            CC.boardCast("&6&l公告! &7正在关闭服务器...");
            CombatListener.removeAllBanner();//清理增益旗帜
            npcPlaceHolderExpansion.unregister();
            pitPapiHook.unregister();
            for (AbstractPitNPC abstractPitNPC : NpcFactory.getPitNpc()) {
                abstractPitNPC.getNpc().despawn();
                abstractPitNPC.getNpc().destroy();
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                    if (profile.isLoaded()) {
                        profile.setInventory(InventoryUtil.playerInventoryFromPlayer(player));
                        profile.save(null);
                        CC.boardCast("&6&l公告! &7正在保存 " + player.getDisplayName() + " 的数据...");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    activityFactory.getActivityData().save();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            CC.boardCast("&6&l公告! &7关闭服务器...");
        }
        blockRestoreManager.cancelAllAndClear();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!"BungeeCord".equals(channel)) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        //setServerName
        if ("GetServer".equals(subchannel)) {
            setBungeeServerName(in.readUTF());
        }
    }

    private void initPet() {
        this.petFactory = new PetFactory();
        this.petFactory.init();
        Bukkit.getPluginManager().registerEvents(this.petFactory, this);
    }

    private void initBossBar() {
        this.bossBar = new BossBarHandler();
    }

    private void loadSound() {
        this.soundFactory = new SoundFactory();
        this.soundFactory.init();
    }

    private void loadHologram() {
        this.hologramFactory = new HologramFactory();
        this.hologramFactory.init();
    }

    private void loadMenu() {
        this.getServer().getScheduler().runTaskTimer(this, new MenuUpdateTask(), 20L, 20L);
    }

    public void loadEnchantment() {
        this.enchantmentFactor = new EnchantmentFactor();
    }

    public void loadPerks() {
        this.perkFactory = new PerkFactory();
    }

    private void loadMedals() {
        this.medalFactory = new MedalFactory();
        this.medalFactory.init();
        getServer().getPluginManager().registerEvents(this.medalFactory, this);
    }

    private void loadBuffs() {
        this.buffFactory = new BuffFactory();
        this.buffFactory.init();
    }

    private void loadGame() {
        this.game = new Game();
        this.game.initRunnable();
    }

    @SneakyThrows
    private void loadMoveHandler() {
        this.movementHandler = new PlayerMoveHandler();
        movementDispatcher.addListener(this.movementHandler);
    }

    public void loadQuest() {
        this.questFactory = new QuestFactory();
    }

    public void loadListener() {
        log.info("Loading Listeners...");
        Collection<Class<?>> classes = ClassUtil.getClassesInPackage(this, "cn.charlotte.pit");
        classes.stream()
                .filter(clazz -> clazz.isAnnotationPresent(AutoRegister.class))
                .filter(Listener.class::isAssignableFrom)
                .filter(clazz -> !AbstractEnchantment.class.isAssignableFrom(clazz))
                .filter(clazz -> !isSubclassOfTypedPerk(clazz)) // 排除 TypedPerk 子类
                .map(clazz -> {
                    try {
                        return (Listener) clazz.getDeclaredConstructor().newInstance();
                    } catch (Exception ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(listener -> {
                    Bukkit.getPluginManager().registerEvents(listener, ThePit.getInstance());
                    log.info("    Register Listeners {}", listener.getClass().getName());
                });
    }

    //public void loadListener() {
//    Collection<Class<?>> classes = ClassUtil.getClassesInPackage(this, "cn.charlotte.pit");
//    for (Class<?> clazz : classes) {
//        if (clazz.isAnnotationPresent(AutoRegister.class)) {
//            if (Listener.class.isAssignableFrom(clazz)) {
//                try {
//                    Bukkit.getPluginManager()
//                            .registerEvents((Listener) clazz.newInstance(), ThePit.getInstance());
//                } catch (Exception ignored) {
//
//                }
//            }
//        }
//    }
//}
    private boolean isSubclassOfTypedPerk(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            if (current.getSuperclass() == TypedPerk.class) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private void loadConfig() {
        log.info("Loading configuration...");
        this.pitConfig = new PitConfig(this);
        this.pitConfig.load();
        log.info("Loaded configuration!");

        DEBUG_SERVER = this.pitConfig.isDebugServer();
        if (DEBUG_SERVER) {
            this.getServer().getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void permissionCheckOnJoin(PlayerLoginEvent event) {
                    final Player player = event.getPlayer();
                    if (pitConfig.isDebugServerPublic()) {
                        final String name = RankUtil.getPlayerRealColoredName(player.getUniqueId());
                        if (!player.isOp()) {
                            if (name.contains("&7") || name.contains("§7")) {
                                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "你所在的用户组当前无法进入此分区!");
                            }
                        }
                    } else if (!player.isOp() && !player.hasPermission("thepit.admin")) {
                        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "此分区当前未开放,开放时间请关注官方公告!");
                    }
                }
            }, this);
        }

        if (pitConfig.isRedisEnable()) {
            jedis = new JedisPool(
                    new GenericObjectPoolConfig(),
                    pitConfig.getRedisAddress(),
                    pitConfig.getRedisPort(),
                    Protocol.DEFAULT_TIMEOUT,
                    pitConfig.getRedisPassword(),
                    false
            );
        }
    }

    private void loadDatabase() {
        log.info("Loading mongodb...");
        this.mongoDB = new MongoDB();
        this.mongoDB.connect();
        log.info("Loaded mongodb!");
    }

    private void loadWebhooks() {
        log.info("Loading webhooks...");
        String url = pitConfig.getCustomNameWebhook();
        if (url == null || url.isEmpty()) {
            log.warn("Custom name webhook is not enabled.");
        } else {
            this.customNameValidator = new CustomNameValidWebhook(url);
            log.info("Loaded webhooks!");
        }
    }

    private void loadNpc() {
        log.info("Loading NPCFactory...");
        this.npcFactory = new NpcFactory();
        log.info("Loaded NPCFactory!");
    }

    public void loadEvents() {
        log.info("Loading Events...");
        this.eventFactory = new EventFactory();
        log.info("Loaded Events!");
    }

    public void loadActivity() {
        log.info("Loading Activity...");
        this.activityFactory = new ActivityFactory();
        this.activityFactory.load();
        log.info("Loaded Activity!");
    }

    private void loadDepend() {
        DependencyManager dependencyManager = new DependencyManager(this, new ReflectionClassLoader(this));
        dependencyManager.loadDependencies(
                new Dependency(
                        "annotations",
                        "org.jetbrains",
                        "annotations",
                        "13.0",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "Junit",
                        "junit",
                        "junit",
                        "4.11",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "Pool2",
                        "org.apache.commons",
                        "commons-pool2",
                        "2.4.2",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "Apache Logging",
                        "commons-logging",
                        "commons-logging",
                        "1.2",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "MongoDB",
                        "org.mongodb",
                        "mongo-java-driver",
                        "3.12.2",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "Jedis",
                        "redis.clients",
                        "jedis",
                        "2.9.0",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "MongoJack",
                        "org.mongojack",
                        "mongojack",
                        "4.8.1",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "JackSon-Annotations",
                        "com.fasterxml.jackson.core",
                        "jackson-annotations",
                        "2.10.3",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "JackSon-Databind",
                        "com.fasterxml.jackson.core",
                        "jackson-databind",
                        "2.10.3",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "JackSon-Core",
                        "com.fasterxml.jackson.core",
                        "jackson-core",
                        "2.10.3",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "JackSon-DataType",
                        "com.fasterxml.jackson.datatype",
                        "jackson-datatype-jsr310",
                        "2.10.3",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "Bson4Jackson",
                        "de.undercouch",
                        "bson4jackson",
                        "2.9.2",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "Redisson",
                        "org.redisson",
                        "redisson",
                        "3.0.1",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "Netty-common",
                        "io.netty",
                        "netty-common",
                        "4.0.42.Final",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "Netty-codec",
                        "io.netty",
                        "netty-codec",
                        "4.0.42.Final",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "netty-buffer",
                        "io.netty",
                        "netty-buffer",
                        "4.0.42.Final",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "netty-transport",
                        "io.netty",
                        "netty-transport",
                        "4.0.42.Final",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "netty-handler",
                        "io.netty",
                        "netty-handler",
                        "4.0.42.Final",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "reactor-core",
                        "io.projectreactor",
                        "reactor-core",
                        "3.3.9.RELEASE",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "rxjava",
                        "io.reactivex.rxjava2",
                        "rxjava",
                        "2.2.19",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "cache-api",
                        "javax.cache",
                        "cache-api",
                        "1.0.0",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "byte-buddy",
                        "net.bytebuddy",
                        "byte-buddy",
                        "1.10.14",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "jboss-marshalling-river",
                        "org.jboss.marshalling",
                        "jboss-marshalling-river",
                        "2.0.9.Final",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "jodd-bean",
                        "org.jodd",
                        "jodd-bean",
                        "5.1.6",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "snakeyaml",
                        "org.yaml",
                        "snakeyaml",
                        "1.26",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "slf4j-api",
                        "org.slf4j",
                        "slf4j-api",
                        "1.7.30",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "jboss-marshalling-river",
                        "org.jboss.marshalling",
                        "jboss-marshalling-river",
                        "2.0.9.Final",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "jboss-marshalling",
                        "org.jboss.marshalling",
                        "jboss-marshalling",
                        "2.0.9.Final",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "jackson-dataformat-yaml",
                        "com.fasterxml.jackson.dataformat",
                        "jackson-dataformat-yaml",
                        "2.11.2",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "netty-all",
                        "io.netty",
                        "netty-all",
                        "4.0.42.Final",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "guava",
                        "com.google.guava",
                        "guava",
                        "29.0-jre",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "asm",
                        "org.ow2.asm",
                        "asm",
                        "7.3.1",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "asm-commons",
                        "org.ow2.asm",
                        "asm-commons",
                        "7.3.1",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "asm-tree",
                        "org.ow2.asm",
                        "asm-tree",
                        "7.3.1",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "asm-util",
                        "org.ow2.asm",
                        "asm-util",
                        "7.3.1",
                        LoaderType.REFLECTION
                ),
                new Dependency(
                        "nashorn-core",
                        "org.openjdk.nashorn",
                        "nashorn-core",
                        "15.3",
                        LoaderType.REFLECTION
                )
        );
    }

    public MongoDB getMongoDB() {
        return this.mongoDB;
    }

    public PitConfig getPitConfig() {
        return this.pitConfig;
    }

    public EnchantmentFactor getEnchantmentFactor() {
        return this.enchantmentFactor;
    }

    public NpcFactory getNpcFactory() {
        return this.npcFactory;
    }

    public NametagHandler getNametagHandler() {
        return this.nametagHandler;
    }

    public void setNametagHandler(NametagHandler nametagHandler) {
        this.nametagHandler = nametagHandler;
    }

    public Game getGame() {
        return this.game;
    }

    public MedalFactory getMedalFactory() {
        return this.medalFactory;
    }

    public PerkFactory getPerkFactory() {
        return this.perkFactory;
    }

    public BuffFactory getBuffFactory() {
        return this.buffFactory;
    }

    public HologramFactory getHologramFactory() {
        return this.hologramFactory;
    }

    public EventFactory getEventFactory() {
        return this.eventFactory;
    }

    public PlayerMoveHandler getMovementHandler() {
        return this.movementHandler;
    }

    public QuestFactory getQuestFactory() {
        return this.questFactory;
    }

    public BossBarHandler getBossBar() {
        return this.bossBar;
    }

    public ItemFactor getItemFactor() {
        return this.itemFactor;
    }

    public RebootRunnable getRebootRunnable() {
        return this.rebootRunnable;
    }

    public MiniGameController getMiniGameController() {
        return this.miniGameController;
    }

    public SoundFactory getSoundFactory() {
        return this.soundFactory;
    }

    public PetFactory getPetFactory() {
        return this.petFactory;
    }

    public IActionBarManager getActionBarManager() {
        return actionBarManager;
    }

    public void setActionBarManager(IActionBarManager actionBarManager) {
        this.actionBarManager = actionBarManager;
    }

    public JedisPool getJedis() {
        return jedis;
    }

    public PointsAPI getPointsAPI() {
        return pointsAPI;
    }

    public void setPointsAPI(PointsAPI pointsAPI) {
        this.pointsAPI = pointsAPI;
    }

    public void info(String s) {
        log.info(s);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        if (Bukkit.isPrimaryThread()) {
            command.run();
        } else {
            Bukkit.getScheduler().runTask(this, command);
        }
    }
}
