package cn.charlotte.pit.activity.newyear.medal;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.medal.AbstractMedal;
import cn.charlotte.pit.util.chat.RomanUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class NewYear extends AbstractMedal {

    private final NewYear In;

    public NewYear() {
        this.In = this;
    }

    @Override
    public String getInternalName() {
        return "new_year";
    }

    @Override
    public String getDisplayName(int level) {
        return "持之以恒";
    }

    @Override
    public String getRequirementDescription(int level) {
        return "在[新年]大型活动中使用一次纪念币";
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public int getProgressRequirement(int level) {
        return 1;
    }

    @Override
    public int getRarity(int level) {
        return 3;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public void handleProfileLoaded(PlayerProfile profile) {

    }

//    @Override
//    public Button getButton(PlayerProfile profile, int progress, boolean locked) {
//        return new Button() {
//            @Override
//            public ItemStack getButtonItem(Player player) {
//                ItemBuilder builder = new ItemBuilder(locked ? Material.NETHER_STAR : Material.COAL);
//
//                for (int i = 1; i <= getMaxLevel(); i++) {
//                    builder.name(String.format("&c%s%s", getDisplayName(i), getMaxLevel() > 1 ? " " + RomanUtil.convert(i) : ""));
//                    builder.amount(getRarity(i));
//                }
//                List<String> lines = new ArrayList<>(Util.templateMedalLore(profile, progress, locked, In));
//
//
//                return builder.lore(lines).build();
//            }
//
//            @Override
//            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
//                if (!locked) {
//                    return;
//                }
//            }
//        };
//    }
}
