package cn.charlotte.pit.item.type.occultDoctrine;

import cn.charlotte.pit.event.PitToMysteryBuildEvent;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.menu.mystery.MysteryBuildData;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@AutoRegister
public class MysteryBoxFragment extends AbstractPitItem implements Listener {

    @Override
    public String getInternalName() {
        return "mystery_box_fragment";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&3神秘盒子残片";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.DISC_FRAGMENT_5)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .canTrade(true)
                .canSaveToEnderChest(true)
                .canDrop(false)
                .removeOnJoin(false)
                .lore(
                        "",
                        "&7破碎的残片,集齐10片以及一个行动礼盒即可合成神秘之盒",
                        "",
                        "&7在 &e神话造物台 &7中使用"
                )
                .build();
    }

    @EventHandler
    public void onBuild(PitToMysteryBuildEvent event) {
        MysteryBuildData data = event.getData();
        ItemStack offItem = event.getOffItem();
        ItemStack main = event.getMainItem();

        if (!PitItems.ACTION_GIFT_BOX.isInternalNameEquals(main)) return;
        data.setHandled(true);

        if (PitItems.MYSTERY_BOX_FRAGMENT.isInternalNameEquals(offItem)) {
            if (offItem.getAmount() < 10) {
                data.setCanceled(true);
                data.setCancelMessage("&c残片数量不足10个");
                return;
            }
            data.setDelCount(offItem.getAmount() - 10);
            data.setReturnItemStack(PitItems.MYSTERY_BOX_ONE.toItemStack());
            data.setReturnMessage(List.of("&a合成成功！获得神秘之盒 I"));
            data.setDelivery("TABLE");
        } else {
            data.setCanceled(true);
            data.setCancelMessage("&c所选的材料不对");
        }
    }
}
