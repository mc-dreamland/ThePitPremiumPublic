package cn.charlotte.pit.activity.newyear.item;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.newyear.medal.NewYear;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class CommemorativeCoin extends AbstractPitItem implements IPlayerInteract {

    @Override
    public String getInternalName() {
        return "commemorative_coin";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&e纪念币";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.GOLD_NUGGET;
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(getItemDisplayMaterial())
                .name(getItemDisplayName())
                .lore(
                        "",
                        "&f手持右键签下属于你的名字!",
                        "",
                        "&7&o传说有个幸运儿在签下了自己的名字,得到了一份神秘礼物...",
                        ""
                )
                .changeNbt("name", "null")
                .internalName(getInternalName())
                .canSaveToEnderChest(true)
                .canTrade(true)
                .deathDrop(false)
                .build();
    }


    @Override
    public void loadFromItemStack(ItemStack item) {
        super.loadFromItemStack(item);
    }

    @Override
    public void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent event) {
        @NotNull ItemStack stack = itemStack;
        if (!"null".equals(ItemUtil.getItemStringData(stack, "name"))) {
            if (!event.getAction().isLeftClick()) {
                CC.send(MessageType.MISC, player, "&c这个纪念币已经被刻写了,不能重复刻写");
            }
            return;
        }
        List<String> lore = stack.getLore();
        PlayerProfile profile = Util.getPitProfile(player);

        if (lore != null) {
            lore.clear();
        }
        lore = List.of(
                "",
                "&e持之以恒",
                String.format("&f来自 %s", profile.getFormattedName()),
                String.format("&f签署于 &7%s", Util.tyyyy_MM_dd_h_min_s.format(System.currentTimeMillis())),
                "",
                "&7&o一枚金光闪闪的硬币,上面刻着一位玩家的昵称",
                ""
        );
        PlayerUtil.takeOneItem(player, hand);
        player.getInventory().addItem(
                new ItemBuilder(stack)
                        .changeNbt("name", player.getName())
                        .lore(lore)
                        .shiny()
                        .build());
        new NewYear().addProgress(profile, 1);
    }
}
