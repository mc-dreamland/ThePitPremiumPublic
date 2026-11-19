package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.nametag.hiding.NameHidingManager;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class NameRevealEye extends AbstractPitItem {
    @Override public String getInternalName() { return NameHidingManager.REVEAL_ITEM; }
    @Override protected String getDefaultItemDisplayName() { return "&b显形药水"; }
    @Override public Material getItemDisplayMaterial() { return Material.ENDER_EYE; }

    @Override
    public ItemStack toItemStack() {
        NameHidingManager manager = NameHidingManager.getInstance();
        long seconds = manager == null ? 300 : manager.config().durationSeconds();
        return new ItemBuilder(getItemDisplayMaterial()).name(getItemDisplayName())
                .lore("&7右键消耗一个，在 &b" + seconds + " 秒&7内", "&7看见被隐藏的玩家名字，仅自己可见。",
                        "&7前缀、后缀与其他玩家的视角不受影响。", "&7效果不叠加，离线或重启后失效。", "&7死亡后保留")
                .internalName(getInternalName()).deathDrop(false).canDrop(false).canTrade(true)
                .canSaveToEnderChest(true).build();
    }
}
