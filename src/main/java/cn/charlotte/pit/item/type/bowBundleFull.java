package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AutoRegister
public class bowBundleFull extends AbstractPitItem implements IPlayerInteract {
    
    @Override
    public String getInternalName() {
        return "bow_bundle_full";
    }
    
    @Override
    protected String getDefaultItemDisplayName() {
        return "&b神话之弓收纳箱";
    }
    
    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.CHEST_MINECART)
                .name("&b神话之弓收纳箱")
                .lore(Arrays.asList(
                        "&7死亡后保留",
                        "&7这个收纳箱里装有:",
                        " ",
                        "&e10x 神话之弓",
                        " ",
                        "&7手持并右键以将其取出!"
                ))
                .canSaveToEnderChest(true)
                .canTrade(false)
                .internalName("bow_bundle_full")
                .build();
    }
    
    @Override
    public void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL) {
            e.setCancelled(true);
            return;
        }
        if (hand != EquipmentSlot.HAND) {
            e.setCancelled(true);
            return;
        }
        if (e.getClickedBlock() != null && (e.getClickedBlock().getType() == Material.CRAFTING_TABLE
                || e.getClickedBlock().getType() == Material.ANVIL || e.getClickedBlock().getType() == Material.CHEST || e.getClickedBlock().getType() == Material.TRAPPED_CHEST)) {
            e.setCancelled(true);
            return;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;
        
        if (Arrays.stream(player.getInventory().getContents())
                .filter(stack -> stack == null || stack.getType() == Material.AIR)
                .limit(10)
                .count() < 10) {
            player.sendMessage(CC.translate("&c背包空间不足以取出全部神话之弓!"));
            return;
        }

        player.getInventory().setItemInMainHand(null);
        List<ItemStack> rewards = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            MythicBowItem mythicBow = new MythicBowItem();
            MythicBundleUuidUtil.restore(mythicBow, item, i);
            ItemStack reward = mythicBow.toItemStack();
            rewards.add(reward);
            player.getInventory().addItem(reward);
        }

        JsonObject operationLog = ItemOperationJson.operation("OPEN_BUNDLE", player);
        operationLog.addProperty("bundle", getInternalName());
        operationLog.add("items", ItemOperationJson.grantedItems(player, rewards));
        log.info("{}", ItemOperationJson.toJson(operationLog));
    }
}
