package cn.charlotte.pit.menu.viewer.button;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.perk.PerksPassivesEnum;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.RomanUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/12 16:34
 */
public class PitPassiveStatusButton extends Button {
    private final PlayerProfile profile;

    public PitPassiveStatusButton(PlayerProfile profile) {
        this.profile = profile;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        List<String> lores = new ArrayList<>();
        int xpBoostLevel = profile.getLevel(PerksPassivesEnum.XP_BOOST);
        int goldBoostLevel = profile.getLevel(PerksPassivesEnum.GOLD_BOOST);
        int meleeDamageBoostLevel = profile.getLevel(PerksPassivesEnum.MELEE_DAMAGE);
        int bowDamageBoostLevel = profile.getLevel(PerksPassivesEnum.BOW_DAMAGE);
        int damageReductionBoostLevel = profile.getLevel(PerksPassivesEnum.DAMAGE_REDUCTION);
        int buildBattlerBoostLevel = profile.getLevel(PerksPassivesEnum.BUILD_BATTLER);
        int elGatoBattlerBoostLevel = profile.getLevel(PerksPassivesEnum.EL_GATO);
        lores.add("&7经验获取提升: &e" + (xpBoostLevel == 0 ? "&c未解锁" : RomanUtil.convert(xpBoostLevel)));
        lores.add("&7硬币获取提升: &e" + (goldBoostLevel == 0 ? "&c未解锁" : RomanUtil.convert(goldBoostLevel)));
        lores.add("&7近战伤害提升: &e" + (meleeDamageBoostLevel == 0 ? "&c未解锁" : RomanUtil.convert(meleeDamageBoostLevel)));
        lores.add("&7远程伤害提升: &e" + (bowDamageBoostLevel == 0 ? "&c未解锁" : RomanUtil.convert(bowDamageBoostLevel)));
        lores.add("&7伤害减免: &e" + (damageReductionBoostLevel == 0 ? "&c未解锁" : RomanUtil.convert(damageReductionBoostLevel)));
        lores.add("&7方块存在时间提升: &e" + (buildBattlerBoostLevel == 0 ? "&c未解锁" : RomanUtil.convert(buildBattlerBoostLevel)));
        lores.add("&7先发制人: &e" + (elGatoBattlerBoostLevel == 0 ? "&c未解锁" : RomanUtil.convert(elGatoBattlerBoostLevel)));
        if (!profile.getPlayerOption().isProfileVisibility() && !PlayerUtil.isStaff(player)) {
            lores.clear();
            lores.add("&c此玩家隐藏了他的档案信息.");
        }
        return new ItemBuilder(Material.CAKE).name("&a被动天赋").lore(lores).build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {

    }
}
