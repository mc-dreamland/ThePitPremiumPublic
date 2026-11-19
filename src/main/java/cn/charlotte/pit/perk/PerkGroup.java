package cn.charlotte.pit.perk;

import lombok.Getter;

@Getter
public enum PerkGroup {
    PASSIVE("被动天赋"),
    BOOST("增益天赋"),
    PRESTIGE("声望天赋"),
    SHOP("商店物品"),
    STREAK("超级连杀组合包"),
    KILL_STREAK("连杀天赋");

    private final String displayName;

    PerkGroup(String displayName) {
        this.displayName = displayName;
    }
}
