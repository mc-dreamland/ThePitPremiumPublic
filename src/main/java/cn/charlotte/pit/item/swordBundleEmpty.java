package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AutoRegister
public class swordBundleEmpty extends AbstractPitItem implements IPlayerInteract {
    
    @Override
    public String getInternalName() {
        return "sword_bundle_empty";
    }
    
    @Override
    protected String getDefaultItemDisplayName() {
        return "&b神话之剑收纳箱";
    }
    
    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.MINECART)
                .name("&b神话之剑收纳箱")
                .lore(Arrays.asList("&7死亡后保留", " ", "&7手持右键即可将背包内", "&710件未附魔的神话之剑打包."))
                .canSaveToEnderChest(true)
                .internalName("sword_bundle_empty")
                .build();
    }
    
    @Override
    public void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL) {
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
        
        long mythicCount = Arrays.stream(player.getInventory().getContents())
                .filter(stack -> stack != null && "mythic_sword".equalsIgnoreCase(ItemUtil.getInternalName(stack)))
                .filter(stack -> !MythicOwnershipUtil.isTraded(stack))
                .map(stack -> {
                    AbstractPitItem pitItem = new MythicSwordItem();
                    pitItem.loadFromItemStack(stack);
                    return pitItem;
                })
                .filter(pitItem -> !pitItem.isEnchanted())
                .limit(10)
                .count();
        
        if (mythicCount < 10) {
            player.sendMessage(CC.translate("&c你的背包内需要有10条及以上未附魔且从未交易的神话之剑才能使用此物品!"));
            return;
        }

        int removed = 0;
        List<ItemStack> packedItems = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack == null) continue;
            if ("mythic_sword".equalsIgnoreCase(ItemUtil.getInternalName(stack))) {
                AbstractPitItem pitItem = new MythicSwordItem();
                pitItem.loadFromItemStack(stack);
                if (!pitItem.isEnchanted() && !MythicOwnershipUtil.isTraded(stack)) {
                    packedItems.add(stack.clone());
                    player.getInventory().setItem(i, null);
                    removed++;
                    if (removed >= 10) break;
                }
            }
        }

        ItemBuilder fullBundle = new ItemBuilder(PitItems.SWORD_BUNDLE_FULL.toItemStack());
        for (int i = 0; i < packedItems.size(); i++) {
            MythicBundleUuidUtil.store(fullBundle, i, packedItems.get(i));
        }
        ItemStack packedBundle = fullBundle.build();
        AdvancedItemIdentity.inheritUuid(item, packedBundle);
        player.setItemInHand(packedBundle);

        JsonObject operationLog = ItemOperationJson.operation("PACK_BUNDLE", player);
        operationLog.addProperty("bundle", "sword_bundle_full");
        operationLog.add("items", ItemOperationJson.items(packedItems));
        log.info("{}", ItemOperationJson.toJson(operationLog));
    }
}
