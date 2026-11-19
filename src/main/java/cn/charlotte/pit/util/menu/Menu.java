package cn.charlotte.pit.util.menu;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.util.MaterialConvert;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Menu {

    public static final Map<UUID, Menu> currentlyOpenedMenus = new ConcurrentHashMap<>();

    protected final static ItemStack P_PAGE = new ItemBuilder(Material.ARROW)
            .name("&f上一页")
            .build();

    protected final static ItemStack R_PAGE = new ItemBuilder(Material.ARROW)
            .name("&f主页")
            .build();

    protected final static ItemStack N_PAGE = new ItemBuilder(Material.ARROW)
            .name("&f下一页")
            .build();

    protected static final int[] PRESTIGE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private Map<Integer, Button> buttons = new HashMap<>();
    private boolean autoUpdate = false;
    private boolean updateAfterClick = true;
    private boolean closedByMenu = false;
    private boolean placeholder = false;
    private Button placeholderButton = Button.placeholder(MaterialConvert.GLASS_PANE[15], " ");

    private ItemStack createItemStack(Player player, Button button) {
        ItemStack item = button.getButtonItem(player);

        if (item == null) {
            return new ItemStack(Material.AIR);
        }

        return item;
    }

    public void openMenu(final Player player) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                openMenu(player);
            });
            return;
        }


        try {
            this.buttons = this.getButtons(player);

            Menu previousMenu = Menu.currentlyOpenedMenus.get(player.getUniqueId());
            Inventory inventory = null;
            int size = this.getSize() == -1 ? this.size(this.buttons) : this.getSize();
            boolean update = false;
            String title = CC.translate(this.getTitle(player));

            if (title.length() > 32) {
                title = title.substring(0, 32);
            }

            if (previousMenu == null) {
                player.closeInventory();
            } else {
                int previousSize = player.getOpenInventory().getTopInventory().getSize();
                if (previousSize == size && LegacyComponentSerializer.legacyAmpersand().serialize(player.getOpenInventory().title()).equals(title)) {
                    inventory = player.getOpenInventory().getTopInventory();
                    update = true;
                } else {
                    previousMenu.setClosedByMenu(true);
                    player.closeInventory();
                }
            }

            if (inventory == null) {
                inventory = Bukkit.createInventory(player, size, title);
            }

            inventory.setContents(new ItemStack[inventory.getSize()]);

            currentlyOpenedMenus.put(player.getUniqueId(), this);

            for (Map.Entry<Integer, Button> buttonEntry : this.buttons.entrySet()) {
                inventory.setItem(buttonEntry.getKey(), createItemStack(player, buttonEntry.getValue()));
            }

            if (this.isPlaceholder()) {
                for (int index = 0; index < size; index++) {
                    if (this.buttons.get(index) == null) {
                        this.buttons.put(index, this.placeholderButton);
                        inventory.setItem(index, this.placeholderButton.getButtonItem(player));
                    }
                }
            }

            if (update) {
                player.updateInventory();
            } else {
                player.openInventory(inventory);
            }

            this.onOpen(player);
            this.setClosedByMenu(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateMenu(final Player player) {
        // 检查玩家是否打开了当前菜单
        if (!player.isOnline()) return;

        Inventory topInventory = player.getOpenInventory().getTopInventory();
        if (topInventory == null) return;

        String currentTitle = LegacyComponentSerializer.legacyAmpersand().serialize(player.getOpenInventory().title());
        String expectedTitle = CC.translate(this.getTitle(player));
        String expectedTitleB = this.getTitle(player);
        if (expectedTitle.length() > 32) expectedTitle = expectedTitle.substring(0, 32);

        if (!currentTitle.equals(expectedTitle) && !currentTitle.equals(expectedTitleB)) {
            return;
        }

        // 更新按钮和物品
        try {
            this.buttons = this.getButtons(player);
            topInventory.clear();

            for (Map.Entry<Integer, Button> entry : this.buttons.entrySet()) {
                topInventory.setItem(entry.getKey(), createItemStack(player, entry.getValue()));
            }

            if (this.isPlaceholder()) {
                for (int i = 0; i < topInventory.getSize(); i++) {
                    if (!this.buttons.containsKey(i)) {
                        this.buttons.put(i, this.placeholderButton);
                        topInventory.setItem(i, this.placeholderButton.getButtonItem(player));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public int size(Map<Integer, Button> buttons) {
        int highest = 0;

        for (int buttonValue : buttons.keySet()) {
            if (buttonValue > highest) {
                highest = buttonValue;
            }
        }

        return (int) (Math.ceil((highest + 1) / 9D) * 9D);
    }

    public int getSlot(int x, int y) {
        return ((9 * y) + x);
    }

    public int getSize() {
        return -1;
    }

    public abstract String getTitle(Player player);

    public abstract Map<Integer, Button> getButtons(Player player);

    public void onOpen(Player player) {
    }

    public void onClose(Player player) {
    }

    public void onClickEvent(InventoryClickEvent event) {

    }

    public boolean isAutoUpdate() {
        return this.autoUpdate;
    }

    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    public boolean isUpdateAfterClick() {
        return this.updateAfterClick;
    }

    public void setUpdateAfterClick(boolean updateAfterClick) {
        this.updateAfterClick = updateAfterClick;
    }

    public boolean isClosedByMenu() {
        return this.closedByMenu;
    }

    public void setClosedByMenu(boolean closedByMenu) {
        this.closedByMenu = closedByMenu;
    }

    public boolean isPlaceholder() {
        return this.placeholder;
    }

    public void setPlaceholder(boolean placeholder) {
        this.placeholder = placeholder;
    }

    public Button getPlaceholderButton() {
        return this.placeholderButton;
    }

    public void setPlaceholderButton(Button placeholderButton) {
        this.placeholderButton = placeholderButton;
    }

    public Map<Integer, Button> getButtons() {
        return this.buttons;
    }

    public void setButtons(Map<Integer, Button> buttons) {
        this.buttons = buttons;
    }
}