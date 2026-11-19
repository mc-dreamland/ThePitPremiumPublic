package cn.charlotte.pit.item.type.mythic;

import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class GoldenHelmet extends IMythicItem {

    public GoldenHelmet() {
        this.maxLive = 50;
        this.live = 50;
    }

    @Override
    public String getInternalName() {
        return "kings_helmet";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&6国王的王冠";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.GOLDEN_HELMET;
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
                        "&9皇室",
                        "&7击杀经验&b +10%&7.",
                        "&7击杀金币&e +10%&7.",
                        "",
                        "&6穿着时提供钻石头盔相同的伤害减免效果"
                )
                .canTrade(true)
                .canSaveToEnderChest(true)
                .deathDrop(false)
                .itemArmor(3.0)
                .ArmorToughness(2.0)
                .removeOnJoin(false)
                .internalName(getInternalName())
                .maxLive(this.maxLive)
                .live(this.live)
                .canDrop(false)
                .shiny();
        return applyOwnershipData(builder).buildWithUnbreakable();
    }
}
