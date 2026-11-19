package cn.charlotte.pit.menu.perk.prestige.button;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitPlayerUnlockPerkEvent;
import cn.charlotte.pit.event.PitPlayerUpgradePerkEvent;
import cn.charlotte.pit.menu.perk.UnKnowButton;
import cn.charlotte.pit.menu.perk.prestige.PrestigePerkBuyMenu;
import cn.charlotte.pit.perk.MegaStreak;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.RomanUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.level.LevelUtil;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.menus.ConfirmMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/4 18:45
 */
public class PrestigePerkBuyButton extends Button {
    private final TypedPerk<?> perk;
    private final PerkGroup perkGroup;

    public PrestigePerkBuyButton(TypedPerk<?> perk, PerkGroup perkGroup) {
        this.perk = perk;
        this.perkGroup = perkGroup;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        if (profile.getPrestige() < perk.requirePrestige()) {
            //精通等级不足
            return new UnKnowButton().getButtonItem(player);
        }

        List<String> lore = new ArrayList<>();

        int perkLevel = perk.getLevel(profile);

        if (perkLevel > 0) {
            if (perk.getMaxLevel() > 1) {
                lore.add("&7当前等级: &b" + RomanUtil.convert(perkLevel));
            }
        }
        if (perk.requireLevel() != 0) {
            lore.add("&7解锁等级: " + LevelUtil.getLevelTag(profile.getPrestige(), perk.requireLevel()));
            lore.add("&7价格: &6" + perk.requireCoins() + " 硬币");
            lore.add(" ");
            lore.add("&e" + perk.getDisplayName());
            if (this.perkGroup.equals(PerkGroup.STREAK)) {
                lore.addAll(((MegaStreak) perk).getDescriptionInRenownShop(player));
            } else {
                lore.addAll(perk.getDescription(player));
            }
            lore.add(" ");
            lore.add("&7&o这是一个天赋解锁类天赋,");
            lore.add("&7&o解锁该精通天赋相当于解锁其普通天赋的购买权,");
            lore.add("&7&o仍然需要达到等级要求后花费硬币购买&装备后才能生效.");
        } else {
            lore.addAll(perk.getDescription(player));
        }
        lore.add(" ");
        String color;
        if (perkLevel > 0) {
            if (perkLevel >= perk.getMaxLevel()) {
                lore.add("&a此精通天赋已被提升至最大等级!");
                color = "&a";
            } else {
                lore.add("&7价格: &e" + perk.requireRenown(perkLevel + 1) + " 声望");
                lore.add("&7你的声望: &e" + profile.getRenown() + " 声望");
                lore.add(" ");
                if (perk.requireRenown(perkLevel + 1) > profile.getRenown()) {
                    lore.add("&c你没有足够的声望!");
                    color = "&c";
                } else {
                    lore.add("&e点击升级此精通天赋!");
                    color = "&e";
                }
            }
        } else {
            lore.add("&7价格: &e" + perk.requireRenown(1) + " 声望");
            lore.add("&7你的声望: &e" + profile.getRenown() + " 声望");
            lore.add(" ");
            if (perk.requireRenown(1) > profile.getRenown()) {
                lore.add("&c你没有足够的声望!");
                color = "&c";
            } else {
                lore.add("&e点击解锁此精通天赋!");
                color = "&e";
            }
        }
        if (perk.requireLevel() != 0) {
            return perk.getIconWithNameAndLore(color + "天赋解锁: " + perk.getDisplayName(), lore, 0, 1);
        } else {
            return perk.getIconWithNameAndLore(color + perk.getDisplayName(), lore, 0, 1);
        }
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        int perkLevel = perk.getLevel(profile);
        if (perk.requirePrestige() > profile.getPrestige()) {
            return;
        }
        if (perkLevel > 0) {
            if ((perk.getMaxLevel() > perkLevel)) {
                int price = (int) perk.requireRenown(perkLevel + 1);
                if (price > profile.getRenown()) {
                    player.sendMessage(CC.translate("&c你没有足够的声望!"));
                } else {
                    profile.changeRenown(profile.getRenown() - price);
                    new PitPlayerUpgradePerkEvent(player, perk, perkLevel + 1);
                    profile.setLevel(perk.getInternalPerkName(), perkLevel + 1);
                    perkLevel += 1;
                    perk.onUpgrade(player);
                    player.sendMessage(CC.translate("&a&l天赋升级! &7你升级了精通天赋 &e" + perk.getDisplayName() + " &7至等级 &b" + RomanUtil.convert(perkLevel) + " &7."));
                }
            }
        } else {
            int price = (int) perk.requireRenown(1);
            if (price > profile.getRenown()) {
                player.sendMessage(CC.translate("&c你没有足够的声望!"));
            } else {
                new ConfirmMenu("购买天赋 " + perk.getDisplayName(), confirm -> {
                    if (confirm) {
                        profile.changeRenown(profile.getRenown() - price);

                        profile.getUnlockedPerkMap().put(perk.getInternalPerkName(), 1);
                        perk.onUnlock(player);
                        new PitPlayerUnlockPerkEvent(player, perk).callEvent();
                        if (perk.getInternalPerkName().equals(PerksPrestigeEnum.CELEBRITY.getPerk().getInternalPerkName()))
                            CC.boardCast("&e&l恭喜 " + Util.getPitProfile(player).getFormattedNameWithRoman() + " &f到达了&e旅途的中途站&l!");
                        player.sendMessage(CC.translate("&a&l天赋解锁! &7你解锁了精通天赋 &e" + perk.getDisplayName() + " &7."));
                    }
                    new PrestigePerkBuyMenu(this.perkGroup).openMenu(player);
                }, true, 5, new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                        List<String> lore = new ArrayList<>();

                        lore.add("&7天赋名称: &e" + perk.getDisplayName());
                        lore.add("&7所需精通等级: &b" + perk.requirePrestige());
                        lore.add("&7解锁等级: " + LevelUtil.getLevelTag(profile.getPrestige(), perk.requireLevel()));
                        lore.add("&7价格: &6" + perk.requireCoins() + " 硬币");
                        lore.add("&7声望价格: &e" + perk.requireRenown(1) + " 声望");
                        lore.add(" ");

                        if (perkGroup.equals(PerkGroup.STREAK)) {
                            lore.addAll(((MegaStreak) perk).getDescriptionInRenownShop(player));
                        } else {
                            lore.addAll(perk.getDescription(player));
                        }

                        lore.add(" ");
                        lore.add("&7&o这是一个天赋解锁类天赋,");
                        lore.add("&7&o解锁该精通天赋相当于解锁其普通天赋的购买权,");
                        lore.add("&7&o仍然需要达到等级要求后花费硬币购买&装备后才能生效.");

                        return new ItemBuilder(perk.getIcon())
                                .name("&6" + perk.getDisplayName())
                                .lore(lore)
                                .build();
                    }

                    @Override
                    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                    }
                }).openMenu(player);
            }
        }
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }
}
