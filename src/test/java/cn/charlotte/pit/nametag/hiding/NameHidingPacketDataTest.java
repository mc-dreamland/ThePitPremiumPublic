package cn.charlotte.pit.nametag.hiding;

import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.Action.*;
import static com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.*;
import static org.junit.jupiter.api.Assertions.*;

class NameHidingPacketDataTest {
    @Test void copiedProfilesKeepUuidAndSignedSkinPropertiesWithoutMutatingTheSource() {
        UUID target = UUID.randomUUID();
        var skin = new TextureProperty("textures", "original-texture", "original-signature");
        var original = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                new UserProfile(target, "RealName", new ArrayList<>(List.of(skin))),
                false, 123, GameMode.SURVIVAL, Component.text("Tab Name"), null, 27, true);
        var copy = NameHidingPackets.copy(original);
        assertNotSame(original, copy);
        assertNotSame(original.getGameProfile(), copy.getGameProfile());
        assertNotSame(original.getGameProfile().getTextureProperties(), copy.getGameProfile().getTextureProperties());
        copy.getGameProfile().setName(NameHidingAliases.encode(1));
        assertEquals("RealName", original.getGameProfile().getName());
        assertEquals(target, copy.getProfileId());
        assertEquals("original-signature", copy.getGameProfile().getTextureProperties().getFirst().getSignature());
        assertFalse(copy.isListed());
        assertEquals(123, copy.getLatency());
        assertEquals(Component.text("Tab Name"), copy.getDisplayName());
        assertEquals(27, copy.getListOrder());
        assertTrue(copy.isShowHat());
    }
    @Test void partialLatencyOrGameModeUpdatesDoNotEraseTheCachedRealNameSkinOrCustomTabText() {
        UUID target = UUID.randomUUID();
        var original = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(new UserProfile(target, "RealName",
                new ArrayList<>(List.of(new TextureProperty("textures", "skin", "signature")))),
                true, 5, GameMode.SURVIVAL, Component.text("Custom Tab"), null, 0, true);
        var incoming = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(target);
        incoming.setLatency(99);
        incoming.setGameMode(GameMode.ADVENTURE);
        var merged = NameHidingPackets.merge(original, incoming, EnumSet.of(UPDATE_LATENCY, UPDATE_GAME_MODE));
        assertEquals("RealName", merged.getGameProfile().getName());
        assertEquals(1, merged.getGameProfile().getTextureProperties().size());
        assertEquals(Component.text("Custom Tab"), merged.getDisplayName());
        assertTrue(merged.isListed());
        assertEquals(99, merged.getLatency());
        assertEquals(GameMode.ADVENTURE, merged.getGameMode());
        assertEquals(5, original.getLatency());
        assertEquals(GameMode.SURVIVAL, original.getGameMode());
    }
    @Test void explicitNullTabUpdateCanResetToNativeFormattingInsteadOfKeepingStaleCustomText() {
        UUID target = UUID.randomUUID();
        var original = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(new UserProfile(target, "RealName"),
                true, 5, GameMode.SURVIVAL, Component.text("Old Tab"), null);
        var reset = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(target);
        reset.setDisplayName(null);
        var merged = NameHidingPackets.merge(original, reset, EnumSet.of(UPDATE_DISPLAY_NAME));
        assertNull(merged.getDisplayName());
        assertEquals("RealName", merged.getGameProfile().getName());
    }
    @Test void nameInsertionDoesNotDiscardOriginalTeamPrefixSuffixOrFlags() {
        var original = new ScoreBoardTeamInfo(Component.text("Team"), Component.text("[VIP] "), Component.text(" suffix"),
                NameTagVisibility.HIDE_FOR_OTHER_TEAMS, CollisionRule.NEVER, NamedTextColor.GOLD, OptionData.ALL);
        var info = NameHidingPackets.Appearance.of(original);
        var shown = info.packet(info.overheadPrefix(false, "RealName"));
        var hidden = info.packet(info.overheadPrefix(true, "RealName"));
        assertEquals(Component.empty().append(Component.text("[VIP] ")).append(Component.text("RealName")), shown.getPrefix());
        assertEquals(Component.text("[VIP] "), hidden.getPrefix());
        assertEquals(original.getSuffix(), hidden.getSuffix());
        assertEquals(original.getTagVisibility(), hidden.getTagVisibility());
        assertEquals(original.getColor(), hidden.getColor());
        assertEquals(original.getOptionData(), hidden.getOptionData());
        assertEquals(original.getCollisionRule(), hidden.getCollisionRule());
        assertEquals(Component.text("[VIP] "), original.getPrefix());
    }
}
