package cn.charlotte.pit.item.type.mythic;

import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ArmageddonBoots extends IMythicItem {

    public ArmageddonBoots() {
        this.maxLive = 66;
        this.live = 66;
    }

    @Override
    public String getInternalName() {
        return "armageddon_boots";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&c终末之靴";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.LEATHER_BOOTS;
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
        String lifeColor;
        double ratio = live / (maxLive * 1.0);

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
                        "&7攻击其他玩家时免疫 &9无尽黑暗 &7附魔效果.",
                        ""
                )
                .internalName(getInternalName())
                .maxLive(maxLive)
                .live(live)
                .setLetherColor(Color.RED)
                .deathDrop(false)
                .canSaveToEnderChest(true)
                .removeOnJoin(false)
                .canDrop(false)
                .canTrade(true)
                .shiny();
        return applyOwnershipData(builder).buildWithUnbreakable();
    }
}
