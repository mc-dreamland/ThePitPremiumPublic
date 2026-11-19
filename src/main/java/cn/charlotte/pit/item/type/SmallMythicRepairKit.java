package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.item.type.mythic.LuckyChestplate;
import cn.charlotte.pit.menu.mythicrepair.SmallMythicRepairKitMenu;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class SmallMythicRepairKit extends AbstractPitItem implements IPlayerInteract {

    public static ItemStack toItemStack0() {
        return new ItemBuilder(Material.SHEARS)
                .name("&d简易工匠包")
                .lore(
                        "&7右键打开菜单选择要修复的神话装备",
                        "&7即可恢复选中的神话物品3条生命.",
                        "&7(简易工匠包会在使用后消耗)"
                )
                .internalName("Small_mythic_repair_kit")
                .canSaveToEnderChest(true)
                .canTrade(false)
                .shiny()
                .buildWithUnbreakable();
    }

    @Override
    public String getInternalName() {
        return "Small_mythic_repair_kit";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "简易工匠包";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.SHEARS;
    }

    @Override
    public ItemStack toItemStack() {
        return toItemStack0();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {

    }

    @Override
    public void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent event) {
        if (hand != EquipmentSlot.HAND) return;
        new SmallMythicRepairKitMenu().openMenu(player);
    }
}