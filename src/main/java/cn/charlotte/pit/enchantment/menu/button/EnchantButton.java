package cn.charlotte.pit.enchantment.menu.button;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.ActivityType;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.EnchantmentRecord;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.DoEnch;
import cn.charlotte.pit.enchantment.EnchData;
import cn.charlotte.pit.enchantment.menu.MythicWellMenu;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.event.PitPlayerEnchantEvent;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.occultDoctrine.LuckyStone;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.item.type.mythic.MythicShovelItem;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
import cn.charlotte.pit.menu.shop.button.AbstractShopButton;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.ChatComponentBuilder;
import cn.charlotte.pit.util.chat.RomanUtil;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.item.MythicOwnershipUtil;
import cn.charlotte.pit.util.log.ItemOperationJson;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.random.RandomUtil;
import de.tr7zw.nbtapi.NBT;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
public class EnchantButton extends Button {

    private static final Random random = new Random();
    private final ItemStack item;
    private final MythicWellMenu menu;

    public EnchantButton(ItemStack item, MythicWellMenu menu) {
        this.item = item;
        this.menu = menu;
    }

    private int getPrice(Player player, int level, MythicColor color) {
        int price = color == MythicColor.DARK ? (switch (level) {
            case 1 -> 10000;
            case 2 -> 60000;
            default -> 99999;
        }) : (switch (level) {
            case 1 -> 1000;
            case 2 -> 4000;
            case 3 -> 8000;
            default -> 9999;
        });
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (ActivityType.GENES.isActive() && profile.getActivityPlayerData().getGenesisData().getTier() >= 3) {
            return (int) (0.2 * AbstractShopButton.getDiscountPrice(player, price));
        }
//        if (ThePit.getInstance().getPitConfig().isGenesisEnable() && profile.getGenesisData().getTeam() == GenesisTeam.DEMON && profile.getGenesisData().getTier() >= 3) {
//            return (int) (0.5 * AbstractShopButton.getDiscountPrice(player, price));
//        }
        return AbstractShopButton.getDiscountPrice(player, price);
    }

    public IMythicItem getMythicItem(ItemStack item) {
        return MythicUtil.getMythicItem(item);
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        try {
            if (item == null || item.getType() == Material.AIR) {
                return getDefaultDisplayItem();
            }
            IMythicItem mythicItem = CraftItemStack.asNMSCopy(item).getItem() instanceof SwordItem ?
                    new MythicSwordItem() : CraftItemStack.asNMSCopy(item).getItem() instanceof BowItem ?
                    new MythicBowItem() : CraftItemStack.asNMSCopy(item).getItem() instanceof ShovelItem ?
                    new MythicShovelItem() : new MythicLeggingsItem();
            mythicItem.loadFromItemStack(item);
            MythicColor color = MythicColor.valueOf(ItemUtil.getItemStringData(mythicItem.toItemStack(), "mythic_color").toUpperCase());
            int level = mythicItem.getTier();
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            List<String> lines = new ArrayList<>();
            if (level < (color == MythicColor.DARK ? 2 : 3)) {
                lines.add("&7升级至: &a" + RomanUtil.convert(level + 1) + " 阶");
                lines.add("&7价格: &6" + getPrice(player, level + 1, color) + " 硬币" + (level == (color == MythicColor.DARK ? 1 : 2) ? String.format(" &7+ %s%s色神话之甲", color.getChatColor(), color.getDisplayName()) : ""));
                lines.add(" ");
                if (profile.getCoins() >= getPrice(player, level + 1, color)) {
                    if (PlayerUtil.getPlayerUnlockedPerkLevel(player, PerksPrestigeEnum.MYSTICISM) <= 4 && level == 2) {
                        lines.add("&c天赋 &6神话附魔师 &c等级 &eIV &c后解锁此功能!");
                    } else {
                        ItemStack sinceItem = InventoryUtil.deserializeItemStack(profile.getEnchantingScience());
                        if ((sinceItem == null || sinceItem.getType() == Material.AIR) && level == 2) {
                            lines.add("&e选择背包内的神话之甲作为材料以继续...");
                        } else {
                            lines.add("&e点击进行附魔!");
                        }
                    }
                } else {
                    lines.add("&c你的硬币不足!");
                }
            } else {
                lines.add("&a此附魔物品已被提升至最大等级!");
            }
            if ((color == MythicColor.DARK || color == MythicColor.RAGE) && !PerksPrestigeEnum.HERESY.getPerk().hasUnlock(player)) {
                if (!PerksPrestigeEnum.HERESY.getPerk().hasUnlock(player)) {
                    lines.clear();
                    lines.add("&c清先解锁精通天赋 &6邪术 &c后重试!");
                }
                if (PlayerUtil.getPlayerUnlockedPerkLevel(player, PerksPrestigeEnum.HERESY) <= 3 && level == 1) {
                    lines.clear();
                    lines.add("&c天赋 &6邪术 &c等级 &eIII &c后解锁此功能!");
                }
            }
            return new ItemBuilder(Material.ENCHANTING_TABLE)
                    .name("&d神话之井")
                    .lore(lines)
                    .build();
        } catch (Exception e) {
            log.error("", e);
        }
        return getDefaultDisplayItem();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        IMythicItem mythicItem = getMythicItem(item);

        if (mythicItem == null) return;

        final String mythicColor = ItemUtil.getItemStringData(item, "mythic_color");
        if (mythicColor == null) {
            return;
        }

        MythicColor color = MythicColor.valueOf(mythicColor.toUpperCase());
        int level = mythicItem.getTier();

        if ((color == MythicColor.DARK || color == MythicColor.RAGE) && !PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.HERESY)) {
            if (!PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.HERESY)) {
                return;
            }
            if (PlayerUtil.getPlayerUnlockedPerkLevel(player, PerksPrestigeEnum.HERESY) <= 3 && level == 1) {
                return;
            }
        }
        if (level >= (color == MythicColor.DARK ? 2 : 3)) {
            return;
        }
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        ItemStack attachedItem = InventoryUtil.deserializeItemStack(profile.getAttachedEnchant());
        if ("lucky_stone".equals(ItemUtil.getInternalName(attachedItem))
                && !LuckyStone.canUse(color, level)) {
            player.sendMessage(CC.translate("&c幸运石仅可用于非暗黑神话装备 I 阶升 II 阶，请先取下幸运石。"));
            return;
        }
        if (profile.getCoins() < getPrice(player, level + 1, color)) {
            return;
        }
        if (level == (color == MythicColor.DARK ? 1 : 2)) {
            if (PlayerUtil.getPlayerUnlockedPerkLevel(player, PerksPrestigeEnum.MYSTICISM) <= 4) {
                return;
            }
            if (!removeMythicLegWithColor(player, color)) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                String message = color == MythicColor.RAGE ? "&c请放入一条额外的 &4暴怒之甲 &c才能附魔!" : color == MythicColor.DARK_GREEN ? "&c请放入一条额外的 &2下水道之甲 &c才能附魔!" : color == MythicColor.DARK ? "&c请放入一条额外的 &5暗黑之甲 &c才能附魔!" : String.format("&c请放入一条额外的 %s%s色神话之甲 &c才能附魔!", color.getChatColor(), color.getDisplayName());
                player.sendMessage(CC.translate(message));
                return;
            }
        }
        profile.takeCoins(getPrice(player, level + 1, color));
        doEnchant(item, player, mythicItem);

        menu.getAnimationData().setFinished(false);
        menu.getAnimationData().setStartEnchanting(true);
        menu.getAnimationData().setAnimationTick(0);

        menu.setClosedByMenu(true);
        menu.openMenu(player);
    }

    private void doEnchant(ItemStack item, Player player, IMythicItem mythicItem) {
        MythicOwnershipUtil.bindInitial(item, player);
        mythicItem.loadFromItemStack(item);
        IMythicItem beforeItem = getMythicItem(item);
        beforeItem.loadFromItemStack(item);
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        ItemStack attachedEnchant = profile.getAttachedEnchant() == null
                ? null
                : InventoryUtil.deserializeItemStack(profile.getAttachedEnchant());
        String attachedInternalName = ItemUtil.getInternalName(attachedEnchant);
        boolean attachedPhaseBook = "phase_book".equals(attachedInternalName);
        boolean luckyGemActive = player.hasMetadata("lucky");
        MythicColor color = MythicColor.valueOf(ItemUtil.getItemStringData(item, "mythic_color").toUpperCase());
        double baseRareChance = NewConfiguration.getChance(player, color);
        Map<AbstractEnchantment, Integer> beforeEnchantments = new HashMap<>(mythicItem.getEnchantments());
        int level = mythicItem.getTier();
        int tierBefore = level;
        int maxLive = level > 0 ? mythicItem.getMaxLive() : 0;
        if (color == MythicColor.DARK) {
            mythicItem.setMaxLive(switch (level) {
                case 0 -> RandomUtil.helpMeToChooseOne(20, 25, 30, 35);
                case 1 -> RandomUtil.helpMeToChooseOne(40, 45, 50, 55, 60);
                default -> random.nextInt(36) + 5;
            });
            if (level == 1 && RandomUtil.hasSuccessfullyByChance(0.01)) {
                mythicItem.setMaxLive(135);
            }
        } else {
            if (color == MythicColor.RAGE && level == 0) {
                mythicItem.setMaxLive(RandomUtil.helpMeToChooseOne(4, 5, 6, 7, 8, 9));
            } else {
                mythicItem.setMaxLive(switch (level) {
                    case 0 -> random.nextInt(7) + 3;
                    case 1 -> random.nextInt(6) + 10;
                    case 2 -> RandomUtil.hasSuccessfullyByChance(0.01) ? 100 : random.nextInt(8) + 16;
                    default -> random.nextInt(36) + 5;
                });
            }
        }
        if (level > 0) {
            mythicItem.setLive(mythicItem.getLive() + mythicItem.getMaxLive() - maxLive);
        } else {
            mythicItem.setLive(mythicItem.getMaxLive());
        }
        level++;
        mythicItem.setTier(level);

        List<AbstractEnchantment> list = ThePit.getInstance()
                .getEnchantmentFactor()
                .getEnchantments()
                .stream()
                .filter(abstractEnchantment -> abstractEnchantment.canApply(item))
                .toList();
        List<AbstractEnchantment> enchantments = level > 1 ? new ArrayList<>(mythicItem.getEnchantments().keySet()) : new ArrayList<>();
        boolean announcement = false;

        List<AbstractEnchantment> results = list.stream().filter(abstractEnchantment -> abstractEnchantment.getRarity() == EnchantmentRarity.NORMAL).collect(Collectors.toList());
        List<AbstractEnchantment> rareResults = list.stream().filter(abstractEnchantment -> abstractEnchantment.getRarity() == EnchantmentRarity.RARE).collect(Collectors.toList());

        if (color == MythicColor.DARK) {
            results = list.stream().filter(abstractEnchantment -> abstractEnchantment.getRarity() == EnchantmentRarity.DARK_NORMAL).collect(Collectors.toList());
            rareResults = list.stream().filter(abstractEnchantment -> abstractEnchantment.getRarity() == EnchantmentRarity.DARK_RARE).collect(Collectors.toList());
        } else if (color == MythicColor.RAGE && level == 1) {
            results = list.stream().filter(abstractEnchantment -> abstractEnchantment.getRarity() == EnchantmentRarity.RAGE).collect(Collectors.toList());
            rareResults = list.stream().filter(abstractEnchantment -> abstractEnchantment.getRarity() == EnchantmentRarity.RAGE_RARE).collect(Collectors.toList());
        } else if (color == MythicColor.DARK_GREEN && level == 1) {
            results = list.stream().filter(abstractEnchantment -> abstractEnchantment.getRarity() == EnchantmentRarity.SEWER_NORMAL).collect(Collectors.toList());
            rareResults = list.stream().filter(abstractEnchantment -> abstractEnchantment.getRarity() == EnchantmentRarity.SEWER_RARE).collect(Collectors.toList());
        }
        switch (color) {
            case DARK: {
                if (level == 1) {
                    mythicItem.getEnchantments().put(ThePit.getInstance().getEnchantmentFactor().getEnchantmentMap().get("somber_enchant"), 1);
                    break;
                }
                if (level == 2) {
                    if (RandomUtil.hasSuccessfullyByChance(NewConfiguration.getChance(player, color))) {
                        AbstractEnchantment enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(rareResults.toArray());
                        mythicItem.getEnchantments().put(enchantment, 1);
                        announcement = true;
                    } else {
                        results.removeIf(enchant -> enchant.getNbtName().equals("somber_enchant"));
                        AbstractEnchantment enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(results.toArray());
                        mythicItem.getEnchantments().put(enchantment, 1);
                    }
                }
                break;
            }
            case RAGE: {
                if (level == 1) {
                    if (RandomUtil.hasSuccessfullyByChance(NewConfiguration.getChance(player, color))) {
                        AbstractEnchantment enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(rareResults.toArray());
                        mythicItem.getEnchantments().put(enchantment, RandomUtil.random.nextInt(enchantment.getMaxEnchantLevel() - 1) + 1);
                        announcement = true;
                    } else {
                        AbstractEnchantment enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(results.toArray());
                        mythicItem.getEnchantments().put(enchantment, RandomUtil.random.nextInt(enchantment.getMaxEnchantLevel() - 1) + 1);
                    }
                    break;
                }
            }
            case DARK_GREEN: {
                if (level == 1) {
                    boolean useNormalRarity = RandomUtil.hasSuccessfullyByChance(0.1);
                    List<AbstractEnchantment> finalResults = list.stream()
                            .filter(abstractEnchantment -> useNormalRarity ? abstractEnchantment.getRarity() == EnchantmentRarity.NORMAL : abstractEnchantment.getRarity() == EnchantmentRarity.SEWER_NORMAL)
                            .toList();

                    List<AbstractEnchantment> finalRareResults = list.stream()
                            .filter(abstractEnchantment -> useNormalRarity ? abstractEnchantment.getRarity() == EnchantmentRarity.RARE : abstractEnchantment.getRarity() == EnchantmentRarity.SEWER_RARE)
                            .toList();

                    if (RandomUtil.hasSuccessfullyByChance(NewConfiguration.getChance(player, color) * 0.75d)) {
                        AbstractEnchantment enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(finalRareResults.toArray());
                        mythicItem.getEnchantments().put(enchantment, RandomUtil.random.nextInt(enchantment.getMaxEnchantLevel() - 1) + 1);
                        announcement = true;
                    } else {
                        AbstractEnchantment enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(finalResults.toArray());
                        mythicItem.getEnchantments().put(enchantment, RandomUtil.random.nextInt(enchantment.getMaxEnchantLevel() - 1) + 1);
                    }
                    break;
                }
            }
            default: {
                if (level == 1) {
                    EnchData data = processAttachedEnchant(profile, mythicItem);
                    if (data.getArgs().containsKey("phase_book")) {
                        String nbtName = data.getArgs().get("phase_book").toString();
                        AbstractEnchantment bookEnch = rareResults.stream()
                                .filter(e1 -> e1.getNbtName().equals(nbtName))
                                .findFirst()
                                .orElse(null);
                        if (bookEnch == null) {
                            bookEnch = results.stream()
                                    .filter(e1 -> e1.getNbtName().equals(nbtName))
                                    .findFirst()
                                    .orElse(null);
                        }
                        if (bookEnch != null && RandomUtil.hasSuccessfullyByChance(Util.ifNull((Double) data.getArgs().get("probability"), 0.01)) && !mythicItem.getEnchantments().containsKey(bookEnch)) {
                            data.setCancel(true);
                            mythicItem.getEnchantments().put(bookEnch, 3);
                            mythicItem.boostedByPhaseBook = true;
                            if (bookEnch.getRarity() == EnchantmentRarity.RARE) announcement = true;
                        }
                    }
                    if (!data.isCancel()) {
                        int choice = random.nextInt(3);
                        switch (choice) {
                            case 0: {
                                AbstractEnchantment enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(results.toArray());
                                enchantments.add(enchantment);
                                mythicItem.getEnchantments().put(enchantment, 1);
                                break;
                            }
                            case 1: {
                                for (int i = 0; i < 2; i++) {
                                    results.removeAll(enchantments);
                                    AbstractEnchantment enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(results.toArray());
                                    enchantments.add(enchantment);
                                    mythicItem.getEnchantments().put(enchantment, 1);
                                }
                                break;
                            }
                            case 2: {
                                AbstractEnchantment enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(results.toArray());
                                enchantments.add(enchantment);
                                mythicItem.getEnchantments().put(enchantment, Math.min(enchantment.getMaxEnchantLevel(), 2));
                                break;
                            }
                        }
                    }
                } else if (level == 2) {
                    int amount = mythicItem.getEnchantments().size();
                    EnchData data = processAttachedEnchant(profile, mythicItem);
                    if (data.getArgs().containsKey("phase_book")) {
                        String nbtName = data.getArgs().get("phase_book").toString();
                        AbstractEnchantment bookEnch = rareResults.stream()
                                .filter(e1 -> e1.getNbtName().equals(nbtName))
                                .findFirst()
                                .orElse(null);
                        if (bookEnch == null) {
                            bookEnch = results.stream()
                                    .filter(e1 -> e1.getNbtName().equals(nbtName))
                                    .findFirst()
                                    .orElse(null);
                        }
                        if (bookEnch != null && RandomUtil.hasSuccessfullyByChance(Util.ifNull((Double) data.getArgs().get("probability"), 0.01)) && amount != 3 && !mythicItem.getEnchantments().containsKey(bookEnch)) {
                            data.setCancel(true);
                            mythicItem.getEnchantments().put(bookEnch, 3);
                            mythicItem.boostedByPhaseBook = true;
                            if (bookEnch.getRarity() == EnchantmentRarity.RARE) announcement = true;
                        }
                    }

                    if (!data.isCancel()) {
                        if (amount == 1) {
                            Map.Entry<AbstractEnchantment, Integer> entry = mythicItem.getEnchantments().entrySet().iterator().next();
                            AbstractEnchantment enchantment = entry.getKey();
                            int singleLevel = entry.getValue();
                            if (singleLevel == 1) {
                                int choice = random.nextInt(3);
                                switch (choice) {
                                    case 0: {
                                        mythicItem.getEnchantments().put(enchantment, 3);
                                        if (enchantment.getRarity() == EnchantmentRarity.RARE || enchantment.getRarity() == EnchantmentRarity.RAGE_RARE) {
                                            announcement = true;
                                        }
                                        break;
                                    }
                                    case 1: {
                                        mythicItem.getEnchantments().put(enchantment, 2);
                                        announcement = shouldAnnouncement(data, player, color, mythicItem, enchantments, announcement, results, rareResults);
                                        break;
                                    }
                                    case 2: {
                                        mythicItem.getEnchantments().put(enchantment, 2);
                                        for (int i = 0; i < 2; i++) {
                                            announcement = shouldAnnouncement(data, player, color, mythicItem, enchantments, announcement, results, rareResults);
                                        }
                                    }
                                }
                            } else if (singleLevel == 2) {
                                int choice = random.nextInt(2);
                                switch (choice) {
                                    case 0: {
                                        mythicItem.getEnchantments().put(enchantment, 3);
                                        if (enchantment.getRarity() == EnchantmentRarity.RARE || enchantment.getRarity() == EnchantmentRarity.RAGE_RARE) {
                                            announcement = true;
                                        }
                                        break;
                                    }
                                    case 1: {
                                        announcement = shouldAnnouncement(data, player, color, mythicItem, enchantments, announcement, results, rareResults);
                                        break;
                                    }
                                }
                            } else {
                                announcement = shouldAnnouncement(data, player, color, mythicItem, enchantments, announcement, results, rareResults);
                            }
                        } else if (amount == 2) {
                            int choice = random.nextInt(2);
                            switch (choice) {
                                case 0: {
                                    AbstractEnchantment enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(enchantments.toArray());
                                    mythicItem.getEnchantments().put(enchantment, 2);
                                    break;
                                }
                                case 1: {
                                    results.removeAll(enchantments);
                                    AbstractEnchantment enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(results.toArray());
                                    if (rollRare(data, player, color, 0.75d)) {
                                        announcement = true;
                                        rareResults.removeAll(enchantments);
                                        enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(rareResults.toArray());
                                    }
                                    enchantments.add(enchantment);
                                    mythicItem.getEnchantments().put(enchantment, 1);
                                    break;
                                }
                            }
                        }
                    }
                } else if (level == 3) {
                    int amount = mythicItem.getEnchantments().size();
                    EnchData data = processAttachedEnchant(profile, mythicItem);
                    if (data.getArgs().containsKey("phase_book")) {
                        String nbtName = data.getArgs().get("phase_book").toString();
                        AbstractEnchantment bookEnch = rareResults.stream()
                                .filter(e1 -> e1.getNbtName().equals(nbtName))
                                .findFirst()
                                .orElse(null);
                        if (bookEnch == null) {
                            bookEnch = results.stream()
                                    .filter(e1 -> e1.getNbtName().equals(nbtName))
                                    .findFirst()
                                    .orElse(null);
                        }
                        if (bookEnch != null && RandomUtil.hasSuccessfullyByChance(Util.ifNull((Double) data.getArgs().get("probability"), 0.01)) && amount != 3 && !mythicItem.getEnchantments().containsKey(bookEnch)) {
                            data.setCancel(true);
                            mythicItem.getEnchantments().put(bookEnch, 3);
                            mythicItem.boostedByPhaseBook = true;
                            if (bookEnch.getRarity() == EnchantmentRarity.RARE) announcement = true;
                        }
                    }
                    if (!data.isCancel()) {
                        if (amount == 1) {
                            AbstractEnchantment enchantment = null;
                            for (int i = 0; i < 2; i++) {
                                results.removeAll(enchantments);
                                if (RandomUtil.hasSuccessfullyByChance(NewConfiguration.getChance(player, color) * 0.75d)) {
                                    announcement = true;
                                    rareResults.removeAll(enchantments);
                                    enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(rareResults.toArray());
                                } else {
                                    enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(results.toArray());
                                }
                                enchantments.add(enchantment);
                                mythicItem.getEnchantments().put(enchantment, 1);
                            }
                            for (AbstractEnchantment abstractEnchantment : enchantments) {
                                mythicItem.getEnchantments().put(abstractEnchantment, Math.max(mythicItem.getEnchantments().get(abstractEnchantment), getRandomLevel()));
                            }
                            int totalLevel = mythicItem.getEnchantments().values().stream().mapToInt(Integer::intValue).sum();
                            if ((totalLevel == 8 && RandomUtil.hasSuccessfullyByChance(0.9)) || totalLevel == 9)
                                if (enchantment != null)
                                    mythicItem.getEnchantments().put(enchantment, RandomUtil.hasSuccessfullyByChance(0.1) ? 2 : 1);


                        } else if (amount == 2) {
                            results.removeAll(enchantments);
                            AbstractEnchantment enchantment;
                            if (RandomUtil.hasSuccessfullyByChance(NewConfiguration.getChance(player, color) * 0.75d)) {
                                announcement = true;
                                rareResults.removeAll(enchantments);
                                enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(rareResults.toArray());
                            } else {
                                enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(results.toArray());
                            }
                            enchantments.add(enchantment);
                            mythicItem.getEnchantments().put(enchantment, 1);
                            for (AbstractEnchantment abstractEnchantment : enchantments) {
                                final int currentLevel = Math.max(mythicItem.getEnchantments().get(abstractEnchantment), getRandomLevel());
                                mythicItem.getEnchantments().put(abstractEnchantment, currentLevel);
                                if (currentLevel == 3 && (abstractEnchantment.getRarity() == EnchantmentRarity.RARE || abstractEnchantment.getRarity() == EnchantmentRarity.RAGE_RARE)) {
                                    announcement = true;
                                }
                            }
                            int totalLevel = mythicItem.getEnchantments().values().stream().mapToInt(Integer::intValue).sum();
                            if ((totalLevel == 8 && RandomUtil.hasSuccessfullyByChance(0.9)) || totalLevel == 9) {
                                if (enchantment != null) {
                                    mythicItem.getEnchantments().put(enchantment, RandomUtil.hasSuccessfullyByChance(0.1) ? 2 : 1);
                                }
                            }
                        } else if (amount == 3) {
                            for (AbstractEnchantment abstractEnchantment : enchantments) {
                                final int currentLevel = Math.max(mythicItem.getEnchantments().get(abstractEnchantment), getRandomLevel());
                                mythicItem.getEnchantments().put(abstractEnchantment, currentLevel);
                                if (currentLevel == 3 && (abstractEnchantment.getRarity() == EnchantmentRarity.RARE || abstractEnchantment.getRarity() == EnchantmentRarity.RAGE_RARE))
                                    announcement = true;

                            }
                            int totalLevel = mythicItem.getEnchantments().values().stream().mapToInt(Integer::intValue).sum();
                            if ((totalLevel == 8 && RandomUtil.hasSuccessfullyByChance(0.9)) || totalLevel == 9) {
                                mythicItem.getEnchantments().put((AbstractEnchantment) RandomUtil.helpMeToChooseOne(mythicItem.getEnchantments().keySet().toArray()), 1);
                            }
                        }
                        boolean badLuck = mythicItem.getEnchantments().values().stream().noneMatch(lv -> lv >= 3);
                        if (badLuck) {
                            AbstractEnchantment enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(mythicItem.getEnchantments().keySet().toArray());
                            mythicItem.getEnchantments().put(enchantment, 3);
                            if ((enchantment.getRarity() == EnchantmentRarity.RARE || enchantment.getRarity() == EnchantmentRarity.RAGE_RARE))
                                announcement = true;

                        }
                    }
                }
            }
        }
        mythicItem.getEnchantments().keySet().stream()
                .filter(enchantment -> !beforeEnchantments.containsKey(enchantment))
                .forEach(enchantment -> profile.incrementEnchantmentCount(enchantment.getNbtName()));

        mythicItem.getEnchantmentRecords()
                .add(new EnchantmentRecord(
                        player.getName(),
                        "Enchant Table",
                        System.currentTimeMillis()
                ));

        mythicItem.loadFromItemStack(mythicItem.toItemStack());
        new PitPlayerEnchantEvent(player, beforeItem, mythicItem).callEvent();

        JsonObject operationLog = ItemOperationJson.operation("ENCHANT", player);
        operationLog.add("item", ItemOperationJson.item(mythicItem.toItemStack()));
        operationLog.add("enchantResult", ItemOperationJson.enchantResult(tierBefore, beforeEnchantments, mythicItem));
        operationLog.addProperty("baseRareChance", baseRareChance);
        operationLog.addProperty("luckyGemActive", luckyGemActive);
        if (attachedEnchant != null && !attachedEnchant.getType().isAir()) {
            operationLog.add("attachedItem", ItemOperationJson.item(attachedEnchant));
            operationLog.addProperty("attachedItemConsumed", profile.getAttachedEnchant() == null);
        }
        if (attachedPhaseBook && profile.getAttachedEnchant() == null) {
            operationLog.add("phaseBook", ItemOperationJson.item(attachedEnchant));
            operationLog.addProperty("phaseBookSuccess", mythicItem.isBoostedByPhaseBook());
        }
        log.info("{}", ItemOperationJson.toJson(operationLog));

        if (announcement) {
            Component message = new ChatComponentBuilder(
                    CC.translate("&d&l稀有附魔! &7" + profile.getFormattedNameWithRoman() + " &7在神话之井中获得了稀有物品: " + mythicItem.toItemStack().getItemMeta().getDisplayName() + " &e[查看]")
            ).setCurrentHoverEvent(mythicItem.toItemStack().asHoverEvent()).build();

            NBT.get(mythicItem.toItemStack(), nbt -> {
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(message));
            });
            Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
                Util.playRareEnchantmentEffect(player);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
            }, 1L);
        }
        profile.setEnchantingItem(InventoryUtil.serializeItemStack(mythicItem.toItemStack()));
    }

    private EnchData processAttachedEnchant(PlayerProfile profile, IMythicItem mythicItem) {
        EnchData data = new EnchData();
        if (profile.getAttachedEnchant() != null) {
            String name = ItemUtil.getInternalName(InventoryUtil.deserializeItemStack(profile.getAttachedEnchant()));
            for (PitItems pitItem : PitItems.values()) {
                if (pitItem.getSingletonItem() instanceof DoEnch d && pitItem.getInternalName().equals(name)) {
                    data.setIs(InventoryUtil.deserializeItemStack(profile.getAttachedEnchant()), mythicItem, profile);
                    d.ench(data);
                }
            }
        }
        return data;
    }

    private boolean shouldAnnouncement(EnchData data, Player player, MythicColor color, AbstractPitItem
            mythicItem, List<AbstractEnchantment> enchantments, boolean announcement, List<
            AbstractEnchantment> results, List<AbstractEnchantment> rareResults) {
        results.removeAll(enchantments);
        AbstractEnchantment enchantment;
        if (rollRare(data, player, color, 1.0d)) {
            announcement = true;
            rareResults.removeAll(enchantments);
            enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(rareResults.toArray());
        } else {
            enchantment = (AbstractEnchantment) RandomUtil.helpMeToChooseOne(results.toArray());
        }
        enchantments.add(enchantment);
        mythicItem.getEnchantments().put(enchantment, 1);
        return announcement;
    }

    private boolean rollRare(EnchData data, Player player, MythicColor color, double baseMultiplier) {
        Object rareValue = data.getArgs().remove("rare");
        double rareBonus = rareValue instanceof Number number ? number.doubleValue() / 100.0D : 0.0D;
        double chance = NewConfiguration.getChance(player, color) * baseMultiplier + rareBonus;
        return RandomUtil.hasSuccessfullyByChance(Math.max(0.0D, Math.min(1.0D, chance)));
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }

    public ItemStack getDefaultDisplayItem() {
        return new ItemBuilder(Material.ENCHANTING_TABLE)
                .name("&d神话之井")
                .lore("&7通过击杀玩家来获得", "&e神话之剑&7 &b神话之弓 &7以及", "&c神&6话&9之&a甲 &7等物品 ", " ", "&7在神话之井中为这些物品附魔", "&7可以赋予其大量的强大增益 ", " ", "&d放入神话物品到左侧空格中以开始!")
                .build();
    }

    private int getRandomLevel() {
        return RandomUtil.hasSuccessfullyByChance(0.6) ? 1 : random.nextInt(2) + 2;
    }

    private boolean removeMythicLegWithColor(Player player, MythicColor color) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile.getEnchantingScience() == null) return false;
        ItemStack itemStack = InventoryUtil.deserializeItemStack(profile.getEnchantingScience());
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }
        String mythic_color = ItemUtil.getItemStringData(itemStack, "mythic_color");
        if (color.getInternalName().equals(mythic_color)) {
            profile.setEnchantingScience(InventoryUtil.serializeItemStack(new ItemStack(Material.AIR)));
            return true;
        }
        return false;
    }
}
