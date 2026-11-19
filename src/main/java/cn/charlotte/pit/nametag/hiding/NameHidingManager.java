package cn.charlotte.pit.nametag.hiding;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitConsumeItemEvent;
import cn.charlotte.pit.perk.MegaStreak;
import cn.charlotte.pit.perk.PerksBundlesEnum;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemUtil;
import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Level;

/** Bukkit/rules run on the main thread; packet callbacks only use immutable snapshots and per-viewer caches. */
public final class NameHidingManager implements Listener {
    public static final String REVEAL_ITEM = "name_reveal_lens";
    private static volatile NameHidingManager instance;
    private final ThePit plugin;
    private final NameHidingUpdates updates = new NameHidingUpdates();
    private final NameHidingAliases aliases = new NameHidingAliases();
    private final NameHidingPackets packets;
    private final Map<UUID, Long> revealUntil = new HashMap<>();
    private NameHidingPolicy policy = NameHidingPolicy.OFF;
    private NameHidingRules.Regions regions;
    private NameHidingConfig config;
    private BukkitTask task;
    private boolean registered, faulted;
    private int untilCheck;
    private long nextErrorLog;

    public NameHidingManager(ThePit plugin) {
        this.plugin = plugin;
        this.packets = new NameHidingPackets(updates, plugin.getLogger());
    }
    public static NameHidingManager getInstance() { return instance; }
    public NameHidingConfig config() { return config; }
    public int pendingUpdates() { return updates.size(); }
    public boolean faulted() { return faulted; }

    public void start() throws Exception {
        reload();
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        PacketEvents.getAPI().getEventManager().registerListener(packets);
        registered = true;
        // Normally no players are online during onEnable. Adopting an existing connection is
        // handled by its next source player-info update, never by blindly resurrecting vanished players.
        refreshSafely();
    }
    public void reload() throws Exception {
        NameHidingConfig loaded = NameHidingConfig.load(plugin);
        NameHidingRules.Regions loadedRegions = readRegions();
        if (task != null) task.cancel();
        config = loaded;
        regions = loadedRegions;
        faulted = false;
        packets.resetFailure();
        untilCheck = 0;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickSafely, 1L, 1L);
        if (registered) { refreshSafely(); packets.markAll(config.changeDelayTicks()); }
    }

    public static void suspendForEvent() {
        NameHidingManager manager = instance;
        if (manager == null) return;
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(manager.plugin, NameHidingManager::suspendForEvent);
            return;
        }
        manager.publish(new NameHidingPolicy(manager.policy.enabled(), manager.policy.aliases(), Set.of(), manager.policy.revealed()));
    }
    /** Post-assignment hook. Reaching a threshold schedules one coalesced update, not one refresh per kill. */
    public static void revealIfExempt(PlayerProfile profile) {
        NameHidingManager manager = instance;
        if (manager == null || !profile.isLoaded()) return;
        try {
            if (!Bukkit.isPrimaryThread()) {
                if (manager.plugin.isEnabled()) Bukkit.getScheduler().runTask(manager.plugin, () -> revealIfExempt(profile));
                return;
            }
            UUID target = profile.getPlayerUUID();
            if (!manager.policy.hidden().contains(target) || PlayerProfile.getCacheProfile().get(target) != profile) return;
            if (manager.config.rules().showsByStats(profile.getStreakKills(), profile.getBounty()) || megaActive(profile)) {
                Set<UUID> hidden = new HashSet<>(manager.policy.hidden());
                hidden.remove(target);
                manager.publish(new NameHidingPolicy(manager.policy.enabled(), manager.policy.aliases(), hidden, manager.policy.revealed()));
            }
        } catch (Exception | LinkageError exception) { manager.failOpen(exception); }
    }

    private NameHidingRules.Regions readRegions() {
        var pit = plugin.getPitConfig();
        Location a = pit.getPitLocA(), b = pit.getPitLocB(), center = pit.getRagePitMiddle();
        NameHidingRules.Box spawn = null;
        if (a != null && b != null && a.getWorld() != null && a.getWorld().equals(b.getWorld())) {
            spawn = new NameHidingRules.Box(a.getWorld().getName(), Math.min(a.getBlockX(), b.getBlockX()),
                    Math.min(a.getBlockY(), b.getBlockY()), Math.min(a.getBlockZ(), b.getBlockZ()),
                    Math.max(a.getBlockX(), b.getBlockX()), Math.max(a.getBlockY(), b.getBlockY()), Math.max(a.getBlockZ(), b.getBlockZ()));
        }
        if (center != null && center.getWorld() != null)
            return new NameHidingRules.Regions(center.getWorld().getName(), spawn, center.getBlockX(), center.getBlockY(),
                    center.getBlockZ(), pit.getRagePitRadius(), pit.getRagePitHeight());
        return new NameHidingRules.Regions(spawn == null ? null : spawn.world(), spawn, 0, 0, 0, 0, 0);
    }
    private boolean eventActive() {
        var factory = plugin.getEventFactory();
        return factory == null || factory.getActiveNormalEvent() != null || factory.getActiveEpicEvent() != null || factory.isMajorEventBusy();
    }
    static boolean megaActive(PlayerProfile profile) {
        String[] selected = profile.getChosePerk();
        if (selected == null || selected.length <= 4 || selected[4] == null) return false;
        try {
            var perk = PerksBundlesEnum.valueOf(selected[4]).getPerk();
            return perk instanceof MegaStreak mega && NameHidingRules.megaThresholdReached(profile.getStreakKills(), mega.getStreakNeed());
        } catch (IllegalArgumentException ignored) { return false; }
    }

    private void tickSafely() {
        try {
            updates.advance();
            if (packets.failed() && !faulted) failOpen(null);
            if (--untilCheck <= 0) { refreshSafely(); untilCheck = config.refreshTicks(); }
            if (updates.size() == 0) return;
            NameHidingRefresh batch = new NameHidingRefresh();
            int profiles = 0;
            for (int i = 0; i < config.packetUpdatesPerTick(); i++) {
                var pair = updates.poll();
                if (pair == null) break;
                Player viewer = Bukkit.getPlayer(pair.viewer()), target = Bukkit.getPlayer(pair.target());
                if (viewer == null || target == null || !viewer.canSee(target)) continue;
                if (packets.needsIdentity(pair.viewer(), pair.target()) || (faulted && packets.aliased(pair.viewer(), pair.target()))) {
                    if (profiles >= config.profileRefreshesPerTick()) {
                        updates.mark(pair.viewer(), pair.target(), 1);
                        continue;
                    }
                    profiles++;
                }
                if (faulted) packets.restore(pair.viewer(), pair.target(), send -> batch.run(viewer, target, send));
                else packets.flush(pair.viewer(), pair.target(), send -> batch.run(viewer, target, send));
            }
        } catch (Exception | LinkageError exception) { failOpen(exception); }
    }
    private void refreshSafely() {
        try { refresh(); } catch (Exception | LinkageError exception) { failOpen(exception); }
    }
    private void refresh() {
        long now = System.currentTimeMillis();
        revealUntil.entrySet().removeIf(entry -> entry.getValue() <= now);
        Map<UUID, String> identities = new HashMap<>();
        Set<UUID> hidden = new HashSet<>();
        boolean event = eventActive();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasMetadata("NPC")) continue;
            UUID uuid = player.getUniqueId();
            identities.put(uuid, aliases.name(uuid));
            if (faulted || !config.rules().enabled() || event || player.isDead() || player.getGameMode() == GameMode.SPECTATOR) continue;
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(uuid);
            if (!profile.isLoaded()) continue;
            Location at = player.getLocation();
            var area = regions.area(player.getWorld().getName(), at.getX(), at.getY(), at.getZ());
            if (config.rules().hides(area, profile.getPrestige(), profile.getStreakKills(), profile.getBounty(),
                    event, megaActive(profile), false, false)) hidden.add(uuid);
        }
        publish(new NameHidingPolicy(config.rules().enabled() && !faulted, identities, hidden, revealUntil.keySet()));
    }
    private void publish(NameHidingPolicy next) {
        NameHidingPolicy before = policy;
        policy = next;
        packets.policy(next);
        int delay = config.changeDelayTicks();
        if (before.enabled() != next.enabled()) { packets.markAll(delay); return; }
        Set<UUID> targets = new HashSet<>(before.hidden());
        targets.addAll(next.hidden());
        targets.addAll(next.aliases().keySet());
        targets.removeIf(target -> before.hidden().contains(target) == next.hidden().contains(target)
                && Objects.equals(before.aliases().get(target), next.aliases().get(target)));
        for (UUID target : targets) packets.markTarget(target, delay);
        Set<UUID> observers = new HashSet<>(before.revealed());
        observers.addAll(next.revealed());
        observers.removeIf(observer -> before.revealed().contains(observer) == next.revealed().contains(observer));
        for (UUID observer : observers) packets.markViewer(observer, delay);
    }
    private void failOpen(Throwable exception) {
        faulted = true;
        publish(new NameHidingPolicy(false, policy.aliases(), Set.of(), Set.of()));
        packets.markAll(0);
        if (System.currentTimeMillis() >= nextErrorLog) {
            nextErrorLog = System.currentTimeMillis() + 5000;
            plugin.getLogger().log(Level.WARNING, "匿名名牌处理异常，已暂停隐藏并安排恢复显示；可修正后 /namehide reload", exception);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) { untilCheck = 0; }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        revealUntil.remove(uuid);
        Map<UUID, String> remaining = new HashMap<>(policy.aliases());
        Set<UUID> hidden = new HashSet<>(policy.hidden());
        remaining.remove(uuid);
        hidden.remove(uuid);
        publish(new NameHidingPolicy(policy.enabled(), remaining, hidden, revealUntil.keySet()));
        updates.remove(uuid);
        packets.removePlayer(uuid);
        aliases.remove(uuid);
        untilCheck = 0;
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick() || event.getHand() == null || event.getItem() == null
                || event.getItem().getType() != Material.ENDER_EYE || !REVEAL_ITEM.equals(ItemUtil.getInternalName(event.getItem()))) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (faulted || !config.rules().enabled() || !config.revealItemEnabled()) {
            player.sendMessage(CC.translate("&c当前未开启玩家名隐藏/识名道具功能，未消耗道具。"));
            return;
        }
        long now = System.currentTimeMillis(), remaining = revealUntil.getOrDefault(player.getUniqueId(), 0L) - now;
        if (remaining > 0) {
            player.sendMessage(CC.translate("&e识名效果剩余 " + (remaining / 1000 + 1) + " 秒，未重复消耗。"));
            return;
        }
        var held = player.getInventory().getItem(event.getHand());
        if (held.getAmount() < 1 || !REVEAL_ITEM.equals(ItemUtil.getInternalName(held))) return;
        long expiry = NameHidingRules.revealExpiry(now, config.durationSeconds());
        PlayerUtil.takeOneItem(player, event.getHand());
        revealUntil.put(player.getUniqueId(), expiry);
        refreshSafely();
        new PitConsumeItemEvent(player, REVEAL_ITEM).callEvent();
        player.sendMessage(CC.translate("&a已使用显形药水，接下来 " + config.durationSeconds() + " 秒可看到完整玩家名牌（稍后刷新）。"));
    }

    public void close() {
        if (task != null) { task.cancel(); task = null; }
        packets.policy(NameHidingPolicy.OFF);
        if (registered) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packets);
            HandlerList.unregisterAll(this);
            registered = false;
        }
        // A stopping server disconnects these clients; do not spend a full-server refresh during shutdown saves.
        if (!Bukkit.getServer().isStopping()) {
            NameHidingRefresh batch = new NameHidingRefresh();
            for (UUID observer : packets.viewerIds()) {
                Player viewer = Bukkit.getPlayer(observer);
                if (viewer == null) continue;
                for (UUID target : packets.aliasedTargets(observer)) {
                    try { packets.restore(observer, target, send -> batch.run(viewer, Bukkit.getPlayer(target), send)); }
                    catch (Exception | LinkageError exception) {
                        plugin.getLogger().log(Level.WARNING, "恢复客户端原始名牌失败，重新连接可恢复", exception);
                    }
                }
            }
        }
        packets.clear();
        revealUntil.clear();
        if (instance == this) instance = null;
    }
}
