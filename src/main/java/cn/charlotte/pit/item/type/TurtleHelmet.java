package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.item.ItemBuilder;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@AutoRegister
public class TurtleHelmet extends IMythicItem implements Listener {

    public TurtleHelmet() {
        this.maxLive = 50;
        this.live = 50;
    }

    @Override
    public String getInternalName() {
        return "Turtle_helmet";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&a海龟头";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.TURTLE_HELMET;
    }

    @Override
    public boolean isEnchanted() {
        return true;
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

    @Override
    public ItemStack toItemStack() {
        double ratio = live / (maxLive * 1.0);
        String lifeColor;

        if (ratio <= 0.3) {
            lifeColor = "&c";
        } else if (ratio <= 0.6) {
            lifeColor = "&e";
        } else {
            lifeColor = "&a";
        }

        ItemBuilder builder = new ItemBuilder(getItemDisplayMaterial())
                .name(getItemDisplayName())
                .lore(
                        "&7生命: " + lifeColor + live + "&7/" + maxLive,
                        "",
                        "&9硬度",
                        "&7非常的硬!跟下界合金头盔一样硬!",
                        "",
                        "&6穿着时提供下界合金头盔相同的伤害减免效果"
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
