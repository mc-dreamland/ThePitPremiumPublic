package cn.charlotte.pit.commands.player;


import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@CommandAlias("cool|Cool|COOL|NB|nb")
public class Cool extends BaseCommand {
    private final PitCommands pitCommands;

    public Cool(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Default
    public void onCoolCommand(Player player) {
        if (!PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.COOL_CONCLAVE)) {
            player.sendMessage(CC.translate("&c你当前无法使用该指令!"));
            return;
        }
        List<String> list = new ArrayList<>();
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (PlayerUtil.isVanished(target)) {
                continue;
            }
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(target.getUniqueId());
            if (profile.isNicked() && profile.getNickPrestige() > 0) {
                list.add(CC.translate(profile.getFormattedNameWithRoman()));
            } else if (!profile.isNicked() && profile.getPrestige() > 0) {
                list.add(CC.translate(profile.getFormattedNameWithRoman()));
            }
        }
        if (list.size() > 0) {
            player.sendMessage(CC.translate("&b当前在线精通玩家数: &6" + list.size()));
            list.forEach(player::sendMessage);
        } else {
            player.sendMessage(CC.translate("&c当前没有精通玩家在线!"));
        }
    }

}
