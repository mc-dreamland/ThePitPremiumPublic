package cn.charlotte.pit.events.impl.major;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.event.PitPlayerSpawnEvent;
import cn.charlotte.pit.events.IEpicEvent;
import cn.charlotte.pit.events.MajorEventMapEdits;
import cn.charlotte.pit.events.MajorEventTasks;
import cn.charlotte.pit.events.IScoreBoardInsert;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.medal.impl.challenge.SpireFloorMedal;
import cn.charlotte.pit.movement.PlayerMoveHandler;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.FaweUtils;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.TitleUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.random.RandomUtil;
import cn.charlotte.pit.util.time.TimeUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SpireEvents implements IEpicEvent, Listener, IScoreBoardInsert {
    private final Map<UUID, PlayerSpireData> dataMap;
    private final Map<UUID, Integer> rankMap = new HashMap<>();
    private final List<UUID> cooldownList = new ArrayList<>();
    private final HashSet<UUID> teleporting = new HashSet<>();
    private Cooldown timer;
    private BukkitRunnable runnable;
    private final MajorEventMapEdits mapEdits = new MajorEventMapEdits();
    private boolean runtimeStarted;

    public SpireEvents() {
        this.dataMap = new HashMap<>();
    }

    @Override
    public String getEventInternalName() {
        return "spire";
    }

    @Override
    public List<String> getLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&6&l>> &e尖塔夺魁事件 &6&l<<");
        lore.add("");
        lore.add("&6&l事件机制");
        lore.add("&7- 通过传送门进入多层尖塔竞技场");
        lore.add("&7- 所有天赋和连杀天赋失效");
        lore.add("&7- 击杀玩家升至更高楼层并获得灵魂和 &b断裂的灵魂");
        lore.add("&7- 使用 &b断裂的灵魂 &7获得增益效果");
        lore.add("&3抗性提升 II &f(0:03) &7+ &74&c❤&7 回复 &7+ &73&6❤&7 伤害吸收");
        lore.add("&7- 每层提供更强装备和更小活动范围");
        lore.add("&7- 被击杀降至下层并损失灵魂");
        lore.add("");
        lore.add("&6&l事件奖励机制");
        lore.add("&7> 个人排名：按收集的灵魂数量排名");
        lore.add("&7> 楼层奖励：&e达到6层或以上 &a+1声望");
        lore.add("");
        lore.add("&6&l排名奖励");
        lore.add("&f第1-3名: &65000金币 &f+ &b2声望");
        lore.add("&f第4-20名: &62000金币 &f+ &b1声望");
        lore.add("&f第21-40名: &61000金币");
        lore.add("&f其他玩家: &6500金币");
        return lore;
    }

    @Override
    public String getEventName() {
        return "&5&l尖塔夺魁";
    }

    @Override
    public int requireOnline() {
        return CombatListener.isBedrockRoom() ? 20 : 50;
    }

    @Override
    public java.util.concurrent.CompletableFuture<Void> prepare() {
        var config = ThePit.getInstance().getPitConfig();
        Location tower = java.util.Objects.requireNonNull(config.getSpireLoc(), "未配置尖塔").clone();
        Location door = java.util.Objects.requireNonNull(config.getPitDoorCenter(), "未配置天坑门").clone();
        Location a = java.util.Objects.requireNonNull(config.getPortalPosA(), "未配置尖塔传送门").clone();
        Location b = java.util.Objects.requireNonNull(config.getPortalPosB(), "未配置尖塔传送门").clone();
        if (a.getWorld() == null || a.getWorld() != b.getWorld() || config.getSpawnLocations().isEmpty())
            throw new IllegalStateException("尖塔传送门世界/出生点配置无效");
        var towerWorld = new BukkitWorld(java.util.Objects.requireNonNull(tower.getWorld()));
        var doorWorld = new BukkitWorld(java.util.Objects.requireNonNull(door.getWorld()));
        var portalWorld = new BukkitWorld(a.getWorld());
        var towerAt = FaweUtils.toBlockVector3(tower);
        var doorAt = FaweUtils.toBlockVector3(door);
        var posA = FaweUtils.toBlockVector3(a);
        var posB = FaweUtils.toBlockVector3(b);
        var towerFile = FaweUtils.requireSchematic("spire.schem");
        var doorFile = FaweUtils.requireSchematic("pit_door.schem");
        dataMap.clear(); rankMap.clear(); cooldownList.clear(); teleporting.clear();
        return mapEdits.prepare(batch -> {
            batch.edit(towerWorld, edit -> FaweUtils.pasteSchematicOrThrow(edit, towerAt, towerFile, false));
            batch.edit(doorWorld, edit -> FaweUtils.pasteSchematicOrThrow(edit, doorAt, doorFile, true));
            batch.edit(portalWorld, edit -> edit.replaceBlocks(new CuboidRegion(posA, posB),
                    new BlockMask(edit.getExtent(), BlockTypes.AIR.getDefaultState().toBaseBlock()),
                    BlockTypes.END_PORTAL.getDefaultState().toBaseBlock()));
        });
    }

    @Override public java.util.concurrent.CompletableFuture<Void> restore() { return mapEdits.restore(); }

    private void stopRuntime() {
        HandlerList.unregisterAll(this);
        if (runnable != null) { runnable.cancel(); runnable = null; }
        for (UUID uuid : new ArrayList<>(cooldownList)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) PlayerMoveHandler.getCantMoveList().remove(player);
        }
        cooldownList.clear();
    }

    private void restorePlayers() {
        if (!runtimeStarted) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = Util.getPitProfile(player);
            if (profile.isLoaded() && profile.isTempInvUsing()) {
                profile.getInventory().applyItemToPlayer(player);
                profile.setTempInvUsing(false);
            }
            var spawns = ThePit.getInstance().getPitConfig().getSpawnLocations();
            if (!spawns.isEmpty()) player.teleport(spawns.get(RandomUtil.random.nextInt(spawns.size())));
        }
        runtimeStarted = false;
    }

    @Override public void onAbort() {
        stopRuntime();
        restorePlayers();
        dataMap.clear(); rankMap.clear(); teleporting.clear();
    }

    @Override
    public void onActive() {
        runtimeStarted = true;
        this.timer = new Cooldown(5, TimeUnit.MINUTES);
        this.runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (timer.hasExpired()) {
                    cancel();
                    if (SpireEvents.this.equals(ThePit.getInstance().getEventFactory().getActiveEpicEvent())) {
                        ThePit.getInstance().getEventFactory().inactiveEvent(SpireEvents.this);
                    }
                    return;
                }

                refreshCooldownPlayer();
            }
        };

        MajorEventTasks.track(this, this.runnable.runTaskTimer(ThePit.getInstance(), 20, 20));

        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setMaxHealth(player.getMaxHealth());
            player.setHealth(player.getMaxHealth());

            Location location = ThePit.getInstance().getPitConfig()
                    .getSpawnLocations()
                    .get(RandomUtil.random.nextInt(ThePit.getInstance().getPitConfig().getSpawnLocations().size()));
            PlayerProfile profile = Util.getPitProfile(player);
            profile.getEventData().callHistoricalEvents(getEventName(), 1);
            player.teleport(location);

            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 0.5F);
        }

    }

    @EventHandler
    public void onJoinPortal(PlayerMoveEvent event) {
        Location eventTo = event.getTo();
        Location portalA = ThePit.getInstance().getPitConfig().getPortalPosA();
        Location portalB = ThePit.getInstance().getPitConfig().getPortalPosB();
        if (!eventTo.getWorld().getName().equals(portalA.getWorld().getName())) {
            return;
        }


        int minX = Math.min(portalA.getBlockX(), portalB.getBlockX());
        int maxX = Math.max(portalA.getBlockX(), portalB.getBlockX());
        int minY = Math.min(portalA.getBlockY(), portalB.getBlockY());
        int maxY = Math.max(portalA.getBlockY(), portalB.getBlockY());
        int minZ = Math.min(portalA.getBlockZ(), portalB.getBlockZ());
        int maxZ = Math.max(portalA.getBlockZ(), portalB.getBlockZ());

        int x = eventTo.getBlockX();
        int y = eventTo.getBlockY();
        int z = eventTo.getBlockZ();

        if (x < minX || x > maxX || y > maxY || z < minZ || z > maxZ) {
            return;
        }

        final Player player = event.getPlayer();

        if (!dataMap.containsKey(player.getUniqueId())) {

            final PlayerSpireData data = new PlayerSpireData();
            data.uuid = player.getUniqueId();
            data.name = player.getName();
            data.reachedSixFloor = false;
            data.floor = 0;

            dataMap.put(player.getUniqueId(), data);
        }

        final PlayerSpireData data = dataMap.get(player.getUniqueId());
        if (data.inArea || Util.getPitProfile(player).isTempInvUsing()) return;

        data.floor = 0;
        data.inArea = true;
        data.soul = 0;

        player.closeInventory();
        final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        profile.setInventory(PlayerInv.fromPlayerInventory(player.getInventory()));
        profile.setTempInvUsing(true);

        PlayerUtil.clearPlayer(player, true, true);
        this.giveInvSets(player);
        teleporting.add(event.getPlayer().getUniqueId());
        this.teleportPlayer(player, data);
    }

    @EventHandler
    public void onSpawn(PitPlayerSpawnEvent event) {
        final PlayerSpireData data = dataMap.get(event.getPlayer().getUniqueId());
        if (data != null) {
            dataMap.remove(event.getPlayer().getUniqueId());
        }
        final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getPlayer().getUniqueId());


        if (profile.isLoaded() && profile.isTempInvUsing()) {
            profile.getInventory()
                    .applyItemToPlayer(event.getPlayer());
            profile.setTempInvUsing(false);
        }
    }

    private void giveInvSets(Player player) {
        final PlayerSpireData data = this.dataMap.get(player.getUniqueId());
        if (data == null) {
            return;
        }
        final int floor = data.floor;

        if (floor == 0) {
            player.getInventory().addItem(PitItems.SPIRE_WOODEN_SWORD.toItemStack());
        } else if (floor == 1 || floor == 2) {
            player.getInventory().addItem(PitItems.SPIRE_WOODEN_SWORD.toItemStack());
            player.getInventory().setChestplate(PitItems.SPIRE_LEATHER_CHESTPLATE.toItemStack());
            player.getInventory().setLeggings(PitItems.SPIRE_CHAINMAIL_LEGGINGS.toItemStack());
        } else if (floor == 3 || floor == 4 || floor == 5) {
            player.getInventory().addItem(PitItems.SPIRE_IRON_SWORD.toItemStack());
            player.getInventory().setChestplate(PitItems.SPIRE_IRON_CHESTPLATE.toItemStack());
            player.getInventory().setLeggings(PitItems.SPIRE_CHAINMAIL_LEGGINGS.toItemStack());
            player.getInventory().setBoots(PitItems.SPIRE_IRON_BOOTS.toItemStack());
        } else if (floor >= 6) {
            player.getInventory().addItem(PitItems.SPIRE_DIAMOND_SWORD.toItemStack());
            player.getInventory().setChestplate(PitItems.SPIRE_DIAMOND_CHESTPLATE.toItemStack());
            player.getInventory().setLeggings(PitItems.SPIRE_IRON_LEGGINGS.toItemStack());
            player.getInventory().setBoots(PitItems.SPIRE_DIAMOND_BOOTS.toItemStack());
        }

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final PlayerSpireData data = dataMap.get(player.getUniqueId());
        if (data == null) {
            return;
        }
        final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile.isTempInvUsing()) {
            profile.getInventory().applyItemToPlayer(player);
            profile.setTempInvUsing(false);
        }
        dataMap.remove(player.getUniqueId());
        teleporting.remove(player.getUniqueId());
        PlayerMoveHandler.getCantMoveList().remove(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            if (cooldownList.contains(event.getDamager().getUniqueId()) || cooldownList.contains(event.getEntity().getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onKill(PitKillEvent event) {
        try {
            final Player killer = event.getKiller();
            final PlayerSpireData data = dataMap.get(killer.getUniqueId());
            if (data == null) {
                return;
            }

            //floor up start


            //玩家升级，给予新的物品以及传送至新的一层
            if (data.floor < 9) {
                data.lastFloorChange = System.currentTimeMillis();
                data.floor++;
            } else {
                new SpireFloorMedal().setProgress(PlayerProfile.getPlayerProfileByUuid(event.getKiller().getUniqueId()), 1);
            }

            if (data.floor == 6) {
                data.reachedSixFloor = true;
            }

            event.getResourceManager().getExpModifiers().put("尖塔夺魁", getKillRewardGain(data));
            event.getResourceManager().getGoldModifiers().put("尖塔夺魁", getKillRewardGain(data));

            data.soul = data.soul + this.getKillRewardSouls(data);
            data.kills++;
            TitleUtil.sendTitle(killer, CC.translate("&a击杀!"), this.getFloorColor(data.floor) + "第" + (data.floor) + "层", 5, 20, 10);
            killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5F);

            MajorEventTasks.later(this, () -> {
                int amount = InventoryUtil.getAmountOfItem(killer, "broken_soul");

                this.handleUpdate(killer, data);

                if (amount < 2) {
                    amount++;
                }

                killer.getInventory().addItem(
                        PitItems.BROKEN_SOUL.toItemStacks(amount)
                );
            }, 1L);
            //floor up end

            LivingEntity target1 = event.getTarget();
            final Player target;
            final PlayerSpireData targetData;
            if (target1 instanceof Player) {
                target = (Player) event.getTarget();
                targetData = dataMap.get(target.getUniqueId());
                if (targetData == null) {
                    return;
                }
            } else {
                return;
            }


            targetData.floor--;
            if (targetData.floor < 0) {
                targetData.floor = 0;
                targetData.lastFloorChange = System.currentTimeMillis();
                targetData.soul = targetData.soul - Math.min(targetData.soul, this.getDeadPunishmentSouls(targetData));

                TitleUtil.sendTitle(target, CC.translate("&c被击杀"), this.getFloorColor(data.floor) + "第" + (targetData.floor + 1) + "层", 5, 10, 20);
            }
            MajorEventTasks.later(this, () -> this.handleUpdate(target, targetData), 2L);

            refreshRank();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getArenaOnline(int floor) {
        int online = 0;
        for (Map.Entry<UUID, PlayerSpireData> entry : this.dataMap.entrySet()) {
            final PlayerSpireData value = entry.getValue();
            if (value.inArea) {
                if (value.floor == floor) {
                    online++;
                }
            }
        }
        return online;
    }

    private void refreshRank() {
        final Map<UUID, Integer> map = new HashMap<>();

        final AtomicInteger rank = new AtomicInteger(this.dataMap.size());
        this.dataMap.values()
                .stream()
                .sorted(Comparator.comparingInt(data -> data.soul))
                .forEach(data -> {
                    map.put(data.uuid, rank.getAndDecrement());
                });

        this.rankMap.clear();
        this.rankMap.putAll(map);
    }

    private void teleportPlayer(Player player, PlayerSpireData data) {
        final List<Location> loc = ThePit.getInstance()
                .getPitConfig()
                .getSpireFloorLoc();

        //该段代码导致报错??? 具体报错信息在已存入 bug/Test/SpireEvents_2025_12_21_ERROR
        final Location center = loc.get(Math.min(data.floor, loc.size()));

        int radius = 32 - data.floor * 3;
        //x取值范围为[-radius,radius],但是因为要平方计算，所以只取正数
        int randomX = RandomUtil.random.nextInt(radius);
        //由x^2 + y^2 = r^2得
        //y^2 = r^2 - x^2
        //开根后得到y值
        double randomZ = Math.sqrt(radius * radius - randomX * randomX);
        //因x,y均为正，所以随机取负值
        if (RandomUtil.hasSuccessfullyByChance(0.5)) {
            randomX = -randomX;
        }
        if (RandomUtil.hasSuccessfullyByChance(0.5)) {
            randomZ = -randomZ;
        }
        //最后，换算成坐标
        final Location finalLocation = new Location(center.getWorld(), center.getX() + randomX, center.getY(), center.getZ() + randomZ);
        player.teleport(finalLocation);
    }

    private void handleUpdate(Player player, PlayerSpireData data) {
        if (data == null || !dataMap.containsKey(player.getUniqueId())) {
            return;
        }
        PlayerUtil.clearPlayer(player, true, true);
        this.giveInvSets(player);
        MajorEventTasks.later(this, () -> {
            if (!player.isOnline() || !dataMap.containsKey(player.getUniqueId())) return;
            this.teleportPlayer(player, data);

            if (data.floor >= 4) {
                if (this.getArenaOnline(data.floor) >= 2) {
                    this.cooldownList.add(player.getUniqueId());
//                    PlayerMoveHandler.getCantMoveList().add(player);
                }
            }
        }, 0L);
    }

    private int getKillRewardSouls(PlayerSpireData data) {
        return switch (data.floor) {
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 4;
            case 5 -> 5;
            case 6 -> 6;
            case 7 -> 8;
            case 8 -> 10;
            case 9 -> 25;
            default -> 1;
        };
    }

    private double getKillRewardGain(PlayerSpireData data) {
        return switch (data.floor) {
            case 4 -> 1.25D;
            case 5 -> 1.5D;
            case 6 -> 2.0D;
            case 7 -> 2.5D;
            case 8 -> 3.0D;
            case 9 -> 3.5D;
            default -> 1.0D;
        };
    }

    private int getDeadPunishmentSouls(PlayerSpireData data) {
        return switch (data.floor) {
            case 3 -> 1;
            case 4 -> 2;
            case 5 -> 3;
            case 6 -> 4;
            case 7 -> 5;
            case 8 -> 6;
            case 9 -> 20;
            default -> 0;
        };
    }

    @Override
    public void onInactive() {
        stopRuntime();
        refreshRank();

        CC.boardCast(CC.CHAT_BAR);
        CC.boardCast("&6&l天坑事件结束: " + this.getEventName() + "&6&l!");

        for (Player player : Bukkit.getOnlinePlayers()) {
            final PlayerSpireData data = dataMap.get(player.getUniqueId());

            final int rank = rankMap.containsKey(player.getUniqueId()) ? (rankMap.get(player.getUniqueId()) + 1) : -1;
            final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

            double rewardCoins = 0;
            int rewardRenown = 0;
            if (rank <= 3) {
                rewardCoins += 5000;
                rewardRenown += 2;
            } else if (rank <= 20) {
                rewardCoins += 2000;
                rewardRenown += 1;
            } else if (rank <= 40) {
                rewardCoins += 1000;
            } else {
                rewardCoins += 500;
            }
            if (data != null && data.reachedSixFloor) {
                rewardRenown++;
            }
            if (ThePit.getInstance().getPitConfig().isGenesisEnable() && profile.getGenesisData().getTier() >= 5 && rewardRenown > 0) {
                rewardRenown++;
            }
            int enchantBoostLevel = Utils.getEnchantLevel(player.getInventory().getLeggings(), "paparazzi_enchant");
            if (PlayerUtil.isVenom(player) || PlayerUtil.isEquippingSomber(player)) {
                enchantBoostLevel = 0;
            }
            if (enchantBoostLevel > 0) {
                rewardCoins += 0.5 * enchantBoostLevel * rewardCoins;
                rewardRenown += Math.floor(0.5 * enchantBoostLevel * rewardRenown);
                MythicLeggingsItem mythicLeggings = new MythicLeggingsItem();
                mythicLeggings.loadFromItemStack(player.getInventory().getLeggings());
                if (mythicLeggings.isEnchanted()) {
                    if (mythicLeggings.getMaxLive() > 0 && mythicLeggings.getLive() <= 2) {
                        player.getInventory().setLeggings(new ItemStack(Material.AIR));
                    } else {
                        mythicLeggings.setLive(mythicLeggings.getLive() - 2);
                        player.getInventory().setLeggings(mythicLeggings.toItemStack());
                    }
                }
            }
            if (PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.SELF_CONFIDENCE)) {
                if (rank <= 5) {
                    rewardCoins += 5000;
                } else if (rank <= 10) {
                    rewardCoins += 2500;
                } else if (rank <= 15) {
                    rewardCoins += 1000;
                }
            }
            if (data == null) {
                rewardCoins = 100;
                rewardRenown = 0;
            }
            profile.getEventData().addEventrenown(rewardRenown);
            profile.grindCoins(rewardCoins);
            profile.setCoins(profile.getCoins() + rewardCoins);
            profile.changeRenown(profile.getRenown() + rewardRenown);

            profile.kingsQuestsData.checkUpdate();
            if (profile.kingsQuestsData.getAccepted()) {
                if (!profile.kingsQuestsData.getCompleted()) {
                    profile.kingsQuestsData.setCollectedRenown(profile.kingsQuestsData.getCollectedRenown() + rewardRenown);
                }
            }

            player.sendMessage(CC.translate("&6你的奖励: &6+" + rewardCoins + "硬币 &e+" + rewardRenown + "声望"));
            if (rank > 0) {
                player.sendMessage(CC.translate("&6&l你: &7收集了总计 &b" + (data == null ? 0 : data.soul) + " &7的灵魂 (排名#" + rank + ")"));
            }
            if (data != null && data.reachedSixFloor) {
                player.sendMessage(CC.translate("&6&l额外奖励: &a&l成功! &7最高进入过第六层或更高!"));
            } else {
                player.sendMessage(CC.translate("&6&l额外奖励: &c&l失败! &7你没能进入过第六层!"));
            }
            if (rankMap.size() >= 3) {
                Map<Integer, Player> rankPlayerMap = new HashMap<>();
                for (Map.Entry<UUID, Integer> entry : rankMap.entrySet()) {
                    if (entry.getValue() <= 3) {
                        rankPlayerMap.put(entry.getValue(), Bukkit.getPlayer(entry.getKey()));
                    }
                }

                player.sendMessage(CC.translate("&6顶级玩家: "));
                for (int i = 0; i < 3; i++) {
                    final Player top = rankPlayerMap.get(i);
                    if (top != null && top.isOnline()) {
                        final PlayerSpireData spireData = dataMap.get(top.getUniqueId());
                        PlayerProfile topProfile = PlayerProfile.getPlayerProfileByUuid(top.getUniqueId());

                        player.sendMessage(CC.translate(" &e&l#" + (i) + " " + topProfile.getFormattedName() + " &7收集了 &b" + spireData.soul + "&b灵魂"));
                    }
                }
            }
        }
        CC.boardCast(CC.CHAT_BAR);
        restorePlayers();
        dataMap.clear(); rankMap.clear(); teleporting.clear();
    }

    private void refreshCooldownPlayer() {
        final ArrayList<UUID> list = new ArrayList<>(cooldownList);
        final long now = System.currentTimeMillis();
        for (UUID uuid : list) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                final PlayerSpireData data = dataMap.get(uuid);
                if (data != null) {
                    if (now - data.lastFloorChange >= 3 * 1000) {
                        cooldownList.remove(player.getUniqueId());
                        PlayerMoveHandler.getCantMoveList().remove(player);
                    } else {
                        //fixme: player still can move on here`
                        final long lastTime = (3 * 1000 - (now - data.lastFloorChange)) / 1000;
                        TitleUtil.sendTitle(player, CC.translate("&a获得击杀"), this.getFloorColor(data.floor) + "第" + (data.floor + 1) + CC.translate("层&e(" + lastTime + ")"), 5, 20, 5);
                    }
                } else {
                    cooldownList.remove(player.getUniqueId());
                    PlayerMoveHandler.getCantMoveList().remove(player);
                }
            } else {
                cooldownList.remove(uuid);
                if (player != null) {
                    PlayerMoveHandler.getCantMoveList().remove(player);
                }
            }
        }
    }

    @Override
    public void onPreActive() {

    }

    public String getDisplayFloor(UUID uuid) {
        final PlayerSpireData data = this.dataMap.get(uuid);
        if (data == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(CC.translate("&" + this.getFloorColor(data.floor).getChar()));

        switch (data.floor) {
            case 0: {
                builder.append("零");
                break;
            }
            case 1: {
                builder.append("壹");
                break;
            }
            case 2: {
                builder.append("贰");
                break;
            }
            case 3: {
                builder.append("叁");
                break;
            }
            case 4: {
                builder.append("肆");
                break;
            }
            case 5: {
                builder.append("伍");
                break;
            }
            case 6: {
                builder.append("陆");
                break;
            }
            case 7: {
                builder.append("柒");
                break;
            }
            case 8: {
                builder.append("捌");
                break;
            }
            case 9: {
                builder.append("玖");
                break;
            }
        }

        return builder.toString();
    }

    private ChatColor getFloorColor(int floor) {
        //todo: 根据地图颜色适配chat color;
        return switch (floor) {
            case 0 -> ChatColor.RED;
            case 1 -> ChatColor.YELLOW;
            case 2 -> ChatColor.GREEN;
            case 3 -> ChatColor.AQUA;
            case 4 -> ChatColor.BLUE;
            case 5 -> ChatColor.DARK_PURPLE;
            case 6 -> ChatColor.LIGHT_PURPLE;
            case 7 -> ChatColor.WHITE;
            case 8 -> ChatColor.GRAY;
            default -> ChatColor.WHITE;
        };
    }

    @Override
    public int getRank(UUID playerUUID) {
        return this.rankMap.getOrDefault(playerUUID, -1);
    }

    @Override
    public List<String> insert(Player player) {
        List<String> line = new ArrayList<>();
        line.add("&f剩余时间: &a" + TimeUtil.millisToTimer(this.timer.getRemaining()));
        UUID uniqueId = player.getUniqueId();
        final PlayerSpireData data = this.dataMap.get(uniqueId);
        if (data == null) {
            line.add("&f灵魂: &b0");
            return line;
        }

        line.add("&f楼层: " + this.getDisplayFloor(uniqueId));
        line.add("&f灵魂: &b" + data.soul + (this.getRank(uniqueId) > 0 ? " &7[&e#" + this.getRank(uniqueId) + "&7]" : ""));

        return line;
    }

    public static class PlayerSpireData {
        private UUID uuid;
        private String name;
        private int kills;
        private int soul;
        private long lastFloorChange;

        private boolean reachedSixFloor;

        private int floor;
        private boolean inArea;
    }
}
