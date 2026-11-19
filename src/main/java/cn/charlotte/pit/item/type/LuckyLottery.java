package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class LuckyLottery extends AbstractPitItem {

    @Override
    public String getInternalName() {
        return "LuckyLottery";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&a幸运转盘抽奖券";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.MUSIC_DISC_WARD;
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(getItemDisplayMaterial())
                .internalName(getInternalName())
                .canDrop(false)
                .canTrade(false)
                .deathDrop(true)
                .removeOnJoin(true)
                .canSaveToEnderChest(false)
                .name(getItemDisplayName())
                .lore(
                        "&7死亡后掉落",
                        "&7使用 &a幸运转盘抽奖券 &7可以找唱片机获得随机物品"
                )
                .build();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        // 空实现
    }

}
