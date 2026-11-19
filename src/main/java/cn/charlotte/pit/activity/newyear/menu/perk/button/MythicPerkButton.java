package cn.charlotte.pit.activity.newyear.menu.perk.button;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.newyear.NewYearData;
import cn.charlotte.pit.activity.newyear.menu.perk.PerkMenu;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MythicPerkButton extends Button {

    @Override
    public ItemStack getButtonItem(Player player) {
        List<String> lore = new ArrayList<>();
        PlayerProfile profile = Util.getPitProfile(player);
        lore.add("");
        Integer level = profile.getActivityPlayerData().getNewYearData().getPerkMap().get("mysticism");

        if (level == null) {
            level = 0;
        }

        if (PerksPrestigeEnum.MYSTICISM.getPerk() == null || PerksPrestigeEnum.MYSTICISM.getPerk().getLevel(profile) <= 0)
            lore.add("&c请你先解锁 &b精通 &c里的神话附魔师再来这里解锁!");
        else if (level == 0)
            lore.add("&7该天赋将提升你的神话装备掉落概率!");
        else
            lore.add(String.format("&7该天赋提供的神话装备掉落概率: &e&l%s%%", level * 10));

        lore.add("");

        if (level > 0) lore.add(String.format("&7目前等级 &e&l%s", level));
        else lore.add("&7尚未解锁");

        if (level > 0) {
            if (level == 5) lore.add("&a已到达最高等级了!");
            else {
                lore.add(String.format("&7升级所需活动点数: %s", 5000 * level));
                if (level > 3) lore.add(String.format("&7所需&b特殊活动点数&7:%s", 200 * level));
            }
        } else {
            lore.add("&7解锁所需活动点数: 5000");
        }

        lore.add("");
        return new ItemBuilder(Material.GOLDEN_SWORD)
                .name("&d神话附魔学徒")
                .lore(lore)
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        PlayerProfile profile = Util.getPitProfile(player);
        NewYearData data = profile.getActivityPlayerData().getNewYearData();
        if (PerksPrestigeEnum.MYSTICISM.getPerk().getLevel(profile) <= 0) {
            CC.send(player, "&c请你先解锁 &e神话附魔师");
            return;
        }
        Integer level = data.getPerkMap().get("mysticism");
        if (level == null) level = 0;
        if (level == 5) {
            CC.send(player, "&c你已到达最高等级了");
            return;
        }
        if (level == 0)
            toUp(data, 0, 5000, player, true);
        else
            toUp(data, level > 3 ? 200 * level : 0, 5000 * level, player, false);

    }

    private void toUp(NewYearData data, int special, int coins, Player player, boolean first) {
        if (data.getCoins() < coins) {
            CC.send(player, "&c你的活动点数不足!");
            return;
        }

        if (special != 0 && data.getPoints() < special) {
            CC.send(player, "&c你的特殊活动点数不足!");
            return;
        }
        data.removeCoins(coins);
        if (special != 0) data.removePoint(special);
        if (first) CC.send(player, "&a解锁天赋!");
        else CC.send(player, "&a天赋升级!");
        data.getPerkMap().merge("mysticism", 1, Integer::sum);
        new PerkMenu().openMenu(player);
    }
}