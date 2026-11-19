package cn.charlotte.pit.hologram.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.ActivityType;
import cn.charlotte.pit.activity.newyear.NewYearData;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.hologram.AbstractHologram;
import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.Utils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class EnchantTipHologram extends AbstractHologram {

    private static final DecimalFormat format = new DecimalFormat("0.##");

    @Override
    public String getInternalName() {
        return "HelpHoloGram";
    }

    @Override
    public List<String> getText(Player player) {
        double modifiers = 0;
        PlayerProfile profile = Util.getPitProfile(player);
        int level = PerksPrestigeEnum.MYSTICISM.getPerk().getLevel(profile);
        modifiers += Math.max(0, level <= 0 ? 0 : (level - 1) * 0.2d);
        int pantsLevel = Utils.getEnchantLevel(player.getInventory().getLeggings(), "pants_radar");
        int mainHandLevel = Utils.getEnchantLevel(player.getInventory().getItemInMainHand(), "pants_radar");
        if (pantsLevel > 0) {
            modifiers += pantsLevel * 0.3;
        }
        if (mainHandLevel > 0) {
            modifiers += mainHandLevel * 0.3;
        }

        if (ThePit.getInstance().getActivityFactory().getFactoryType() == ActivityType.NEW_YEAR) {
            NewYearData data = profile.getActivityPlayerData().getNewYearData();
            if (data.getPerkMap().containsKey("mysticism")) modifiers += data.getPerkMap().get("mysticism") * 0.1D;
        }
        if (profile.getBuffData().getBuff("mythic_buff").getTier() > 0) {
            modifiers *= 2;
        }
        return Arrays.asList("&e&l天坑乱斗!!!",
                "&d神话装备基础掉落概率&f：&e" + format.format(NewConfiguration.getMythicDropChance(player) * 100) + "%",
                "&d神话之甲(稀有)&e附魔概率&f：&e" + format.format(NewConfiguration.getChance(player, MythicColor.NONE) * 100) + "%",
                "&5暗黑之甲(稀有)&e附魔概率&f：&e" + format.format(NewConfiguration.getChance(player, MythicColor.DARK) * 100) + "%",
                "&4暴怒之甲(稀有)&e附魔概率&f：&e" + format.format(NewConfiguration.getChance(player, MythicColor.RAGE) * 100) + "%",
                "&2下水道之甲(稀有)&e附魔概率&f：&e" + format.format(NewConfiguration.getChance(player, MythicColor.DARK_GREEN) * 100) + "%",
                "&f掉落倍率: " + String.format("%.2f",modifiers * 100) + "%"
        );
    }

    @Override
    public boolean shouldLoop() {
        return true;
    }

    @Override
    public int loopTicks() {
        return 20;
    }

    @Override
    public double getHologramHighInterval() {
        return 0.40;
    }

    @Override
    public Location getLocation() {
        return ThePit.getInstance().getPitConfig().getEnchantTipHoloLocation();
    }
}
