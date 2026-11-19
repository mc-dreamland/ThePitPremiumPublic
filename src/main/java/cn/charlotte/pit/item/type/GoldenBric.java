package cn.charlotte.pit.item.type;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class GoldenBric extends AbstractPitItem implements IPlayerInteract {

    private int count;

    public GoldenBric(){
        this.count = 0;
    }

    public GoldenBric(int count) {
        this.count = count;
    }

    @Override
    public void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent event) {
        PlayerUtil.takeOneItem(player,hand);
        var tag = NBT.readNbt(itemStack);
        int count = 0;
        ReadableNBT compound = tag.getCompound("extra");
        if (compound != null) {
            count = compound.getInteger("goden_bric");
        }
        if(count < 0) return;
        Util.getPitProfile(player).addCoins(count);
        CC.send(player,"&a获得了&e%s金币",count);
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.GOLD_INGOT)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .canSaveToEnderChest(true)
                .canTrade(true)
                .changeNbt("goden_bric",this.count)
                .lore(
                        "",
                        "&7一块厚重的金砖.",
                        String.format("&7价值: %s",this.count),
                        ""
                )
                .shiny()
                .build();
    }

    @Override
    public String getInternalName() {
        return "goden_bric";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "金砖";
    }
}
