package cn.charlotte.pit.career;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * getItemDisplayName() 要求返回职业自定义昵称
 * getInternalName() 内部名
 * requirePrestige 解锁需要的最少精通等级
 * requireCoin 解锁以及升级金币
 * requireRenown 初始解锁以及升级声望
 * getIcon 返回图标
 * getItemStack 给予物品
 * requireLevel 多少等级启用
 * getDescription 描述
 * getMaxLevel 最大可升级
 * toEnable 启用
 * toDeprecated 弃用
 * getDirection 可用的转职业方向
 * directionLevel 最低转职业等级
 * getNextPlayer 用于一些特殊获得
 * requirePlayer 用于一些特殊职业
 */
public abstract class AbstractCareer extends AbstractHandleOfCareer {

    public abstract String getDisplayName(int level, int direction);

    public abstract String getInternalName();

    public abstract int requirePrestige(int level, int direction);

    public abstract int requireCoin(int level, int direction);

    public abstract Material getIcon(int level, int direction);

    public abstract Map<itemStackType, ItemStack> getItemStack(int level, int direction);

    public abstract int requireLevel();

    public abstract List<String> getDescription(Player player, int level, int direction);

    public abstract int getMaxLevel();

    public abstract void toEnable(int level, int direction);

    public abstract void toDeprecated(int level, int direction);

    public abstract int getDirection(int level, int direction);

    public abstract int directionLevel(int level, int direction);

    public abstract int requireRenown(int level, int direction);

    public abstract boolean getNextPlayer(int level, int direction, Player player);

    public abstract boolean requirePlayer(int level, int direction, Player player);

    public enum itemStackType {
        SHOP("&e商店物品"),
        DEFAULT("&f默认"),
        SPECIAL("&a特殊");
        @Getter
        private final String getDisplayName;

        itemStackType(String name) {
            this.getDisplayName = name;
        }
    }
}
