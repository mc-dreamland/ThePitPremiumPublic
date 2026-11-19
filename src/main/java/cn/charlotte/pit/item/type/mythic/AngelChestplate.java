package cn.charlotte.pit.item.type.mythic;

import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class AngelChestplate extends IMythicItem {

    public AngelChestplate() {
        // 对应 Kotlin init 块
        this.maxLive = 100;
        this.live = 100;
    }

    @Override
    public String getInternalName() {
        return "angel_chestplate";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&f天使之甲";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.DIAMOND_CHESTPLATE;
    }

    @Override
    public boolean isEnchanted() {
        return true;
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        if (item == null) {
            return;
        }
        // 调用 Kotlin 扩展函数 reflectGetNmsItem
        // 返回值实际上是一个 NMS ItemStack，需要你按实际版本导入并替换下面的类型
        ReadableNBT tag = NBT.readNbt(item);

        ReadableNBT extra = tag.getCompound("extra");
        if (extra == null) {
            return;
        }
        loadOwnershipData(extra);
        this.maxLive = extra.getInteger("maxLive");
        this.live = extra.getInteger("live");
    }

    @Override
    public ItemStack toItemStack() {
        // 和 Kotlin 版一模一样的链式构建
        ItemBuilder builder = new ItemBuilder(getItemDisplayMaterial())
                .name(getItemDisplayName())
                .lore(
                        "&7生命: "
                                + (this.live / (this.maxLive * 1.0) <= 0.6
                                ? (this.live / (this.maxLive * 1.0) <= 0.3 ? "&c" : "&e")
                                : "&a")
                                + this.live + "&7/" + this.maxLive,
                        "",
                        "&7装备时,自身受到的伤害 &9-10% &7.",
                        ""
                )
                .canTrade(true)
                .canSaveToEnderChest(true)
                .deathDrop(false)
                .removeOnJoin(false)
                .internalName(getInternalName())
                .maxLive(this.maxLive)
                .live(this.live)
                .deathDrop(false)
                .canSaveToEnderChest(true)
                .removeOnJoin(false)
                .canDrop(false)
                .canTrade(true)
                .shiny();
        return applyOwnershipData(builder).buildWithUnbreakable();
    }
}
