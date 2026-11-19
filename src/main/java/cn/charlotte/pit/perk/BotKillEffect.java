package cn.charlotte.pit.perk;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Explicit opt-in: never run an unreviewed player-kill hook for practice bots. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BotKillEffect {
    Kind value();

    enum Kind {
        /** Reviewed currency-only kill AND assist hooks, before damage sharing. */
        REWARD,
        /** Only the assist hook is safe; the kill hook may advance streaks. */
        ASSIST_REWARD,
        /** Ordinary healing/items/buffs (including normal buff stacks), only for the eligible final attacker. */
        KILL_EFFECT
    }
}
