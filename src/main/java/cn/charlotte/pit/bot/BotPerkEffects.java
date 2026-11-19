package cn.charlotte.pit.bot;

import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.perk.AbstractHandle;
import cn.charlotte.pit.perk.BotKillEffect;
import cn.charlotte.pit.perk.MegaStreak;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/** Kill progression and generic kill events are deliberately outside this dispatcher. */
public final class BotPerkEffects {
    private BotPerkEffects() { }

    public static boolean apply(AbstractHandle perk, int level, Player player, Entity bot,
                                ResourceManager resources, boolean killingBlow) {
        if (perk instanceof MegaStreak) return false;
        BotKillEffect effect = perk.getClass().getAnnotation(BotKillEffect.class);
        if (effect == null || (effect.value() == BotKillEffect.Kind.KILL_EFFECT && !killingBlow)) return false;
        if (effect.value() == BotKillEffect.Kind.ASSIST_REWARD && killingBlow) return false;
        if (!killingBlow) {
            // Only reviewed currency assist hooks reach here; do not call CombatListener.handleAssist.
            // These reward hooks do not use damage arguments; sharing is applied once to the final pool.
            perk.handlePlayerAssist(level, player, bot, 0, 0, resources);
            return true;
        }
        perk.handleBotKilled(level, player, bot, resources);
        return true;
    }
}
