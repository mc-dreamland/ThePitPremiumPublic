package cn.charlotte.pit.item.type.mythic;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.item.ItemBuilder;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@AutoRegister
public class FlyBoots extends IMythicItem implements Listener {

    public FlyBoots() {
        this.live = 25;
        this.maxLive = 25;
    }

    @Override
    public boolean isEnchanted() {
        return true;
    }

    @Override
    public String getInternalName() {
        return "fly_boots";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&b小飞鞋";
    }

    @Override
    public ItemStack toItemStack() {
        ItemBuilder builder = new ItemBuilder(Material.IRON_BOOTS)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .live(this.live)
                .maxLive(this.maxLive)
                .lore(
                        "",
                        String.format("&7生命: %s%s&7/%s", Util.getLiveColor(this.live, this.maxLive), live, maxLive),
                        "",
                        "&b疾跑",
                        " &7装备时给予 10% 的速度提升(飞一般的感觉).",
                        "&f韧性",
                        " &7装备时 盔甲韧性 + 0.5 (比一般的装备有韧性).",
                        ""
                )
                .canTrade(true)
                .canSaveToEnderChest(true)
                .canDrop(false)
                .removeOnJoin(false)
                .ArmorToughness(0.5)
                .speed(0.01);
        return applyOwnershipData(builder).buildWithUnbreakable();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        if (item == null) return;

        var tag = NBT.readNbt(item);
        if (tag == null) return;

        var extra = tag.getCompound("extra");
        if (extra == null) return;

        loadOwnershipData(extra);
        this.maxLive = extra.getInteger("maxLive");
        this.live = extra.getInteger("live");
    }
}
