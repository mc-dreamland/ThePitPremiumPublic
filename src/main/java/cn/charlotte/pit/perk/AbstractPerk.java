package cn.charlotte.pit.perk;

import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 21:18
 */
public abstract class AbstractPerk extends AbstractHandle {

    public abstract String getInternalPerkName();

    public abstract String getDisplayName();

    public abstract Material getIcon();

    public abstract PerkGroup getPerkGroup();

    public abstract double requireCoins();

    public abstract double requireRenown(int level);

    public abstract int requirePrestige();

    public abstract int requireLevel();

    public abstract List<String> getDescription(Player player);

    public abstract int getMaxLevel();

    public abstract PerkType getPerkType();

    public abstract void onPerkActive(Player player);

    public abstract void onPerkInactive(Player player);

    public void onUnlock(Player player) {

    }

    public void onUpgrade(Player player) {

    }

    public ItemStack getIconWithNameAndLore(String name, List<String> lore, int durability, int amount) {
        return new ItemBuilder(getIcon())
                .name(name)
                .lore(lore)
                .amount(1)
                .build();
    }

}
