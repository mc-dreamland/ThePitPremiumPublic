package cn.charlotte.pit.enchantment.menu;

import cn.charlotte.pit.addon.impl.EnchantBook;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.menu.button.*;
import cn.charlotte.pit.enchantment.runnable.AnimationRunnable;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.occultDoctrine.LuckyStone;
import cn.charlotte.pit.menu.mystery.MysteryMenu;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.MaterialConvert;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import cn.charlotte.pit.util.menu.buttons.DisplayButton;
import cn.charlotte.pit.util.random.RandomUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/17 14:23
 */
@Slf4j
@Getter
@Setter
public class MythicWellMenu extends Menu {
    private static final List<String> ImythicItemList = List.of(
            "mythic_sword", "mythic_bow", "mythic_leggings", "mythic_reel", "FancyHat", "lucky_stone",
            PitItems.PHASE_BOOK.getInternalName()
    );
    public static AnimationRunnable runnable = new AnimationRunnable();
    private final int[] ANIMATIONS_SLOT = new int[]{19, 10, 11, 12, 21, 30, 29, 28};
    private final int INPUT_SLOT = 20;
    private final int CLICK_SLOT = 25;
    private final MythicWellMenu instance;
    private AnimationRunnable.AnimationData animationData;

    public MythicWellMenu(Player player) {
        this.instance = this;
        this.animationData = new AnimationRunnable.AnimationData(player);

        runnable.getAnimations().put(player.getUniqueId(), this.animationData);

        runnable.sendStart(player);
    }

    @Override
    public String getTitle(Player player) {
        return "神话之井";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> button = new HashMap<>();
        PlayerProfile currentProfile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if ((!animationData.isStartEnchanting() || animationData.isFinished())
                && currentProfile.getAttachedEnchant() != null) {
            button.put(32, new EnchantBookButton(this));
        }
        if (animationData.isFinished()) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            String mythic_color = ItemUtil.getItemStringData(InventoryUtil.deserializeItemStack(profile.getEnchantingItem()), "mythic_color");
            MythicColor foundColor = null;
            for (MythicColor color : MythicColor.values()) {
                if (color.getInternalName().equals(mythic_color)) {
                    foundColor = color;
                    break;
                }
            }
            if (foundColor == null) {
                return button;
            }

            for (int i : ANIMATIONS_SLOT) {
                button.put(i, new DisplayButton(new ItemBuilder(MaterialConvert.GLASS_PANE[foundColor.getColorByte()]).name("&c").build(), true));
            }
            String enchantingItem = profile.getEnchantingItem();
            ItemStack itemStack = InventoryUtil.deserializeItemStack(enchantingItem);
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                return button;
            }
            button.put(INPUT_SLOT, new EnchantDisplayButton(itemStack, this));
            button.put(CLICK_SLOT, new EnchantButton(itemStack, this));


            Integer tier = ItemUtil.getItemIntData(itemStack, "tier");
            if (tier != null && tier == (foundColor == MythicColor.DARK ? 1 : 2)) {
                button.put(14, new EnchantSinceButton(this, foundColor));
                if (EnchantBook.INSTANCE.getEnchantBook()) {
                    button.put(32, new EnchantBookButton(this));
                }
            } else if (tier != null && tier != (foundColor == MythicColor.DARK ? 2 : 3)) {
                if (EnchantBook.INSTANCE.getEnchantBook()) {
                    button.remove(32);
                    button.put(23, new EnchantBookButton(this));
                }
            } else if (profile.getAttachedEnchant() != null) {
                button.put(32, new EnchantBookButton(this));
            }

            return button;
        }


        if (animationData.isStartEnchanting()) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            String mythic_color = ItemUtil.getItemStringData(InventoryUtil.deserializeItemStack(profile.getEnchantingItem()), "mythic_color");
            MythicColor foundColor = null;
            for (MythicColor color : MythicColor.values()) {
                if (color.getInternalName().equals(mythic_color)) {
                    foundColor = color;
                    break;
                }
            }
            if (foundColor == null) {
                return button;
            }

            if (animationData.getAnimationTick() == 0 || animationData.getAnimationTick() == 1 || animationData.getAnimationTick() == 4 || animationData.getAnimationTick() == 5) {
                for (int i : ANIMATIONS_SLOT) {
                    button.put(i, new DisplayButton(new ItemBuilder(MaterialConvert.GLASS_PANE[foundColor.getColorByte()]).name("&c").build(), true));
                }
            } else if (animationData.getAnimationTick() == 2 || animationData.getAnimationTick() == 3 || animationData.getAnimationTick() == 6 || animationData.getAnimationTick() == 7) {
                for (int i : ANIMATIONS_SLOT) {
                    button.put(i, new DisplayButton(new ItemBuilder(MaterialConvert.GLASS_PANE[0]).name("&c").build(), true));
                }
            } else {
                if (animationData.getAnimationTick() <= 23) {
                    int slot = animationData.getAnimationTick() % 8;
                    for (int i = 0; i < ANIMATIONS_SLOT.length; i++) {
                        if (slot == i) {
                            button.put(ANIMATIONS_SLOT[i], new AnimationButton(animationData.getColor()));
                        } else {
                            button.put(ANIMATIONS_SLOT[i], new AnimationButton(0));
                        }
                    }
                    button.put(INPUT_SLOT, new DisplayButton(new ItemBuilder(MaterialConvert.COLORED_DYE[Math.min(animationData.getAnimationTick() - 7, 15)])
                            .name("&a正在选择!")
                            .build(), true));
                }
            }

        } else {
            for (int i = 0; i < ANIMATIONS_SLOT.length; i++) {
                if (animationData.getAnimationTick() / 2 == i) {
                    button.put(ANIMATIONS_SLOT[i], new AnimationButton(animationData.getColor()));
                } else {
                    button.put(ANIMATIONS_SLOT[i], new AnimationButton(0));
                }
            }
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            ItemStack enchantingItem;
            String enchantingItemStr = profile.getEnchantingItem();
            if (enchantingItemStr == null) {
                enchantingItem = new ItemStack(Material.AIR);
            } else {
                enchantingItem = InventoryUtil.deserializeItemStack(enchantingItemStr);
                if (enchantingItem == null) {
                    enchantingItem = new ItemStack(Material.AIR);
                }
            }
            if (enchantingItem.getType() != Material.AIR) {

                String mythic_color = ItemUtil.getItemStringData(enchantingItem, "mythic_color");
                if (mythic_color != null) {
                    for (MythicColor color : MythicColor.values()) {
                        if (color.getInternalName().equals(mythic_color)) {
                            byte colorByte = color.getColorByte();
                            this.animationData.setColor(colorByte);

                            Integer tier = ItemUtil.getItemIntData(enchantingItem, "tier");
                            if (tier != null && tier == (color == MythicColor.DARK ? 1 : 2)) {
                                button.put(14, new EnchantSinceButton(this, color));
                                if (EnchantBook.INSTANCE.getEnchantBook()) {
                                    button.put(32, new EnchantBookButton(this));
                                }
                            } else if (tier != null && tier != (color == MythicColor.DARK ? 2 : 3)) {
                                if (EnchantBook.INSTANCE.getEnchantBook()) {
                                    button.remove(32);
                                    button.put(23, new EnchantBookButton(this));
                                }
                            }
                            break;
                        }
                    }
                }


            } else {
                this.animationData.setColor((byte) 6);
            }

            button.put(INPUT_SLOT, new EnchantDisplayButton(enchantingItem, this));
            button.put(CLICK_SLOT, new EnchantButton(enchantingItem, this));
        }
        button.put(44,new Button(){
            @Override
            public ItemStack getButtonItem(Player player){
                return new ItemBuilder(Material.SCULK_SHRIEKER)
                        .name("&3神秘造物台")
                        .build();
            }

            @Override
            public void clicked(Player player,int slot,ClickType clickType,int hotbarButton,ItemStack currentItem){
                new MysteryMenu().openMenu(player);
            }
        });
        return button;
    }

    @Override
    public void onClickEvent(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (animationData.isStartEnchanting() && !animationData.isFinished()) {
            return;
        }

        if (event.getClickedInventory() instanceof PlayerInventory) {
            event.setCancelled(true);
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                player.sendMessage(CC.translate("&c别乱点.."));
                return;
            }
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) {
                return;
            }

            String internalName = ItemUtil.getInternalName(item);
            boolean isIMythicItem = internalName != null && ImythicItemList.contains(internalName);

            if (!isIMythicItem) {
                player.sendMessage(CC.translate("&c你必须放入未被附魔的神话武器才可以进行附魔!"));
                player.sendMessage(CC.translate("&c神话武器有概率从战斗中掉落!"));

                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1.2F);
                return;
            }


            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            if (InventoryUtil.isInvFull(player.getInventory())) {
                player.sendMessage(CC.translate("&c背包已满,无法操作背包物品!"));
                return;
            }

            if ("lucky_stone".equals(internalName)) {
                ItemStack enchantingItem = InventoryUtil.deserializeItemStack(profile.getEnchantingItem());
                Integer currentTier = ItemUtil.getItemIntData(enchantingItem, "tier");
                MythicColor color = MythicColor.valueOfInternalName(
                        ItemUtil.getItemStringData(enchantingItem, "mythic_color"));
                if (currentTier == null || !LuckyStone.canUse(color, currentTier)) {
                    player.sendMessage(CC.translate("&c幸运石仅可用于非暗黑神话装备 I 阶升 II 阶，请先放入符合条件的装备。"));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1.2F);
                    return;
                }
            }

            if (item.getType() == Material.LEATHER_LEGGINGS) {
                if (PlayerUtil.getPlayerUnlockedPerkLevel(player, PerksPrestigeEnum.MYSTICISM) < 9 && profile.getEnchantingItem() == null && MythicColor.valueOf(ItemUtil.getItemStringData(item, "mythic_color").toUpperCase()) == MythicColor.DARK_GREEN) {
                    player.sendMessage(CC.translate("&c你还不能升级 &c神&6话&9之&a甲 &c!"));
                    player.sendMessage(CC.translate("&c请升级天赋 &6神话附魔师 &c至等级 &eIX &c后重试!"));
                    return;
                }
            }

            if (profile.getEnchantingItem() != null) {
                ItemStack itemStack = InventoryUtil.deserializeItemStack(profile.getEnchantingItem());
                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    Integer tier = ItemUtil.getItemIntData(itemStack, "tier");
                    String mythic_color = ItemUtil.getItemStringData(itemStack, "mythic_color");
                    switch (ItemUtil.getInternalName(item)) {
                        case "mythic_reel" -> {
                            if (tier != null && tier == (mythic_color.equalsIgnoreCase("DARK") ? 2 : 3)) return;
                            ItemStack itemStack1 = event.getCurrentItem();
                            putItem(profile, player, event.getSlot(), itemStack1);
                            return;
                        }
                        case "lucky_stone", "phase_book" -> {
                            ItemStack itemStack1 = event.getCurrentItem();
                            putItem(profile, player, event.getSlot(), itemStack1);
                            return;
                        }
                        case null, default -> {
                        }
                    }

                    if (tier != null && tier == (mythic_color.equalsIgnoreCase("DARK") ? 1 : 2)) {
                        //判断是否放入的是神话甲
                        if (item.getType() == Material.LEATHER_LEGGINGS) {
                            //是tier2 所以获取物品颜色
                            //edit: 因为dark pants在t1需要额外材料 此步骤代码提前
                            for (MythicColor color : MythicColor.values()) {
                                if (color.getInternalName().equals(mythic_color)) {
                                    //找到点击物品颜色
                                    String mythicColor = ItemUtil.getItemStringData(item, "mythic_color");
                                    for (MythicColor targetColor : MythicColor.values()) {
                                        if (targetColor.getInternalName().equals(mythicColor)) {
                                            //再检查点击物品颜色是否符合附魔物品
                                            if (color == targetColor) {
                                                //如果之前放了一条，那么退还给玩家
                                                if (profile.getEnchantingScience() != null) {
                                                    ItemStack oldSince = InventoryUtil.deserializeItemStack(profile.getEnchantingScience());
                                                    if (oldSince != null && oldSince.getType() != Material.AIR) {
                                                        if (InventoryUtil.isInvFull(player.getInventory())) {
                                                            player.sendMessage("&c你的背包已满,无法进行此操作!");
                                                            return;
                                                        }
                                                        player.getInventory().addItem(oldSince);
                                                    }
                                                }
                                                //放入材料Slot
                                                profile.setEnchantingScience(InventoryUtil.serializeItemStack(event.getCurrentItem()));
                                                event.getClickedInventory().setItem(event.getSlot(), new ItemBuilder(Material.AIR).build());
                                                this.openMenu(player);
                                                player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.9F);
                                                return;
                                            }
                                        }
                                    }
                                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1.2F);
                                    player.sendMessage(CC.translate("&c颜色不符合! 您需要放入 " + color.getChatColor() + color.getDisplayName() + "色神话之甲 &c作为附魔材料."));
                                    return;
                                }
                            }
                        } else {
                            return;
                        }
                    } else {
                        event.getClickedInventory().addItem(itemStack);
                        profile.setEnchantingItem(InventoryUtil.serializeItemStack(new ItemStack(Material.AIR)));
                    }
                }
            }
            IMythicItem mythicItem = Utils.getMythicItem(item);
            if ("FancyHat".equals(ItemUtil.getInternalName(item))) {
                profile.setEnchantingItem(InventoryUtil.serializeItemStack(item));
            } else {
                if (mythicItem == null) return;
                if (ItemUtil.getItemStringData(mythicItem.toItemStack(), "mythic_color") == null) {
                    mythicItem.setColor(RandomUtil.helpMeToChooseOne(MythicColor.RED, MythicColor.ORANGE, MythicColor.BLUE, MythicColor.GREEN, MythicColor.YELLOW));
                }
                profile.setEnchantingItem(InventoryUtil.serializeItemStack(mythicItem.toItemStack()));
            }

            event.getClickedInventory().setItem(event.getSlot(), new ItemBuilder(Material.AIR).build());
            this.openMenu(player);
            player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.9F);
        }
    }

    @Override
    public int getSize() {
        return 5 * 9;
    }

    @Override
    public void onClose(Player player) {
        runnable.sendReset(player);

        if (!isClosedByMenu()) {
            runnable.getAnimations().remove(player.getUniqueId());
        }

        //nothing to do
    }

    private void putItem(PlayerProfile profile, Player player, int slot, ItemStack itemStack) {
        if (profile.getAttachedEnchant() == null || profile.getAttachedEnchant().isBlank()) {
            ItemStack CitemStack = itemStack.clone();
            CitemStack.setAmount(1);
            profile.setAttachedEnchant(InventoryUtil.serializeItemStack(CitemStack));
            if (itemStack.getAmount() == 1) {
                player.getInventory().setItem(slot, PitItems.AIR.toItemStack());
            } else {
                itemStack.setAmount(itemStack.getAmount() - 1);
            }
            this.openMenu(player);
            player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.9F);
        } else {
            CC.send(MessageType.MISC, player, "&c附属附魔中已有物品请你先取下来才能放置物品!");
        }
    }
}
