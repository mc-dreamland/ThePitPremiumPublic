package cn.charlotte.pit.item;

import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.item.MythicBundleUuidUtil;
import cn.charlotte.pit.util.log.ItemOperationJson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

@Slf4j
@AutoRegister
public class pantsBundleFull extends AbstractPitItem implements IPlayerInteract {
    
    @Override
    public String getInternalName() {
        return "pants_bundle_full";
    }
    
    @Override
    protected String getDefaultItemDisplayName() {
        return "&b神话之甲收纳箱";
    }
    
    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.CHEST_MINECART)
                .name(getItemDisplayName())
                .lore(Arrays.asList(
                        "&7死亡后保留",
                        "&7这个收纳箱里装有:",
                        " ",
                        "&e10x 神话之甲",
                        " ",
                        "&7手持并右键以将其取出!"
                ))
                .canSaveToEnderChest(true)
                .canTrade(false)
                .internalName(getInternalName())
                .build();
    }
    
    @Override
    public void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) {
            event.setCancelled(true);
            return;
        }

        if (hand != EquipmentSlot.HAND) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedBlock() != null && (event.getClickedBlock().getType() == Material.CRAFTING_TABLE
                || event.getClickedBlock().getType() == Material.ANVIL
                || event.getClickedBlock().getType() == Material.CHEST
                || event.getClickedBlock().getType() == Material.TRAPPED_CHEST)) {
            event.setCancelled(true);
            return;
        }
        
        ItemStack handItem = player.getItemInHand();
        if (handItem == null || handItem.getType() == Material.AIR) return;
        if (!getInternalName().equals(ItemUtil.getInternalName(handItem))) return;
        
        long emptySlots = Arrays.stream(player.getInventory().getContents())
                .filter(stack -> stack == null || stack.getType() == Material.AIR)
                .limit(10)
                .count();
        if (emptySlots < 10) {
            player.sendMessage(CC.translate("&c背包空间不足以取出全部神话之甲!"));
            return;
        }

        player.getInventory().setItemInMainHand(null);
        java.util.List<ItemStack> rewards = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ItemStack itemStack1 = new MythicLeggingsItem().toItemStack();
            itemStack1 = new ItemBuilder(itemStack1).changeNbt("mythic_color", ItemUtil.getItemStringData(itemStack, "pants_" + i)).build();
            MythicLeggingsItem mythicItem = new MythicLeggingsItem();
            mythicItem.loadFromItemStack(itemStack1);
            MythicBundleUuidUtil.restore(mythicItem, itemStack, i);
            ItemStack reward = mythicItem.toItemStack();
            rewards.add(reward);
            player.getInventory().addItem(reward);
        }

        JsonObject operationLog = ItemOperationJson.operation("OPEN_BUNDLE", player);
        operationLog.addProperty("bundle", getInternalName());
        operationLog.add("items", ItemOperationJson.grantedItems(player, rewards));
        log.info("{}", ItemOperationJson.toJson(operationLog));
    }
}
