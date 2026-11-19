package cn.charlotte.pit.menu.perk.normal.buy.button;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.perk.PerksPassivesEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.RomanUtil;
import cn.charlotte.pit.util.level.LevelUtil;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/7 15:47
 */
public class BoostButton extends Button {
    private final TypedPerk<?> perk;

    public BoostButton(TypedPerk<?> perk) {
        this.perk = perk;
    }

    @Override
    public @Nullable ItemStack getButtonItem(Player player) {
        int[] price = new int[0];
        int[] limit = new int[]{0, 10, 20, 30, 40};
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        List<String> lore = new ArrayList<>();
        if (!perk.isPassivePerk()) {
            return null;
        }

        int level = perk.getLevel(profile);
        lore.add("&7当前等级: &a" + RomanUtil.convert(level));
        lore.add(" ");
        lore.addAll(perk.getDescription(player));
        lore.add(" ");
        if (level >= 5) {
            lore.add("&c此加成等级已达到上限!");
        } else {
            if (perk.getType().equals(PerksPassivesEnum.XP_BOOST)) {
                price = new int[]{500, 1000, 2500, 10000, 25000};
            }

            if (perk.getType().equals(PerksPassivesEnum.GOLD_BOOST)) {
                price = new int[]{1000, 2500, 10000, 25000, 40000};
            }

            if (perk.getType().equals(PerksPassivesEnum.MELEE_DAMAGE)
                    || perk.getType().equals(PerksPassivesEnum.BOW_DAMAGE)
                    || perk.getType().equals(PerksPassivesEnum.DAMAGE_REDUCTION)
            ) {
                price = new int[]{450, 1050, 1500, 2250, 3000};
            }

            if (perk.getType().equals(PerksPassivesEnum.BUILD_BATTLER)) {
                price = new int[]{750, 1750, 2750, 3750, 5000};
            }

            if (perk.getType().equals(PerksPassivesEnum.EL_GATO)) {
                price = new int[]{1000, 2000, 3000, 4000, 5000};
            }
            if (limit[level] > profile.getLevel()) {
                lore.add("&c等级不足: " + LevelUtil.getLevelTag(profile.getPrestige(), limit[level]));
            }
            if (price.length >= level + 1) {
                lore.add("&7升级价格: &6" + price[level] + " 硬币");
                if (price[level] > profile.getCoins()) {
                    lore.add("&c你的硬币不足!");
                } else {
                    lore.add("&a点击升级此加成!");
                }
            }
        }
        return perk.getIconWithNameAndLore("&f" + perk.getDisplayName(), lore, (int) perk.requireRenown(0), 1);
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        int[] price = new int[0];
        int[] limit = new int[]{0, 10, 20, 30, 40};

        if (!(perk.isPassivePerk())) {
            return;
        }

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        int level = perk.getLevel(profile);
        if (level < 5) {
            if (perk.getType().equals(PerksPassivesEnum.XP_BOOST)) {
                price = new int[]{500, 1000, 2500, 10000, 25000};
            }
            if (perk.getType().equals(PerksPassivesEnum.GOLD_BOOST)) {
                price = new int[]{1000, 2500, 10000, 25000, 40000};
            }
            if (perk.getType().equals(PerksPassivesEnum.MELEE_DAMAGE)
                    || perk.getType().equals(PerksPassivesEnum.BOW_DAMAGE)
                    || perk.getType().equals(PerksPassivesEnum.DAMAGE_REDUCTION)
            ) {
                price = new int[]{450, 1050, 1500, 2250, 3000};
            }
            if (perk.getType().equals(PerksPassivesEnum.BUILD_BATTLER)) {
                price = new int[]{750, 1750, 2750, 3750, 5000};
            }
            if (perk.getType().equals(PerksPassivesEnum.EL_GATO)) {
                price = new int[]{1000, 2000, 3000, 4000, 5000};
            }
        }
        if (level >= limit.length || limit[level] > profile.getLevel()) {
            return;
        }
        if (price.length >= level + 1) {
            if (price[level] <= profile.getCoins()) {
                profile.takeCoins(price[level]);
                profile.getUnlockedPerkMap().put(perk.getInternalPerkName(), level + 1);
                player.sendMessage(CC.translate("&a&l升级成功! &7成功升级加成 &a" + perk.getDisplayName() + " &7至等级 &b" + RomanUtil.convert(level + 1) + " &7."));
            }
        }
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }
}
