package cn.charlotte.pit.events.impl.major;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.config.PitConfig;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.type.rare.PaparazziEnchant;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.events.IEpicEvent;
import cn.charlotte.pit.events.MajorEventTasks;
import cn.charlotte.pit.events.IScoreBoardInsert;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.BoomFirework;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.time.TimeUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.*;
import org.bukkit.block.Banner;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3f;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class SquadsEvents implements IEpicEvent, Listener, IScoreBoardInsert {

    private static final char startChars = 'A';
    private static final List<ChatColor> colors = new ArrayList<>();

    static {
        for (ChatColor c : ChatColor.values()) {
            if (BannerData.chatColorToColor.get(c) != null) {
                colors.add(c);
            }
        }
    }

    public final Set<Character> usedChars = new HashSet<>();
    public final Map<UUID, PlayerData> playerMap = new HashMap<>();
    public final Map<UUID, TeamData> teamMap = new HashMap<>();
    private Cooldown timer = new Cooldown(5, TimeUnit.MINUTES);
    private final Map<Location, org.bukkit.block.BlockState> originalBanners = new HashMap<>();
    private final List<ChatColor> unusedColor = new ArrayList<>(colors);
    private final Set<TeamData> unFullTeams = new HashSet<>();
    private final List<BannerData> banners = new ArrayList<>();
    private final DecimalFormat format = new DecimalFormat("###,###,###,###,###");
    private State state = State.PREPARE;
    private BukkitRunnable logicRunnable = null;
    private BukkitRunnable effectRunnable = null;

    @Override
    public String getEventInternalName() {
        return "squads";
    }

    @Override
    public String getEventName() {
        return "&b&l旗帜争夺战";
    }

    @Override
    public List<String> getLore() {
        ArrayList<String> lore = new ArrayList<>();
        lore.add("&6&l>> &b旗帜争夺战 &6&l<<");
        lore.add("");
        lore.add("&6&l事件机制");
        lore.add("&7- 玩家自动组成3人小队(字母标识+颜色)");
        lore.add("&7- 占领地图旗帜获得持续积分");
        lore.add("&7- 旗帜区域多人时进入争夺状态");
        lore.add("&7- 击杀敌人获得 &b+25积分/次");
        lore.add("");
        lore.add("&6&l事件奖励机制");
        lore.add("&7> 个人排名：按队伍总积分排名");
        lore.add("&7> 占领奖励：&b2-4积分/秒 &7(随时间递增)");
        lore.add("");
        lore.add("&6&l排名奖励");
        lore.add("&f第1-3名队伍: &62000金币 &f+ &b2声望");
        lore.add("&f第4-20名队伍: &6500金币 &f+ &b1声望");
        lore.add("&f其他队伍: &6100金币");
        return lore;
    }

    @Override
    public int requireOnline() {
        return CombatListener.isBedrockRoom() ? 20 : 50;
    }

//    private val logicRunnable by lazy {
//        object : BukkitRunnable() {
//            override fun run() {
//                for (banner in banners) {
//                    banner.refreshBannerLogic(this@SquadsEvent)
//                }
//            }
//        }
//    }
//
//    private val effectRunnable by lazy {
//        object : BukkitRunnable() {
//            override fun run() {
//                for (banner in banners) {
//                    banner.drawParticle()
//                }
//                refreshRank()
//            }
//        }
//    }

    @Override
    public void onActive() {
        clearState();
        timer = new Cooldown(5, TimeUnit.MINUTES);
        for (Player player : Bukkit.getOnlinePlayers()) playerMap.put(player.getUniqueId(), new PlayerData(player.getUniqueId()));
        logicRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                for (BannerData banner : banners) {
                    banner.refreshBannerLogic(SquadsEvents.this);
                }
            }
        };
        MajorEventTasks.track(this, logicRunnable.runTaskTimer(ThePit.getInstance(), 20L, 20L));

        effectRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                for (BannerData banner : banners) {
                    banner.drawParticle();
                }
                refreshRank();
            }
        };
        MajorEventTasks.track(this, effectRunnable.runTaskTimer(ThePit.getInstance(), 20L, 10L));

        PitConfig config = ThePit.getInstance().getPitConfig();
        for (Location location : config.getSquadsLocations()) {
            originalBanners.putIfAbsent(location.clone(), location.getBlock().getState());
            location.getBlock().setType(Material.WHITE_BANNER);
            org.bukkit.block.BlockState blockState = location.getBlock().getState();
            if (blockState instanceof Banner banner) {
                banner.setBaseColor(DyeColor.WHITE);
                banner.update();
            }

            BannerData bannerData = new BannerData(location);
            banners.add(bannerData);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            TeamData data = teamMap.get(player.getUniqueId());
            if (data == null) {
                data = createTeam();
                teamMap.put(player.getUniqueId(), data);
                addPlayerToTeam(data, player);
            }

            if (data.players.size() >= 3) {
                continue;
            }

            List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
            // players.sortedBy { it.location.distanceSquared(player.location) } // Kotlin 中此行未生效，保持原意

            for (Player target : players) {
                TeamData teamData = teamMap.get(target.getUniqueId());
                if (teamData != null) {
                    continue;
                }

                teamMap.put(target.getUniqueId(), data);
                addPlayerToTeam(data, target);
                if (data.players.size() >= 3) {
                    unFullTeams.remove(data);
                    break;
                }
            }
        }

        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());

        MajorEventTasks.track(this, new BukkitRunnable() {
            @Override
            public void run() {
                if (timer.hasExpired()) {
                    ThePit.getInstance().getEventFactory().inactiveEvent(SquadsEvents.this);
                    cancel();
                }
            }
        }.runTaskTimer(ThePit.getInstance(), 20L, 20L));

        state = State.GAMING;
    }

    @Override
    public void onInactive() {
        stopRuntime();

        refreshRank();

        Bukkit.broadcastMessage(CC.CHAT_BAR);
        Bukkit.broadcastMessage(CC.translate("&6&l天坑事件结束: " + this.getEventName() + "&6&l!"));

        for (Player player : Bukkit.getOnlinePlayers()) {
            TeamData team = teamMap.get(player.getUniqueId());
            if (team == null) continue;

            int rank = team.rank;
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            double rewardCoins = 0.0;
            int rewardRenown = 0;
            if (rank <= 3) {
                rewardCoins += 2000.0;
                rewardRenown += 2;
            } else if (rank <= 20) {
                rewardCoins += 500.0;
                rewardRenown += 1;
            } else {
                rewardCoins += 100.0;
            }
//            if (data != null && data.reachedSixFloor) {
//                rewardRenown++
//            }
            if (ThePit.getInstance().getPitConfig().isGenesisEnable() && profile.getGenesisData().getTier() >= 5 && rewardRenown > 0) {
                rewardRenown++;
            }
            int enchantBoostLevel = new PaparazziEnchant().getItemEnchantLevel(player.getInventory().getLeggings());
            if (PlayerUtil.shouldIgnoreEnchant(player)) {
                enchantBoostLevel = 0;
            }
            if (enchantBoostLevel > 0) {
                rewardCoins += 0.5 * enchantBoostLevel * rewardCoins;
                rewardRenown += (int) Math.floor(0.5 * enchantBoostLevel * rewardRenown);

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
                    rewardCoins += 5000.0;
                } else if (rank <= 10) {
                    rewardCoins += 2500.0;
                } else if (rank <= 15) {
                    rewardCoins += 1000.0;
                }
            }
//            if (profile.isBetterPlayer) {
//                rewardCoins *= 1.5
//                rewardRenown += 1
//            }
            profile.grindCoins(rewardCoins);
            profile.setCoins(profile.getCoins() + rewardCoins);
            profile.changeRenown(profile.getRenown() + rewardRenown);
            player.sendMessage(CC.translate("&6你的奖励: &6+" + rewardCoins + "硬币 &e+" + rewardRenown + "声望"));
            if (rank > 0) {
                player.sendMessage(CC.translate("&6&l你的队伍: &7获得了 &b" + team.score + " &7分数 (排名#" + rank + ")"));
            }
            List<TeamData> list = new ArrayList<>(new HashSet<>(teamMap.values()));
            list.sort((a, b) -> Integer.compare(b.score, a.score));
            if (list.size() >= 3) {
                player.sendMessage(CC.translate("&6顶级玩家: "));
                for (int i = 1; i <= 3; i++) {
                    TeamData data = list.get(i - 1);
                    StringBuilder builder = new StringBuilder();
                    for (PlayerProfile topProfile : data.players) {
                        builder.append(topProfile.getFormattedNameWithRoman()).append("&7, ");
                    }

                    player.sendMessage(CC.translate(" &e&l#" + i + " " + data.chatColor + data.charId + " &b" + data.score + "积分 &7(" + builder + ")"));
                }
            }
        }
        Bukkit.broadcastMessage(CC.CHAT_BAR);
        clearState();
    }

    private void stopRuntime() {
        state = State.PREPARE;
        if (logicRunnable != null) logicRunnable.cancel();
        if (effectRunnable != null) effectRunnable.cancel();
        logicRunnable = effectRunnable = null;
        HandlerList.unregisterAll(this);
    }

    @Override public java.util.concurrent.CompletableFuture<Void> restore() {
        try {
            var iterator = originalBanners.entrySet().iterator();
            while (iterator.hasNext()) {
                var block = iterator.next().getValue();
                if (!block.update(true, false)) throw new IllegalStateException("无法恢复旗帜位置: " + block.getLocation());
                iterator.remove();
            }
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        } catch (RuntimeException ex) { return java.util.concurrent.CompletableFuture.failedFuture(ex); }
    }

    private void clearState() {
        playerMap.clear(); teamMap.clear(); banners.clear(); usedChars.clear(); unFullTeams.clear();
        unusedColor.clear(); unusedColor.addAll(colors);
    }

    @Override public void onAbort() { stopRuntime(); clearState(); }

    @Override
    public void onPreActive() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.playerMap.put(player.getUniqueId(), new PlayerData(player.getUniqueId()));
        }
    }

    private void refreshRank() {
        Set<TeamData> teams = new HashSet<>(teamMap.values());
        List<TeamData> sortedTeam = new ArrayList<>(teams);
        sortedTeam.sort((a, b) -> Integer.compare(b.score, a.score));

        for (int index = 0; index < sortedTeam.size(); index++) {
            sortedTeam.get(index).rank = index + 1;
        }
    }

    @EventHandler
    public void onKill(PitKillEvent event) {
        Player killer = event.getKiller();
        TeamData data = teamMap.get(killer.getUniqueId());
        if (data == null) return;
        data.score += 25;

        killer.sendMessage(CC.translate("&b&l击杀! &7您通过击杀获得了 &b25积分&7 !"));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (originalBanners.containsKey(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = this.playerMap.get(player.getUniqueId());

        if (data == null) {
            PlayerData playerData = new PlayerData(player.getUniqueId());
            this.playerMap.put(player.getUniqueId(), playerData);
        }

        if (state == State.GAMING) {
            TeamData teamData = unFullTeams.isEmpty() ? null : unFullTeams.iterator().next();
            if (teamData == null) {
                TeamData team = createTeam();
                teamMap.put(player.getUniqueId(), team);
                teamData = team;
            }

            addPlayerToTeam(teamData, player);
        }
    }

    private void addPlayerToTeam(TeamData team, Player player) {
        int size = team.players.size();
        if (size >= 3) {
            return;
        }

        teamMap.put(player.getUniqueId(), team);
        team.players.add(PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()));
        if (team.players.size() >= 3) {
            unFullTeams.remove(team);
        }
    }

    private TeamData createTeam() {
        TeamData teamData = new TeamData(nextChar());

        if (unusedColor.isEmpty()) {
            unusedColor.addAll(colors);
        }

        int idx = ThreadLocalRandom.current().nextInt(unusedColor.size());
        teamData.chatColor = unusedColor.get(idx);
        unusedColor.remove(idx);
        unFullTeams.add(teamData);

        return teamData;
    }

    private char nextChar() {
        int used = usedChars.size();
        char result = (char) (startChars + used);

        usedChars.add(result);
        return result;
    }

    @Override
    public List<String> insert(Player player) {
        TeamData data = teamMap.get(player.getUniqueId());
        if (data == null) return new ArrayList<>();

        List<String> list = new ArrayList<>();
        list.add("&f剩余时间: &a" + TimeUtil.millisToTimer(timer.getRemaining()));
        list.add("&7[" + data.chatColor + data.charId + "&7] &e#" + data.rank + ", &b" + format.format(data.score) + " 总积分");
        list.add("&f+&b" + data.getScorePerSecond() + "积分&f/秒, &a" + data.banners.size() + "&f 旗帜已占领");
        return list;
    }

    @Override
    public int getRank(UUID playerUUID) {
        TeamData team = teamMap.get(playerUUID);
        if (team == null) return 100000;
        return ((int) 'Z') - ((int) team.charId);
    }

    public enum BannerStatus {
        CAPTURED,
        CAPTURING,
        FIGHTING,
        FREE
    }

    public enum State {
        PREPARE,
        GAMING
    }

    public static class BannerData {

        public static final SimpleDateFormat timeFormat = new SimpleDateFormat("mm分ss秒");
        public static final int MAX_CAPTURING_STATE = 5;

        public static final Map<ChatColor, Color> chatColorToColor;
        public static final Map<ChatColor, Integer> chatColorToData;

        static {
            Map<ChatColor, Color> colorMap = new HashMap<>();
            colorMap.put(ChatColor.BLACK, Color.BLACK);
            colorMap.put(ChatColor.RED, Color.RED);
            colorMap.put(ChatColor.DARK_GREEN, Color.GREEN);
            colorMap.put(ChatColor.DARK_BLUE, Color.BLUE);
            colorMap.put(ChatColor.DARK_PURPLE, Color.MAROON);
            colorMap.put(ChatColor.DARK_AQUA, Color.fromRGB(0, 170, 170));
            colorMap.put(ChatColor.DARK_GRAY, Color.fromRGB(85, 85, 85));
            colorMap.put(ChatColor.LIGHT_PURPLE, Color.fromRGB(255, 85, 255));
            colorMap.put(ChatColor.GREEN, Color.LIME);
            colorMap.put(ChatColor.YELLOW, Color.YELLOW);
            colorMap.put(ChatColor.AQUA, Color.AQUA);
            colorMap.put(ChatColor.GOLD, Color.ORANGE);
            chatColorToColor = Collections.unmodifiableMap(colorMap);

            Map<ChatColor, Integer> dataMap = new HashMap<>();
            dataMap.put(ChatColor.BLACK, 0);
            dataMap.put(ChatColor.RED, 1);
            dataMap.put(ChatColor.DARK_GREEN, 2);
            dataMap.put(ChatColor.DARK_BLUE, 4);
            dataMap.put(ChatColor.DARK_PURPLE, 5);
            dataMap.put(ChatColor.DARK_AQUA, 6);
            dataMap.put(ChatColor.DARK_GRAY, 8);
            dataMap.put(ChatColor.LIGHT_PURPLE, 9);
            dataMap.put(ChatColor.GREEN, 10);
            dataMap.put(ChatColor.YELLOW, 11);
            dataMap.put(ChatColor.AQUA, 12);
            dataMap.put(ChatColor.GOLD, 14);
            chatColorToData = Collections.unmodifiableMap(dataMap);
        }

        public final Location location;

        public TeamData capturedTeam = null;
        public TeamData capturingTeam = null;
        public int capturingState = 0;
        public BannerStatus bannerStatus = BannerStatus.FREE;

        public long capturedStartAt = -1L;

//        var firstLine = DHAPI.createHologram(
//            location.clone().add(0.0, 2.5, 0.0), "&f&l站在周围占领旗帜".colored()
//        ).also {
//            it.spawn()
//        }
//
//        var secondLine = HologramAPI.createHologram(
//            location.clone().add(0.0, 2.8, 0.0),
//            "&f无人占领".colored()
//        ).also {
//            it.spawn()
//        }

//        var thirdLine = HologramAPI.createHologram(
//            location.clone().add(0.0, 2.25, 0.0),
//            ""
//        ).also {
//            it.spawn()
//        }

        public BannerData(Location location) {
            this.location = location;
        }

        public void refreshBannerLogic(SquadsEvents event) {
            try {
                World world = location.getWorld();
                if (world == null) return;
                List<Player> players = new ArrayList<>();
                for (org.bukkit.entity.Entity entity : world.getNearbyEntities(location, 3, 3, 3))
                    if (entity instanceof Player player) players.add(player);
                Set<TeamData> teams = new HashSet<>();
                boolean isOtherTeamPresent = false;

                for (Player player : players) {
                    TeamData teamData = event.teamMap.get(player.getUniqueId());
                    if (teamData == null) continue;
                    teams.add(teamData);
                    if (teamData != this.capturedTeam) {
                        isOtherTeamPresent = true;
                    }
                }

                if (teams.size() > 1) {
                    this.bannerStatus = BannerStatus.FIGHTING;
                } else if (isOtherTeamPresent && !teams.isEmpty()) {
                    TeamData capturingTeam = teams.iterator().next();
                    if (this.capturingTeam != capturingTeam) {
                        this.capturingTeam = capturingTeam;
                    }

                    this.capturingState++;
                    this.bannerStatus = BannerStatus.CAPTURING;

                    if (capturingState > 5) {
                        MajorEventTasks.later(event, new Runnable() {
                            @SuppressWarnings("deprecation")
                            @Override
                            public void run() {
                                org.bukkit.block.BlockState state = location.getBlock().getState();
                                if (state instanceof Banner banner) {
                                    Integer data = chatColorToData.get(capturingTeam.chatColor);
                                    if (data != null) {
                                        DyeColor dye = DyeColor.getByDyeData(data.byteValue());
                                        if (dye != null) {
                                            banner.setBaseColor(dye);
                                            banner.update(true, false);
                                        }
                                    }
                                }
                            }
                        }, 0L);

                        String gotPlayer = PlayerProfile.getPlayerProfileByUuid(players.get(0).getUniqueId()).getFormattedLevelTag();

                        if (capturedTeam != null && capturedTeam.players != null) {
                            for (PlayerProfile player : capturedTeam.players) {
                                Player bukkitPlayer = Bukkit.getPlayer(player.getPlayerUUID());
                                if (bukkitPlayer != null) {
                                    bukkitPlayer.sendMessage(CC.translate("&b&l夺旗! &c-&c&l旗帜&a! &7被 " + gotPlayer + " &7偷走了一面旗帜!"));
                                }
                            }
                        }

                        for (PlayerProfile player : capturingTeam.players) {
                            Player bukkitPlayer = Bukkit.getPlayer(player.getPlayerUUID());
                            if (bukkitPlayer != null) {
                                bukkitPlayer.sendMessage(CC.translate("&b&l夺旗! &a+&a&l旗帜&a! &7" + gotPlayer + " &7拿到了一面旗帜!"));
                            }
                        }

                        if (this.capturedTeam != null) {
                            this.capturedTeam.banners.remove(this);
                        }

                        this.bannerStatus = BannerStatus.CAPTURED;
                        this.capturingState = 0;
                        this.capturingTeam = null;
                        this.capturedTeam = capturingTeam;
                        this.capturedStartAt = System.currentTimeMillis();
                        setCapturedHologram();

                        capturingTeam.banners.add(this);

                        MajorEventTasks.later(event, new Runnable() {
                            @Override
                            public void run() {
                                BoomFirework firework = new BoomFirework(((CraftWorld) location.getWorld()).getHandle());
                                org.bukkit.inventory.meta.FireworkMeta meta = ((Firework) firework.getBukkitEntity()).getFireworkMeta();
                                meta.addEffect(org.bukkit.FireworkEffect.builder()
                                        .withColor(chatColorToColor.get(capturingTeam.chatColor))
                                        .with(org.bukkit.FireworkEffect.Type.BURST)
                                        .build());
                                ((Firework) firework.getBukkitEntity()).setFireworkMeta(meta);
                                firework.setPos(location.getX(), location.getY(), location.getZ());
                                if (((CraftWorld) location.getWorld()).getHandle().addFreshEntity(firework)) {
                                    firework.setInvisible(true);
                                }
                            }
                        }, 0L);
                    }
                } else {
                    this.capturingState = 0;
                    this.capturingTeam = null;
                    this.bannerStatus = BannerStatus.CAPTURED;
                    if (this.capturedTeam != null) {
                        long delta = System.currentTimeMillis() - this.capturedStartAt;
                        int per = Math.min(4, Math.max(2, (int) (delta / (1000 * 60L))));
                        this.capturedTeam.score = this.capturedTeam.score + per;
                    }
                }

                refreshBannerHolograms();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void drawParticle() {
            if (bannerStatus == BannerStatus.FIGHTING) {
                ParticleCircleUtil.drawParticle(location.clone(), BannerStatus.CAPTURING, capturingTeam, chatColorToColor);
            } else if (bannerStatus == BannerStatus.CAPTURING && capturingTeam != null) {
                // no-op (保持与原 Kotlin 逻辑一致)
            }
        }

        public void refreshBannerHolograms() {
//            if (bannerStatus == BannerStatus.FIGHTING) {
//                if (capturedTeam != null) {
//                    setCapturedHologram()
//                }
//                thirdLine.text = "&c争夺中!!!".colored()
//            } else if (bannerStatus == BannerStatus.CAPTURING) {
//                if (capturedTeam != null) {
//                    setCapturedHologram()
//                }
//                if (capturingTeam != null) {
//                    val color = capturingTeam!!.chatColor
//                    val char = capturingTeam!!.char
//                    val builder = StringBuilder("&7[${color}${char}&7] &7[${color}")
//                    for (index in 0 until capturingState) {
//                        builder.append("●")
//                    }
//                    builder.append("&7")
//                    for (index in 0 until MAX_CAPTURING_STATE - capturingState) {
//                        builder.append("●")
//                    }
//                    builder.append("&7]")
//
//                    thirdLine.text = builder.toString().colored()
//                }
//            } else if (bannerStatus == BannerStatus.CAPTURED) {
//                setCapturedHologram()
//                thirdLine.text = ""
//            }
        }

        public void setCapturedHologram() {
//            val teamData = capturedTeam ?: return
//
//            val color = teamData.chatColor
//            firstLine.text = "${color}由 &7[${color}${teamData.char}&7]${color} 队伍占领".colored()
//            val capturedTime = System.currentTimeMillis() - capturedStartAt
//
//            secondLine.text = "&f${timeFormat.format(capturedTime)} &6| &f+${
//                (max(
//                    2,
//                    min(4, (System.currentTimeMillis() - this.capturedStartAt) / (1000 * 60L)).toInt()
//                ))
//            } &b积分/秒".colored()
        }

        public void setNoneHologram() {
//            firstLine.text = "&f&l站在周围占领旗帜".colored()
//            secondLine.text = "&f无人占领".colored()
        }
    }

    public static class PlayerData {
        public final UUID uuid;
        public final Map<UUID, Cooldown> invites = new HashMap<>();

        public PlayerData(UUID uuid) {
            this.uuid = uuid;
        }
    }

    public static class TeamData {
        public final Set<PlayerProfile> players = new HashSet<>();
        public final Set<BannerData> banners = new HashSet<>();
        public char charId;
        public int rank = -1;
        public int score = 0;
        public ChatColor chatColor;

        public TeamData(char c) {
            this.charId = c;
        }

        public int getScorePerSecond() {
            int sum = 0;
            long now = System.currentTimeMillis();
            for (BannerData b : banners) {
                long delta = now - b.capturedStartAt;
                int v = Math.min(4, Math.max(2, (int) (delta / (1000 * 60L))));
                sum += v;
            }
            return sum;
        }
    }

    /**
     * 任意半径圆环粒子绘制工具，已兼容 1.20.6
     */
    public static class ParticleCircleUtil {

        /**
         * 圆环半径（方块）
         */
        private static final double RADIUS = 3.0;

        /**
         * 圆环精度（角度间隔，度）
         */
        private static final int STEP = 3;

        /* ---------- 对外入口 ---------- */

        public static void drawParticle(
                Location location,
                BannerStatus bannerStatus,
                TeamData capturingTeam,
                Map<ChatColor, Color> chatColorToColor
        ) {
            switch (bannerStatus) {
                case FIGHTING:
                    drawCircle(location, ParticleTypes.FLAME);
                    break;
                case CAPTURING:
                    if (capturingTeam != null) {
                        Color color = chatColorToColor.get(capturingTeam.chatColor);
                        if (color != null) {
                            DustParticleOptions particle = colorToDust(color);
                            drawCircle(location, particle);
                        }
                    }
                    break;
                default:
                    // no-op
                    break;
            }
        }

        /* ---------- 内部实现 ---------- */

        /**
         * 把 Bukkit Color 转为红石尘粒子的 DustParticleOptions
         */
        private static DustParticleOptions colorToDust(Color color) {
            float r = Math.max(1f / 255f, color.getRed() / 255f);
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            return new DustParticleOptions(new Vector3f(r, g, b), 1.0f); // 最后一个参数是粒子大小
        }

        /**
         * 在给定位置绘制圆环
         */
        private static void drawCircle(Location origin, ParticleOptions particle) {
            double worldY = origin.getY();
            List<ServerPlayer> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                players.add(((CraftPlayer) p).getHandle());
            }
            if (players.isEmpty()) return;

            for (int degree = 0; degree < 360; degree += STEP) {
                double rad = Math.toRadians(degree);
                double dx = Math.cos(rad) * RADIUS;
                double dz = Math.sin(rad) * RADIUS;
                double x = origin.getX() + dx;
                double z = origin.getZ() + dz;

                ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                        particle,          // 粒子类型
                        true,              // overrideLimiter（强制可见距离）
                        x, worldY, z,      // 绝对坐标
                        0f, 0f, 0f,        // xDist, yDist, zDist（0 = 无扩散）
                        0f,                // maxSpeed (同旧 offsetSpeed)
                        0                  // count; 0 = 单粒子
                );

                for (ServerPlayer sp : players) {
                    sp.connection.send(packet);
                }
            }
        }
    }
}
