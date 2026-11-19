package cn.charlotte.pit.bot;

import cn.charlotte.pit.event.PitStreakKillChangeEvent;
import cn.charlotte.pit.medal.impl.challenge.FiftyStreaksMedal;
import cn.charlotte.pit.medal.impl.challenge.RamboMedal;
import cn.charlotte.pit.perk.type.bundles.beastmode.BeastModeMegaStreak;
import cn.charlotte.pit.perk.type.bundles.hermit.HermitMegaStreak;
import cn.charlotte.pit.perk.type.bundles.highlander.HighlanderMegaStreak;
import cn.charlotte.pit.perk.type.bundles.magnumopus.ApostleForTheGesusKillStreak;
import cn.charlotte.pit.perk.type.bundles.magnumopus.MagnumOpusMegaStreak;
import cn.charlotte.pit.perk.type.bundles.overdrive.OverDriveMegaStreak;
import cn.charlotte.pit.perk.type.bundles.tothemoon.ToTheMoonMegaStreak;
import cn.charlotte.pit.perk.type.bundles.uber.UberStreak;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BotStreakEventTest {
    @Test
    void existingConstructorsRemainNormalAndKeepTheirCancellationState() {
        assertFalse(new PitStreakKillChangeEvent(null, 1, 2).isBotKill());
        var cancelled = new PitStreakKillChangeEvent(null, 1, 2, true);
        assertFalse(cancelled.isBotKill());
        assertTrue(cancelled.isCancelled());
        var bot = new PitStreakKillChangeEvent(null, 29, 30, PitStreakKillChangeEvent.Source.BOT);
        assertTrue(bot.isBotKill());
        assertFalse(bot.isCancelled());
        assertEquals(29, bot.getFrom());
        assertEquals(30, bot.getTo());
    }

    @Test
    void allRegisteredMegaHandlersRejectBotEventsBeforeAccessingPlayerState() throws Exception {
        var botEvent = new PitStreakKillChangeEvent(null, 49, 100, PitStreakKillChangeEvent.Source.BOT);
        for (Object handler : List.of(new BeastModeMegaStreak(), new HermitMegaStreak(), new HighlanderMegaStreak(),
                new MagnumOpusMegaStreak(), new OverDriveMegaStreak(), new ToTheMoonMegaStreak(), new UberStreak())) {
            invokeStreakHandler(handler, botEvent);
        }
    }

    @Test
    void botEventsCannotGrantMythicsThroughDiceOrAdvanceStreakMedals() throws Exception {
        var botEvent = new PitStreakKillChangeEvent(null, 9, 50, PitStreakKillChangeEvent.Source.BOT);
        for (Object handler : List.of(new ApostleForTheGesusKillStreak(), new RamboMedal(), new FiftyStreaksMedal())) {
            invokeStreakHandler(handler, botEvent);
        }
    }

    private static void invokeStreakHandler(Object handler, PitStreakKillChangeEvent event) throws Exception {
        var method = Arrays.stream(handler.getClass().getMethods())
                .filter(m -> Arrays.equals(m.getParameterTypes(), new Class<?>[]{PitStreakKillChangeEvent.class}))
                .findFirst().orElseThrow();
        // A null profile is deliberate: any unsafe work before the source guard would fail the test.
        assertDoesNotThrow(() -> method.invoke(handler, event), handler.getClass().getName());
    }
}
