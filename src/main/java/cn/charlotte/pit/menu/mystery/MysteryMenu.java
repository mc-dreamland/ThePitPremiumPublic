package cn.charlotte.pit.menu.mystery;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.menu.mystery.button.MysteryBuildButton;
import cn.charlotte.pit.menu.mystery.button.MysteryMainButton;
import cn.charlotte.pit.menu.mystery.button.MysteryOffButton;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.log.ItemOperationJson;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class MysteryMenu extends Menu {

    private static final List<String> mainList = new ArrayList<>(List.of(
            PitItems.MYSTERY_BOX_ONE.getInternalName(),
            PitItems.MYSTERY_BOX_TWO.getInternalName(),
            PitItems.MYSTERY_BOX_THREE.getInternalName(),
            PitItems.ACTION_GIFT_BOX.getInternalName()
    ));
    private static final List<String> offList = new ArrayList<>(List.of(
            PitItems.MYSTERIOUS_FRAGMENTS.getInternalName(),
            PitItems.CHUNK_OF_VILE.getInternalName(),
            PitItems.MYSTERY_BOX_FRAGMENT.getInternalName()
    ));

    @Override
    public String getTitle(Player player) {
        return "神秘造物台";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        return Map.of(
                13, new MysteryMainButton(),
                22, new MysteryOffButton(),
                31, new MysteryBuildButton()
        );
    }

    @Override
    public void onClickEvent(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!(event.getClickedInventory() instanceof PlayerInventory)) return;

        ItemStack clickItemStack = event.getCurrentItem();
        if (clickItemStack == null || clickItemStack.getType() == Material.AIR) return;

        PlayerProfile profile = Util.getPitProfile(player);
        String internalName = ItemUtil.getInternalName(clickItemStack);
        boolean mainContains = mainList.contains(internalName);
        boolean offContains = offList.contains(internalName);
        if (internalName == null || (!mainContains && !offContains)) {
            CC.send(MessageType.MISC, player, "&c请放入特殊物品!");
            return;
        }
        if (mainContains && !Util.isEmpty(profile.getMysteryMainItem())) {
            CC.send(MessageType.MISC, player, "&c请先取下造物台中的物品!");
            return;
        }
        ItemStack offItem = InventoryUtil.deserializeItemStack(profile.getMysteryOffItem());
        if (offItem != null) {
            if(offContains && !internalName.equals(ItemUtil.getInternalName(offItem))) {
                CC.send(player, "&c造物台中的材料与你所选的材料不匹配!");
                return;
            }
            if (offContains && offItem.getAmount() >= 64) {
                CC.send(player, "&c造物台中的材料够多了!");
                return;
            }
        }

        ItemStack itemToStore = clickItemStack.clone();
        itemToStore.setAmount(1);
        String serializedItem = InventoryUtil.serializeItemStack(itemToStore);

        if (mainContains) {
            profile.setMysteryMainItem(serializedItem);
        } else if (offItem == null) {
            profile.setMysteryOffItem(serializedItem);
        } else {
            ItemStack updatedOffItem = offItem.clone();
            updatedOffItem.setAmount(offItem.getAmount() + 1);
            profile.setMysteryOffItem(InventoryUtil.serializeItemStack(updatedOffItem));
        }

        int newAmount = clickItemStack.getAmount() - 1;
        if (newAmount <= 0) {
            event.setCurrentItem(PitItems.AIR.toItemStack());
        } else {
            ItemStack remainingInventoryItem = clickItemStack.clone();
            remainingInventoryItem.setAmount(newAmount);
            event.setCurrentItem(remainingInventoryItem);
        }

        JsonObject operationLog = ItemOperationJson.operation("MYSTERY_STORE", player);
        operationLog.addProperty("slot", mainContains ? "INPUT" : "MATERIAL");
        operationLog.add("item", ItemOperationJson.item(itemToStore));
        log.info("{}", ItemOperationJson.toJson(operationLog));
        this.openMenu(player);
    }

    @Override
    public int getSize() {
        return 54;
    }
}
