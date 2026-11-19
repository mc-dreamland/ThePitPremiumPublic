package cn.charlotte.pit.commands.admin;

/** Keep the existing destructive Shift action separate from the new ordinary right-click navigation. */
final class CurrentHistoryItemActions {
    enum Action { HISTORY, ENCHANTMENTS, BLACKLIST }
    private CurrentHistoryItemActions() { }

    static boolean canViewEnchantments(String internalName, String itemUuid) {
        return itemUuid != null && !itemUuid.isBlank() && MaterialEnchantmentHistory.eligible(internalName);
    }

    static Action choose(boolean currentHistory, boolean shiftClick, boolean ordinaryRightClick,
                         String internalName, String itemUuid) {
        if (currentHistory && shiftClick) return Action.BLACKLIST;
        if (currentHistory && ordinaryRightClick && canViewEnchantments(internalName, itemUuid)) return Action.ENCHANTMENTS;
        return Action.HISTORY;
    }
}
