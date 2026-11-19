package cn.charlotte.pit.menu.customname;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitConsumeItemEvent;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.type.CustomNamePaper;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import cn.charlotte.pit.util.menu.builder.ButtonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomNamePaperMenu extends Menu {

    private final String name;
    private final CustomNamePaperUse use;

    public CustomNamePaperMenu(UUID playerUuid, CustomNamePaper paper) {
        this.name = paper.getCustomName();
        this.use = new CustomNamePaperUse(playerUuid, paper.getUuid(), name);
    }

    @Override
    public String getTitle(Player player) {
        return "选择要命名的神话装备";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        if (!use.isActive()) return buttons;
        int slot = 0;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack itemStack = player.getInventory().getItem(i);
            if (!isRenameTarget(itemStack)) continue;

            final int inventorySlot = i;
            IMythicItem mythicItem = MythicUtil.getMythicItem(itemStack);
            if (mythicItem == null || !mythicItem.isEnchanted()) continue;
            ItemStack expectedItem = itemStack.clone();
            Button button = new ButtonBuilder(expectedItem.clone())
                    // Closing must not trigger ButtonListener's automatic reopen branch.
                    .setShouldUpdateHandler((viewer, buttonSlot, clickType) -> false)
                    .addClickHandler((viewer, buttonSlot, clickType, hotbarButton, currentItem) -> {
                        if (clickType != ClickType.LEFT && clickType != ClickType.RIGHT) return;
                        rename(viewer, inventorySlot, expectedItem);
                    }).build();
            buttons.put(slot++, button);
        }

        return buttons;
    }

    private void rename(Player player, int inventorySlot, ItemStack expectedItem) {
        if (!use.isActive() || currentlyOpenedMenus.get(player.getUniqueId()) != this) return;

        ItemStack target = player.getInventory().getItem(inventorySlot);
        if (!expectedItem.equals(target) || !isRenameTarget(target)) {
            reject(player, "&c目标装备已发生变化，请重新打开改名界面。");
            return;
        }
        // Parse afresh: renaming must not mutate the shared mythic-item cache before payment.
        IMythicItem mythicItem = Utils.getMythicItem0(target);
        if (mythicItem == null || !mythicItem.isEnchanted()) {
            reject(player, "&c仅支持修改已附魔的神话之剑和神话之弓。");
            return;
        }
        PlayerProfile profile = Util.getPitProfile(player);
        if (profile == null) {
            reject(player, "&c玩家数据尚未就绪，请稍后重试。");
            return;
        }
        // Build first: a serialization failure must not consume the voucher.
        mythicItem.setCustomName(CC.translate(name));
        ItemStack renamedItem = mythicItem.toItemStack();

        ItemStack paperItem = player.getInventory().getItemInMainHand();
        if (paperItem.getType() != Material.PAPER || paperItem.getAmount() < 1
                || !"CustomNamePaper".equals(ItemUtil.getInternalName(paperItem))) {
            reject(player, "&c请手持打开此界面时使用的已审核改名券。");
            return;
        }
        CustomNamePaper paper = new CustomNamePaper();
        try {
            paper.loadFromItemStack(paperItem);
        } catch (IllegalArgumentException exception) {
            reject(player, "&c改名券数据异常，请联系管理员。");
            return;
        }
        if (!use.tryUse(player.getUniqueId(), paper.getUuid(), paper.getCustomName(), paper.isAllowed())) {
            reject(player, "&c改名券已失效或发生变化，请重新打开改名界面。");
            return;
        }

        try {
            PlayerUtil.takeOneItem(player, EquipmentSlot.HAND);
            player.getInventory().setItem(inventorySlot, renamedItem);
            new PitConsumeItemEvent(player, "CustomNamePaper").callEvent();
            CC.boardCast("&e&l恭喜 " + profile.getFormattedName() + " &f将一件物品更名为了 " + name);
        } finally {
            closeMenu(player);
        }
    }

    private static boolean isRenameTarget(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() < 1) return false;
        String internalName = ItemUtil.getInternalName(item);
        return "mythic_sword".equals(internalName) || "mythic_bow".equals(internalName);
    }

    private void reject(Player player, String message) {
        use.close();
        player.sendMessage(CC.translate(message));
        closeMenu(player);
    }

    private void closeMenu(Player player) {
        // InventoryClickEvent is still processing; close next tick without closing a newer menu.
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            if (currentlyOpenedMenus.get(player.getUniqueId()) == this) {
                player.closeInventory();
            }
        });
    }

    @Override
    public void onClose(Player player) {
        use.close();
    }
}
