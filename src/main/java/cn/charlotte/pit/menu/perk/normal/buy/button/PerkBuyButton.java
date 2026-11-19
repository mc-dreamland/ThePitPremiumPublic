package cn.charlotte.pit.menu.perk.normal.buy.button;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.perk.UnKnowButton;
import cn.charlotte.pit.perk.AbstractPerk;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksBundlesEnum;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.level.LevelUtil;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 23:10
 */
public class PerkBuyButton extends Button {
    private final TypedPerk<?> perk;
    private final int page;
    private final PerkType perkType;

    public PerkBuyButton(TypedPerk<?> perk, int page, PerkType perkType) {
        this.perk = perk;
        this.page = page;
        this.perkType = perkType;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        DecimalFormat df = new DecimalFormat(",###,###,###,###");
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        boolean special = false;
        //精通天赋 or SpecialPerk
        if (perk.requirePrestige() > 0 || special || perk.getPerkType() == PerkType.KILL_STREAK || perk.getPerkType() == PerkType.MEGA_STREAK) {
            boolean unlocked = false;
            if (perk.getPerkType() == PerkType.PERK) {
                unlocked = profile.getUnlockedPerkMap().containsKey(perk.getInternalPerkName());
            }
            //killStreak purchase check
            if (perk.getPerkType() == PerkType.KILL_STREAK || perk.getPerkType() == PerkType.MEGA_STREAK) {
                unlocked = true;
                if (perk.getType() instanceof PerksBundlesEnum be) {
                    unlocked = be.hasUnlockParent(player);
                }
            }
            if (!unlocked && ((perk.requirePrestige() <= profile.getPrestige()) || special)) {
                List<String> lore = new ArrayList<>(perk.getDescription(player));
                lore.add(" ");
                lore.add("&7价格: &6" + df.format(perk.requireCoins()) + "硬币");
                if (perk.requireLevel() > profile.getLevel()) {
                    lore.add("&c等级不足: " + LevelUtil.getLevelTag(Math.max(profile.getPrestige(), perk.requirePrestige()), perk.requireLevel()));
                }
                if (special) {
                    lore.add("&c此%type%只能通过特殊获取方式获得!".replace("%type%", perkType.getDisplayName()));
                } else {
                    lore.add("&c请先在精通商店中解锁此%type%的购买权!".replace("%type%", perkType.getDisplayName()));
                }
                if (special) {
                    return new ItemBuilder(Material.BEDROCK).name("&c" + perk.getDisplayName()).lore(lore).shiny().build();
                } else {
                    return new ItemBuilder(Material.BEDROCK).name("&c" + perk.getDisplayName()).lore(lore).build();
                }
            }
        }

        if ((perk.getPerkType() == PerkType.PERK && profile.getLevel() < perk.requireLevel() && !PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.THE_WAY)) || (perk.requirePrestige() > 0 && profile.getPrestige() < perk.requirePrestige())) {
            return new UnKnowButton().getButtonItem(player);
        }


        List<String> lore = new ArrayList<>(perk.getDescription(player));
        lore.add("");

        int data = profile.getUnlockedBoughtPerkMap().getOrDefault(perk.getInternalPerkName(), 0);
        if (data == 0) {
            lore.add("&7价格: &6" + df.format(perk.requireCoins()) + " 硬币");
        } else {
            lore.add("");
        }
        if (perk.requireLevel() > profile.getLevel()) {
            if (!PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.THE_WAY)) {
                lore.add("&c等级不足: " + LevelUtil.getLevelTag(profile.getPrestige(), perk.requireLevel()));
                return new ItemBuilder(Material.BEDROCK)
                        .name("&c" + perk.getDisplayName())
                        .lore(lore)
                        .build();
            } else {
                if (perk.getPerkType() != PerkType.PERK) {
                    lore.add("&c等级不足: " + LevelUtil.getLevelTag(profile.getPrestige(), perk.requireLevel()));
                    return new ItemBuilder(Material.BEDROCK)
                            .name("&c" + perk.getDisplayName())
                            .lore(lore)
                            .build();
                }
            }
        }
        if (perk.hasEquip(profile)) {
//            if ((profile.getChosePerk()[page - 1]).equals(perk.getInternalPerkName())) {
            if (profile.perkEqualsChosePerk(perk.getInternalPerkName(), page - 1)) {
                lore.add("&c你已经装备了此%type%!".replace("%type%", perkType.getDisplayName()));
            } else {
                lore.add("&c你在其他栏位装备了本%type%!".replace("%type%", perkType.getDisplayName()));
            }
            return perk.getIconWithNameAndLore("&c" + perk.getDisplayName(), lore, 0, 1);
        }

        if (data > 0) {
            if (profile.getChosePerk()[page - 1] != null) {
                lore.add("&a点击更换该%type%!".replace("%type%", perkType.getDisplayName()));
            } else {
                lore.add("&a点击使用该%type%!".replace("%type%", perkType.getDisplayName()));
            }
        } else {
            if (perk.requireLevel() > profile.getLevel()) {
                if (!PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.THE_WAY)) {
                    lore.add("&c等级不足: " + LevelUtil.getLevelTag(profile.getPrestige(), perk.requireLevel()));
                    return new ItemBuilder(Material.BEDROCK)
                            .name("&c" + perk.getDisplayName())
                            .lore(lore)
                            .build();
                } else {
                    if (perk.getPerkType() != PerkType.PERK) {
                        lore.add("&c等级不足: " + LevelUtil.getLevelTag(profile.getPrestige(), perk.requireLevel()));
                        return new ItemBuilder(Material.BEDROCK)
                                .name("&c" + perk.getDisplayName())
                                .lore(lore)
                                .build();
                    }
                }
            }
            if (profile.getCoins() < perk.requireCoins()) {
                lore.add("&c硬币不足!");
            } else {
                if (profile.getChosePerk()[page - 1] != null) {
                    lore.add("&e点击购买并更换该%type%!".replace("%type%", perkType.getDisplayName()));
                } else {
                    lore.add("&e点击购买并使用该%type%!".replace("%type%", perkType.getDisplayName()));
                }
            }
            return perk.getIconWithNameAndLore("&e" + perk.getDisplayName(), lore, 0, 1);
        }
        return perk.getIconWithNameAndLore("&a" + perk.getDisplayName(), lore, 0, 1);
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        //check perk prestige requirement
        if (perk.requirePrestige() > profile.getPrestige()) {
            return;
        }
        //check perk level requirement
        if (perk.requireLevel() > profile.getLevel()) {
            //check if perk type is PERK and player unlocked The Way
            if (!PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.THE_WAY)) {
                return;
            } else {
                if (perk.getPerkType() != PerkType.PERK) {
                    return;
                }
            }
        }
        //check if player has equipped this perk

        if (perk.hasEquip(profile)) {
            if (profile.perkEqualsChosePerk(perk.getInternalPerkName(), page - 1)) {
//            if ((profile.getChosePerk()[page - 1]).equals(perk.getInternalPerkName())) {
                player.sendMessage(CC.translate("&c你已经装备了此%type%!".replace("%type%", perkType.getDisplayName())));
            } else {
                player.sendMessage(CC.translate("&c你在其他栏位装备了本%type%!".replace("%type%", perkType.getDisplayName())));
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        int perkLevel = profile.getUnlockedBoughtPerkMap().getOrDefault(perk.getInternalPerkName(), 0);

        //check if player bought the perk
        if (perkLevel > 0) {
            //inactive the old perk
            String chosePerk = profile.getChosePerk()[page - 1];
            if (chosePerk != null) {
                ThePit.getInstance().getPerkFactory().getPerkMap().get(chosePerk).onPerkInactive(player);
            }
            profile.getChosePerk()[page - 1] = perk.getInternalPerkName();
            player.sendMessage(CC.translate("&a&l装备成功! &7成功在 &f#" + page + " &7%type%栏装备%type% &a".replace("%type%", perkType.getDisplayName()) + perk.getDisplayName() + " &7."));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
            perk.onPerkActive(player);
        } else {
            if (profile.getPrestige() >= perk.requirePrestige()) {
                boolean special = false; //if a perk is limited
                if (special || perk.requirePrestige() > 0 || perk.getPerkType() == PerkType.KILL_STREAK || perk.getPerkType() == PerkType.MEGA_STREAK) {
                    boolean unlocked = false;
                    if (perk.getPerkType() == PerkType.PERK) {
                        int level = perk.getLevel(profile);
                        if (level > 0) {
                            unlocked = true;
                        }
                    }
                    //killStreak purchase check
                    if (perk.getPerkType() == PerkType.KILL_STREAK || perk.getPerkType() == PerkType.MEGA_STREAK) {
                        if (perk.getType() instanceof PerksBundlesEnum be) {
                            unlocked = be.hasUnlockParent(player);
                        }
                    }
                    if (!unlocked) {
                        return;
                    }
                }
                if (profile.getCoins() < perk.requireCoins()) {
                    player.sendMessage(CC.translate("&c你的硬币不足!"));
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                } else {
                    String chosePerkName = profile.getChosePerk()[page - 1];
                    if (chosePerkName != null) {
                        AbstractPerk chosePerk = ThePit.getInstance().getPerkFactory().getPerkMap().get(chosePerkName);
                        if (chosePerk != null) {
                            chosePerk.onPerkInactive(player);
                        }
                    }
                    profile.getUnlockedBoughtPerkMap().put(perk.getInternalPerkName(), 1);
                    profile.takeCoins(perk.requireCoins());
                    profile.getChosePerk()[page - 1] = perk.getInternalPerkName();
                    player.sendMessage(CC.translate("&a&l装备成功! &7成功在 &f#" + page + " &7%type%栏装备%type% &a".replace("%type%", perkType.getDisplayName()) + perk.getDisplayName() + " &7."));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                    perk.onPerkActive(player);
                }
            }
        }
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }
}
