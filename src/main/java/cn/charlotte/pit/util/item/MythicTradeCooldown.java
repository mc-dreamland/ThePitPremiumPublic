package cn.charlotte.pit.util.item;

import cn.charlotte.pit.ThePit;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MythicTradeCooldown {

    public static final long ONCE_ONLY = -1L;

    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)([dhm])$", Pattern.CASE_INSENSITIVE);
    private static volatile String lastWarnedInvalidValue;

    private MythicTradeCooldown() {
    }

    public static CheckResult check(ItemStack item) {
        if (!MythicOwnershipUtil.isEquipment(item) || !MythicOwnershipUtil.isTraded(item)) {
            return CheckResult.allowedResult();
        }

        long cooldownMillis = getConfiguredCooldownMillis();
        if (cooldownMillis == ONCE_ONLY) {
            return CheckResult.onceOnlyResult();
        }

        long now = System.currentTimeMillis();
        long tradeTime = MythicOwnershipUtil.getTradeTime(item);
        if (tradeTime <= 0L) {
            tradeTime = MythicOwnershipUtil.ensureTradeTime(item, now);
        }

        long elapsed = Math.max(0L, now - tradeTime);
        long remaining = cooldownMillis - elapsed;
        return remaining <= 0L ? CheckResult.allowedResult() : CheckResult.cooldownResult(remaining);
    }

    public static Optional<CheckResult> findRestriction(Collection<ItemStack> items) {
        if (items == null) {
            return Optional.empty();
        }
        for (ItemStack item : items) {
            CheckResult result = check(item);
            if (!result.allowed()) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    public static long parseDurationMillis(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("交易冷却时间不能为空");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("-1".equals(normalized)) {
            return ONCE_ONLY;
        }

        Matcher matcher = DURATION_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("交易冷却时间必须为 -1 或数字加 d/h/m，例如 1d、1h、1m");
        }

        long amount;
        try {
            amount = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("交易冷却时间数值过大", exception);
        }

        long unitMillis = switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
            case "d" -> 24L * 60L * 60L * 1000L;
            case "h" -> 60L * 60L * 1000L;
            case "m" -> 60L * 1000L;
            default -> throw new IllegalArgumentException("不支持的交易冷却时间单位");
        };

        try {
            return Math.multiplyExact(amount, unitMillis);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("交易冷却时间数值过大", exception);
        }
    }

    private static long getConfiguredCooldownMillis() {
        String configured = ThePit.getInstance().getPitConfig().getMythicEquipmentTradeCooldown();
        try {
            return parseDurationMillis(configured);
        } catch (IllegalArgumentException exception) {
            if (!String.valueOf(configured).equals(lastWarnedInvalidValue)) {
                lastWarnedInvalidValue = String.valueOf(configured);
                ThePit.getInstance().getSLF4JLogger().warn(
                        "Invalid trade.mythic-equipment-cooldown '{}', falling back to -1: {}",
                        configured,
                        exception.getMessage()
                );
            }
            return ONCE_ONLY;
        }
    }

    private static String formatRemaining(long remainingMillis) {
        long totalMinutes = Math.max(1L, (remainingMillis - 1L) / 60_000L + 1L);
        long days = totalMinutes / (24L * 60L);
        long hours = totalMinutes % (24L * 60L) / 60L;
        long minutes = totalMinutes % 60L;

        StringBuilder output = new StringBuilder();
        if (days > 0L) {
            output.append(days).append("天");
        }
        if (hours > 0L) {
            output.append(hours).append("小时");
        }
        if (minutes > 0L || output.length() == 0) {
            output.append(minutes).append("分钟");
        }
        return output.toString();
    }

    public record CheckResult(boolean allowed, boolean onceOnly, long remainingMillis) {

        private static CheckResult allowedResult() {
            return new CheckResult(true, false, 0L);
        }

        private static CheckResult onceOnlyResult() {
            return new CheckResult(false, true, -1L);
        }

        private static CheckResult cooldownResult(long remainingMillis) {
            return new CheckResult(false, false, remainingMillis);
        }

        public String deniedMessage() {
            if (onceOnly) {
                return "此装备仅限交易一次，无法再次交易!";
            }
            return "此装备仍处于交易冷却中，剩余 " + formatRemaining(remainingMillis) + "!";
        }
    }
}
