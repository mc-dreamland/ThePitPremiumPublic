package cn.charlotte.pit.menu.admin.item;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.CustomNamePaper;
import cn.charlotte.pit.item.type.GoldenBric;
import cn.charlotte.pit.item.type.MusicalRune;
import cn.charlotte.pit.item.type.mythic.GoldenHelmet;
import cn.charlotte.pit.item.type.mythic.LuckyChestplate;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.menu.admin.item.button.MythicItemButton;
import cn.charlotte.pit.menu.admin.item.button.PitItemButton;
import cn.charlotte.pit.menu.admin.item.button.RedPacketButton;
import cn.charlotte.pit.menu.admin.item.button.ShopItemButton;
import cn.charlotte.pit.menu.shop.button.type.CombatSpadeShopButton;
import cn.charlotte.pit.menu.shop.button.type.GoldPickaxeShopButton;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminItemMenu extends Menu {
    private final boolean admin;
    private final List<Button> allButtons = new ArrayList<>();
    private int page = 0;
    private boolean showPitItems = false; // 控制显示模式的标志

    public AdminItemMenu() {
        this.admin = true;
        generateButtons();
    }

    public AdminItemMenu(boolean isAdmin) {
        this.admin = isAdmin;
        generateButtons();
    }

    private void generateButtons() {
        allButtons.clear();

        if (showPitItems) {
            for (PitItems pitItem : PitItems.values()) {
                allButtons.add(new PitItemButton(pitItem.toItemStack()));
            }
        } else {
            try {
                allButtons.add(new MythicItemButton(0));
                allButtons.add(new MythicItemButton(1));
                allButtons.add(new MythicItemButton(2));
                allButtons.add(new MythicItemButton(3));
                allButtons.add(new MythicItemButton(4));
                allButtons.add(new MythicItemButton(8));
                allButtons.add(new ShopItemButton(Material.DIAMOND_HELMET, "shopItem"));
                allButtons.add(new ShopItemButton(Material.DIAMOND_CHESTPLATE, "shopItem"));
                allButtons.add(new ShopItemButton(Material.DIAMOND_LEGGINGS, "shopItem"));
                allButtons.add(new ShopItemButton(Material.DIAMOND_BOOTS, "shopItem"));
                allButtons.add(new ShopItemButton(Material.DIAMOND_SWORD, "shopItem"));
                allButtons.add(new ShopItemButton(Material.OBSIDIAN, "shopItem", 64));
                allButtons.add(new GoldPickaxeShopButton(false));
                allButtons.add(new CombatSpadeShopButton(false));

                if (admin) {
                    allButtons.add(new PitItemButton(PitItems.JUMP_BOOST_POTION.toItemStack()));
                    allButtons.add(new PitItemButton(ThePit.getApi().getMythicItemItemStack("bounty_solvent_potion")));
                    allButtons.add(new PitItemButton(PitItems.FOUR_IN_A_ROW_GADGET.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.CHUNK_OF_VILE.toItemStack()));
                    allButtons.add(new RedPacketButton());
                    allButtons.add(new PitItemButton(PitItems.FUNKY_FEATHER.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.PIT_CACTUS.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.MYTHIC_REPAIRKIT.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.JEWEL_SWORD.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.ARMAGEDDON_BOOTS.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.TOTALLY_LEGIT_GEM.toItemStack()));
                    allButtons.add(new MythicItemButton(5));
                    allButtons.add(new MythicItemButton(6));
                    allButtons.add(new PitItemButton(PitItems.GRIM_REAPER.toItemStack()));
                    allButtons.add(new PitItemButton(new GoldenHelmet().toItemStack()));
                    final MythicLeggingsItem item = new MythicLeggingsItem();
                    item.setColor(MythicColor.RAGE);
                    allButtons.add(new PitItemButton(item.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.TACTICAL_INSERTIONS.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.UBER_DROP.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.SUGAR.toItemStack()));
                    final MythicLeggingsItem aquaLeggings = new MythicLeggingsItem();
                    aquaLeggings.setColor(MythicColor.AQUA);
                    allButtons.add(new PitItemButton(aquaLeggings.toItemStack()));

                    allButtons.add(new PitItemButton(PitItems.CHERRY.toItemStack()));

                    final MythicLeggingsItem darkGreenLeggings = new MythicLeggingsItem();
                    darkGreenLeggings.setColor(MythicColor.DARK_GREEN);
                    allButtons.add(new PitItemButton(darkGreenLeggings.toItemStack()));

                    allButtons.add(new PitItemButton(PitItems.MINI_CAKE.toItemStack()));

                    MusicalRune musicalRune = new MusicalRune();
                    allButtons.add(new PitItemButton(musicalRune.toItemStack()));
                    allButtons.add(new PitItemButton(new LuckyChestplate().toItemStack()));

                    allButtons.add(new PitItemButton(PitItems.DIMENSIONAL_CACTUS.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.SACROSANCT_CACTUS.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.GLOBAL_ATTENTION_GEM.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.KILL_COUNTER_GEM.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.LUCKY_GEM.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.SUPER_PACKAGE.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.PACKAGED_BALE.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.WHEAT.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.HIGH_GRADE_EGGS.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.RUBBIN.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.YUMMY_BREAD.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.CAKE_CHERRY.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.FISH_1.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.FISH_2.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.FISH_3.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.FISH_4.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.FISH_5.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.FISH_6.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.BAKED_POTATO.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.BROKEN_SOUL.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.CUSTOM_NAME_PAPER.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.ACTION_GIFT_BOX.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.GAIN_BANNER.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.SMALL_MYTHIC_REPAIR_KIT.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.BOUNTY_SOLVENT_POTION.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.BEEF.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.MAGNET.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.TURTLE_HELMET.toItemStack()));
                    allButtons.add(new PitItemButton(PitItems.PHASE_BOOK.toSingletonItemStack()));
                    allButtons.add(new PitItemButton(new GoldenBric(1000).toItemStack()));
                    allButtons.add(new PitItemButton(CustomNamePaper.createTestItem("114514").toItemStack()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getTitle(Player player) {
        int maxPages = (int) Math.ceil((double) allButtons.size() / 45);
        String mode = showPitItems ? "PitItems" : "默认列表";
        return "Pit物品 (" + (page + 1) + "/" + maxPages + ") - " + mode;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttonMap = new HashMap<>();
        int itemsPerPage = 45;
        int startIndex = page * itemsPerPage;

        for (int slot = 0; slot < itemsPerPage; slot++) {
            int index = startIndex + slot;
            if (index < allButtons.size()) {
                buttonMap.put(slot, allButtons.get(index));
            }
        }

        int previousSlot = 45;
        int nextSlot = 53;
        int toggleSlot = 49; // 中间位置

        if (page > 0) {
            buttonMap.put(previousSlot, new PreviousPageButton());
        }

        int maxPages = (int) Math.ceil((double) allButtons.size() / itemsPerPage);


        buttonMap.put(toggleSlot, new ToggleDisplayButton());

        buttonMap.put(50, new PageInfoButton(page, maxPages));

        if (page < maxPages - 1) {
            buttonMap.put(nextSlot, new NextPageButton());
        }

        return buttonMap;
    }

    @Override
    public void onClickEvent(InventoryClickEvent event) {
        if (event.getClickedInventory() instanceof PlayerInventory inv) {
            inv.setItem(event.getSlot(), new ItemStack(Material.AIR));
        }
    }

    private class ToggleDisplayButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.COMPARATOR)
                    .name(showPitItems ? "&a显示模式: PitItems" : "&a显示模式: 默认列表")
                    .lore(
                            "&7点击切换显示模式",
                            "",
                            "&e当前显示: " + (showPitItems ? "PitItems所有物品" : "默认物品列表"),
                            "&e物品总数: &f" + allButtons.size(),
                            "",
                            "&a点击切换"
                    )
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            showPitItems = !showPitItems;
            page = 0;
            generateButtons();
            openMenu(player);
        }
    }

    private class PageInfoButton extends Button {
        private final int currentPage;
        private final int maxPages;

        public PageInfoButton(int currentPage, int maxPages) {
            this.currentPage = currentPage;
            this.maxPages = maxPages;
        }

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.BOOK)
                    .name("&e页码信息")
                    .lore(
                            "&7当前页: &f" + (currentPage + 1),
                            "&7总页数: &f" + maxPages,
                            "&7物品总数: &f" + allButtons.size(),
                            "",
                            "&7显示模式: &f" + (showPitItems ? "PitItems" : "默认列表")
                    )
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            // 不做任何操作
        }
    }

    private class PreviousPageButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.FEATHER)
                    .name("&6上一页")
                    .lore(
                            "&7点击返回上一页",
                            "",
                            "&a当前页: &f" + (page + 1),
                            "&a总页数: &f" + (int) Math.ceil((double) allButtons.size() / 45)
                    )
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            if (page > 0) {
                page--;
                openMenu(player);
            }
        }
    }

    private class NextPageButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.FEATHER)
                    .name("&6下一页")
                    .lore(
                            "&7点击前往下一页",
                            "",
                            "&a当前页: &f" + (page + 1),
                            "&a总页数: &f" + (int) Math.ceil((double) allButtons.size() / 45)
                    )
                    .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            int maxPages = (int) Math.ceil((double) allButtons.size() / 45);
            if (page < maxPages - 1) {
                page++;
                openMenu(player);
            }
        }
    }
}