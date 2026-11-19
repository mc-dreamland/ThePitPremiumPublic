package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.events.EventFactory;
import cn.charlotte.pit.util.chat.CC;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.entity.Player;

@CommandAlias("startevent|openevent")
@CommandPermission("pit.admin")
public class StartEvent extends BaseCommand {

    private final PitCommands pitCommands;

    public StartEvent(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Default
    @CommandCompletion("附魔爆发|经验福地|疯狂天坑|红蓝大战|速算|空投|全员通缉|龙蛋|庇护|猎杀|蛋糕争夺战|天坑外卖|尖塔夺魁|旗帜争夺战|竞拍|方块划地战|占山为王|高梯之王|红包大战|幸运轮盘|boss true|false -1代表默认|多少秒后开始大型事件|默认300秒")
    public void setEvent(Player player, String name, boolean force, int seconds) {
        final boolean success = ThePit.getApi().openEvent(player, name, force, seconds);
        if (success) {
            player.sendMessage(CC.translate("&a成功!"));
        } else {
            player.sendMessage(CC.translate("&c失败, 错误的参数"));
        }
    }

    @Subcommand("Cancel")//关闭事件? 我不确定是否有用这是来自旧版插件的cancel指令
    public void CancelEvent() {
        EventFactory factory = ThePit.getInstance().getEventFactory();
        factory.cancelMajorEvent();
        if (factory.getActiveNormalEvent() != null) {
            factory.inactiveEvent(factory.getActiveNormalEvent());
        }
    }
}
