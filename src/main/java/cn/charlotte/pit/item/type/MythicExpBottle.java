package cn.charlotte.pit.item.type;

import cn.charlotte.pit.buff.AbstractPitBuff;
import cn.charlotte.pit.buff.impl.MythicBuff;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class MythicExpBottle extends AbstractPitItem implements IPlayerInteract {

    @Override
    protected String getDefaultItemDisplayName() {
        return "&d神秘的瓶子";
    }

    @Override
    public String getInternalName() {
        return "mysterious_bottle";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name(getItemDisplayName())
                .lore(
                        "&7死亡后保留",
                        "",
                        "&7质朴的瓶子,手持并右键使用",
                        " &a-将你的神话掉落倍率翻倍!&7(持续30分钟)",
                        ""
                )
                .internalName("mysterious_bottle")
                .canSaveToEnderChest(true)
                .canTrade(true)
                .deathDrop(false)
                .shiny()
                .build();
    }

    @Override
    public void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent event) {
        event.setCancelled(true);
        PlayerUtil.takeOneItem(player, hand);
        CC.send(MessageType.MISC, player, "&a你感觉有一股神秘的力量传入到了你的身体里");
        AbstractPitBuff buff = new MythicBuff();
        buff.stackBuff(player, 1800000L, 1, true);
    }
}
