package cn.charlotte.pit.menu.Help.button;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelpEnchant extends Menu {
    private int page = 0;
    private FilterType currentFilter = FilterType.ALL;
    private boolean showSpecificItems = false;

    public HelpEnchant() {
    }

    @Override
    public String getTitle(Player player) {
        return "附魔 (" + currentFilter.displayName + ")";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttonMap = new HashMap<>();
        List<AbstractEnchantment> allEnchantments = ThePit.getInstance().getEnchantmentFactor().getEnchantments();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        List<AbstractEnchantment> filteredEnchantments = new ArrayList<>();
        int unlockedCount = 0; // 统计当前过滤条件下已解锁的附魔数量

        for (AbstractEnchantment enchantment : allEnchantments) {
            boolean matchFilter = false;
            switch (currentFilter) {
                case SWORD:
                    if (enchantment.isWeaponOnly()) matchFilter = true;
                    break;
                case BOW:
                    if (enchantment.isBowOnly()) matchFilter = true;
                    break;
                case PANTS:
                    if (enchantment.isArmorOnly()) matchFilter = true;
                    break;
                default:
                    matchFilter = true;
            }

            if (isSpecialEnchantment(enchantment)) {
                continue;
            }

            if (matchFilter) {
                filteredEnchantments.add(enchantment);
                int count = profile.getEnchantmentCount(enchantment.getNbtName());
                if (count > 0) {
                    unlockedCount++;
                }
            }
        }

        int num = 45 * page;
        int slot = 0;

        for (int i = 0; i < 45; i++) {
            if (filteredEnchantments.size() <= i + num) {
                break;
            }

            AbstractEnchantment enchantment = filteredEnchantments.get(i + num);
            buttonMap.put(slot, createEnchantmentButton(player, enchantment));
            slot++;
        }

        int finalUnlockedCount = unlockedCount;
        buttonMap.put(49, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {

                return new ItemBuilder(currentFilter.icon)
                        .name("&e过滤类型: &a" + currentFilter.displayName)
                        .lore("",
                                "&7点击切换过滤类型",
                                "&7当前: &a" + currentFilter.description,
                                "",
                                "&e已解锁: &a" + finalUnlockedCount + " &7/ &b" + filteredEnchantments.size(),
                                "&e当前显示: &a" + filteredEnchantments.size() + " &e个附魔")
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                FilterType[] values = FilterType.values();
                currentFilter = values[(currentFilter.ordinal() + 1) % values.length];
                page = 0;
                openMenu(player);
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return true;
            }
        });

        buttonMap.put(45, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.ARROW)
                        .name("&f上一页")
                        .lore("&7当前页: &a" + (page + 1))
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                if (page == 0) return;
                page--;
                openMenu(player);
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return true;
            }
        });
        buttonMap.put(47, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.NETHER_STAR)
                        .name(showSpecificItems ? "&a物品显示模式" : "&7书显示模式")
                        .lore("",
                                "&7点击切换附魔的显示物品",
                                "&7当前模式: " + (showSpecificItems ? "&a显示特定物品" : "&7显示为书"),
                                "")
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                showSpecificItems = !showSpecificItems;
                openMenu(player);
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return true;
            }
        });
        buttonMap.put(53, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                boolean hasNext = (page + 1) * 45 < filteredEnchantments.size();
                return new ItemBuilder(Material.ARROW)
                        .name(hasNext ? "&f下一页" : "&c最后一页")
                        .lore("&7当前页: &a" + (page + 1),
                                hasNext ? "&7总页数: &a" + ((filteredEnchantments.size() / 45) + 1) : "&c没有更多页面")
                        .build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                if ((page + 1) * 45 >= filteredEnchantments.size()) return;
                page++;
                openMenu(player);
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return true;
            }
        });

        return buttonMap;
    }

    private boolean isSpecialEnchantment(AbstractEnchantment enchantment) {
        EnchantmentRarity rarity = enchantment.getRarity();
        if (rarity == null) return false;

        String name = rarity.name().toUpperCase();
        return name.contains("SPECIAL") || name.contains("OP") || name.contains("GENESIS");
    }

    private Button createEnchantmentButton(Player player, AbstractEnchantment enchantment) {
        return new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                int count = profile.getEnchantmentCount(enchantment.getNbtName());

                Material material;
                if (showSpecificItems) {
                    switch (currentFilter) {
                        case SWORD:
                            material = Material.GOLDEN_SWORD;
                            break;
                        case BOW:
                            material = Material.BOW;
                            break;
                        case PANTS:
                            material = Material.LEATHER_LEGGINGS;
                            break;
                        default:
                            if (enchantment.isWeaponOnly()) {
                                material = Material.GOLDEN_SWORD;
                            } else if (enchantment.isBowOnly()) {
                                material = Material.BOW;
                            } else if (enchantment.isArmorOnly()) {
                                material = Material.LEATHER_LEGGINGS;
                            } else {
                                material = Material.NETHER_STAR;
                            }
                    }
                } else {
                    material = count > 0 ? Material.ENCHANTED_BOOK : Material.BOOK;
                }

                ItemBuilder builder = new ItemBuilder(material)
                        .name("&e" + enchantment.getEnchantName());

                List<String> lore = new ArrayList<>();
                if (count > 0) {
                    lore.add(CC.translate("&a已获得: &e" + count + "次"));
                }

                lore.add(CC.translate("&7物品类型: " + getItemType(enchantment)));
                lore.add(CC.translate("&7稀有度: " + getRarityDisplay(enchantment.getRarity())));
                lore.add(CC.translate("&7最大等级: " + enchantment.getMaxEnchantLevel()));
                lore.add("");

                for (int level = 1; level <= enchantment.getMaxEnchantLevel(); level++) {
                    lore.add(CC.translate("&9" + enchantment.getEnchantName() + " " + toRoman(level)));

                    String description = enchantment.getUsefulnessLore(level);
                    String[] lines = description.replace("/s", "\n").split("\n");

                    for (String line : lines) {
                        lore.add(CC.translate(line));
                    }

                    if (level < enchantment.getMaxEnchantLevel()) {
                        lore.add("");
                    }
                }

                builder.lore(lore);
                return builder.build();
            }

            private String toRoman(int level) {
                return switch (level) {
                    case 1 -> "I";
                    case 2 -> "II";
                    case 3 -> "III";
                    case 4 -> "IV";
                    case 5 -> "V";
                    case 6 -> "VI";
                    case 7 -> "VII";
                    case 8 -> "VIII";
                    case 9 -> "IX";
                    case 10 -> "X";
                    default -> String.valueOf(level); // 超过10的用原数字
                };
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return false;
            }
        };
    }

    private String getItemType(AbstractEnchantment enchantment) {
        StringBuilder types = new StringBuilder();
        if (enchantment.isWeaponOnly()) {
            types.append("&e神话之剑, ");
        }
        if (enchantment.isBowOnly()) {
            types.append("&b神话之弓, ");
        }
        if (enchantment.isArmorOnly()) {
            if (enchantment.getRarity().name().contains("RAGE")) {
                types.append("&4暴怒之甲, ");
            } else if (enchantment.getRarity().name().contains("SEWER")) {
                types.append("&2下水道之甲, ");
            } else if (enchantment.getRarity().name().contains("DARK")) {
                types.append("&5暗黑之甲, ");
            } else {
                types.append("&d神话之甲, ");
            }
        }

        if (!types.isEmpty()) {
            types.setLength(types.length() - 2);
        } else {
            types.append("通用");
        }

        return types.toString();
    }

    private String getRarityDisplay(EnchantmentRarity rarity) {
        if (rarity == null) return "未知";

        String name = rarity.name().toUpperCase();
        if (name.contains("FISH")) {
            return "&b天蓝色之甲";
        } else if (name.contains("GENESIS")) {
            return "&b阵营";
        } else if (name.contains("SPECIAL") || name.contains("OP")) {
            return "&cOP专属附魔";
        } else if (name.contains("AUCTION")) {
            return "&e拍卖限定";
        }

        return switch (rarity.getParentType()) {
            case NORMAL -> "&7普通";
            case RARE -> "&d稀有";
            case LIMITED -> "&c限定";
        };
    }

    private enum FilterType {
        ALL("通用", Material.CLOCK, "显示所有附魔"),
        SWORD("剑", Material.GOLDEN_SWORD, "仅显示剑类附魔"),
        BOW("弓", Material.BOW, "仅显示弓类附魔"),
        PANTS("裤子", Material.LEATHER_LEGGINGS, "仅显示裤子附魔");

        private final String displayName;
        private final Material icon;
        private final String description;

        FilterType(String displayName, Material icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }
    }
}