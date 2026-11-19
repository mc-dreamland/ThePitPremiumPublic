package cn.charlotte.pit.item.type.occultDoctrine;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
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

public class ExperienceBottle extends AbstractPitItem implements IPlayerInteract {

    private int count;

    public ExperienceBottle(){
        this.count = 0;
    }

    public ExperienceBottle(int count) {
        this.count = count;
    }

    @Override
    public void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent event) {
        PlayerUtil.takeOneItem(player,hand);
        var tag = NBT.readNbt(itemStack);
        int count = 0;
        ReadableNBT compound = tag.getCompound("extra");
        if (compound != null) {
            count = compound.getInteger("experience_bottle");
        }
        if(count < 0) return;
        Util.getPitProfile(player).addExp(count);
        CC.send(player,"&a获得了&b%s经验",count);
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .canSaveToEnderChest(true)
                .canTrade(true)
                .changeNbt("experience_bottle",this.count)
                .lore(
                        "",
                        "&7一瓶装满经验的瓶子.",
                        String.format("&7存储经验: %s",this.count),
                        ""
                )
                .shiny()
                .build();
    }

    @Override
    public String getInternalName() {
        return "experience_bottle";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "经验之瓶";
    }
}
