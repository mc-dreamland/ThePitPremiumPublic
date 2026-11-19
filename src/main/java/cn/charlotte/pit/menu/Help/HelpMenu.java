package cn.charlotte.pit.menu.Help;

import cn.charlotte.pit.menu.Help.button.HelpEnchant;
import cn.charlotte.pit.menu.Help.button.HelpEvent;
import cn.charlotte.pit.menu.Help.button.UpdateMenu;
import cn.charlotte.pit.menu.Help.button.wiki.WikiMenu;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class HelpMenu extends Menu {

    private static final ItemStack HELP = new ItemBuilder(Material.WRITABLE_BOOK)
            .name("&e天坑乱斗更新日志")
            .lore(
                    "",
                    "§e特别鸣谢-",
                    " §7- §6ThePitCommunity§7/§bThePitPremium",
                    "   §7- 感谢其开源相关项目",
                    "",
                    " §7- §b天坑神秘人 - 感谢其对本插件做出的后续相关更新",
                    "",
                    "§8升级至1.20.6后的相关源码可在gitee搜索BuGlandStudio/ThePit 查看",
                    "§8(不包含后续其他更新)"
            )
            .build();

    private static final ItemStack ENCHANT_LIST = new ItemBuilder(Material.ENCHANTING_TABLE)
            .name("&d天坑乱斗神话装备及附魔图鉴")
            .build();


    private static final ItemStack EVENTS_LIST = new ItemBuilder(Material.MAP)
            .name("&e天坑乱斗事件图鉴")
            .build();

    private static final ItemStack ITEM_LIST = new ItemBuilder(Material.IRON_INGOT)
            .name("&e天坑乱斗物品")
            .build();

    private static final ItemStack WIKI = new ItemBuilder(Material.BEACON)
            .name("&e天坑乱斗百科")
            .build();

    public HelpMenu() {
    }

    @Override
    public String getTitle(Player player) {
        return "天坑图鉴";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        return createActiveButtons();

    }

    private Map<Integer, Button> createActiveButtons() {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(10, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return HELP;
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                new UpdateMenu().openMenu(player);
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return true;
            }
        });
        buttons.put(13, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return ENCHANT_LIST;
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                new HelpEnchant().openMenu(player);
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return true;
            }
        });
        buttons.put(16, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return EVENTS_LIST;
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                new HelpEvent().openMenu(player);
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return true;
            }
        });
        buttons.put(29, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return WIKI;
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                new WikiMenu().openMenu(player);
            }
        });
//        buttons.put(29, new Button() {
//            @Override
//            public ItemStack getButtonItem(Player player) {
//                return ITEM_LIST;
//            }
//
//            @Override
//            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
//                new HelpItemMenu().openMenu(player);
//            }
//
//            @Override
//            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
//                return true;
//            }
//        });
//        buttons.put(33, new Button() {
//            @Override
//            public ItemStack getButtonItem(Player player) {
//                return WIKI;
//            }
//
//            @Override
//            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
//                player.sendMessage(CC.translate("&c神秘人太懒了!!!尚未上架"));
////                new BetterHelpMenu().openMenu(player);
//            }
//
//            @Override
//            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
//                return true;
//            }
//        });
        return buttons;
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public boolean isAutoUpdate() {
        return true;
    }

    @Override
    public boolean isUpdateAfterClick() {
        return true;
    }
}