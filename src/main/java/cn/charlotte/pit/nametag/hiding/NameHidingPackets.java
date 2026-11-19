package cn.charlotte.pit.nametag.hiding;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.Action.*;
import static com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.*;

/**
 * Original packets are cached before rewriting; synthetic packets bypass this listener.
 * Stable invisible profile names mean ordinary visibility changes ONLY update team prefixes.
 * Network callbacks never access Bukkit/world/player/profile state.
 */
final class NameHidingPackets extends PacketListenerAbstract {
    private static final EnumSet<WrapperPlayServerPlayerInfoUpdate.Action> INITIALIZE =
            EnumSet.of(ADD_PLAYER, INITIALIZE_CHAT, UPDATE_GAME_MODE, UPDATE_LISTED, UPDATE_LATENCY, UPDATE_DISPLAY_NAME);
    private final Map<UUID, Viewer> viewers = new ConcurrentHashMap<>();
    private final NameHidingUpdates updates;
    private final Logger logger;
    private final AtomicBoolean failed = new AtomicBoolean();
    private volatile NameHidingPolicy policy = NameHidingPolicy.OFF;

    static final class Viewer {
        final User user;
        final Map<UUID, WrapperPlayServerPlayerInfoUpdate.PlayerInfo> profiles = new HashMap<>();
        final Map<String, UUID> byName = new HashMap<>();
        final Map<UUID, String> aliases = new HashMap<>();
        final Map<String, SourceTeam> teams = new HashMap<>();
        final Map<String, String> teamByName = new HashMap<>();
        final Map<UUID, Rendered> rendered = new HashMap<>();
        final Map<ScoreKey, Score> scores = new HashMap<>();
        Viewer(User user) { this.user = user; }
    }
    private record SourceTeam(Appearance info, Set<String> members) { }
    record Appearance(Component display, Component prefix, Component suffix, NameTagVisibility visibility,
                              CollisionRule collision, NamedTextColor color, OptionData options) {
        static Appearance of(ScoreBoardTeamInfo info) {
            return new Appearance(info.getDisplayName(), info.getPrefix(), info.getSuffix(), info.getTagVisibility(),
                    info.getCollisionRule(), info.getColor(), info.getOptionData());
        }
        ScoreBoardTeamInfo packet(Component newPrefix) {
            return new ScoreBoardTeamInfo(display, newPrefix, suffix, visibility, collision, color, options);
        }
        Component originalName(String name) {
            return Component.empty().color(color).append(prefix).append(Component.text(name)).append(suffix);
        }
        Component overheadPrefix(boolean hidden, String name) {
            // Name is a sibling, not a child of the prefix: it must inherit TEAM colour, not prefix colour.
            return hidden ? prefix : Component.empty().append(prefix).append(Component.text(name));
        }
    }
    private static final Appearance EMPTY = new Appearance(Component.empty(), Component.empty(), Component.empty(),
            NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, NamedTextColor.WHITE, OptionData.NONE);
    private record Rendered(String team, String name, Appearance raw, Component prefix, Component tab) { }
    private record ScoreKey(String owner, String objective) { }
    private record Score(int value, Component display, ScoreFormat format) {
        WrapperPlayServerUpdateScore packet(String owner, String objective, Component fallback) {
            return new WrapperPlayServerUpdateScore(owner, WrapperPlayServerUpdateScore.Action.CREATE_OR_UPDATE_ITEM,
                    objective, value, display == null ? fallback : display, format);
        }
    }

    NameHidingPackets(NameHidingUpdates updates, Logger logger) {
        super(PacketListenerPriority.HIGHEST);
        this.updates = updates;
        this.logger = logger;
    }
    void policy(NameHidingPolicy policy) { this.policy = policy; }
    boolean failed() { return failed.get(); }
    void resetFailure() { failed.set(false); }
    Set<UUID> viewerIds() { return Set.copyOf(viewers.keySet()); }
    Viewer viewer(User user) {
        return viewers.compute(user.getUUID(), (key, previous) ->
                previous == null || previous.user != user ? new Viewer(user) : previous);
    }

    @Override public void onUserDisconnect(UserDisconnectEvent event) {
        User user = event.getUser();
        if (user == null || user.getUUID() == null) return;
        Viewer old = viewers.get(user.getUUID());
        // A late disconnect from an old socket must not remove a newly reconnected player's cache.
        if (old != null && old.user == user && viewers.remove(user.getUUID(), old)) updates.remove(user.getUUID());
    }

    @Override public void onPacketSend(PacketSendEvent event) {
        if (event.isCancelled() || event.getUser() == null || event.getUser().getUUID() == null) return;
        var type = event.getPacketType();
        if (type != PacketType.Play.Server.PLAYER_INFO_UPDATE && type != PacketType.Play.Server.PLAYER_INFO_REMOVE
                && type != PacketType.Play.Server.TEAMS && type != PacketType.Play.Server.UPDATE_SCORE
                && type != PacketType.Play.Server.RESET_SCORE && type != PacketType.Play.Server.SCOREBOARD_OBJECTIVE) return;
        UUID id = event.getUser().getUUID();
        Viewer viewer = viewer(event.getUser());
        try {
            synchronized (viewer) {
                if (type == PacketType.Play.Server.PLAYER_INFO_UPDATE) {
                    var packet = new WrapperPlayServerPlayerInfoUpdate(event);
                    if (profile(id, viewer, packet)) event.markForReEncode(true);
                }
                else if (type == PacketType.Play.Server.PLAYER_INFO_REMOVE) {
                    for (UUID target : new WrapperPlayServerPlayerInfoRemove(event).getProfileIds()) {
                        // A vanish/plugin removal must NOT be undone by our refresh queue.
                        var removed = viewer.profiles.remove(target);
                        if (removed != null) viewer.byName.remove(removed.getGameProfile().getName(), target);
                    }
                } else if (type == PacketType.Play.Server.TEAMS) team(new WrapperPlayServerTeams(event), id, viewer);
                else if (type == PacketType.Play.Server.UPDATE_SCORE) {
                    var packet = new WrapperPlayServerUpdateScore(event);
                    if (score(packet, viewer)) event.markForReEncode(true);
                } else if (type == PacketType.Play.Server.RESET_SCORE) {
                    var packet = new WrapperPlayServerResetScore(event);
                    if (resetScore(packet, viewer)) event.markForReEncode(true);
                }
                else {
                    var objective = new WrapperPlayServerScoreboardObjective(event);
                    if (objective.getMode() == WrapperPlayServerScoreboardObjective.ObjectiveMode.REMOVE)
                        viewer.scores.keySet().removeIf(key -> key.objective.equals(objective.getName()));
                }
            }
        } catch (Exception | LinkageError exception) {
            if (failed.compareAndSet(false, true))
                logger.log(Level.WARNING, "匿名名牌数据包处理失败，将在主线程恢复原始玩家信息", exception);
        }
    }

    boolean profile(UUID observer, Viewer viewer, WrapperPlayServerPlayerInfoUpdate packet) {
        var actions = packet.getActions();
        var outputActions = actions.clone();
        var rewritten = new ArrayList<WrapperPlayServerPlayerInfoUpdate.PlayerInfo>(packet.getEntries().size());
        boolean changed = false;
        for (var incoming : packet.getEntries()) {
            UUID target = incoming.getProfileId();
            var previous = viewer.profiles.get(target);
            if (previous == null && !actions.contains(ADD_PLAYER)) { rewritten.add(incoming); continue; }
            var source = merge(previous, incoming, actions);
            viewer.profiles.put(target, source);
            String name = source.getGameProfile().getName();
            if (previous != null && !Objects.equals(previous.getGameProfile().getName(), name))
                viewer.byName.remove(previous.getGameProfile().getName(), target);
            viewer.byName.put(name, target);
            String alias = viewer.aliases.get(target);
            // First ADD is safe to rewrite directly; an already-created client entity needs a one-time refresh.
            if (alias == null && previous == null && actions.contains(ADD_PLAYER)) {
                alias = policy.alias(observer, target);
                if (alias != null) viewer.aliases.put(target, alias);
            }
            var output = copy(incoming);
            if (alias != null) {
                if (actions.contains(ADD_PLAYER)) {
                    output.setGameProfile(new UserProfile(target, alias, new ArrayList<>(source.getGameProfile().getTextureProperties())));
                    outputActions.add(UPDATE_DISPLAY_NAME);
                }
                output.setDisplayName(tab(viewer, source));
                changed = true;
                if (actions.contains(ADD_PLAYER) || actions.contains(UPDATE_DISPLAY_NAME)) updates.mark(observer, target, 0);
            } else {
                output.setDisplayName(source.getDisplayName());
            }
            rewritten.add(output);
        }
        if (changed) { packet.setEntries(rewritten); packet.setActions(outputActions); }
        return changed;
    }

    static WrapperPlayServerPlayerInfoUpdate.PlayerInfo copy(WrapperPlayServerPlayerInfoUpdate.PlayerInfo source) {
        var profile = source.getGameProfile();
        return new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(new UserProfile(profile.getUUID(), profile.getName(),
                new ArrayList<>(profile.getTextureProperties())), source.isListed(), source.getLatency(), source.getGameMode(),
                source.getDisplayName(), source.getChatSession(), source.getListOrder(), source.isShowHat());
    }
    static WrapperPlayServerPlayerInfoUpdate.PlayerInfo merge(WrapperPlayServerPlayerInfoUpdate.PlayerInfo old,
            WrapperPlayServerPlayerInfoUpdate.PlayerInfo incoming, Set<WrapperPlayServerPlayerInfoUpdate.Action> actions) {
        var result = copy(old == null ? incoming : old);
        if (actions.contains(ADD_PLAYER)) {
            var profile = incoming.getGameProfile();
            result.setGameProfile(new UserProfile(profile.getUUID(), profile.getName(), new ArrayList<>(profile.getTextureProperties())));
        }
        if (actions.contains(INITIALIZE_CHAT)) result.setChatSession(incoming.getChatSession());
        if (actions.contains(UPDATE_GAME_MODE)) result.setGameMode(incoming.getGameMode());
        if (actions.contains(UPDATE_LISTED)) result.setListed(incoming.isListed());
        if (actions.contains(UPDATE_LATENCY)) result.setLatency(incoming.getLatency());
        if (actions.contains(UPDATE_DISPLAY_NAME)) result.setDisplayName(incoming.getDisplayName());
        if (actions.contains(UPDATE_LIST_ORDER)) result.setListOrder(incoming.getListOrder());
        if (actions.contains(UPDATE_HAT)) result.setShowHat(incoming.isShowHat());
        return result;
    }

    void team(WrapperPlayServerTeams packet, UUID observer, Viewer viewer) {
        String id = packet.getTeamName();
        SourceTeam old = viewer.teams.get(id);
        Set<String> members = new HashSet<>(old == null ? Set.of() : old.members);
        Set<String> affected = new HashSet<>(members);
        if (packet.getTeamMode() == TeamMode.REMOVE || packet.getTeamMode() == TeamMode.CREATE) {
            members.forEach(name -> viewer.teamByName.remove(name, id));
            members.clear();
        }
        if (packet.getTeamMode() == TeamMode.CREATE || packet.getTeamMode() == TeamMode.ADD_ENTITIES) {
            for (String name : packet.getPlayers()) {
                String previous = viewer.teamByName.put(name, id);
                if (previous != null && !previous.equals(id)) {
                    SourceTeam before = viewer.teams.get(previous);
                    if (before != null) {
                        Set<String> remaining = new HashSet<>(before.members);
                        remaining.remove(name);
                        viewer.teams.put(previous, new SourceTeam(before.info, Set.copyOf(remaining)));
                    }
                }
                members.add(name);
                affected.add(name);
            }
        } else if (packet.getTeamMode() == TeamMode.REMOVE_ENTITIES) {
            for (String name : packet.getPlayers()) {
                members.remove(name);
                viewer.teamByName.remove(name, id);
                affected.add(name);
            }
        }
        if (packet.getTeamMode() == TeamMode.REMOVE) viewer.teams.remove(id);
        else {
            Appearance info = packet.getTeamInfo().map(Appearance::of).orElse(old == null ? EMPTY : old.info);
            viewer.teams.put(id, new SourceTeam(info, Set.copyOf(members)));
        }
        for (String name : affected) {
            UUID target = viewer.byName.get(name);
            if (target != null && viewer.aliases.containsKey(target)) updates.mark(observer, target, 0);
        }
        // Original teams stay intact for self/other entities. Alias-specific teams allow different
        // players in a shared source team to retain their own name visibility and sorting priority.
    }

    private static Appearance appearance(Viewer viewer, String name) {
        SourceTeam team = viewer.teams.get(viewer.teamByName.get(name));
        return team == null ? EMPTY : team.info;
    }
    private static Component tab(Viewer viewer, WrapperPlayServerPlayerInfoUpdate.PlayerInfo source) {
        return source.getDisplayName() != null ? source.getDisplayName()
                : appearance(viewer, source.getGameProfile().getName()).originalName(source.getGameProfile().getName());
    }
    boolean score(WrapperPlayServerUpdateScore packet, Viewer viewer) {
        String owner = packet.getEntityName();
        viewer.scores.put(new ScoreKey(owner, packet.getObjectiveName()), new Score(packet.getValue().orElse(0),
                packet.getEntityDisplayName(), packet.getScoreFormat()));
        UUID target = viewer.byName.get(owner);
        String alias = target == null ? null : viewer.aliases.get(target);
        if (alias == null) return false;
        if (packet.getEntityDisplayName() == null) packet.setEntityDisplayName(appearance(viewer, owner).originalName(owner));
        packet.setEntityName(alias);
        return true;
    }
    boolean resetScore(WrapperPlayServerResetScore packet, Viewer viewer) {
        String owner = packet.getTargetName();
        viewer.scores.keySet().removeIf(key -> key.owner.equals(owner)
                && (packet.getObjective() == null || key.objective.equals(packet.getObjective())));
        UUID target = viewer.byName.get(owner);
        String alias = target == null ? null : viewer.aliases.get(target);
        if (alias != null) packet.setTargetName(alias);
        return alias != null;
    }

    boolean needsIdentity(UUID observer, UUID target) {
        Viewer viewer = viewers.get(observer);
        if (viewer == null) return false;
        synchronized (viewer) {
            return policy.alias(observer, target) != null && viewer.profiles.containsKey(target) && !viewer.aliases.containsKey(target);
        }
    }
    boolean aliased(UUID observer, UUID target) {
        Viewer viewer = viewers.get(observer);
        if (viewer == null) return false;
        synchronized (viewer) { return viewer.aliases.containsKey(target); }
    }
    /** Main thread. refreshEntity is only needed when adopting an already-created real client profile. */
    void flush(UUID observer, UUID target, java.util.function.Consumer<Runnable> refreshEntity) {
        Viewer viewer = viewers.get(observer);
        if (viewer == null) return;
        synchronized (viewer) {
            var source = viewer.profiles.get(target);
            if (source == null) return; // Never resurrect a profile deliberately removed by another plugin.
            String alias = viewer.aliases.get(target);
            if (alias == null) {
                alias = policy.alias(observer, target);
                if (alias == null) return;
                viewer.aliases.put(target, alias);
                String assigned = alias;
                refreshEntity.accept(() -> {
                    viewer.user.sendPacketSilently(new WrapperPlayServerPlayerInfoRemove(target));
                    var output = copy(source);
                    output.setGameProfile(new UserProfile(target, assigned, new ArrayList<>(source.getGameProfile().getTextureProperties())));
                    output.setDisplayName(tab(viewer, source));
                    viewer.user.sendPacketSilently(new WrapperPlayServerPlayerInfoUpdate(INITIALIZE.clone(), output));
                });
            }
            render(observer, target, viewer, source, alias);
        }
    }
    private void render(UUID observer, UUID target, Viewer viewer,
                        WrapperPlayServerPlayerInfoUpdate.PlayerInfo source, String alias) {
        String name = source.getGameProfile().getName(), origin = viewer.teamByName.get(name);
        Appearance raw = appearance(viewer, name);
        String id = NameHidingAliases.teamId(origin, alias);
        Component prefix = raw.overheadPrefix(policy.hides(observer, target), name);
        Component tab = tab(viewer, source);
        Rendered before = viewer.rendered.get(target);
        boolean create = before == null || !before.team.equals(id);
        if (create && before != null)
            viewer.user.sendPacketSilently(new WrapperPlayServerTeams(before.team, TeamMode.REMOVE, (ScoreBoardTeamInfo) null, List.of()));
        if (create || !raw.equals(before.raw) || !prefix.equals(before.prefix))
            viewer.user.sendPacketSilently(new WrapperPlayServerTeams(id, create ? TeamMode.CREATE : TeamMode.UPDATE,
                    raw.packet(prefix), create ? List.of(alias) : List.of()));
        if (before == null || !tab.equals(before.tab)) {
            var display = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(target);
            display.setDisplayName(tab);
            viewer.user.sendPacketSilently(new WrapperPlayServerPlayerInfoUpdate(UPDATE_DISPLAY_NAME, display));
        }
        if (before == null || !raw.equals(before.raw) || !name.equals(before.name)) {
            if (before != null && !name.equals(before.name)) viewer.user.sendPacketSilently(new WrapperPlayServerResetScore(alias, null));
            for (var entry : viewer.scores.entrySet()) if (entry.getKey().owner.equals(name)) {
                viewer.user.sendPacketSilently(new WrapperPlayServerResetScore(name, entry.getKey().objective));
                viewer.user.sendPacketSilently(entry.getValue().packet(alias, entry.getKey().objective, raw.originalName(name)));
            }
        }
        viewer.rendered.put(target, new Rendered(id, name, raw, prefix, tab));
    }

    void markAll(int delay) {
        viewers.forEach((observer, viewer) -> {
            synchronized (viewer) {
                for (UUID target : viewer.profiles.keySet())
                    if (viewer.aliases.containsKey(target) || policy.alias(observer, target) != null) updates.mark(observer, target, delay);
            }
        });
    }
    void markTarget(UUID target, int delay) {
        viewers.forEach((observer, viewer) -> {
            synchronized (viewer) { if (viewer.profiles.containsKey(target)) updates.mark(observer, target, delay); }
        });
    }
    void markViewer(UUID observer, int delay) {
        Viewer viewer = viewers.get(observer);
        if (viewer != null) synchronized (viewer) {
            for (UUID target : viewer.aliases.keySet()) updates.mark(observer, target, delay);
        }
    }
    void removePlayer(UUID player) {
        viewers.remove(player);
        viewers.values().forEach(viewer -> {
            synchronized (viewer) {
                var rendered = viewer.rendered.remove(player);
                if (rendered != null) viewer.user.sendPacketSilently(new WrapperPlayServerTeams(rendered.team,
                        TeamMode.REMOVE, (ScoreBoardTeamInfo) null, List.of()));
                String alias = viewer.aliases.remove(player);
                if (alias != null) viewer.user.sendPacketSilently(new WrapperPlayServerResetScore(alias, null));
                var source = viewer.profiles.remove(player);
                String name = source == null ? rendered == null ? null : rendered.name : source.getGameProfile().getName();
                if (name != null) {
                    viewer.byName.remove(name, player);
                    viewer.scores.keySet().removeIf(key -> key.owner.equals(name));
                }
            }
        });
    }
    Set<UUID> aliasedTargets(UUID observer) {
        Viewer viewer = viewers.get(observer);
        if (viewer == null) return Set.of();
        synchronized (viewer) { return Set.copyOf(viewer.aliases.keySet()); }
    }
    void restore(UUID observer, UUID target, java.util.function.Consumer<Runnable> refreshEntity) {
        Viewer viewer = viewers.get(observer);
        if (viewer == null) return;
        synchronized (viewer) {
            String alias = viewer.aliases.get(target);
            if (alias == null) return;
            var source = viewer.profiles.get(target);
            if (source != null) refreshEntity.accept(() -> {
                viewer.user.sendPacketSilently(new WrapperPlayServerPlayerInfoRemove(target));
                viewer.user.sendPacketSilently(new WrapperPlayServerPlayerInfoUpdate(INITIALIZE.clone(), copy(source)));
            });
            viewer.aliases.remove(target);
            var before = viewer.rendered.remove(target);
            if (before != null) viewer.user.sendPacketSilently(new WrapperPlayServerTeams(before.team,
                    TeamMode.REMOVE, (ScoreBoardTeamInfo) null, List.of()));
            viewer.user.sendPacketSilently(new WrapperPlayServerResetScore(alias, null));
            if (source != null) {
                String name = source.getGameProfile().getName();
                for (var entry : viewer.scores.entrySet()) if (entry.getKey().owner.equals(name))
                    viewer.user.sendPacketSilently(entry.getValue().packet(name, entry.getKey().objective, appearance(viewer, name).originalName(name)));
            }
        }
    }
    void clear() { policy = NameHidingPolicy.OFF; viewers.clear(); updates.clear(); }
}
