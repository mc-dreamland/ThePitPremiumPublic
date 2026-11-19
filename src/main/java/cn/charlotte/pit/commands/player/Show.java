package cn.charlotte.pit.commands.player;


import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.ChatComponentBuilder;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.time.TimeUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@CommandAlias("show|hand")
public class Show extends BaseCommand {
    private final PitCommands pitCommands;

    public Show(PitCommands pitCommands) {
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
        if (player.getItemInHand() == null || player.getItemInHand().getType().equals(Material.AIR)) {
            player.sendMessage(CC.translate("&c请先手持要展示的物品!"));
            return;
        }
        if ((player.getItemInHand().getItemMeta().getDisplayName() == null && !player.hasPermission("pit.admin")) || Optional.of("CustomNamePaper".equals(ItemUtil.getInternalName(player.getInventory().getItemInMainHand()))).orElse(false)) {
            player.sendMessage(CC.translate("&c此物品无法被用于展示!"));
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(new ChatComponentBuilder(CC.translate(
                    "&a&l物品展示! &7" + profile.getFormattedName() + " &7正在展示物品: &f" +
                            (player.getItemInHand().getItemMeta().getDisplayName() == null ?
                                    player.getItemInHand().getType().name() : player.getItemInHand().getItemMeta().getDisplayName()
                            ) + (player.getInventory().getItemInMainHand().getAmount() > 1 ? " &f* " + player.getInventory().getItemInMainHand().getAmount() : "") + " &e[查看]"))
                    .setCurrentHoverEvent(player.getItemInHand().asHoverEvent()).build());
            if (CombatListener.isBedrockRoom()) {
                CC.send(MessageType.MISC, p, player.getItemInHand().getItemMeta().getDisplayName() == null ?
                        player.getItemInHand().getType().name() : player.getItemInHand().getItemMeta().getDisplayName());
                CC.send(MessageType.MISC, p, player.getInventory().getItemInMainHand().getLore());
            }
        }
    }
}
