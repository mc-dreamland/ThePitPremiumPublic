package cn.charlotte.pit.item.type.mythic;

import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.IMythicShovel;
import org.bukkit.Material;

public class MythicShovelItem extends IMythicItem implements IMythicShovel {

    @Override
    public String getInternalName() {
        return "MythicShovel";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&3神话之铲";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.GOLDEN_SHOVEL;
    }

    @Override
    public double getItemDamage() {
        return 5.5;
    }
}
