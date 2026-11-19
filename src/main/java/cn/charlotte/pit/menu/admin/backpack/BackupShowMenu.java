package cn.charlotte.pit.menu.admin.backpack;

import cn.charlotte.pit.data.PlayerInvBackup;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.menu.admin.backpack.button.ItemShowButton;
import cn.charlotte.pit.menu.admin.backpack.button.RollbackButton;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import cn.charlotte.pit.util.menu.buttons.BackButton;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: EmptyIrony
 * @Date: 2021/2/4 20:47
 */
public class BackupShowMenu extends Menu {
    private static final int MENU_SIZE = 54;
    private static final int CONTENT_SLOTS = 45;
    private static final int PREVIOUS_BUTTON_SLOT = 45;
    private static final int BACK_BUTTON_SLOT = 49;
    private static final int NEXT_BUTTON_SLOT = 53;
    private static final int PREVIOUS_WAREHOUSE_PAGE_SLOT = 46;
    private static final int ROLLBACK_BUTTON_SLOT = 47;
    private static final int DETAIL_BACK_BUTTON_SLOT = 50;
    private static final int NEXT_WAREHOUSE_PAGE_SLOT = 52;

    private final PlayerProfile playerProfile;
    private final PlayerInvBackup backup;
    private final boolean right;
    private final int warehousePage;
    private final Menu backMenu;
    private final Button previousBackupButton;
    private final Button nextBackupButton;

    public BackupShowMenu(PlayerProfile playerProfile, PlayerInvBackup backup, boolean right) {
        this(playerProfile, backup, right, 0, null, null, null);
    }

    public BackupShowMenu(PlayerProfile playerProfile, PlayerInvBackup backup, boolean right, Menu backMenu) {
        this(playerProfile, backup, right, 0, backMenu, null, null);
    }

    public BackupShowMenu(PlayerProfile playerProfile,
                          PlayerInvBackup backup,
                          boolean right,
                          int warehousePage,
                          Menu backMenu) {
        this(playerProfile, backup, right, warehousePage, backMenu, null, null);
    }

    public BackupShowMenu(PlayerProfile playerProfile,
                          PlayerInvBackup backup,
                          boolean right,
                          Menu backMenu,
                          Button previousBackupButton,
                          Button nextBackupButton) {
        this(playerProfile, backup, right, 0, backMenu, previousBackupButton, nextBackupButton);
    }

    private BackupShowMenu(PlayerProfile playerProfile,
                           PlayerInvBackup backup,
                           boolean right,
                           int warehousePage,
                           Menu backMenu,
                           Button previousBackupButton,
                           Button nextBackupButton) {
        this.playerProfile = playerProfile;
        this.backup = backup;
        this.right = right;
        this.warehousePage = Math.max(0, warehousePage);
        this.backMenu = backMenu;
        this.previousBackupButton = previousBackupButton;
        this.nextBackupButton = nextBackupButton;
    }

    @Override
    public String getTitle(Player player) {
        if (right) {
            return playerProfile.getPlayerName() + " 的仓库备份 (" + (warehousePage + 1) + ")";
        }
        return playerProfile.getPlayerName() + " 的背包备份";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttonMap = new HashMap<>();

        if (right) {
            addWarehouseButtons(buttonMap);
        } else {
            if (backup.getInv() != null) {
                ItemStack[] contents = backup.getInv().getContents();
                if (contents != null) {
                    for (int i = 0; i < Math.min(contents.length, CONTENT_SLOTS); i++) {
                        buttonMap.put(i, new ItemShowButton(contents[i], true));
                    }
                }

                ItemStack[] armorContents = backup.getInv().getArmorContents();
                if (armorContents != null) {
                    for (int i = 0; i < Math.min(armorContents.length, 4); i++) {
                        buttonMap.put(36 + i, new ItemShowButton(armorContents[i], true));
                    }
                }
            }

            int rollbackSlot = hasBackupNavigation() ? ROLLBACK_BUTTON_SLOT : PREVIOUS_BUTTON_SLOT;
            buttonMap.put(rollbackSlot, new RollbackButton(
                    new ItemBuilder(Material.CHEST).name("&a回滚至该背包").shiny().build(),
                    playerProfile,
                    backup
            ));
            buttonMap.put(BACK_BUTTON_SLOT, new ViewWarehouseButton());
            if (backMenu != null) {
                buttonMap.put(hasBackupNavigation() ? DETAIL_BACK_BUTTON_SLOT : NEXT_BUTTON_SLOT,
                        new BackButton(backMenu));
            }
        }

        addBackupNavigationButtons(buttonMap);

        return buttonMap;
    }

    @Override
    public int getSize() {
        return MENU_SIZE;
    }

    public boolean isWarehouseView() {
        return right;
    }

    private void addWarehouseButtons(Map<Integer, Button> buttonMap) {
        List<ItemStack> warehouseItems = getWarehouseItems();
        int start = warehousePage * CONTENT_SLOTS;
        int end = Math.min(start + CONTENT_SLOTS, warehouseItems.size());

        for (int slot = start; slot < end; slot++) {
            buttonMap.put(slot - start, new ItemShowButton(warehouseItems.get(slot), true));
        }

        int pageCount = Math.max(1, (warehouseItems.size() + CONTENT_SLOTS - 1) / CONTENT_SLOTS);
        if (warehousePage > 0) {
            buttonMap.put(hasBackupNavigation() ? PREVIOUS_WAREHOUSE_PAGE_SLOT : PREVIOUS_BUTTON_SLOT,
                    new WarehousePreviousPageButton());
        }
        if (warehousePage + 1 < pageCount) {
            buttonMap.put(hasBackupNavigation() ? NEXT_WAREHOUSE_PAGE_SLOT : NEXT_BUTTON_SLOT,
                    new WarehouseNextPageButton());
        }
        if (backMenu != null) {
            buttonMap.put(hasBackupNavigation() ? DETAIL_BACK_BUTTON_SLOT : BACK_BUTTON_SLOT,
                    new BackButton(backMenu));
        }
        if (hasBackupNavigation()) {
            buttonMap.put(BACK_BUTTON_SLOT, new ViewInventoryButton());
        }
    }

    private boolean hasBackupNavigation() {
        return previousBackupButton != null || nextBackupButton != null;
    }

    private void addBackupNavigationButtons(Map<Integer, Button> buttonMap) {
        if (previousBackupButton != null) {
            buttonMap.put(PREVIOUS_BUTTON_SLOT, previousBackupButton);
        }
        if (nextBackupButton != null) {
            buttonMap.put(NEXT_BUTTON_SLOT, nextBackupButton);
        }
    }

    private List<ItemStack> getWarehouseItems() {
        List<ItemStack> items = new ArrayList<>();
        PlayerEnderChest[] chests = backup.getChest();
        if (chests == null) {
            return items;
        }

        for (PlayerEnderChest chest : chests) {
            if (chest == null) {
                continue;
            }
            Inventory inventory = chest.getInventory();
            if (inventory == null) {
                continue;
            }
            for (ItemStack item : inventory.getContents()) {
                items.add(item);
            }
        }
        return items;
    }

    private class ViewWarehouseButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.ENDER_CHEST)
                    .name("&b查看仓库")
                    .lore("&7查看该备份中的仓库内容")
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            Button.playNeutral(player);
            Menu warehouseBackMenu = hasBackupNavigation() ? backMenu : BackupShowMenu.this;
            new BackupShowMenu(
                    playerProfile,
                    backup,
                    true,
                    0,
                    warehouseBackMenu,
                    previousBackupButton,
                    nextBackupButton
            ).openMenu(player);
        }
    }

    private class ViewInventoryButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.CHEST)
                    .name("&b查看背包")
                    .lore("&7查看该备份中的背包内容")
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            Button.playNeutral(player);
            new BackupShowMenu(
                    playerProfile,
                    backup,
                    false,
                    0,
                    backMenu,
                    previousBackupButton,
                    nextBackupButton
            ).openMenu(player);
        }
    }

    private class WarehousePreviousPageButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.ARROW)
                    .name("&6上一页仓库")
                    .lore("&7当前页: &f" + (warehousePage + 1))
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            Button.playNeutral(player);
            new BackupShowMenu(
                    playerProfile,
                    backup,
                    true,
                    warehousePage - 1,
                    backMenu,
                    previousBackupButton,
                    nextBackupButton
            ).openMenu(player);
        }
    }

    private class WarehouseNextPageButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.ARROW)
                    .name("&a下一页仓库")
                    .lore("&7当前页: &f" + (warehousePage + 1))
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            Button.playNeutral(player);
            new BackupShowMenu(
                    playerProfile,
                    backup,
                    true,
                    warehousePage + 1,
                    backMenu,
                    previousBackupButton,
                    nextBackupButton
            ).openMenu(player);
        }
    }
}
