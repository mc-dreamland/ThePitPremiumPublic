package cn.charlotte.pit.util.menu.builder;

import cn.charlotte.pit.util.menu.Button;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ButtonBuilder {

    private @NonNull getItemStackHandler itemStack;
    private @NonNull List<ClickHandler> clickHandler;
    private @NonNull shouldCancelHandler shouldCancel = (player, slot, clickType) -> true;
    private @NonNull shouldUpdateHandler shouldUpdate = (player, slot, clickType) -> true;

    public ButtonBuilder(@NonNull getItemStackHandler itemStack, @NonNull List<ClickHandler> clickHandler, @NonNull shouldCancelHandler shouldCancel, @NonNull shouldUpdateHandler shouldUpdate) {
        this.itemStack = itemStack;
        this.clickHandler = clickHandler;
        this.shouldCancel = shouldCancel;
        this.shouldUpdate = shouldUpdate;
    }

    public ButtonBuilder() {
        this.itemStack = (player) -> null;
        this.clickHandler = new ArrayList<>();
    }

    public ButtonBuilder(Button button) {
        this.itemStack = button::getButtonItem;
        this.clickHandler = new ArrayList<>();
        this.clickHandler.add(button::clicked);
        this.shouldCancel = button::shouldCancel;
        this.shouldUpdate = button::shouldUpdate;
    }

    public ButtonBuilder(ButtonBuilder buttonBuilder) {
        this.itemStack = buttonBuilder.getItemStack();
        this.clickHandler = buttonBuilder.getClickHandler();
        this.shouldCancel = buttonBuilder.shouldCancel;
        this.shouldUpdate = buttonBuilder.shouldUpdate;
    }
    public ButtonBuilder(ItemStack itemStack) {
        this.itemStack = (Player player)-> itemStack;
        this.clickHandler = new ArrayList<>();
    }

    public ButtonBuilder(@org.jspecify.annotations.NonNull getItemStackHandler itemStack, ClickHandler clickHandler) {
        this.itemStack = itemStack;
        this.clickHandler = new ArrayList<>();
        this.clickHandler.add(clickHandler);
    }

    public ButtonBuilder addClickHandler(ClickHandler handler) {
        this.clickHandler.add(handler);
        return this;
    }

    public ButtonBuilder setItemStackHandler(getItemStackHandler handler) {
        this.itemStack = handler;
        return this;
    }

    public ButtonBuilder setShouldCancel(shouldCancelHandler handler) {
        this.shouldCancel = handler;
        return this;
    }

    public ButtonBuilder setShouldUpdateHandler(shouldUpdateHandler handler) {
        this.shouldUpdate = handler;
        return this;
    }

    public Button build() {
        return new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return itemStack.getButtonItem(player);
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                for (ClickHandler handler : clickHandler) {
                    handler.onClick(player, slot, clickType, hotbarButton, currentItem);
                }
            }

            @Override
            public boolean shouldCancel(Player player, int slot, ClickType clickType) {
                return shouldCancel.shouldCancel(player, slot, clickType);
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return shouldUpdate.shouldUpdate(player, slot, clickType);
            }
        };
    }
}