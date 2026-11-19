//package cn.charlotte.pit.commands.player;
//
//
//import cn.charlotte.pit.ThePit;
//import cn.charlotte.pit.Util;
//import cn.charlotte.pit.api.PitInternalHook;
//import cn.charlotte.pit.commands.PitCommands;
//import cn.charlotte.pit.data.PlayerProfile;
//import cn.charlotte.pit.item.PitItems;
//import cn.charlotte.pit.perk.PerksPrestigeEnum;
//import cn.charlotte.pit.util.PlayerUtil;
//import cn.charlotte.pit.util.chat.CC;
//import co.aikar.commands.BaseCommand;
//import co.aikar.commands.annotation.CommandAlias;
//import co.aikar.commands.annotation.Default;
//import org.bukkit.Bukkit;
//import org.bukkit.entity.Player;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//
//@CommandAlias("qd")
//public class qd extends BaseCommand {
//    private final PitCommands pitCommands;
//
//    public qd(PitCommands pitCommands) {
//        this.pitCommands = pitCommands;
//    }
//
//    @Default
//    public void onCoolCommand(Player player) {
//        if("rage_pit".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())){
//            player.sendMessage(CC.translate("&c疯狂天坑下不允许使用该指令"));
//            return;
//        }
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        String today = sdf.format(new Date());
//        PlayerProfile profile = Util.getPitProfile(player);
//        if(!profile.isSupporter()){
//            player.sendMessage("&c你没有 &e天坑增值包");
//            return;
//        }
//        if(today.equals(profile.getLastday())){
//            player.sendMessage(CC.translate("&c你已经签到过了"));
//            return;
//        }
//        profile.setLastday(today);
//        player.getInventory().addItem(PitItems.ACTION_GIFT_BOX.toItemStack());
//        player.sendMessage(CC.translate("&a签到成功"));
//    }
//}
