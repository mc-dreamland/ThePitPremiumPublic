package cn.charlotte.pit.nametag.hiding;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.NettyManager;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.*;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.Action.*;
import static com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.*;
import static org.junit.jupiter.api.Assertions.*;

/** Headless protocol-data tests. Does not connect to or change any Minecraft server. */
class NameHidingPacketFlowTest {
    private PacketEventsAPI<?> previous;
    private final UUID observer = UUID.randomUUID(), target = UUID.randomUUID();
    private final String alias = NameHidingAliases.encode(1);
    private final NameHidingUpdates updates = new NameHidingUpdates();
    private final NameHidingPackets packets = new NameHidingPackets(updates, Logger.getAnonymousLogger());
    private RecordingUser user;
    private NameHidingPackets.Viewer client;

    @BeforeEach void setup() {
        previous = PacketEvents.getAPI();
        PacketEvents.setAPI(new DataOnlyApi());
        user = new RecordingUser(observer);
        client = packets.viewer(user);
        packets.policy(new NameHidingPolicy(true, Map.of(target, alias), Set.of(target), Set.of()));
    }
    @AfterEach void teardown() { PacketEvents.setAPI(previous); }

    private WrapperPlayServerPlayerInfoUpdate add(UUID id, String name) {
        var data = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(new UserProfile(id, name,
                new ArrayList<>(List.of(new TextureProperty("textures", "skin", "signature")))),
                true, 17, GameMode.SURVIVAL, null, null);
        return new WrapperPlayServerPlayerInfoUpdate(EnumSet.of(ADD_PLAYER, UPDATE_DISPLAY_NAME, UPDATE_LATENCY,
                UPDATE_GAME_MODE, UPDATE_LISTED), data);
    }
    private void team(String id, String... names) {
        packets.team(new WrapperPlayServerTeams(id, TeamMode.CREATE,
                new ScoreBoardTeamInfo(Component.text("Team"), Component.text("[VIP] ").color(NamedTextColor.GOLD),
                        Component.text(" suffix"), NameTagVisibility.ALWAYS, CollisionRule.NEVER,
                        NamedTextColor.WHITE, OptionData.NONE), List.of(names)), observer, client);
    }
    private void flush() { packets.flush(observer, target, Runnable::run); }

    @Test void firstAddUsesAnInvisibleNameButPreservesSourceIdentitySkinAndVisibleTab() {
        var add = add(target, "RealName");
        UserProfile source = add.getEntries().getFirst().getGameProfile();
        assertTrue(packets.profile(observer, client, add));
        assertEquals(alias, add.getEntries().getFirst().getGameProfile().getName());
        assertEquals("RealName", source.getName());
        assertEquals("RealName", client.profiles.get(target).getGameProfile().getName());
        assertEquals(target, add.getEntries().getFirst().getProfileId());
        assertEquals("signature", add.getEntries().getFirst().getGameProfile().getTextureProperties().getFirst().getSignature());
        assertEquals(Component.empty().color(NamedTextColor.WHITE).append(Component.empty())
                .append(Component.text("RealName")).append(Component.empty()), add.getEntries().getFirst().getDisplayName());
        assertFalse(packets.needsIdentity(observer, target));
    }

    @Test void revealAndRegionChangesSendOnlyTeamUpdatesAndDoNotRecreateThePlayer() {
        packets.profile(observer, client, add(target, "RealName"));
        team("00001", "RealName");
        flush();
        user.sent.clear();
        packets.policy(new NameHidingPolicy(true, Map.of(target, alias), Set.of(target), Set.of(observer)));
        AtomicInteger identityRefreshes = new AtomicInteger();
        packets.flush(observer, target, send -> { identityRefreshes.incrementAndGet(); send.run(); });
        assertEquals(0, identityRefreshes.get());
        assertEquals(1, user.sent.size());
        var shown = assertInstanceOf(WrapperPlayServerTeams.class, user.sent.getFirst());
        assertEquals(TeamMode.UPDATE, shown.getTeamMode());
        assertEquals(Component.empty().append(Component.text("[VIP] ").color(NamedTextColor.GOLD))
                .append(Component.text("RealName")), shown.getTeamInfo().orElseThrow().getPrefix());
        assertEquals(Component.text(" suffix"), shown.getTeamInfo().orElseThrow().getSuffix());
        assertEquals(alias, client.aliases.get(target));
        user.sent.clear();
        flush();
        assertTrue(user.sent.isEmpty(), "An unchanged pair must send no more packets");
        packets.policy(new NameHidingPolicy(false, Map.of(target, alias), Set.of(), Set.of()));
        flush();
        assertTrue(user.sent.isEmpty(), "Disabling hiding need not refresh a profile that is already visually shown");
    }

    @Test void sharedSourceTeamDoesNotPutOnePlayersNameOnAnotherPlayersHead() {
        UUID another = UUID.randomUUID();
        String secondAlias = NameHidingAliases.encode(2);
        packets.policy(new NameHidingPolicy(true, Map.of(target, alias, another, secondAlias), Set.of(target), Set.of()));
        packets.profile(observer, client, add(target, "First"));
        packets.profile(observer, client, add(another, "Second"));
        team("shared", "First", "Second");
        flush();
        packets.flush(observer, another, Runnable::run);
        var teams = user.sent.stream().filter(WrapperPlayServerTeams.class::isInstance).map(WrapperPlayServerTeams.class::cast).toList();
        assertEquals(2, teams.size());
        assertNotEquals(teams.get(0).getTeamName(), teams.get(1).getTeamName());
        assertEquals(List.of(alias), new ArrayList<>(teams.get(0).getPlayers()));
        assertEquals(Component.text("[VIP] ").color(NamedTextColor.GOLD), teams.get(0).getTeamInfo().orElseThrow().getPrefix());
        assertEquals(Component.empty().append(Component.text("[VIP] ").color(NamedTextColor.GOLD))
                .append(Component.text("Second")), teams.get(1).getTeamInfo().orElseThrow().getPrefix());
    }

    @Test void existingProfilesAreAdoptedOnlyOnceAndExplicitlyRemovedProfilesAreNotResurrected() {
        packets.policy(NameHidingPolicy.OFF);
        packets.profile(observer, client, add(target, "RealName"));
        packets.policy(new NameHidingPolicy(true, Map.of(target, alias), Set.of(target), Set.of()));
        assertTrue(packets.needsIdentity(observer, target));
        AtomicInteger refreshes = new AtomicInteger();
        packets.flush(observer, target, send -> { refreshes.incrementAndGet(); send.run(); });
        packets.flush(observer, target, send -> { refreshes.incrementAndGet(); send.run(); });
        assertEquals(1, refreshes.get());
        assertFalse(packets.needsIdentity(observer, target));
        client.profiles.remove(target); // The same state transition as a real PLAYER_INFO_REMOVE.
        user.sent.clear();
        flush();
        assertTrue(user.sent.isEmpty());
    }

    @Test void scoreOwnersFollowTheClientIdentityButSidebarDisplayStillUsesTheRealName() {
        packets.profile(observer, client, add(target, "RealName"));
        team("team", "RealName");
        var score = new WrapperPlayServerUpdateScore("RealName",
                WrapperPlayServerUpdateScore.Action.CREATE_OR_UPDATE_ITEM, "health", 20, null, null);
        assertTrue(packets.score(score, client));
        assertEquals(alias, score.getEntityName());
        assertNotNull(score.getEntityDisplayName());
        assertEquals(1, client.scores.size());
        var reset = new WrapperPlayServerResetScore("RealName", "health");
        assertTrue(packets.resetScore(reset, client));
        assertEquals(alias, reset.getTargetName());
        assertTrue(client.scores.isEmpty());
    }

    @Test void restorationUsesOriginalDataAndRemovesAllClientOnlyTeamsAndAliases() {
        var original = add(target, "RealName");
        original.getEntries().getFirst().setDisplayName(Component.text("Custom Tab"));
        packets.profile(observer, client, original);
        team("team", "RealName");
        flush();
        user.sent.clear();
        packets.restore(observer, target, Runnable::run);
        var restored = user.sent.stream().filter(WrapperPlayServerPlayerInfoUpdate.class::isInstance)
                .map(WrapperPlayServerPlayerInfoUpdate.class::cast).filter(packet -> packet.getActions().contains(ADD_PLAYER)).findFirst().orElseThrow();
        assertEquals("RealName", restored.getEntries().getFirst().getGameProfile().getName());
        assertEquals(Component.text("Custom Tab"), restored.getEntries().getFirst().getDisplayName());
        assertEquals("signature", restored.getEntries().getFirst().getGameProfile().getTextureProperties().getFirst().getSignature());
        assertTrue(packets.aliasedTargets(observer).isEmpty());
        assertTrue(client.rendered.isEmpty());
        assertTrue(user.sent.stream().filter(WrapperPlayServerTeams.class::isInstance)
                .map(WrapperPlayServerTeams.class::cast).anyMatch(packet -> packet.getTeamMode() == TeamMode.REMOVE));
    }

    @Test void selfAndNpcProfilesNotPresentInThePolicyAreNeverRenamed() {
        var self = add(observer, "Self");
        var npc = add(UUID.randomUUID(), "Bot");
        assertFalse(packets.profile(observer, client, self));
        assertFalse(packets.profile(observer, client, npc));
        assertEquals("Self", self.getEntries().getFirst().getGameProfile().getName());
        assertEquals("Bot", npc.getEntries().getFirst().getGameProfile().getName());
        assertTrue(client.aliases.isEmpty());
    }

    @Test void failedIdentityRestorationKeepsEnoughStateToRetryAndRecover() {
        packets.profile(observer, client, add(target, "RealName"));
        flush();
        assertThrows(IllegalStateException.class, () -> packets.restore(observer, target,
                send -> { throw new IllegalStateException("connection failed"); }));
        assertTrue(packets.aliased(observer, target));
        packets.restore(observer, target, Runnable::run);
        assertFalse(packets.aliased(observer, target));
    }

    @Test void lateDisconnectCannotDropTheCacheOfANewerConnectionWithTheSameUuid() {
        packets.profile(observer, client, add(target, "RealName"));
        var replacement = new RecordingUser(observer);
        var current = packets.viewer(replacement);
        packets.onUserDisconnect(new UserDisconnectEvent(user));
        assertSame(current, packets.viewer(replacement));
        assertTrue(packets.viewerIds().contains(observer));
        packets.onUserDisconnect(new UserDisconnectEvent(replacement));
        assertFalse(packets.viewerIds().contains(observer));
    }

    private static final class RecordingUser extends User {
        final List<PacketWrapper<?>> sent = new ArrayList<>();
        RecordingUser(UUID id) { super(null, ConnectionState.PLAY, ServerVersion.V_1_20_6.toClientVersion(), new UserProfile(id, "Observer")); }
        @Override public void sendPacketSilently(PacketWrapper<?> packet) { sent.add(packet); }
    }
    private static final class DataOnlyApi extends PacketEventsAPI<Object> {
        private final NettyManager netty = new io.github.retrooper.packetevents.impl.netty.NettyManagerImpl();
        @Override public void load() { }
        @Override public boolean isLoaded() { return true; }
        @Override public void init() { }
        @Override public boolean isInitialized() { return true; }
        @Override public void terminate() { }
        @Override public boolean isTerminated() { return false; }
        @Override public Object getPlugin() { return this; }
        @Override public ServerManager getServerManager() { return () -> ServerVersion.V_1_20_6; }
        @Override public ProtocolManager getProtocolManager() { return null; }
        @Override public PlayerManager getPlayerManager() { return null; }
        @Override public NettyManager getNettyManager() { return netty; }
        @Override public ChannelInjector getInjector() { return null; }
    }
}
