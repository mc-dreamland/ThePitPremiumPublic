package cn.charlotte.pit.menu.admin.itembuilder;

import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ItemBuilderMenu extends Menu {

    private static final ItemStack on = new ItemBuilder(Material.GREEN_WOOL).name("&a状态:启用").build();
    private static final ItemStack off = new ItemBuilder(Material.RED_WOOL).name("&a状态:禁用").build();

    private final ItemBuilder itemBuilder;

    public ItemBuilderMenu(ItemBuilder itemBuilder) {
        this.itemBuilder = itemBuilder;
    }

    @Override
    public String getTitle(Player player) {
        return "构造界面";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        int slot = 0;

        return buttons;
    }

    private class ShinyButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return itemBuilder.isShiny() ? on : off;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            itemBuilder.shiny(!itemBuilder.isShiny());
            new ItemBuilderMenu(itemBuilder).openMenu(player);
        }
    }

    private class CanDropButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return itemBuilder.isCanDrop() ? on : off;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            itemBuilder.canDrop(!itemBuilder.isCanDrop());
            new ItemBuilderMenu(itemBuilder).openMenu(player);
        }
    }

    private class CanTrashButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return itemBuilder.isCanTrash() ? on : off;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            itemBuilder.canTrash(!itemBuilder.isCanTrash());
            new ItemBuilderMenu(itemBuilder).openMenu(player);
        }
    }

    private class HealingItemButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return itemBuilder.isHealingItem() ? on : off;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            itemBuilder.isHealingItem(!itemBuilder.isHealingItem());
            new ItemBuilderMenu(itemBuilder).openMenu(player);
        }
    }

    private class CanTradeButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return itemBuilder.isCanTrade() ? on : off;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            itemBuilder.canTrade(!itemBuilder.isCanTrade());
            new ItemBuilderMenu(itemBuilder).openMenu(player);
        }
    }

    private class DefaultItemButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return itemBuilder.isDefaultItem() ? on : off;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            itemBuilder.defaultItem(!itemBuilder.isDefaultItem());
            new ItemBuilderMenu(itemBuilder).openMenu(player);
        }
    }

    private class CanSaveToEnderChestButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return itemBuilder.isCanSaveToEnderChest() ? on : off;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            itemBuilder.canSaveToEnderChest(!itemBuilder.isCanSaveToEnderChest());
            new ItemBuilderMenu(itemBuilder).openMenu(player);
        }
    }

    private class RemoveOnJoinButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return itemBuilder.isRemoveOnJoin() ? on : off;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            itemBuilder.removeOnJoin(!itemBuilder.isRemoveOnJoin());
            new ItemBuilderMenu(itemBuilder).openMenu(player);
        }
    }

    private class DeathDropButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return itemBuilder.isDeathDrop() ? on : off;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            itemBuilder.deathDrop(!itemBuilder.isDeathDrop());
            new ItemBuilderMenu(itemBuilder).openMenu(player);
        }
    }

    private class BoostedByGlobalGemButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return itemBuilder.isBoostedByGlobalGem() ? on : off;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            itemBuilder.makeBoostedByGlobalGem(!itemBuilder.isBoostedByGlobalGem());
            new ItemBuilderMenu(itemBuilder).openMenu(player);
        }
    }

    private class BoostedByGemButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return itemBuilder.isBoostedByGem() ? on : off;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            itemBuilder.makeBoostedByGem(!itemBuilder.isBoostedByGem());
            new ItemBuilderMenu(itemBuilder).openMenu(player);
        }
    }

    private class BoostedByEternalButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return itemBuilder.isBoostedByEternal() ? on : off;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            itemBuilder.makeBoostedByEternal(!itemBuilder.isBoostedByEternal());
            new ItemBuilderMenu(itemBuilder).openMenu(player);
        }
    }

    private class BoostedByBookButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return itemBuilder.isBoostedByBook() ? on : off;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            itemBuilder.makeBoostedByBook(!itemBuilder.isBoostedByBook());
            new ItemBuilderMenu(itemBuilder).openMenu(player);
        }
    }

    private class BoostedByPhaseBookButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return itemBuilder.isBoostedByPhaseBook() ? on : off;
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            itemBuilder.makeBoostedByPhaseBook(!itemBuilder.isBoostedByPhaseBook());
            new ItemBuilderMenu(itemBuilder).openMenu(player);
        }
    }
}