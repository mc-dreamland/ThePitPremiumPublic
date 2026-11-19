//package cn.charlotte.pit.commands.admin;
//
//
//import cn.charlotte.pit.addon.impl.GachaPool;
//import cn.charlotte.pit.commands.PitCommands;
//import cn.charlotte.pit.util.chat.CC;
//import co.aikar.commands.BaseCommand;
//import co.aikar.commands.annotation.CommandAlias;
//import co.aikar.commands.annotation.CommandCompletion;
//import co.aikar.commands.annotation.CommandPermission;
//import co.aikar.commands.annotation.Subcommand;
//import co.aikar.commands.bukkit.contexts.OnlinePlayer;
//import com.mongodb.client.model.Filters;
//import com.mongodb.client.model.ReplaceOptions;
//import org.bukkit.command.CommandSender;
//import org.bukkit.entity.Player;
//
//@CommandAlias("gacha")
//@CommandPermission("pit.admin")
//public class Gacha extends BaseCommand {
//    private final PitCommands pitCommands;
//
//    public Gacha(PitCommands pitCommands) {
//        this.pitCommands = pitCommands;
//    }
//
//    @Subcommand("open")
//    @CommandCompletion("@players")
//    public void onCommand(CommandSender player, OnlinePlayer onlinePlayer) {
//        GachaPool.INSTANCE.gacha(onlinePlayer.getPlayer());
//    }
//
//    @Subcommand("give")
//    @CommandCompletion("@players 数量|1|2|3|4|5")
//    public void giveGacha(CommandSender player, OnlinePlayer onlinePlayer, String amountStr) {
//        Player target = onlinePlayer.getPlayer();
//        int amount = Integer.parseInt(amountStr);
//        if (GachaPool.INSTANCE.getEnable()) {
//            GachaPool.GachaData data = GachaPool.INSTANCE.getKeysCollections().findOne(Filters.eq("playerName", target.getName()));
//            if (data == null) {
//                data = new GachaPool.GachaData();
//                data.playerName = target.getName();
//            }
//
//            data.setKeys(data.getKeys() + amount);
//            GachaPool.INSTANCE.getKeysCollections().replaceOne(Filters.eq("playerName", target.getName()), data, new ReplaceOptions().upsert(true));
//
//            target.sendMessage(CC.translate("&a你现在拥有 &e" + data.getKeys() + " &a个钥匙"));
//        }
//    }
//
//    @Subcommand("preview")
//    public void preview(Player player) {
//        if (GachaPool.INSTANCE.getEnable()) {
//            GachaPool.Preview preview = new GachaPool.Preview();
//            preview.openMenu(player);
//        }
//    }
//
//    @Subcommand("check")
//    @CommandCompletion("@players")
//    public void preview(CommandSender player, String name) {
//        if (GachaPool.INSTANCE.getEnable()) {
//            GachaPool.GachaData data = GachaPool.INSTANCE.getKeysCollections().findOne(Filters.eq("playerName", name));
//            if (data == null) {
//                data = new GachaPool.GachaData();
//                data.playerName = name;
//            }
//
//            player.sendMessage(name + " 有 " + data.getKeys());
//        }
//    }
//
//    @Subcommand("set")
//    @CommandCompletion("@players 数量|1|2|3|4|5")
//    public void preview(CommandSender player, OnlinePlayer onlinePlayer, String amountStr) {
//        Player target = onlinePlayer.getPlayer();
//        int amount = Integer.parseInt(amountStr);
//        if (GachaPool.INSTANCE.getEnable()) {
//            if (target == null) return;
//
//            GachaPool.GachaData data = GachaPool.INSTANCE.getKeysCollections().findOne(Filters.eq("playerName", target.getName()));
//            if (data == null) {
//                data = new GachaPool.GachaData();
//                data.playerName = target.getName();
//            }
//
//            data.setKeys(amount);
//            GachaPool.INSTANCE.getKeysCollections().replaceOne(Filters.eq("playerName", target.getName()), data, new ReplaceOptions().upsert(true));
//
//            target.sendMessage(CC.translate("&a你现在拥有 &e" + data.getKeys() + " &a个钥匙"));
//        }
//    }
//
//}
