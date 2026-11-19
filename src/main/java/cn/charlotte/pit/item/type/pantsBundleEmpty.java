package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.AdvancedItemIdentity;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.item.MythicBundleUuidUtil;
import cn.charlotte.pit.util.item.MythicOwnershipUtil;
import cn.charlotte.pit.util.log.ItemOperationJson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@AutoRegister
public class pantsBundleEmpty extends AbstractPitItem implements IPlayerInteract {
    
    @Override
    public String getInternalName() {
        return "pants_bundle_empty";
    }
    
    @Override
    protected String getDefaultItemDisplayName() {
        return "&b神话之甲收纳箱";
    }
    
    @Override
    public ItemStack toItemStack() {
        List<String> lore = new ArrayList<>();
        lore.add("&7死亡后保留");
        lore.add(" ");
        lore.add("&7手持右键即可将背包内");
        lore.add("&710件未附魔的神话之甲打包.");
        return new ItemBuilder(Material.MINECART)
                .name(getItemDisplayName())
                .lore(lore)
                .canSaveToEnderChest(true)
                .canTrade(true)
                .internalName(getInternalName())
                .build();
    }
    
    @Override
    public void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) {
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
        
        int mythicCount = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack != null && "mythic_leggings".equalsIgnoreCase(ItemUtil.getInternalName(stack))) {
                AbstractPitItem pitItem = new MythicLeggingsItem();
                pitItem.loadFromItemStack(stack);
                if (!pitItem.isEnchanted() && !MythicOwnershipUtil.isTraded(stack)) {
                    mythicCount++;
                    if (mythicCount >= 10) break;
                }
            }
        }
        if (mythicCount < 10) {
            player.sendMessage(CC.translate("&c你的背包内需要有10条及以上未附魔且从未交易的神话之甲才能使用此物品!"));
            return;
        }
        
        int count = 0;
        List<ItemStack> packedItems = new ArrayList<>();
        ItemBuilder fullBuilder = new ItemBuilder(Material.CHEST_MINECART)
                .name("&b神话之甲收纳箱")
                .canSaveToEnderChest(true)
                .canTrade(false)
                .internalName("pants_bundle_full");
        HashMap<MythicColor, Integer> colorCount = new HashMap<>();
        try {
            for (int i = 0; i < 36; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack != null && "mythic_leggings".equalsIgnoreCase(ItemUtil.getInternalName(stack))) {
                    AbstractPitItem pitItem = new MythicLeggingsItem();
                    pitItem.loadFromItemStack(stack);
                    if (!pitItem.isEnchanted() && !MythicOwnershipUtil.isTraded(stack)) {
                        packedItems.add(stack.clone());
                        String internalColor = ItemUtil.getItemStringData(stack, "mythic_color");
                        fullBuilder.changeNbt("pants_" + count, internalColor);
                        MythicBundleUuidUtil.store(fullBuilder, count, stack);
                        player.getInventory().setItem(i, new ItemBuilder(Material.AIR).build());
                        colorCount.put(MythicColor.valueOfInternalName(internalColor),
                                colorCount.getOrDefault(MythicColor.valueOfInternalName(internalColor), 0) + 1);
                        count++;
                        if (count >= 10) break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("", e);
            return;
        }
        
        List<String> lore = new ArrayList<>();
        lore.add("&7死亡后保留");
        lore.add("&7这个收纳箱里装有:");
        lore.add(" ");
        colorCount.forEach((color, amount) -> lore.add(String.format("&f%sx %s色神话之甲", amount, color.getChatColor() + color.getDisplayName())));
        lore.add(" ");
        lore.add("&7手持并右键以将其取出!");
        fullBuilder.lore(lore);

        ItemStack packedBundle = fullBuilder.build();
        AdvancedItemIdentity.inheritUuid(handItem, packedBundle);
        player.setItemInHand(packedBundle);

        JsonObject operationLog = ItemOperationJson.operation("PACK_BUNDLE", player);
        operationLog.addProperty("bundle", "pants_bundle_full");
        operationLog.add("items", ItemOperationJson.items(packedItems));
        log.info("{}", ItemOperationJson.toJson(operationLog));
    }
}
