package cn.charlotte.pit.perk;

public interface PitPerkType {
    PerkGroup getCategory();

    // 所有实现都必须是枚举 → 方便在工具方法里取得 name()
    String name();


}