package cn.charlotte.pit.activity.newyear.menu.perk.button;

import cn.charlotte.pit.activity.newyear.NewYearData;
import cn.charlotte.pit.activity.newyear.menu.perk.PerkMenu;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class potionPerkButton extends Button {

    private NewYearData data;

    public potionPerkButton(NewYearData data) {
        this.data = data;
    }
    
    @Override
    public ItemStack getButtonItem(Player player) {
        if (data == null) return null;
        Integer level = this.data.getPerkMap().get("PotionKill");
        
        List<String> lore = new ArrayList<>(List.of(
                "",
                "&7该天赋升级将给你带来更多的活动点数"
        ));
        if (level != null && level > 0) {
            lore.add("");
            lore.add(String.format("&7- 击杀玩家获得 &e1-%s 活动点数", level - 1));
            if (level >= 3) lore.add(String.format("&7- 击杀玩家%s%%概率获得&b特殊活动点数", level * 2));
            lore.add(String.format("&f目前等级:&e&l%s", level));
        }
        if (level == null || level <= 0) {
            lore.add("");
            lore.add("&a解锁所需: 无");
            lore.add("");
        } else if (level < 5) {
            int activityCost;
            int specialCost = 0;
            if (level < 3) {
                activityCost = 2000 * level;
            } else {
                activityCost = 4000 * level;
                specialCost = 100 * level;
            }
            lore.add("");
            lore.add(String.format("&a升级所需: &e%s活动点数", activityCost));
            if (specialCost > 0) {
                lore.add(String.format("&a额外所需: &b%s特殊点数", specialCost));
            }
            lore.add("");
        } else {
            lore.add("");
            lore.add("&a已到达最高等级!");
            lore.add("");
        }
        
        return new ItemBuilder(Material.DIAMOND_BLOCK)
                .name("&b活动点数升级")
                .lore(lore)
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        if (data == null) return;

        Integer level = this.data.getPerkMap().get("PotionKill");
        if (level == null || level == 0) {
            this.toUp(0, 0, player, true);
            return;
        }

        if (level >= 5) {
            CC.send(player, "&c已经升级到最大等级");
            return;
        }

        if (level < 3)
            this.toUp(0, 2000 * level, player, false);
        else
            this.toUp(100 * level, 4000 * level, player, false);

    }

    private void toUp(int special, int coins, Player player, boolean first) {
        if (this.data.getCoins() < coins) {
            CC.send(player, "&c你的活动点数不足!");
            return;
        }

        if (special != 0 && this.data.getPoints() < special) {
            CC.send(player, "&c你的特殊活动点数不足!");
            return;
        }
        this.data.removeCoins(coins);
        if (special != 0) this.data.removePoint(special);
        if (first) CC.send(player, "&a解锁天赋!");
        else CC.send(player, "&a天赋升级!");
        this.data.getPerkMap().merge("PotionKill", 1, Integer::sum);
        new PerkMenu().openMenu(player);
    }
}
