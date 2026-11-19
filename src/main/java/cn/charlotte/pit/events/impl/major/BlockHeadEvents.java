package cn.charlotte.pit.events.impl.major;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.type.rare.PaparazziEnchant;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.event.PitProfileLoadedEvent;
import cn.charlotte.pit.events.IEpicEvent;
import cn.charlotte.pit.events.MajorEventMapEdits;
import cn.charlotte.pit.events.MajorEventTasks;
import cn.charlotte.pit.events.IScoreBoardInsert;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.manager.BlockRestoreManager;
import cn.charlotte.pit.movement.PlayerMovementListener;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.time.TimeUtil;
import com.mojang.datafixers.util.Pair;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.api.holograms.HologramLine;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class BlockHeadEvents implements IEpicEvent, IScoreBoardInsert, Listener, PlayerMovementListener {

    /* ------------------------------------------------------------ */
    /* Static helpers                                               */
    /* ------------------------------------------------------------ */

    private static final DecimalFormat FORMAT = new DecimalFormat("##.##");
    /**
     * A filtered list of blocks that are allowed to be chosen for players.
     */
    private static final List<Material> BLOCK_LIST;

    /* ------------------------------------------------------------ */
    /* Instance fields                                              */
    /* ------------------------------------------------------------ */

    static {
        BLOCK_LIST = Arrays.stream(Material.values())
                .filter(m -> !m.isLegacy() && m.isBlock() && m.isSolid()
                        && m != Material.SAND
                        && m != Material.GRAVEL
                        && m != Material.CRAFTING_TABLE
                        && m != Material.SPAWNER
                        && m != Material.ANVIL
                        && m != Material.CHIPPED_ANVIL
                        && m != Material.DAMAGED_ANVIL
                        && m != Material.DISPENSER
                        && m != Material.ENCHANTING_TABLE
                        && m != Material.DRAGON_EGG
                        && m != Material.CAMPFIRE
                        && m != Material.SOUL_CAMPFIRE
                        && m != Material.SLIME_BLOCK
                        && m != Material.BEACON
                        && m != Material.CACTUS
                        && m != Material.CHORUS_PLANT
                        && m != Material.CHORUS_FRUIT
                        && m != Material.TNT
                        && m != Material.LEGACY_BED_BLOCK
                        && m != Material.LEGACY_GLOWING_REDSTONE_ORE
                        && m != Material.LEGACY_THIN_GLASS
                        && m != Material.BREWING_STAND
                        && m != Material.HOPPER
                        && m != Material.HOPPER_MINECART
                        && m != Material.IRON_BARS
                        && m != Material.SOUL_LANTERN
                        && m != Material.LANTERN
                        && m != Material.LEGACY_MOB_SPAWNER
                        && m != Material.TRIAL_SPAWNER
                        && m != Material.SNOW_BLOCK
                        && m != Material.SNOW
                        && m != Material.WATER
                        && m != Material.SCULK
                        && m != Material.SCULK_CATALYST
                        && m != Material.SCULK_SENSOR
                        && m != Material.SCULK_SHRIEKER
                        && m != Material.CALIBRATED_SCULK_SENSOR
                        && m != Material.LIGHTNING_ROD
                        && m != Material.DAYLIGHT_DETECTOR
                        && m != Material.STONECUTTER
                        && m != Material.CAULDRON
                        && m != Material.BELL
                        && m != Material.CONDUIT
                        && m != Material.SCAFFOLDING
                        && m != Material.GRINDSTONE
                        && m != Material.COMPOSTER
                        && m != Material.BAMBOO
                        && m != Material.BAMBOO_BLOCK
                        && m != Material.DECORATED_POT
                        && m != Material.CHISELED_BOOKSHELF
                        && m != Material.LECTERN
                        && m != Material.BARREL
                        && m != Material.REPEATER
                        && m != Material.COMPARATOR
                        && m != Material.WATER_CAULDRON
                        && m != Material.MAGMA_BLOCK
                        && m != Material.SOUL_SAND
                        && m != Material.END_PORTAL
                        && m != Material.END_PORTAL_FRAME
                        && m != Material.NETHER_PORTAL
                        && m != Material.LAVA
                        && m != Material.CHAIN
                        && m != Material.POINTED_DRIPSTONE
                        && !m.name().contains("SIGN")
                        && !m.name().contains("DOOR")
                        && !m.name().contains("PLATE")
                        && !m.name().contains("CHEST")
                        && !m.name().contains("STAIRS")
                        && !m.name().contains("ICE")
                        && !m.name().contains("FURNACE")
                        && !m.name().contains("STEP")
                        && !m.name().contains("PISTON")
                        && !m.name().contains("WALL")
                        && !m.name().contains("FENCE")
                        && !m.name().contains("BANNER")
                        && !m.name().contains("SLAB")
                        && !m.name().contains("LEAVE")
                        && !m.name().contains("CARPET")
                        && !m.name().contains("GLASS")
                        && !m.name().contains("SHULKER")
                        && !m.name().contains("CANDLE")
                        && !m.name().contains("CAKE")
                        && !m.name().contains("CORAL")
                        && !m.name().contains("PUMPKIN")
                        && !m.name().contains("_BED")
                        && !m.name().contains("STAINED_GLASS_PANE")
                        && !m.name().contains("DETECTOR")
                        && !m.name().contains("AMETHYST")
                )
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Original terrain snapshot (for rollback).
     */
    private final Map<Location, BlockCache> globalCache = new HashMap<>();
    private volatile Map<com.sk89q.worldedit.world.World, Map<BlockVector3, com.sk89q.worldedit.world.block.BaseBlock>> pendingRestoration;

    /**
     * Which player owns a painted block.
     */
    private final Map<Location, UUID> locationToPlayer = new HashMap<>();

    /**
     * Per-player data.
     */
    private final Map<UUID, BlockHeadData> cache = new HashMap<>();

    /**
     * Active buff orbs.
     */
    private final List<BuffData> buffCache = new ArrayList<>();

    /**
     * Players who currently have the QUICK_TRAIL buff and their cooldown.
     */
    private final Map<UUID, Cooldown> quickTrail = new HashMap<>();
    private Cooldown timer;
    private List<Material> BLOCK_LIST_TEMP;
    /**
     * Periodic task that:
     * <ul>
     *     <li>Ends the event when the timer expires,</li>
     *     <li>Culls any expired QUICK_TRAIL cooldowns.</li>
     * </ul>
     */
    private BukkitRunnable runnable = null;

    /**
     * Helper for other classes to query a player's block-type while the event is running.
     */
    public static Material getType(UUID uuid) {
        IEpicEvent active = ThePit.getInstance().getEventFactory().getActiveEpicEvent();
        if (active instanceof BlockHeadEvents event) {
            BlockHeadData data = event.cache.get(uuid);
            return data == null ? null : data.block;
        }
        return null;
    }

    public void freshRunnable() {
        runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (timer != null && timer.hasExpired()) {
                    ThePit.getInstance().getEventFactory().inactiveEvent(BlockHeadEvents.this);
                    cancel();
                    return;
                }

                // Remove finished quick-trail entries.
                Bukkit.getOnlinePlayers().forEach(p -> {
                    Cooldown cd = quickTrail.get(p.getUniqueId());
                    if (cd != null && cd.hasExpired()) {
                        quickTrail.remove(p.getUniqueId());
                    }
                });
            }
        };
    }

    /* ------------------------------------------------------------ */
    /* IEvent / IEpicEvent implementation                           */
    /* ------------------------------------------------------------ */

    @Override
    public String getEventInternalName() {
        return "block_head";
    }

    @Override
    public List<String> getLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&6&l>> &e方块划地战 &6&l<<");
        lore.add("");
        lore.add("&6&l事件机制");
        lore.add("&7- 每人分配专属方块 &a(实时显示在头顶)");
        lore.add("&7- &e击杀敌人 &7立即占领对方已占领的方块");
        lore.add("&7- &c处决敌人 &7在脚下生成 &b5x5专属方块");
        lore.add("&7- &7地图周边随时出现 &a增益球 效果如下");
        lore.add("");
        lore.add("&a&l增益球效果");
        lore.add("&a> 快速染径 &8[30秒]");
        lore.add("&7  移动时自动铺设专属方块");
        lore.add("&e> 超级回复");
        lore.add("&7  恢复满血 + 获得 &c3颗心额外生命");
        lore.add("&b> 钻石护甲");
        lore.add("&7  立即装备全套钻石防具");
        lore.add("");
        lore.add("&6&l事件奖励机制");
        lore.add("&7> 个人排名：按每人占领的方块数进行排名");
        lore.add("&7> 全服目标：&e20,000方块 &a全员获得金币(事件金币)翻倍");
        lore.add("");
        lore.add("&6&l排名奖励");
        lore.add("&f第1-5名: &64000金币 &f+ &b2声望");
        lore.add("&f第6-15名: &61500金币 &f+ &b1声望");
        lore.add("&f其他玩家: &6100金币");
        return lore;
    }

    @Override
    public String getEventName() {
        return CC.translate("&9方块划地战");
    }

    @Override
    public int requireOnline() {
        return CombatListener.isBedrockRoom() ? 20 : 50;
    }

    /* ------------------------------------------------------------ */
    /* Movement & combat logic                                      */
    /* ------------------------------------------------------------ */

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        IEpicEvent activeEpicEvent = ThePit.getInstance().getEventFactory().getActiveEpicEvent();
        if (activeEpicEvent instanceof BlockHeadEvents) {
            event.setCancelled(true);
            CC.DebugMessage(event.getPlayer(), "BlockHeadEvents");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        IEpicEvent activeEpicEvent = ThePit.getInstance().getEventFactory().getActiveEpicEvent();
        if (activeEpicEvent instanceof BlockHeadEvents) {
            event.setCancelled(true);
        }
    }


    /**
     * Called externally by the movement listener (not included in this port).
     */
    @Override
    public void handleUpdateLocation(Player player, Location from, Location to) {
        if (!isActive() || to == null) return;
        if (!Bukkit.isPrimaryThread()) {
            var factory = ThePit.getInstance().getEventFactory();
            long generation = factory.majorGeneration();
            Location capturedFrom = from == null ? null : from.clone(), capturedTo = to.clone();
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                if (factory.isMajorRun(this, generation) && player.isOnline()) handleUpdateLocation(player, capturedFrom, capturedTo);
            });
            return;
        }
        // Handle buff orbs pick-up.
        for (BuffData data : buffCache) {
            if (!data.cooldown.hasExpired()) continue;
            if (to.getWorld().equals(data.location.getWorld()) && to.distanceSquared(data.location) <= 2 * 2) {
                handleBuffPickup(player, data);
                break;
            }
        }

        // QUICK_TRAIL logic.
        Cooldown cd = quickTrail.get(player.getUniqueId());
        if (cd == null || cd.hasExpired()) {
            quickTrail.remove(player.getUniqueId());
            return;
        }

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (!profile.isInArena()) {
            return;
        }

        if (player.isOnGround()) {
            Location blockBelow = player.getLocation().getBlock().getLocation().add(0, -1, 0);
            if (!player.getLocation().getBlock().getType().equals(Material.AIR)) {
                return;
            }
            if (blockBelow.getBlock().getType().equals(Material.AIR)) {
                return;
            }
            if (!blockBelow.getBlock().isSolid()) {
                return;
            }

            globalCache.computeIfAbsent(blockBelow, loc -> new BlockCache(loc.getBlock().getType(), loc.getBlock().getBlockData()));

            UUID previousOwner = locationToPlayer.get(blockBelow);
            if (previousOwner != null) {
                BlockHeadData previousData = cache.get(previousOwner);
                if (previousData != null) previousData.gotBlocks.remove(blockBelow);
            }

            BlockHeadData data = cache.get(player.getUniqueId());
            if (data == null) return;

            locationToPlayer.put(blockBelow, data.uuid);
            data.gotBlocks.add(blockBelow);

            blockBelow.getBlock().setType(data.block);
            blockBelow.getBlock().setBlockData(data.data);
        }
    }

    private void handleBuffPickup(Player player, BuffData data) {
        switch (data.type) {
            case QUICK_TRAIL:
                quickTrail.put(player.getUniqueId(), new Cooldown(30, TimeUnit.SECONDS));
                PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.SPEED, 20 * 30, 1, true, true));
                CC.send(MessageType.EVENT, player, "&9&l捡起! &7你获得了30秒的 &f速度 II&7, 以及你经过的地方将被你染色!");
                break;
            case SUPER_HEAL:
                player.setHealth(player.getMaxHealth());
//                player.setAbsorptionAmount(player.getAbsorptionAmount() + 6.0);
                PlayerUtil.addAbsorption(player, 6);
                CC.send(MessageType.EVENT, player, "&9&l捡起! &7你获得了3颗心的额外生命!并且你的血量已恢复最大值");
                break;
            case DIAMOND_ARMOR:
                equipDiamondArmor(player);
                CC.send(MessageType.EVENT, player, "&9&l捡起! &7你已装备钻石套装!");
                break;
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 4F, 2F);
        data.cooldown = new Cooldown(30, TimeUnit.SECONDS);

        // Update hologram for all players.
        Bukkit.getOnlinePlayers().forEach(p -> data.changeItem(false));
        MajorEventTasks.later(this, () -> Bukkit.getOnlinePlayers().forEach(p -> data.changeItem(true)), 20 * 30L);
    }

    private void equipDiamondArmor(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack itemStack = new ItemBuilder(Material.DIAMOND_HELMET).removeOnJoin(true).deathDrop(true).canTrade(false).canDrop(false).buildWithUnbreakable();
        if (helmet == null || ItemUtil.getInternalName(helmet) == null || helmet.getType() == Material.AIR) {
            player.getInventory().setHelmet(itemStack);
        } else {
            player.getInventory().addItem(itemStack);
        }

        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null || chest.getType() != Material.DIAMOND_CHESTPLATE) {
            player.getInventory().setChestplate(new ItemBuilder(Material.DIAMOND_CHESTPLATE).removeOnJoin(true).deathDrop(true).canTrade(false).canDrop(false).buildWithUnbreakable());
        }

        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || (boots.getType() != Material.LEATHER_BOOTS && boots.getType() != Material.DIAMOND_BOOTS && Utils.getMythicItem(boots) == null)) {
            player.getInventory().setBoots(new ItemBuilder(Material.DIAMOND_BOOTS).removeOnJoin(true).deathDrop(true).canTrade(false).canDrop(false).buildWithUnbreakable());
        }

        ItemStack legs = player.getInventory().getLeggings();
        if (legs == null || (legs.getType() != Material.LEATHER_LEGGINGS && legs.getType() != Material.DIAMOND_LEGGINGS)) {
            player.getInventory().setLeggings(new ItemBuilder(Material.DIAMOND_LEGGINGS).removeOnJoin(true).deathDrop(true).canTrade(false).canDrop(false).buildWithUnbreakable());
        }

        player.playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, 1F, 1F);
    }

    @EventHandler
    public void whenLoaded(PitProfileLoadedEvent event) {
        PlayerProfile profile = event.getPlayerProfile();
        int idx = new Random().nextInt(BLOCK_LIST_TEMP.size());
        Material material = BLOCK_LIST_TEMP.get(idx);
        BLOCK_LIST_TEMP.remove(idx);

        UUID uuid = profile.getPlayerUUID();
        cache.put(uuid, new BlockHeadData(uuid, material));

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            buffCache.stream().filter(b -> !b.cooldown.hasExpired()).forEach(b -> b.changeItem(false));
        }
    }

    @EventHandler
    public void whenKill(PitKillEvent event) {
        BlockHeadData killerData = cache.get(event.getKiller().getUniqueId());
        BlockHeadData targetData = cache.get(event.getTarget().getUniqueId());
        if (killerData == null || targetData == null) return;

        // Transfer territory
        for (Location loc : targetData.gotBlocks) {
            if (loc.getBlock().getType().equals(Material.AIR)) {
                continue;
            }

            loc.getBlock().setType(killerData.block);
            loc.getBlock().setBlockData(killerData.data);
            killerData.gotBlocks.add(loc);
        }
        targetData.gotBlocks.clear();

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(killerData.uuid);
        if (!profile.isInArena()) return;

        Player killer = event.getKiller();
        Location loc = killer.getLocation();
        Location now = loc.getBlock().getLocation();

        generateBlock(killerData, now);

        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 4F, 1F);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);

        List<FallingBlockEntity> dropped = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            dropped.add(generateFallingBlocks(loc, killerData.block, killerData.data));
//        }

        MajorEventTasks.later(this, () -> {
            for (FallingBlockEntity fb : dropped) {
                ClientboundRemoveEntitiesPacket pkt = new ClientboundRemoveEntitiesPacket(fb.getId());
                Bukkit.getOnlinePlayers().forEach(p -> ((CraftPlayer) p).getHandle().connection.send(pkt));
            }
        }, 20L);
    }

    private boolean isAir(Location location) {
        return location.getBlock().getType() == Material.AIR ||
                BlockRestoreManager.INSTANCE.isScheduled(location);
    }

    private void generateBlock(BlockHeadData data, Location location) {
        for (int x = 5; x >= -5; x--) {
            for (int z = 5; z >= -5; z--) {
                Location offs = location.clone().add(x, 0, z).getBlock().getLocation();

                // 向下检查最多两层空气
                boolean valid = false;
                Location checkLoc = offs.clone();
                for (int i = 0; i < 3; i++) {
                    if (checkLoc.getBlock().getType() != Material.AIR) {
                        valid = true;
                        break;
                    }
                    checkLoc.add(0, -1, 0); // 向下移动一格
                }

                // 如果连续三层都是空气，跳过
                if (!valid) continue;

                Block block = checkLoc.getBlock();
                if (block.getType().equals(Material.AIR)) {
                    continue;
                }

                // 使用找到的非空气位置
                // 这里checkLoc已经是最后检查的位置

                globalCache.computeIfAbsent(checkLoc, loc -> new BlockCache(loc.getBlock().getType(), loc.getBlock().getBlockData()));

                UUID previous = locationToPlayer.get(checkLoc);
                if (previous != null) {
                    BlockHeadData prevData = cache.get(previous);
                    if (prevData != null) prevData.gotBlocks.remove(checkLoc);
                }

                locationToPlayer.put(checkLoc, data.uuid);
                checkLoc.getBlock().setType(data.block);
                checkLoc.getBlock().setBlockData(data.data);
                data.gotBlocks.add(checkLoc);
            }
        }
    }


    private FallingBlockEntity generateFallingBlocks(Location origin, Material type, BlockData data) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double vx = rnd.nextDouble() - 0.5;
        double vy = rnd.nextDouble() - 0.5;
        double vz = rnd.nextDouble() - 0.5;

        net.minecraft.world.level.Level nmsWorld = ((CraftWorld) origin.getWorld()).getHandle();
        FallingBlockEntity falling = new FallingBlockEntity(EntityType.FALLING_BLOCK, nmsWorld);
        falling.setPos(origin.getX(), origin.getY(), origin.getZ());
        falling.setDeltaMovement(new Vec3(vx, vy, vz));
        falling.hasImpulse = true;

        ClientboundAddEntityPacket spawnPkt = new ClientboundAddEntityPacket(falling);
        ClientboundSetEntityMotionPacket motionPkt = new ClientboundSetEntityMotionPacket(falling);
        ClientboundSetEntityDataPacket metaPacket =
                new ClientboundSetEntityDataPacket(falling.getId(), falling.getEntityData().packDirty());

        PlayerUtil.getNearbyPlayers(origin, 30.0).forEach(p -> {
            ((CraftPlayer) p).getHandle().connection.send(spawnPkt);
            ((CraftPlayer) p).getHandle().connection.send(metaPacket);
            ((CraftPlayer) p).getHandle().connection.send(motionPkt);
        });
        return falling;
    }

    /* ------------------------------------------------------------ */
    /* Event lifecycle                                              */
    /* ------------------------------------------------------------ */

    @Override
    public void onActive() {
        if (!globalCache.isEmpty() || pendingRestoration != null) throw new IllegalStateException("上次方块划地战地图尚未恢复");
        cache.clear(); locationToPlayer.clear(); buffCache.clear(); quickTrail.clear();
        BLOCK_LIST_TEMP = new ArrayList<>(BLOCK_LIST);
        // 强制清理玩家放置的方块
        BlockRestoreManager.INSTANCE.forceRestoreAll();
        freshRunnable();

        timer = new Cooldown(5, TimeUnit.MINUTES);
        ThePit.getInstance().getMovementDispatcher().addListener(this);

        ThePit.getInstance().getPitConfig().getBlockHeadLocations().forEach(loc -> {
            BuffData bd = new BuffData();
            bd.cooldown = new Cooldown(0);
            bd.location = loc;
            bd.type = BuffType.values()[new Random().nextInt(BuffType.values().length)];
            buffCache.add(bd);
        });
        buffCache.forEach(BuffData::spawn);

        // Assign blocks to online players.
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (BLOCK_LIST_TEMP.isEmpty()) BLOCK_LIST_TEMP.addAll(BLOCK_LIST);
            int idx = new Random().nextInt(BLOCK_LIST_TEMP.size());
            Material mat = BLOCK_LIST_TEMP.remove(idx);
            cache.put(player.getUniqueId(), new BlockHeadData(player.getUniqueId(), mat));
            Util.getPitProfile(player).getEventData().callHistoricalEvents(getEventName(), 1);
        }


//        ClearRunnable.getClearRunnable().getPlacedBlock().forEach(data -> data.getLocation().getBlock().setType(Material.AIR));

        // Show helmets to everyone.
        for (Player self : Bukkit.getOnlinePlayers()) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                BlockHeadData d = cache.get(target.getUniqueId());
                if (d == null) continue;

                ((CraftPlayer) self).getHandle().connection.send(new ClientboundSetEquipmentPacket(target.getEntityId(),
                        Collections.singletonList(new Pair<>(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(new ItemStack(d.block, 1))))));
            }
        }

        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());
        MajorEventTasks.track(this, runnable.runTaskTimer(ThePit.getInstance(), 20L, 20L));
    }

    private void stopRuntime() {
        if (BLOCK_LIST_TEMP != null) BLOCK_LIST_TEMP.clear();
        HandlerList.unregisterAll(this);
        ThePit.getInstance().getMovementDispatcher().removeListener(this);
        if (runnable != null) {
            runnable.cancel();
            runnable = null;
        }

        buffCache.forEach(BuffData::despawn);
        buffCache.clear();
        quickTrail.clear();

        // Restore player helmets.
        for (Player self : Bukkit.getOnlinePlayers()) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                ((CraftPlayer) self).getHandle().connection.send(new ClientboundSetEquipmentPacket(target.getEntityId(),
                        Collections.singletonList(new Pair<>(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(target.getInventory().getHelmet())))));
            }
        }

    }

    @Override public void onAbort() { stopRuntime(); }

    @Override
    public void onInactive() {
        stopRuntime();

        List<BlockHeadData> sorted = new ArrayList<>(cache.values());
        sorted.sort(Comparator.comparingInt((BlockHeadData d) -> d.gotBlocks.size()).reversed());
        Map<UUID, Integer> rank = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) rank.put(sorted.get(i).uuid, i);

        boolean success = globalCache.size() >= 20000;
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = Util.getPitProfile(player);

            Integer position = rank.get(player.getUniqueId());
            BlockHeadData data = cache.get(player.getUniqueId());
            if (position == null || data == null) continue;

            if (profile == null) continue;


            int rewardCoins = 0;
            int rewardRenown = 0;
            int index = position + 1;
            if (index <= 5) {
                rewardCoins += 4000;
                rewardRenown += 2;
            } else if (index <= 15) {
                rewardCoins += 1500;
                rewardRenown += 1;
            } else {
                rewardCoins += 100;
            }

            if (ThePit.getInstance().getPitConfig().isGenesisEnable() && profile.getGenesisData().getTier() >= 5 && rewardRenown > 0) {
                rewardRenown++;
            }

            int enchantBoostLevel = new PaparazziEnchant().getItemEnchantLevel(player.getInventory().getLeggings());
            if (PlayerUtil.shouldIgnoreEnchant(player)) enchantBoostLevel = 0;
            if (enchantBoostLevel > 0) {
                rewardCoins += (int) (0.5 * enchantBoostLevel * rewardCoins);
                rewardRenown += (int) Math.floor(0.5 * enchantBoostLevel * rewardRenown);

                MythicLeggingsItem mythic = new MythicLeggingsItem();
                mythic.loadFromItemStack(player.getInventory().getLeggings());
                if (mythic.isEnchanted()) {
                    if (mythic.getMaxLive() > 0 && mythic.getLive() <= 2) {
                        player.getInventory().setLeggings(new ItemStack(Material.AIR));
                    } else {
                        mythic.setLive(mythic.getLive() - 2);
                        player.getInventory().setLeggings(mythic.toItemStack());
                    }
                }
            }

            if (PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.SELF_CONFIDENCE)) {
                if (index <= 5) {
                    rewardCoins += 5000;
                } else if (index <= 10) {
                    rewardCoins += 2500;
                } else if (index <= 15) {
                    rewardCoins += 1000;
                }
            }

            if (success) rewardCoins *= 2;
            if (profile.isBetterPlayer()) {
                rewardCoins += rewardCoins / 2;
                rewardRenown += 1;
            }
            profile.grindCoins(rewardCoins);
            profile.setCoins(profile.getCoins() + rewardCoins);
            profile.changeRenown(profile.getRenown() + rewardRenown);
            profile.getEventData().setEventrenown(profile.getEventData().getEventrenown() + rewardRenown);
            player.sendMessage(CC.GOLD + CC.CHAT_BAR);
            player.sendMessage(CC.translate("&6你的奖励: &6+" + rewardCoins + "硬币 &e+" + rewardRenown + "声望"));
            if (success) {
                player.sendMessage(CC.translate("&6&l全局奖励: &a&l成功! &7所有人获得的金币翻倍!"));
            } else {
                player.sendMessage(CC.translate("&6&l全局奖励: &c&l失败! &7全服只占领了 &c" + globalCache.size() + " &7个方块"));
            }
            player.sendMessage(CC.translate("&6你的战绩: &7共占领了 " + data.gotBlocks.size() + " 个方块! 排名 &8[#" + (position + 1) + "]"));
            player.sendMessage(CC.translate("&6&l顶级玩家: "));
            for (int i = 0; i < Math.min(3, sorted.size()); i++) {
                BlockHeadData hd = sorted.get(i);
                PlayerProfile pProfile = PlayerProfile.getPlayerProfileByUuid(hd.uuid);
                player.sendMessage(CC.translate("&6" + (i + 1) + " - " + (pProfile == null ? hd.uuid : pProfile.getFormattedNameWithRoman()) + " &9共占领了&e " + hd.gotBlocks.size() + " &9个方块"));
            }
            player.sendMessage(CC.GOLD + CC.CHAT_BAR);
        }

    }

    @Override
    public java.util.concurrent.CompletableFuture<Void> restore() {
        if (pendingRestoration == null) {
            Map<com.sk89q.worldedit.world.World, Map<BlockVector3, com.sk89q.worldedit.world.block.BaseBlock>> grouped = new HashMap<>();
            for (var entry : globalCache.entrySet()) {
                Location at = entry.getKey();
                var world = new BukkitWorld(java.util.Objects.requireNonNull(at.getWorld()));
                grouped.computeIfAbsent(world, ignored -> new HashMap<>()).put(
                        BlockVector3.at(at.getBlockX(), at.getBlockY(), at.getBlockZ()), BukkitAdapter.adapt(entry.getValue().data).toBaseBlock());
            }
            Map<com.sk89q.worldedit.world.World, Map<BlockVector3, com.sk89q.worldedit.world.block.BaseBlock>> frozen = new HashMap<>();
            grouped.forEach((world, blocks) -> frozen.put(world, Map.copyOf(blocks)));
            pendingRestoration = Map.copyOf(frozen);
            globalCache.clear(); locationToPlayer.clear(); cache.clear();
        }
        final var captured = pendingRestoration;
        return MajorEventMapEdits.run(() -> {
            for (var entry : captured.entrySet()) {
                try (EditSession target = WorldEdit.getInstance().newEditSessionBuilder().world(entry.getKey()).changeSetNull().build()) {
                    for (var block : entry.getValue().entrySet()) target.setBlock(block.getKey(), block.getValue());
                }
            }
        }).thenRun(() -> pendingRestoration = null);
    }

    /* ------------------------------------------------------------ */
    /* Scoreboard                                                   */
    /* ------------------------------------------------------------ */

    @Override
    public List<String> insert(Player player) {
        BlockHeadData data = cache.get(player.getUniqueId());
        if (data == null) return Collections.emptyList();
        int size = data.gotBlocks.size();
        String timeLeft = TimeUtil.millisToRoundedTime(timer.getRemaining());

        String percentage = globalCache.isEmpty() ? "0" : FORMAT.format((double) size / (double) globalCache.size() * 100.0);
        return Arrays.asList(
                "&f剩余时间: &a" + timeLeft,
                "&f已占领方块: &a" + size + " &7(" + percentage + "%)"
        );
    }

    @Override
    public void handleUpdateRotation(Player var1, Location var2, Location var3) {

    }

    /* ------------------------------------------------------------ */
    /* Inner helper classes                                         */
    /* ------------------------------------------------------------ */

    public enum BuffType {
        QUICK_TRAIL("&a&l快速染径", Material.BLACK_DYE),
        SUPER_HEAL("&e&l超级回复", Material.GOLDEN_APPLE),
        DIAMOND_ARMOR("&b&l钻石护甲", Material.DIAMOND_HELMET);

        public final String display;
        public final Material material;

        BuffType(String display, Material material) {
            this.display = display;
            this.material = material;
        }
    }

    public static class BlockHeadData {
        final UUID uuid;
        final Material block;
        final Set<Location> gotBlocks = new HashSet<>();
        BlockData data;

        BlockHeadData(UUID uuid, Material block) {
            this(uuid, block, block.createBlockData());
        }

        BlockHeadData(UUID uuid, Material block, BlockData data) {
            this.uuid = uuid;
            this.block = block;
            this.data = data;
        }
    }

    public static class BlockCache {
        final Material material;
        final BlockData data;

        public BlockCache(Material material, BlockData data) {
            this.material = material;
            this.data = data;
        }
    }

    public class BuffData {
        UUID uuid = UUID.randomUUID();
        Hologram hologram;
        BuffType type;
        Location location;
        Cooldown cooldown = new Cooldown(0);

        void spawn() {
            Location add = location.clone().add(0, 1, 0);
            hologram = DHAPI.getHologram(uuid.toString());
            if (hologram == null) {
                hologram = DHAPI.createHologram(uuid.toString(), add, false);
            } else {
                hologram.setLocation(add);
                hologram.removePage(0);
            }
            hologram.addPage();
            HologramLine line1 = new HologramLine(hologram.getPage(0), location, CC.translate(type.display));
            HologramLine line2 = new HologramLine(hologram.getPage(0), location, "#ICON:" + type.name());
            hologram.getPage(0).addLine(line1);
            hologram.getPage(0).addLine(line2);
            hologram.showAll();
        }

        void changeItem(boolean available) {
            if (hologram == null) return;
            HologramLine l1 = hologram.getPage(0).getLine(0);
            HologramLine l2 = hologram.getPage(0).getLine(1);
            if (available) {
                l1.setContent(CC.translate(type.display));
                l2.setContent("#ICON:" + type.name());
            } else {
                l1.setContent(CC.translate(type.display + " &7-&c 冷却中"));
                l2.setContent("#ICON:" + Material.BARRIER.name());
            }
        }

        void despawn() {
            DHAPI.removeHologram(uuid.toString());
        }
    }
}
