package cn.charlotte.pit.lambda;

import cn.charlotte.pit.data.PlayerProfile;
import org.bukkit.entity.Player;

public interface PitProfilePlayerReturnStringLambda {
    String execute(Player player, PlayerProfile profile);
}
