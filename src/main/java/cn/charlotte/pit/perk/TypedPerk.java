package cn.charlotte.pit.perk;

import cn.charlotte.pit.data.PlayerProfile;
import org.bukkit.entity.Player;

public abstract class TypedPerk<E extends Enum<E> & PitPerkType> extends AbstractPerk {
    private E type;

    protected TypedPerk() {
    }

    public E getType() {
        return type;
    }

    public void setType(E type) {
        this.type = type;
    }

    /**
     * 判断玩家是否已选择 / 装备此 Perk
     *
     * @param player Bukkit 玩家
     * @return true 已装备（或被动已解锁），false 否
     */
    public boolean hasEquip(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        return hasEquip(profile);
    }

    public boolean hasEquip(PlayerProfile profile) {
        // 被动 只要解锁就视作已装备
        if (this.getClass().isAnnotationPresent(Passive.class)) {
            return getLevel(profile) > 0;
        }

        // 其他类型 必须在槽位中
        return profile.hasEquipPerk(getInternalPerkName());
    }

    public boolean hasUnlock(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile == null) {
            return false;
        }
        return hasUnlock(profile);

    }

    /**
     * 从解锁列表中获取等级
     *
     * @param player
     * @return 0 代表未解锁
     */
    public int getLevel(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile == null) {
            return 0;
        }
        return getLevel(profile);
    }

    public int getLevel(PlayerProfile profile) {
        return profile.getLevel(this.getType());
    }

    public int getEquippedLevel(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile == null) {
            return 0;
        }
        return getEquippedLevel(profile);

    }

    /**
     * 获取穿戴的天赋等级
     * 天赋必须穿戴在指定槽位或为被动天赋
     * 如果已解锁但未穿戴或未解锁，返回-1
     *
     * @param profile
     * @return
     */
    public int getEquippedLevel(PlayerProfile profile) {
        int level = getLevel(profile);
        if (isPassivePerk()) {
            return level;
        }

        if (!hasEquip(profile)) {
            return -1;
        }

        return level;

    }

    public boolean hasUnlock(PlayerProfile profile) {
        return profile.getUnlockedPerkMap().containsKey(getInternalPerkName());
    }

    public boolean isPassivePerk() {
        return getType() instanceof PerksPassivesEnum;
    }
}