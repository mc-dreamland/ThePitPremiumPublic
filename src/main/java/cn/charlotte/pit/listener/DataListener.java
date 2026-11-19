package cn.charlotte.pit.listener;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.FixedRewardData;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.event.PitProfileLoadedEvent;
import cn.charlotte.pit.menu.Help.button.UpdateMenu;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.runnable.BountyRunnable;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.level.LevelUtil;
import cn.charlotte.pit.util.rank.LuckPermsUtil;
import cn.charlotte.pit.util.rank.RankUtil;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Author: EmptyIrony
 * @Date: 2020/12/30 22:27
 */
@AutoRegister
@Slf4j
public class DataListener implements Listener {
    private static final long SAVE_BARRIER_TTL_SECONDS = 120L;

    private final ScheduledExecutorService executor;

    private final Map<UUID, BukkitRunnable> loadingMap = new ConcurrentHashMap<>();
    private final Set<UUID> pendingSaves = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Player> saveBarrierBypassSessions = new ConcurrentHashMap<>();

    public DataListener() {
        this.executor = Executors.newSingleThreadScheduledExecutor();
        JedisPool jedisPool = ThePit.getInstance().getJedis();
        if (jedisPool != null) {
            executor.scheduleAtFixedRate(() -> {
                String databaseName = ThePit.getInstance().getPitConfig().getDatabaseName();

                try (Jedis jedis = jedisPool.getResource()) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (loadingMap.containsKey(player.getUniqueId())) {
                            continue;
                        }

                        jedis.expire(sessionLockKey(databaseName, player.getUniqueId()), 15);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 10L, 10L, TimeUnit.SECONDS);
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        LuckPermsUtil.invalidatePrefixCache(player.getUniqueId());
        PlayerUtil.clearPlayer(player, true, true);

        UUID playerUUID = event.getPlayer().getUniqueId();
        if (canBypassSaveBarrier(player)) {
            saveBarrierBypassSessions.put(playerUUID, player);
        } else {
            saveBarrierBypassSessions.remove(playerUUID);
        }
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                Player player = Bukkit.getPlayer(playerUUID);
                if (player == null || !player.isOnline()) {
                    cancel();
                    return;
                }

                if (canLoad(player)) {
                    cancel();
                    load(player);
                }

                //continue waiting
            }
        };

        runnable.runTaskTimerAsynchronously(ThePit.getInstance(), 1L, 1L);

        loadingMap.put(player.getUniqueId(), runnable);

        event.setJoinMessage(null);

        Bukkit.getScheduler().runTaskLaterAsynchronously(ThePit.getInstance(), () -> {
            if (player.isOp()) {
                return;
            }
            // 可能是外挂、异常账号、bot账号的标签，拥有这些标签的玩家降低收益、禁止触发连杀。
            if (player.hasPermission("meta.clienttype.badaccount") || player.hasPermission("meta.modpc.1") || player.hasPermission("meta.thirdparty.1")
                    || player.hasPermission("meta.env.sandbox")
                    || player.hasPermission("meta.queuepool.hell") || ((player.hasPermission("meta.queuepool.CROSS") || player.hasPermission("meta.queuepool.cross")) && !playerUUID.toString().startsWith("000000"))) {
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(playerUUID);
                if (profile != null) {
                    profile.setMaybeBot(true);
                    ThePit.getInstance().getLogger().info(player.getName() + " 可能是BOT！");
                }
            }
        }, 120L);

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);

        UUID playerUuid = event.getPlayer().getUniqueId();
        LuckPermsUtil.invalidatePrefixCache(playerUuid);
        saveBarrierBypassSessions.remove(playerUuid, event.getPlayer());

        BukkitRunnable loadingFuture = loadingMap.remove(playerUuid);
        if (loadingFuture != null) {
            try {
                loadingFuture.cancel();
            } catch (Exception ignored) {

            }
            return;
        }

        if (/*!PlayerUtil.isStaffSpectating(event.getPlayer()) && */PlayerProfile.getCacheProfile().containsKey(event.getPlayer().getUniqueId())) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getPlayer().getUniqueId());

            if (profile.isLoaded()) {
                if (profile.isScreenShare()) {
                    CC.boardCastWithPermission("&4&l查端时退出! &7玩家 " + LevelUtil.getLevelTagWithRoman(profile.getPrestige(), profile.getLevel()) + " " + RankUtil.getPlayerRealColoredName(event.getPlayer().getUniqueId() + " &7在查端时退出了游戏!"), PlayerUtil.getStaffPermission());
                }

                if (!profile.isTempInvUsing()) {
                    profile.setInventory(PlayerInv.fromPlayerInventory(event.getPlayer().getInventory()));
                }
                profile.setLastLogoutTime(System.currentTimeMillis());

                profile.setTotalPlayedTime(profile.getTotalPlayedTime() + profile.getLastLogoutTime() - profile.getLastLoginTime());
                //reset if data have an error (old bug)
                if (profile.getTotalPlayedTime() > System.currentTimeMillis() - profile.getRegisterTime()) {
                    profile.setTotalPlayedTime(0);
                }

                profile.setLogin(false);
                beginSaveBarrier(playerUuid);
                this.executor.execute(() -> saveAndRelease(playerUuid, profile));
                final BountyRunnable.AnimationData animationData = BountyRunnable.getAnimationDataMap().get(event.getPlayer().getUniqueId());
                if (animationData != null) {
                    for (BountyRunnable.HologramDisplay hologram : animationData.getHolograms()) {
                        hologram.getHologram().deSpawn();
                    }
                }
                BountyRunnable.getAnimationDataMap().remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler
    public void onShoot(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Arrow arrow) {
            if (arrow.getShooter() instanceof Player) {
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(((Player) arrow.getShooter()).getUniqueId());
                profile.setShootAttack(profile.getShootAttack() + 1);
            }
        } else if (event.getEntity() instanceof FishHook hook) {
            if (hook.getShooter() instanceof Player) {
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(((Player) hook.getShooter()).getUniqueId());
                profile.setRodUsed(profile.getRodUsed() + 1);
            }
        }
    }

    public boolean canLoad(Player player) {
        UUID playerUuid = player.getUniqueId();
        boolean bypassSaveBarrier = saveBarrierBypassSessions.get(playerUuid) == player;
        if (!bypassSaveBarrier && pendingSaves.contains(playerUuid)) {
            return false;
        }

        String databaseName = ThePit.getInstance().getPitConfig().getDatabaseName();
        JedisPool jedisPool = ThePit.getInstance().getJedis();
        if (jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                if (bypassSaveBarrier) {
                    return true;
                }
                if (jedis.exists(saveBarrierKey(databaseName, playerUuid))) {
                    return false;
                }
                return "OK".equals(jedis.set(
                        sessionLockKey(databaseName, playerUuid),
                        "locked", "NX", "EX", 30L
                ));
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        return bypassSaveBarrier || !pendingSaves.contains(playerUuid);
    }

    public void load(Player player) {
        if (Bukkit.getPlayer(player.getUniqueId()) == player && player.isOnline()) {
            PlayerProfile profile = new PlayerProfile(player.getUniqueId(), player.getName());

            PlayerProfile load = profile.load();

            load.setPlayerName(player.getName());
            load.setLowerName(player.getName().toLowerCase());

            if (!player.isOnline()) {
                return;
            }

            PlayerProfile.getCacheProfile().put(player.getUniqueId(), load);

            if (load.getRegisterTime() <= 1) {
                load.setRegisterTime(System.currentTimeMillis());
            }
            load.setLastLoginTime(System.currentTimeMillis());

            if (profile.getActivityPlayerData().getVer() != UpdateMenu.getVer()) {
                profile.getActivityPlayerData().clear();
            }

            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> new PitProfileLoadedEvent(PlayerProfile.getPlayerProfileByUuid(player.getUniqueId())).callEvent());

            if (player.isOnline()) {
                FixedRewardData.sendMail(load, player);
            }
        } else {
            PlayerProfile.getCacheProfile().remove(player.getUniqueId());
        }

        loadingMap.remove(player.getUniqueId());
        saveBarrierBypassSessions.remove(player.getUniqueId(), player);
    }

    private void beginSaveBarrier(UUID playerUuid) {
        pendingSaves.add(playerUuid);
        refreshRedisSaveBarrier(playerUuid);
    }

    private void saveAndRelease(UUID playerUuid, PlayerProfile profile) {
        try {
            profile.save(null);
        } catch (Exception exception) {
            log.error("Failed to save profile {} on quit; no automatic retry will be attempted",
                    playerUuid, exception);
        } finally {
            PlayerProfile.getCacheProfile().remove(playerUuid, profile);
            pendingSaves.remove(playerUuid);
            releaseRedisSaveBarrier(playerUuid);
        }
    }

    private void refreshRedisSaveBarrier(UUID playerUuid) {
        JedisPool jedisPool = ThePit.getInstance().getJedis();
        if (jedisPool == null) {
            return;
        }

        String databaseName = ThePit.getInstance().getPitConfig().getDatabaseName();
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(saveBarrierKey(databaseName, playerUuid), (int) SAVE_BARRIER_TTL_SECONDS, "saving");
            jedis.expire(sessionLockKey(databaseName, playerUuid), (int) SAVE_BARRIER_TTL_SECONDS);
        } catch (Exception exception) {
            log.error("Failed to create Redis save barrier for {}", playerUuid, exception);
        }
    }

    private void releaseRedisSaveBarrier(UUID playerUuid) {
        JedisPool jedisPool = ThePit.getInstance().getJedis();
        if (jedisPool == null) {
            return;
        }

        String databaseName = ThePit.getInstance().getPitConfig().getDatabaseName();
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(saveBarrierKey(databaseName, playerUuid));
            jedis.del(sessionLockKey(databaseName, playerUuid));
        } catch (Exception exception) {
            log.error("Unable to release Redis save barrier for profile {}", playerUuid, exception);
        }
    }

    private boolean canBypassSaveBarrier(Player player) {
        return player.hasPermission("pit.save.bypass")
                || player.hasPermission("pit.admin")
                || player.hasPermission("thepit.admin");
    }

    private static String sessionLockKey(String databaseName, UUID playerUuid) {
        return "THEPIT_" + databaseName + "_" + playerUuid;
    }

    private static String saveBarrierKey(String databaseName, UUID playerUuid) {
        return "THEPIT_SAVING_" + databaseName + "_" + playerUuid;
    }

}
