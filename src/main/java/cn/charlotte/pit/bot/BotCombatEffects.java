package cn.charlotte.pit.bot;

import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.parm.type.BowOnly;
import cn.charlotte.pit.parm.type.ThrowOnly;
import cn.charlotte.pit.perk.AbstractHandle;
import cn.charlotte.pit.perk.BotCombatEffect;
import cn.charlotte.pit.perk.MegaStreak;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Ordinary perk combat only. Never dispatches equipment enchants, quests or generic Pit events. */
public final class BotCombatEffects {
    private BotCombatEffects() { }

    private record Hook(Class<?> type, String name) { }
    private static final Map<Hook, Method> METHODS = new ConcurrentHashMap<>();

    public static boolean attack(AbstractHandle perk, int level, Player player, Entity bot,
                                 boolean projectile, boolean arrow, double damage, AtomicDouble extraDamage,
                                 AtomicDouble multiplier, AtomicBoolean cancel) {
        String name = projectile ? "handleShootEntity" : "handleAttackEntity";
        Method method = eligible(perk, level, name);
        if (method == null || (projectile && ((method.isAnnotationPresent(BowOnly.class) && !arrow)
                || (method.isAnnotationPresent(ThrowOnly.class) && arrow)))) return false;
        if (projectile) perk.handleShootEntity(level, player, bot, damage, extraDamage, multiplier, cancel);
        else perk.handleAttackEntity(level, player, bot, damage, extraDamage, multiplier, cancel);
        return true;
    }

    public static boolean damaged(AbstractHandle perk, int level, Player player, Entity bot, double damage,
                                  AtomicDouble extraDamage, AtomicDouble multiplier, AtomicBoolean cancel) {
        if (eligible(perk, level, "handlePlayerDamaged") == null) return false;
        perk.handlePlayerDamaged(level, player, bot, damage, extraDamage, multiplier, cancel);
        return true;
    }

    private static Method eligible(AbstractHandle perk, int level, String name) {
        if (level < 0 || perk instanceof MegaStreak) return null;
        Method method = METHODS.computeIfAbsent(new Hook(perk.getClass(), name), key -> {
            try {
                return key.type().getMethod(key.name(), int.class, Player.class, Entity.class, double.class,
                        AtomicDouble.class, AtomicDouble.class, AtomicBoolean.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Missing perk combat hook: " + key, e);
            }
        });
        if ((method.isAnnotationPresent(PlayerOnly.class) || perk.getClass().isAnnotationPresent(PlayerOnly.class))
                && !method.isAnnotationPresent(BotCombatEffect.class)) return null;
        return method;
    }

    /** Raw hit caps and prestige scaling run first; ordinary perk boosts must not be discarded afterward. */
    public static double damage(double base, double multiplier, double extraDamage) {
        if (!Double.isFinite(base) || !Double.isFinite(multiplier) || !Double.isFinite(extraDamage)) return 0;
        return Math.max(0, Math.max(0, base) * Math.max(0.2, multiplier) + extraDamage);
    }
}
