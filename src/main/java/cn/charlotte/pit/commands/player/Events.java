package cn.charlotte.pit.commands.player;


import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.event.previewer.EventPreviewerMenu;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import org.bukkit.entity.Player;

@CommandAlias("event|events")
public class Events extends BaseCommand {
    private final PitCommands pitCommands;

    public Events(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Default
    public void previewEvents(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (!profile.isSupporter() && !PlayerUtil.isStaff(player)) {
            player.sendMessage(CC.translate("&c你需要购买 &e天坑乱斗增值包 &c才可以使用此指令!"));
            return;
        }
        new EventPreviewerMenu().openMenu(player);
    }

}
