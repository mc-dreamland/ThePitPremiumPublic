package cn.charlotte.pit.perk;

import org.bukkit.entity.Player;

import java.util.List;

public interface MegaStreak {

    int getStreakNeed();

    List<String> getDescriptionInRenownShop(Player player);

}
