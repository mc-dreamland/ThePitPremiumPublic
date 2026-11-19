package cn.charlotte.pit.bot;

/** Pure boundary policy, shared by event handlers and regression tests. */
final class TrainingCombatRules {
    private TrainingCombatRules() { }

    static boolean blocked(boolean sourceInside, boolean targetInside, boolean sourcePlayer, boolean targetPlayer,
                           boolean sourceBot, boolean targetBot, Boolean projectileLaunchedInside) {
        // Only player -> training bot may cross the boundary, including arrows launched outside.
        // This exception never applies to a player victim or a bot attacking out of the arena.
        if (targetInside && targetBot && !targetPlayer && sourcePlayer && !sourceBot) return false;
        if (projectileLaunchedInside != null && projectileLaunchedInside != targetInside) return true;
        if (!sourceInside && !targetInside) return false;
        if (!sourceInside || !targetInside) return true;
        return !((sourcePlayer && targetPlayer && !sourceBot && !targetBot)
                || (sourcePlayer && targetBot) || (sourceBot && targetPlayer));
    }

    static double playerDamageMultiplier(boolean sourceInside, boolean targetInside) {
        if (sourceInside != targetInside) return 0;
        return sourceInside ? 0.1 : 1;
    }
}
