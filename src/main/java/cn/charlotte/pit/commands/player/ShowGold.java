package cn.charlotte.pit.commands.player;


import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.time.TimeUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

@CommandAlias("showgold")
public class ShowGold extends BaseCommand {
    private final PitCommands pitCommands;
    private final DecimalFormat df = new DecimalFormat(",###,###,###,###");
    private final DecimalFormat numFormatTwo = new DecimalFormat("0.00");

    public ShowGold(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Default
    public void show(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (!profile.isSupporter() && !PlayerUtil.isStaff(player)) {
            player.sendMessage(CC.translate("&c你需要购买 &e天坑乱斗增值包 &c才可以使用此指令!"));
            return;
        }
        this.pitCommands.COOLDOWN_SHOW.putIfAbsent(player.getUniqueId(), new Cooldown(0));
        if (!this.pitCommands.COOLDOWN_SHOW.get(player.getUniqueId()).hasExpired() && !player.hasPermission("thepit.admin")) {
            player.sendMessage(CC.translate("此指令仍在冷却中: " + TimeUtil.millisToTimer(this.pitCommands.COOLDOWN_SHOW.get(player.getUniqueId()).getRemaining())));
            return;
        }
        this.pitCommands.COOLDOWN_SHOW.put(player.getUniqueId(), new Cooldown(60, TimeUnit.SECONDS));

        String gold;
        if (profile.getCoins() >= 10000) {
            gold = df.format(profile.getCoins());
        } else {
            gold = numFormatTwo.format(profile.getCoins());
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(CC.translate(CC.translate("&a&l金币展示! &f" + profile.getFormattedName() + " &f展示了金币数量 &6&l" + gold + "&f金币")));
        }
    }
}
