package cn.charlotte.pit.config;

import cn.charlotte.pit.util.configuration.Configuration;
import cn.charlotte.pit.util.configuration.annotations.ConfigData;
import cn.charlotte.pit.util.configuration.annotations.ConfigSerializer;
import cn.charlotte.pit.util.configuration.serializer.ConfigRegion;
import cn.charlotte.pit.util.configuration.serializer.ConfigRegionSerializer;
import cn.charlotte.pit.util.configuration.serializer.LocationSerializer;
import cn.charlotte.pit.util.configuration.serializer.StringValueSerializer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: EmptyIrony
 * @Date: 2020/12/30 22:02
 */
@Getter
@Setter
public class PitConfig extends Configuration {

    @ConfigData(path = "trade.enabled")
    private boolean tradeEnabled = true;

    @ConfigData(path = "trade.mythic-equipment-cooldown")
    @ConfigSerializer(serializer = StringValueSerializer.class)
    private String mythicEquipmentTradeCooldown = "-1";

    @ConfigData(
            path = "enchants.disable"
    )
    public List<String> disableEnchants = new ArrayList<>();
    @ConfigData(path = "loc.events.egg.loc")
    @ConfigSerializer(serializer = LocationSerializer.class)
    public Location eggLoc;
    @ConfigData(
            path = "loc.sewers.chests"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    public List<Location> sewersChestsLocations = new ArrayList<>();
    @ConfigData(
            path = "loc.squads"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    public List<Location> squadsLocations = new ArrayList<>();
    @ConfigData(
            path = "loc.blockHead"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    public List<Location> blockHeadLocations = new ArrayList<>();
    @ConfigData(
            path = "loc.hologram.workTable"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    public Location workTableLocation;
    @ConfigData(
            path = "loc.npc.contract"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    public Location contractnpcLocation;
    @ConfigData(
            path = "loc.hologram.fireTable"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    public Location fireTableLocation;
    @ConfigData(
            path = "loc.leader"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    public List<Location> leaderLocations = new ArrayList<>();
    @ConfigData(
            path = "loc.npc.helpNPC"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    public Location HelpNPCLocations;
    @ConfigData(
            path = "loc.Event.LuckyWheel"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    public List<Location> luckyWheelLocation = new ArrayList<>();
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @ConfigData(
            path = "service.webhooks.customName"
    )
    private String customNameWebhook;
    @ConfigData(
            path = "service.mongodb.ip"
    )
    private String mongoDBAddress;
    @ConfigData(
            path = "service.mongodb.port"
    )
    private int mongoDBPort;
    @ConfigData(
            path = "service.mongodb.database"
    )
    private String databaseName;
    @ConfigData(
            path = "service.mongodb.user"
    )
    private String mongoUser;
    @ConfigData(
            path = "service.mongodb.password"
    )
    private String mongoPassword;
    @ConfigData(
            path = "service.redis.enable"
    )
    private boolean redisEnable;
    @ConfigData(
            path = "service.redis.ip"
    )
    private String redisAddress;
    @ConfigData(
            path = "service.redis.port"
    )
    private int redisPort;
    @ConfigData(
            path = "service.redis.password"
    )
    private String redisPassword;
    @ConfigData(
            path = "arenaHighestY"
    )
    private int arenaHighestY;
    @ConfigData(
            path = "loc.spawn"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private List<Location> spawnLocations = new ArrayList<>();
    @ConfigData(
            path = "loc.npc.shop"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location shopNpcLocation;
    @ConfigData(
            path = "loc.npc.quest"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location questNpcLocation;
    @ConfigData(
            path = "loc.npc.perk"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location perkNpcLocation;
    @ConfigData(path = "server-name")
    private String serverName = "&e天坑乱斗";
    @ConfigData(path = "server-qun")
    private String serverQun = "§7交流群: §f1022026158";
    @ConfigData(
            path = "loc.npc.prestige"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location prestigeNpcLocation;
    @ConfigData(
            path = "loc.npc.status"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location statusNpcLocation;
    @ConfigData(
            path = "loc.npc.keeper"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location keeperNpcLocation;
    @ConfigData(
            path = "loc.npc.leaderBoard"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location leaderBoardNpcLocation;
    @ConfigData(
            path = "loc.npc.mail"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location mailNpcLocation;
    @ConfigData(
            path = "loc.npc.genesis_demon"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location genesisDemonNpcLocation;
    @ConfigData(
            path = "loc.hologram.spawn"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location hologramLocation;
    @ConfigData(
            path = "loc.hologram.trashBin"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location TrashBinLocation;
    @ConfigData(
            path = "loc.npc.genesis_angel"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location genesisAngelNpcLocation;
    @ConfigData(
            path = "loc.hologram.leaderBoard"
    )

    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location leaderBoardHologram;
    @ConfigData(
            path = "loc.hologram.mythic"
    )

    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location mythicHologram;
    @ConfigData(
            path = "loc.hologram.chest"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location chestHologram;
    @ConfigData(
            path = "loc.hologram.helper"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location helperHologramLocation;
    @ConfigData(
            path = "loc.npc.king"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location KingerLocation;
    @ConfigData(
            path = "loc.npc.cake_bake"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location CakeBakeLocation;
    @ConfigData(
            path = "loc.npc.chicken"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location ChickenLocation;
    @ConfigData(
            path = "loc.npc.silver_fish"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location SilverfishLocation;
    @ConfigData(
            path = "loc.region.pitA"
    )

    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location pitLocA;
    @ConfigData(
            path = "loc.region.pitB"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location pitLocB;
    @ConfigData(
            path = "loc.region.doorCenter"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location pitDoorCenter;
    @ConfigData(
            path = "loc.region.enchant"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location enchantLocation;
    @ConfigData(
            path = "loc.events.RandomRedEnvelopesEvent"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private List<Location> randomRedEnvelopesEventLoc;
    @ConfigData(
            path = "loc.events.hamburger.shop"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location hamburgerShopLoc;
    @ConfigData(
            path = "loc.events.hamburger.villager.a"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private List<Location> hamburgerNpcLocA = new ArrayList<>();
    @ConfigData(
            path = "loc.events.spire.spireLoc"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location spireLoc;
    //每层塔地面中心坐标 (1~9)
    @ConfigData(
            path = "loc.events.spire.spireFloorLocations"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private List<Location> spireFloorLoc = new ArrayList<>();
    @ConfigData(
            path = "loc.events.hamburger.villager.a-offer" //the villager who offer the ham
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location hamburgerOfferNpcLocA;
    @ConfigData(
            path = "loc.events.rage.middle" //the middle point of rage pit
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location ragePitMiddle;
    @ConfigData(
            path = "loc.events.rage.radius" //the radius of rage pit
    )
    private int ragePitRadius;
    @ConfigData(
            path = "loc.events.rage.height" //the height of rage pit
    )
    private int ragePitHeight;
    @ConfigData(
            path = "loc.portal.posA" //Middle portal posA
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location portalPosA;
    @ConfigData(
            path = "loc.portal.posB" //Middle portal posA
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location portalPosB;
    @ConfigData(path = "loc.events.koth.locs")
    @ConfigSerializer(serializer = LocationSerializer.class)
    private List<Location> kothLocs;
    @ConfigData(
            path = "loc.events.cake.regions"
    )
    @ConfigSerializer(serializer = ConfigRegionSerializer.class)
    private List<ConfigRegion> cakeZones;
    @ConfigSerializer(serializer = LocationSerializer.class)
    private List<Location> angelSpawns = new ArrayList<>();
    @ConfigData(
            path = "loc.Genesis.demon"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private List<Location> demonSpawns = new ArrayList<>();
    @ConfigData(
            path = "loc.packages"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private List<Location> packageLocations = new ArrayList<>();
    @ConfigData(
            path = "debug.debugServer"
    )
    private boolean debugServer;
    @ConfigData(
            path = "debug.public"
    )
    private boolean debugServerPublic;
    @ConfigData(
            path = "debug.infinityNpcLoc"
    )
    private Location infinityNpcLocation;
    @ConfigData(
            path = "debug.ienchantNpcLoc"
    )
    private Location enchantNpcLocation;
    @ConfigData(
            path = "booster"
    )
    private double booster = 1.0;
    @ConfigData(
            path = "curfew.enable"
    )
    private boolean curfewEnable;
    @ConfigData(
            path = "curfew.start"
    )
    private int curfewStart;
    @ConfigData(
            path = "curfew.end"
    )
    private int curfewEnd;
    @ConfigData(
            path = "loc.hologram.enchantTip"
    )
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location enchantTipHoloLocation;
    @ConfigData(
            path = "genesis-start-date"
    )
    private long genesisStartDate = 1675339795842L;
    @ConfigData(path = "loc.table.enchantForgeTable")
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location enchantForgeTableLocation;

    @ConfigData(path = "music.loc")
    @ConfigSerializer(serializer = LocationSerializer.class)
    private List<Location> musicLocationList = new ArrayList<>(4);

    @ConfigData(path = "loc.npc.tradeNPC")
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location tradeNPC;

    @ConfigData(path = "loc.event.randomred")
    @ConfigSerializer(serializer = LocationSerializer.class)
    private List<Location> randomredBlockLocationList = new ArrayList<>(3);

    @ConfigData(path = "loc.hologram.mystery")
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location hologramMystery = null;

    @ConfigData(path = "loc.npc.boss")
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location bossLocation = null;

    @ConfigData(path = "loc.npc.activity")
    @ConfigSerializer(serializer = LocationSerializer.class)
    private Location activityNPCLocation = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日HH时mm分");

    public PitConfig(JavaPlugin plugin) {
        super(plugin);
    }

    public boolean isGenesisEnable() {
        try {
            return System.currentTimeMillis() >= getGenesisStartTime() && System.currentTimeMillis() < getGenesisEndTime();
        } catch (Exception ignored) {
            return false;
        }
    }

    public long getGenesisStartTime() {
        return genesisStartDate;
    }

    public long getGenesisOriginalEndTime() {
        return getGenesisStartTime() + 16 * 24 * 60 * 60 * 1000;
    }

    public long getGenesisEndTime() {
        long endTime = getGenesisOriginalEndTime();
        while (endTime < System.currentTimeMillis()) {
            endTime += 56 * 24 * 60 * 60 * 1000L;
        }
        return endTime;
    }


}
