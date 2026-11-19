package cn.charlotte.pit.data;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.ActivityPlayerData;
import cn.charlotte.pit.buff.BuffData;
import cn.charlotte.pit.career.data.CareerData;
import cn.charlotte.pit.contract.ContractData;
import cn.charlotte.pit.data.sub.DamageData;
import cn.charlotte.pit.data.sub.EventData;
import cn.charlotte.pit.data.sub.GenesisData;
import cn.charlotte.pit.data.sub.KillRecap;
import cn.charlotte.pit.data.sub.KingsQuestsData;
import cn.charlotte.pit.data.sub.MedalData;
import cn.charlotte.pit.data.sub.OfferData;
import cn.charlotte.pit.data.sub.PlayerBanData;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.data.sub.PlayerOption;
import cn.charlotte.pit.data.sub.QuestCenter;
import cn.charlotte.pit.data.sub.QuestData;
import cn.charlotte.pit.data.sub.QuestLimit;
import cn.charlotte.pit.data.sub.TradeLimit;
import cn.charlotte.pit.event.PitCostCoinsEvent;
import cn.charlotte.pit.event.PitGainCoinsEvent;
import cn.charlotte.pit.event.PitGainRenownEvent;
import cn.charlotte.pit.event.PitPlayerHistoricalEventsEvent;
import cn.charlotte.pit.event.PitStreakKillChangeEvent;
import cn.charlotte.pit.events.genesis.team.GenesisTeam;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.medal.impl.challenge.HundredLevelMedal;
import cn.charlotte.pit.nametag.hiding.NameHidingManager;
import cn.charlotte.pit.perk.PerksBundlesEnum;
import cn.charlotte.pit.perk.PerksPassivesEnum;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.perk.PitPerkType;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.quest.AbstractQuest;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.chat.RomanUtil;
import cn.charlotte.pit.util.chat.TitleUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.level.LevelUtil;
import cn.charlotte.pit.util.random.RandomUtil;
import cn.charlotte.pit.util.rank.RankUtil;
import com.andan.avipsystem.api.VipSystemAPI;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: EmptyIrony
 * @Date: 2020/12/29 23:04
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PlayerProfile {
    @JsonIgnore
    public final static PlayerProfile NONE_PROFILE = new PlayerProfile(UUID.randomUUID(), "NotLoadPlayer");
    /**
     * 30 天对应的毫秒数
     */
    private static final long THIRTY_DAYS_MILLIS = TimeUnit.DAYS.toMillis(30);
    @JsonIgnore
    private static Map<UUID, PlayerProfile> cacheProfile = new ConcurrentHashMap<>(64);
    @JsonIgnore
    private final Deque<KillEdge> recentVictims = new ArrayDeque<>(15);
    @JsonIgnore
    private final Deque<KillEdge> recentKillers = new ArrayDeque<>(15);
    @JsonIgnore
    private final Deque<KillEdge> slowRecentVictims = new ArrayDeque<>(20);
    @JsonIgnore
    private final Deque<KillEdge> damgelist = new ArrayDeque<>(30);
    public int prestige;
    public List<String> claimedMail;
    public KingsQuestsData kingsQuestsData = new KingsQuestsData();
    public String mysteryMainItem;
    public String mysteryOffItem;
    double update = 0.0;
    boolean autoUpData = false;
    int favorability;
    int fishQuest;
    boolean sewerNPC;
    @JsonIgnore
    int hit = 0;
    @JsonIgnore
    boolean god = false;
    @Getter
    @Setter
    boolean BetterPlayer = false;
    private String playerName;
    private String uuid;
    private String lowerName;
    private long registerTime;
    private long lastLoginTime;
    private long lastLogoutTime;
    private long totalPlayedTime;
    private int kills;
    private int assists;
    private int deaths;
    private int highestStreaks;
    private long totalDamage;
    private long meleeTotalDamage;
    private long arrowTotalDamage;
    private long hurtDamage;
    private long meleeHurtDamage;
    private long bowHurtDamage;
    private int meleeAttack;
    private int shootAttack;
    private int meleeHit;
    private int bowHit;
    private int rodUsed;
    private int rodHit;
    private int goldPicked;
    private int fishingNumber;
    private int goldenHeadEaten;
    private double experience;
    private double coins;
    private int renown;
    private int bounty;
    private int actionBounty;
    private double respawnTime;
    private String joinMessage;
    private PlayerInv inventory;
    private PlayerEnderChest[] enderChests;
    private Map<String, Integer> unlockedPerkMap = new HashMap<>();
    private Map<String, Integer> unlockedBoughtPerkMap = new HashMap<>();
    private Set<String> usedCdk;
    private String[] chosePerk;
    private double totalExp;
    private List<String> autoBuyButtons;
    private TradeLimit tradeLimit;
    private MedalData medalData;
    private QuestLimit questLimit;
    private OfferData offerData;
    //累计获得硬币
    private double grindedCoins;
    private PlayerOption playerOption;
    private PlayerBanData playerBanData;
    //补偿信息
    private int remedyLevel;
    private double remedyExp;
    private String remedyDate;
    private int totalFishTimes;
    private int totalFishTreasureTimes;
    private int totalFishTrashTimes;
    //当前的任务
    private QuestData currentQuest;
    //上一次的任务
    private QuestData lastQuest;
    //是否开启了夜晚任务
    private boolean nightQuestEnable;
    private QuestCenter questCenter;
    private GenesisData genesisData;
    private List<String> currentQuestList;
    private double maxHealth;
    private int foodLevel;
    private float moveSpeed;
    private String enchantingItem;
    private String enchantingScience;
    private boolean login;
    private double goldStackAddon = 0.0;
    private double goldStackMax = 0.5;
    private double xpStackAddon = 0.0;
    private double xpStackMax = 1.0;
    private int todayCompletedUber;
    private long todayCompletedUberLastRefreshed;
    private int profileFormatVersion = 0;
    private Map<String, Integer> enchantment;
    private boolean extraGoldBonus;
    private boolean extraExpBonus;
    private String lastboxinTime;
    private EventData eventData;
    private String ForgeItem;
    private Map<Long, goldHistory> historicalGoldRecord;
    private List<goldHistory> todayGoldRecord;
    private ActivityPlayerData activityPlayerData;
    private double weekCoin;
    private String AttachedEnchant;
    private ContractData contractData;
    private CareerData careerData;
    private String ExorcismAltarItem;
    /**
     * 未解锁时固定为 -1
     */
    private long extraGoldBonusExpireAt = -1;
    private long extraExpBonusExpireAt = -1;
    private HashMap<String, Double> extraMaxHealth = new HashMap<>();
    @JsonIgnore
    private boolean maybeBot;
    @JsonIgnore
    private List<String> chat;
    @JsonIgnore
    private boolean debug = false;
    @JsonIgnore
    private PlayerMailData mailData;
    @JsonIgnore
    private boolean loaded;
    @JsonIgnore
    private BuffData buffData;
    @JsonIgnore
    private boolean inArena;
    @JsonIgnore
    private double streakKills;
    @JsonIgnore
    private Cooldown combatTimer;
    @JsonIgnore
    private boolean editingMode;
    @JsonIgnore
    private double damageReduced;
    @JsonIgnore
    private Map<UUID, DamageData> damageMap;
    @JsonIgnore
    private int strengthNum;
    @JsonIgnore
    private Cooldown strengthTimer;
    @JsonIgnore
    private UUID lastKilledPlayer;
    @JsonIgnore
    private Cooldown bountyCooldown;
    @JsonIgnore
    private int bountyStreak;
    @JsonIgnore
    private KillRecap killRecap;
    @JsonIgnore
    private boolean screenShare;
    @JsonIgnore
    private String screenShareQQ;
    @JsonIgnore
    private boolean nicked;
    @JsonIgnore
    private int nickPrestige;
    @JsonIgnore
    private int nickLevel;
    @JsonIgnore
    private boolean tempInvUsing;
    @JsonIgnore
    private boolean noDamageAnimations;
    @JsonIgnore
    private double liteStreakKill;
    @JsonIgnore
    private long lastActionTimestamp;
    @JsonIgnore
    private List<PlayerInvBackup> cachedInvBackups;
    @JsonIgnore
    private Cooldown streakCooldown;
    @JsonIgnore
    private int streakCount;
    @JsonIgnore
    private boolean bot;
    @JsonIgnore
    private long lastDamageAt = -1L;
    @JsonIgnore
    private UUID playerUUID = null;
    @JsonIgnore
    private double cachedPrestige = -1;
    @JsonIgnore
    private double cachedExperience = -1;
    @JsonIgnore
    private int cachedLevel = -1;
    @JsonIgnore
    private Float force;
    @JsonIgnore
    private String locationsSetup;

    public PlayerProfile(UUID uuid, String playerName) {
        //调用默认构造函数，初始化赋值
        this();

        this.uuid = uuid.toString();
        this.playerName = playerName;
        this.lowerName = playerName.toLowerCase();
        this.mailData = new PlayerMailData(uuid, playerName);
        this.playerUUID = uuid;
    }

    public PlayerProfile() {
        this.inventory = new PlayerInv();
        this.enderChests = new PlayerEnderChest[5];
        this.killRecap = new KillRecap();
        this.buffData = new BuffData();
        this.combatTimer = new Cooldown(0);
        this.damageMap = new HashMap<>();
        this.strengthTimer = new Cooldown(0);
        this.usedCdk = new HashSet<>();
        this.respawnTime = 0.1;
        this.enchantment = new HashMap<>();

        if (ThePit.isDEBUG_SERVER()) {
            this.coins = 10000000;
            this.renown = 100000;
            this.prestige = 20;
            this.experience = LevelUtil.getLevelTotalExperience(20, 120);
        } else {
            this.coins = 5000;
        }

        this.screenShare = false;
        this.screenShareQQ = "none";

        this.chosePerk = new String[8];

        this.autoBuyButtons = new ArrayList<>();

        this.medalData = new MedalData();

        this.tradeLimit = new TradeLimit();
        this.questLimit = new QuestLimit();
        this.offerData = new OfferData();

        this.playerOption = new PlayerOption();
        this.playerBanData = new PlayerBanData();
        this.bountyCooldown = new Cooldown(0);
        this.currentQuestList = new ArrayList<>();
        this.genesisData = new GenesisData();
        this.claimedMail = new ArrayList<>();

        this.nightQuestEnable = false;

        this.totalFishTimes = 0;
        this.totalFishTreasureTimes = 0;
        this.totalFishTrashTimes = 0;

        this.foodLevel = 20;
        this.maxHealth = 20.0d;
        this.moveSpeed = 0.2F;

        //Level 0 : NoRemedy
        //Level 1 : Remedy + 1 Perk
        //Level 2 : Remedy + 2 Perks
        this.remedyLevel = 0;
        this.remedyExp = 0;
        this.remedyDate = "none";

        this.mailData = new PlayerMailData();
        this.eventData = new EventData();
        this.loaded = false;
        this.activityPlayerData = new ActivityPlayerData();
    }

    public static Map<UUID, PlayerProfile> getCacheProfile() {
        return PlayerProfile.cacheProfile;
    }

    @JsonIgnore
    public static PlayerProfile getPlayerProfileByUuid(UUID uuid) {
        PlayerProfile profile = cacheProfile.get(uuid);
        if (profile == null) {
            return NONE_PROFILE;
        }
        return profile;
    }

    /**
     * 该方法用于查找玩家，如果玩家可能离线时请使用本方法
     * 注意！请异步调用本方法，如果在主线程上调用会抛异常
     *
     * @param uuid 寻找的玩家UUID
     * @return 目标玩家玩家档案，如果该玩家未注册，则返回null
     */
    @JsonIgnore
    public static PlayerProfile getOrLoadPlayerProfileByUuid(UUID uuid) {
        PlayerProfile profile = cacheProfile.get(uuid);
        if (profile != null) {
            return profile;
        }
        return loadPlayerProfileByUuid(uuid);
    }

    public static PlayerProfile loadPlayerProfileByUuid(UUID uuid) {
        if (Bukkit.getServer().isPrimaryThread()) {
            throw new RuntimeException("Shouldn't load profile on primary thread!");
        }

        PlayerProfile playerProfile = ThePit.getInstance()
                .getMongoDB()
                .getProfileCollection()
                .find(Filters.eq("uuid", uuid.toString()))
                .first();

        if (playerProfile != null) {
            playerProfile.setPlayerUUID(uuid);
            //load mail
            loadMail(playerProfile, uuid);

            // Removed loading inventory backups into memory to reduce memory usage
            // Inventory backups are now loaded on-demand from database

            if (playerProfile.extraGoldBonus) {
                playerProfile.unlockExtraGoldBonus();
                playerProfile.extraGoldBonus = false;
            }
            if (playerProfile.extraExpBonus) {
                playerProfile.unlockExtraExpBonus();
                playerProfile.extraExpBonus = false;
            }
        }


        return playerProfile;
    }

    /**
     * 该方法用于查找玩家，如果玩家可能离线时请使用本方法
     * 注意！请异步调用本方法，如果在主线程上调用会抛异常
     *
     * @param name 目标玩家名字
     * @return 目标玩家玩家档案，如果该玩家未注册，则返回null
     */
    @JsonIgnore
    public static PlayerProfile getOrLoadPlayerProfileByName(String name) {
        for (Map.Entry<UUID, PlayerProfile> entry : cacheProfile.entrySet()) {
            if (entry.getValue().getPlayerName().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        if (Bukkit.getServer().isPrimaryThread()) {
            throw new RuntimeException("Shouldn't load profile on primary thread!");
        }

        PlayerProfile playerProfile = ThePit.getInstance()
                .getMongoDB()
                .getProfileCollection()
                .find(Filters.eq("lowerName", name.toLowerCase()))
                .first();
        if (playerProfile == null) {
            throw new RuntimeException("Not Found " + name + " 's profile!");
        }

        String uuid = playerProfile.getUuid();
        if (uuid == null) {
            throw new RuntimeException("Loaded " + name + " 's profile, but uuid is NULL!");
        }

        playerProfile.setPlayerUUID(UUID.fromString(uuid));

        // Removed loading inventory backups into memory to reduce memory usage
        // Inventory backups are now loaded on-demand from database

        return playerProfile;
    }

    @JsonIgnore
    public static PlayerProfile getPlayerProfileByName(String name) {
        for (Map.Entry<UUID, PlayerProfile> entry : cacheProfile.entrySet()) {
            if (entry.getValue().getPlayerName().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        Player player = Bukkit.getPlayer(name);
        if (player != null && player.isOnline()) {
            player.sendMessage(CC.translate("&c在加载您的天坑乱斗数据时出现了一个问题,您可以尝试再次进入游戏以重试."));
            player.kickPlayer("加载您的天坑乱斗数据时出现了一个问题,您可以尝试再次进入游戏以重试.");
        }
        throw new RuntimeException(name + "'s Player profile not load");
    }

    public static void saveAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                if (profile.isLoaded()) {
                    profile.setInventory(InventoryUtil.playerInventoryFromPlayer(player));
                    profile.save(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void loadMail(PlayerProfile playerProfile, UUID uuid) {
        PlayerMailData mailData = ThePit.getInstance()
                .getMongoDB()
                .getMailCollection()
                .find(Filters.eq("uuid", uuid.toString()))
                .first();

        if (mailData == null) {
            mailData = new PlayerMailData();
            mailData.setName(playerProfile.playerName);
            mailData.setNameLower(playerProfile.lowerName);
            mailData.setUuid(playerProfile.uuid);
        }

        mailData.cleanUp();

        playerProfile.setMailData(mailData);
    }

    public static void callHistoricalEvents(Player pl, PlayerProfile profile, String eventName, int count) {
        profile.getEventData().callHistoricalEvents(eventName, count);
        new PitPlayerHistoricalEventsEvent(pl, eventName, count).callEvent();
    }

    public void prestigeUp(Player player) {
        this.setCoins(0);
        this.setExperience(0);
        this.setGrindedCoins(0);
        this.setPrestige(this.getPrestige() + 1);
        this.setAutoBuyButtons(new ArrayList<>());

        if (PlayerUtil.isPlayerUnlockedPerk(player, PerksBundlesEnum.GOLD_STACK)) {
            this.setGoldStackAddon(0.0);
            this.setGoldStackMax(this.getGoldStackMax() + 0.1);
        }

        if (PlayerUtil.isPlayerUnlockedPerk(player, PerksBundlesEnum.XP_STACK)) {
            this.setXpStackAddon(0.0);
            this.setXpStackMax(this.getXpStackMax() + 0.2);
        }

        int level = this.getLevel(PerksPrestigeEnum.FAST_PASS);
        if (level > 0) {
            this.setExperience(LevelUtil.getLevelTotalExperience(this.getPrestige(), 50));
        }

        for (String perkName : this.getChosePerk()) {
            if (perkName == null) continue;
            ThePit.getInstance().getPerkFactory().getPerkMap().get(perkName).onPerkInactive(player);
        }

        this.setChosePerk(new String[8]);
        this.getUnlockedBoughtPerkMap().clear();

        for (PerksPassivesEnum value : PerksPassivesEnum.values()) {
            if (value.equals(PerksPassivesEnum.GOLD_CONTRACT) || value.equals(PerksPassivesEnum.XP_CONTRACT)) continue;
            this.getUnlockedPerkMap().remove(value.name());
        }

        this.getUnlockedPerkMap().put(PerksBundlesEnum.OVERDRIVE.name(), 1);
        this.getChosePerk()[4] = PerksBundlesEnum.OVERDRIVE.name();

        int award = 10;
        if (this.getPrestige() > 4) award += 10;
        if (this.getPrestige() > 10) award += 10;
        if (this.getPrestige() > 14) award += 10;
        this.changeRenown(this.getRenown() + award);

        CC.boardCast(MessageType.PRESTIGE, "&e&l精通! &7" + RankUtil.getPlayerColoredName(player.getName()) + " &7解锁了精通 &e" + RomanUtil.convert(this.getPrestige()) + " &7,gg!");
        TitleUtil.sendTitle(player, "&e&l精通!", "&7你解锁了精通 &e" + RomanUtil.convert(this.getPrestige()) + " &7!", 20, 100, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 1);
    }

    public void init() {
        cacheProfile.put(this.getPlayerUUID(), this);
    }

    public PlayerProfile save(Player player) {
        this.totalExp = experience;
        for (int i = 0; i < prestige; i++) {
            this.totalExp = totalExp + LevelUtil.getLevelTotalExperience(i, 120);
        }

        if (!this.loaded) {
            return this;
        }

        saveData(player);
        return this;
    }

    public void saveData(Player player) {
        final long now = System.currentTimeMillis();
        if (player != null) {
            this.setInventory(InventoryUtil.playerInventoryFromPlayer(player));
        }
        final PlayerInvBackup backup = new PlayerInvBackup();
        backup.setUuid(this.uuid);
        backup.setTimeStamp(now);
        backup.setBackupUuid(UUID.randomUUID().toString());
        backup.setInv(this.inventory);
        backup.setChest(this.enderChests);
        backup.setTimeStamp(System.currentTimeMillis());

        backup.save();

        if (this.medalData != null) {
            this.medalData.compact();
        }

        ThePit.getInstance()
                .getMongoDB()
                .getProfileCollection()
                .replaceOne(Filters.eq("uuid", this.uuid), this, new ReplaceOptions().upsert(true));
    }

    public PlayerProfile load() {

        PlayerProfile profile = PlayerProfile.loadPlayerProfileByUuid(this.getPlayerUUID());
        if (profile == null) {
            this.registerTime = System.currentTimeMillis();
            //refresh quests - start
            this.refreshQuest();
            //refresh quests - end


            this.loaded = true;
            return this;
        }

        // 兼容历史数据
        if (profile.medalData == null) {
            profile.medalData = new MedalData();
        }

        //refresh quests - start
        profile.refreshQuest();
        profile.refreshGenesisData();
        //refresh quests - end

        profile.loaded = true;

        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            try {
                final Player player = Bukkit.getPlayer(profile.getPlayerUUID());
                if (player != null) {
                    if (!player.getName().equals(player.getDisplayName())) {
                        profile.nicked = true;
                        if (profile.prestige <= 0) {
                            profile.nickPrestige = 0;
                        } else {
                            profile.nickPrestige = RandomUtil.random.nextInt(profile.getPrestige()) + 1;
                        }

                        profile.nickLevel = RandomUtil.random.nextInt(profile.getLevel() + 1);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return profile;
    }

    private void refreshQuest() {
        if (this.getCurrentQuestList().isEmpty()) {
            List<AbstractQuest> quests = ThePit.getInstance()
                    .getQuestFactory()
                    .getQuests();

            List<AbstractQuest> list = new ArrayList<>(quests);
            Collections.shuffle(list);

            for (int i = 0; i < 3; i++) {

                AbstractQuest quest = list.get(i);
                int level = RandomUtil.random.nextInt(quest.getMaxLevel()) + 1;
                this.getCurrentQuestList().add(quest.getQuestInternalName() + ":" + level);

            }
        }
    }

    public boolean isBanned() {
        return this.playerBanData.getEnd() > System.currentTimeMillis();
    }

    public void checkUpdate(double newExperience) {
        final Player player = Bukkit.getPlayer(this.getPlayerUUID());
        if (player != null) {
            applyExperienceToPlayer(player);
            final int newLevel = LevelUtil.getLevelByExp(prestige, newExperience);
            final int oldLevel = this.getLevel();

            if (newLevel > oldLevel) {

                if (newLevel >= 100) {
                    new HundredLevelMedal().setProgress(this, 1);
                }

                final String newLevelTag = LevelUtil.getLevelTag(this.prestige, newExperience);
                final String oldLevelTag = LevelUtil.getLevelTag(this.prestige, this.experience);

                CC.send(MessageType.MISC, player, "&b&l天坑升级! " + oldLevelTag + " &7➠ " + newLevelTag);
                TitleUtil.sendTitle(player, CC.translate("&6&l升 级!"), CC.translate(oldLevelTag + " &7➠ " + newLevelTag), 10, 20, 10);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            }
        }
    }

    public int getEnchantmentCount(String nbtName) {
        return enchantment.getOrDefault(nbtName, 0);
    }

    public void incrementEnchantmentCount(String nbtName) {
        int currentCount = getEnchantmentCount(nbtName);
        enchantment.put(nbtName, currentCount + 1);
    }

    @JsonIgnore
    public void applyExperienceToPlayer(Player player) {
        player.setLevel(getLevel());
        if (getLevel() >= 120) {
            this.experience = LevelUtil.getLevelTotalExperience(this.prestige, 120);
            player.setExp(1);
            return;
        }
        player.setExp(LevelUtil.getLevelProgress(this.prestige, this.experience));
    }

    @JsonIgnore
    public int getLevel() {
        // 如果缓存里已经有值，并且这次的 prestige/exposure 都与缓存值相同，则直接返回缓存的 level
        if (cachedPrestige != -1
                && cachedExperience != -1
                && this.prestige == cachedPrestige
                && this.experience == cachedExperience) {
            return cachedLevel;
        }

        // 否则重新计算
        int level = LevelUtil.getLevelByExp(this.prestige, this.experience);

        // 更新缓存
        this.cachedPrestige = this.prestige;
        this.cachedExperience = this.experience;
        this.cachedLevel = level;

        return level;
    }

    @JsonIgnore
    public void grindCoins(double coins) {
        final Player player = Bukkit.getPlayer(this.getPlayerUUID());
        if (player != null) {
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                new PitGainCoinsEvent(player, coins).callEvent();
            });
        }
        this.setGrindedCoins(this.getGrindedCoins() + coins);
    }

    @JsonIgnore
    public void refreshGenesisData() {
        int boostTier = this.getGenesisData().getBoostTier();
        GenesisTeam team = this.getGenesisData().getTeam();
        this.setGenesisData(new GenesisData());
        this.getGenesisData().setBoostTier(boostTier);
        this.getGenesisData().setTeam(team);
    }

    @JsonIgnore
    public String getFormattedName() {
        return getFormattedLevelTag() + " " + RankUtil.getPlayerColoredName(this.getPlayerUUID());
    }

    @JsonIgnore
    public String getFormattedNameWithRoman() {
        return getFormattedLevelTagWithRoman() + " " + RankUtil.getPlayerColoredName(this.getPlayerUUID());
    }

    @JsonIgnore
    public String getFormattedLevelTag() {
        if (nicked) {
            return LevelUtil.getLevelTag(this.nickPrestige, this.nickLevel);
        }
        return LevelUtil.getLevelTag(this.getPrestige(), this.getLevel());
    }

    @JsonIgnore
    public String getFormattedLevelTagWithRoman() {
        if (nicked) {
            return LevelUtil.getLevelTagWithRoman(this.nickPrestige, this.nickLevel);
        }
        return LevelUtil.getLevelTagWithRoman(this.getPrestige(), this.getLevel());
    }

    public void addLiteStreakKill(double value) {
        this.liteStreakKill += value;
        if (liteStreakKill > 1) {
            final int streakKill = (int) liteStreakKill;
            liteStreakKill = liteStreakKill - streakKill;

            this.streakKills += liteStreakKill;
        }
    }

    public int addAndGetStreakNumberShort() {
        if (streakCooldown == null || streakCooldown.hasExpired()) {
            streakCount = 0;
            streakCount++;
            streakCooldown = new Cooldown(10, TimeUnit.SECONDS);
            return streakCount;
        }

        streakCooldown = new Cooldown(10, TimeUnit.SECONDS);
        streakCount++;

        return streakCount;
    }

    public void setExperience(double experience) {
        if (loaded) {
            checkUpdate(experience);
        }
        this.experience = experience;
    }

    @JsonIgnore
    public void changeRenown(int renown) {
        final Player player = Bukkit.getPlayer(this.getPlayerUUID());
        if (player != null && renown > this.renown) {
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                new PitGainRenownEvent(player, renown - this.renown).callEvent();
            });
        }
        setRenown(renown);
    }

    @JsonIgnore
    public void addRenown(int renown) {
        final Player player = Bukkit.getPlayer(this.getPlayerUUID());
        if (player != null && renown > 0) {
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> new PitGainRenownEvent(player, renown).callEvent());
        }
        this.renown += renown;
    }

    public void setInventory(PlayerInv inv) {
        if (this.tempInvUsing) {
            return;
        }

        this.inventory = inv;
    }

    @JsonIgnore
    public PlayerEnderChest getDefaultEnderChest() {
        return getEnderChests(0);
    }

    @JsonIgnore
    public PlayerEnderChest getEnderChests(int index) {
        if (this.enderChests == null) {
            this.enderChests = new PlayerEnderChest[8];
        }
        PlayerEnderChest enderChest = this.enderChests[index];
        if (index == 0) {
            if (enderChest == null) {
                enderChest = new PlayerEnderChest(3);
                this.enderChests[index] = enderChest;
            }
        }
        return enderChest;
    }

    public void setEnderChests(PlayerEnderChest[] enderChests) {
        this.enderChests = enderChests;
    }

    public void setEnderChests(PlayerEnderChest enderChests, int index) {
        this.enderChests[index] = enderChests;
    }

    @JsonIgnore
    public boolean isSupporter() {
        if (this.playerUUID == null) {
            return false;
        }
        Player player = Bukkit.getPlayer(this.playerUUID);
        if (player == null) {
            return false;
        }
        if (isBetterPlayer()) {
            return true;
        }
        if (VipSystemAPI.getVipLevel(player) >= 5) {
            setBetterPlayer(true);
            return true;
        }

        return false;
    }

    public double getMaxHealth() {
        return 20.0 + getExtraMaxHealthValue();
    }

    /**
     * 是否在作战区域
     *
     * @return true: 在作战区域 false: 在出生点
     */
    public boolean isInArena() {
        return this.inArena;
    }

    public void setInArena(boolean inArena) {
        if (inArena && !this.inArena) {
            final Player player = Bukkit.getPlayer(getPlayerUUID());
            if (player != null) {
                Util.releaseItem(player);
            }
        }

        this.inArena = inArena;
    }

    public void setStreakKills(double kills) {
        setStreakKills(kills, false);
    }

    public void setStreakKills(double kills, boolean force) {
        setStreakKills(kills, force, PitStreakKillChangeEvent.Source.NORMAL);
    }

    @JsonIgnore
    public void setBotStreakKills(double kills) {
        setStreakKills(kills, false, PitStreakKillChangeEvent.Source.BOT);
    }

    private void setStreakKills(double kills, boolean force, PitStreakKillChangeEvent.Source source) {
        if (CombatListener.isNight()) {
            if (source == PitStreakKillChangeEvent.Source.BOT) return;
            this.streakKills = 0;
            return;
        }


        if ((kills > 0 && Math.floor(kills) != Math.floor(this.streakKills)) || force) {
            final PitStreakKillChangeEvent event = new PitStreakKillChangeEvent(this, this.streakKills, kills, source);
            event.callEvent();
            if (event.isCancelled()) {
                return;
            }
        }

        this.streakKills = kills;
        if (loaded) NameHidingManager.revealIfExempt(this);
    }

    public void setBounty(int bounty) {
        this.bounty = bounty;
        if (loaded) NameHidingManager.revealIfExempt(this);
    }

    public double getExtraMaxHealthValue() {
        return extraMaxHealth
                .values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    //这是特权包
    @JsonIgnore
    public boolean hasExtraGoldBonus() {
        return extraGoldBonusExpireAt != -1
                && System.currentTimeMillis() < extraGoldBonusExpireAt;
    }

    @JsonIgnore
    public boolean hasExtraExpBonus() {
        return extraExpBonusExpireAt != -1
                && System.currentTimeMillis() < extraExpBonusExpireAt;
    }


    public void unlockExtraGoldBonus() {
        long now = System.currentTimeMillis();
        long base = (extraGoldBonusExpireAt == -1 || extraGoldBonusExpireAt < now)
                ? now
                : extraGoldBonusExpireAt;
        extraGoldBonusExpireAt = base + THIRTY_DAYS_MILLIS;
    }

    public void unlockExtraExpBonus() {
        long now = System.currentTimeMillis();
        long base = (extraExpBonusExpireAt == -1 || extraExpBonusExpireAt < now)
                ? now
                : extraExpBonusExpireAt;
        extraExpBonusExpireAt = base + THIRTY_DAYS_MILLIS;
    }


    @JsonIgnore
    public int getLevel(String perkName) {
        return getUnlockedPerkMap().getOrDefault(perkName, 0);
    }

    @JsonIgnore
    public <E extends Enum<E> & PitPerkType> int getLevel(E perk) {
        return unlockedPerkMap.getOrDefault(perk.name(), 0);
    }

    @JsonIgnore
    public boolean perkEqualsChosePerk(String perkName, int choseSlot) {
        return perkName.equals(getChosePerk()[choseSlot]);
    }

    @JsonIgnore
    public boolean hasEquipPerk(String perkName) {
        if (chosePerk == null) {
            return false;
        }
        if (perkName == null) {
            return false;
        }
        boolean match = Arrays.asList(chosePerk).contains(perkName);
//        if (!match) {
//            match = getUnlockedPerkMap().containsKey(perkName) && getUnlockedPerkMap().get(perkName) > 0;
//        }

        return match;
    }


    @JsonIgnore
    public List<TypedPerk<?>> getActivePerks() {
        List<TypedPerk<?>> perks = new ArrayList<>();
        for (String perkName : chosePerk) {
            if (perkName == null) {
                continue;
            }
            TypedPerk<?> perk = ThePit.getInstance().getPerkFactory().getPerkMap().get(perkName);
            if (perk == null) {
                continue;
            }
            perks.add(perk);
        }
        for (PerksPassivesEnum passivesEnum : PerksPassivesEnum.values()) {
            TypedPerk<PerksPassivesEnum> perk = passivesEnum.getPerk();
            if (perk == null) {
                continue;
            }
            if (perk.hasUnlock(this)) {
                perks.add(perk);
            }
        }
        for (PerksPrestigeEnum prestigeEnum : PerksPrestigeEnum.values()) {
            TypedPerk<PerksPrestigeEnum> perk = prestigeEnum.getPerk();
            if (perk == null) {
                continue;
            }
            if (perk.hasUnlock(this)) {
                perks.add(perk);
            }
        }

        return perks;
    }

    @JsonIgnore
    public void setLevel(String perkName, int i) {
        getUnlockedPerkMap().put(perkName, i);
    }

    public void pushVictim(UUID victim) {
        recentVictims.clear();
//        if (recentVictims.size() >= 15) recentVictims.removeFirst();
//        if (Objects.requireNonNull(Bukkit.getPlayer(victim)).isOp()) {
//            return;
//        }
//        recentVictims.addLast(new KillEdge(victim, System.currentTimeMillis()));
    }

    public void pushKiller(UUID killer) {
        recentKillers.clear();
//        if (recentKillers.size() >= 15) recentKillers.removeFirst();
//        if (Objects.requireNonNull(Bukkit.getPlayer(killer)).isOp()) {
//            return;
//        }
//        recentKillers.addLast(new KillEdge(killer, System.currentTimeMillis()));
    }

    public void pushSlowVictim(UUID victim, PlayerProfile killedProfile, PlayerProfile beKilledProfile) {
        slowRecentVictims.clear();
//        if (killedProfile.getPrestige() > 0 && killedProfile.getLevel() >= 50 && beKilledProfile.getPrestige() == 0 && beKilledProfile.getLevel() <= 15) {
//            if (slowRecentVictims.size() >= 20) {
//                slowRecentVictims.removeFirst();
//            }
//            slowRecentVictims.addLast(new KillEdge(victim, System.currentTimeMillis()));
//        } else {
//            slowRecentVictims.removeIf(e -> e.other().equals(victim));
//        }
    }

    public int countSlowVictim(UUID victim) {
        long now = System.currentTimeMillis();
        int c = 0;
        for (KillEdge e : slowRecentVictims) {
            if (e.other().equals(victim) && now - e.timeMillis() <= 3 * 60 * 1000L) c++;
        }
        return c;
    }

    // 统计某个 UUID 在时间窗内出现的次数
    public int countVictim(UUID victim, long windowMs) {
        long now = System.currentTimeMillis();
        int c = 0;
        for (KillEdge e : recentVictims) {
            if (e.other().equals(victim) && now - e.timeMillis() <= windowMs) c++;
        }
        return c;
    }

    public int countKiller(UUID killer, long windowMs) {
        long now = System.currentTimeMillis();
        int c = 0;
        for (KillEdge e : recentKillers) {
            if (e.other().equals(killer) && now - e.timeMillis() <= windowMs) c++;
        }
        return c;
    }

    /**
     * 向Chat列表中添加字符串，如果超过20条则自动删除第一条
     *
     * @param message 要添加的聊天消息
     */
    public void addChatMessage(String message) {
        if (this.chat == null) {
            this.chat = new ArrayList<>();
        }
        this.chat.add(message);
        if (this.chat.size() > 20) {
            this.chat.removeFirst();
        }
    }

    /**
     * 查询Chat列表中与给定文本完全相同的消息数量
     *
     * @param text 要查询的文本
     * @return 匹配的文本数量
     */
    public int countChatMessage(String text) {
        if (this.chat == null || this.chat.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String message : this.chat) {
            if (message != null && message.contains(text)) {
                count++;
            }
        }
        return count;
    }

    public void pushdamage(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;
        if (damgelist.size() >= 15) damgelist.removeFirst();
        if (player.isOp()) {
            return;
        }
        damgelist.addLast(new KillEdge(playerUUID, System.currentTimeMillis()));
    }

    public int countdamage(UUID playerUUID, long windowMs) {
        long now = System.currentTimeMillis();
        int c = 0;
        for (KillEdge e : recentKillers) {
            if (e.other().equals(playerUUID) && now - e.timeMillis() <= windowMs) c++;
        }
        return c;
    }

    /**
     * Load inventory backups on-demand from database
     * This method is used to avoid keeping backups in memory at all times
     *
     * @return List of inventory backups for this player
     */
    @JsonIgnore
    public List<PlayerInvBackup> getInvBackups() {
        if (cachedInvBackups == null) {
            loadAndCacheInventoryBackups();
        }
        return cachedInvBackups;
    }

    /**
     * Load and cache inventory backups from database
     */
    private void loadAndCacheInventoryBackups() {
        cachedInvBackups = new ArrayList<>();
        try {
            final FindIterable<PlayerInvBackup> iterable = ThePit.getInstance()
                    .getMongoDB()
                    .getInvCollection()
                    .find(Filters.eq("uuid", this.uuid));

            long lastTime = 0;
            for (PlayerInvBackup backup : iterable) {
                if (Math.abs(backup.getTimeStamp() - lastTime) < 10 * 60 * 1000) {
                    lastTime = backup.getTimeStamp();
                    // Delete duplicate backups that are too close in time
                    ThePit.getInstance()
                            .getMongoDB()
                            .getInvCollection()
                            .deleteOne(Filters.eq("backupUuid", backup.getBackupUuid()));
                    continue;
                }
                lastTime = backup.getTimeStamp();
                cachedInvBackups.add(backup);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Clear the cached inventory backups to force a reload next time
     */
    @JsonIgnore
    public void clearInvBackupsCache() {
        cachedInvBackups = null;
    }

    /**
     * Get a specific number of recent inventory backups
     *
     * @param limit Maximum number of backups to return
     * @return List of recent inventory backups
     */
    @JsonIgnore
    public List<PlayerInvBackup> getRecentInventoryBackups(int limit) {
        List<PlayerInvBackup> allBackups = getInvBackups();
        // Sort by timestamp descending to get most recent first
        allBackups.sort((a, b) -> Long.compare(b.getTimeStamp(), a.getTimeStamp()));

        if (allBackups.size() > limit) {
            return new ArrayList<>(allBackups.subList(0, limit));
        }
        return allBackups;
    }

    public void setCoins(double count) {
        if (this.coins < count) this.weekCoin += count - this.coins;
        this.coins = count;
    }

    public void addCoins(double amout) {
        if (amout == 0) return;
        this.coins += amout;
    }

    public void takeCoins(double amount) {
        // withdraw coins
        this.coins -= amount;
        final Player pl = Bukkit.getPlayer(this.getPlayerUUID());
        if (pl != null) {
            if (Bukkit.isPrimaryThread()) {
                new PitCostCoinsEvent(pl, amount).callEvent();
            } else {
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () ->
                        new PitCostCoinsEvent(pl, amount).callEvent());
            }
        }
    }

    public void addExp(double amout) {
        if (amout == 0) return;
        this.experience += amout;
    }

    public void addActionBounty(int amout) {
        if (amout == 0) return;
        this.actionBounty += amout;
    }
}
