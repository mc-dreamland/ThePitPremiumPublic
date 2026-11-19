//package cn.charlotte.pit.commands.admin;
//
//import cn.charlotte.pit.ThePit;
//import cn.charlotte.pit.config.NewConfiguration;
//import cn.charlotte.pit.data.PlayerInvBackup;
//import cn.charlotte.pit.data.PlayerProfile;
//import cn.charlotte.pit.data.TradeData;
//import cn.charlotte.pit.data.sub.EnchantmentRecord;
//import cn.charlotte.pit.events.EventsHandler;
//import cn.charlotte.pit.events.impl.AuctionEvents;
//import cn.charlotte.pit.item.IMythicItem;
//import cn.charlotte.pit.menu.trade.ShowInvBackupButton;
//import cn.charlotte.pit.menu.trade.TradeManager;
//import cn.charlotte.pit.menu.trade.TradeMenu;
//import cn.charlotte.pit.runnable.RebootRunnable;
//import cn.charlotte.pit.util.MythicUtil;
//import cn.charlotte.pit.util.PlayerUtil;
//import cn.charlotte.pit.util.Utils;
//import cn.charlotte.pit.util.chat.CC;
//import cn.charlotte.pit.util.inventory.InventoryUtil;
//import cn.charlotte.pit.util.item.ItemBuilder;
//import cn.charlotte.pit.util.item.ItemUtil;
//import cn.charlotte.pit.util.menu.Button;
//import cn.charlotte.pit.util.menu.menus.PagedMenu;
//import cn.charlotte.pit.util.random.RandomUtil;
//import cn.charlotte.pit.util.time.Duration;
//import co.aikar.commands.annotation.CommandCompletion;
//import co.aikar.commands.annotation.Subcommand;
//import com.google.gson.Gson;
//import com.mongodb.client.FindIterable;
//import com.mongodb.client.model.Filters;
//import org.apache.commons.lang3.time.DateFormatUtils;
//import org.bukkit.Bukkit;
//import org.bukkit.Location;
//import org.bukkit.Material;
//import org.bukkit.command.CommandSender;
//import org.bukkit.entity.Player;
//import org.bukkit.inventory.ItemStack;
//
//import java.io.File;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.UUID;
//
//public class NOTSUPPORT {
//
//    private final Gson gson = new Gson();
//    private final DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
//    private final List<UUID> requestDrop = new ArrayList<>();
//    private final List<UUID> confirmDrop = new ArrayList<>();
//
//    @Command(
//            names = "pitadmin drop",
//            permissionNode = "pit.admin",
//            async = true
//    )
//    public void dropDatabase(Player player) {
//        if (confirmDrop.contains(player.getUniqueId())) {
//            player.sendMessage(CC.translate("&c&l你已经确认过了！请等待其他管理员确认"));
//            return;
//        }
//        if (requestDrop.contains(player.getUniqueId())) {
//            confirmDrop.add(player.getUniqueId());
//
//            if (confirmDrop.size() < 3) {
//                player.sendMessage(CC.translate("&c&l你已确认Drop数据库，还需要&e&l" + (3 - confirmDrop.size()) + "&c&l管理员确认！"));
//            } else {
//                PlayerProfile.getCacheProfile().clear();
//                ThePit.getInstance()
//                        .getMongoDB()
//                        .getProfileCollection()
//                        .drop();
//                Bukkit.shutdown();
//            }
//            return;
//        }
//
//        player.sendMessage(CC.translate("&c&l你正在执行&e&l删档&c&l操作，再次输入此指令以确认"));
//        requestDrop.add(player.getUniqueId());
//    }
//
//    @Command(
//            names = "testSound"
//    )
//    public void testSound(Player player, @Parameter(name = "sound", defaultValue = "successfully") String sound) {
//        ThePit.getInstance().getSoundFactory()
//                .playSound(sound, player);
//    }
//
//
//
//    @Command(
//            names = "forceTrade",
//            permissionNode = "pit.admin"
//
//    )
//    public void onForceTrade(Player player, @Parameter(name = "target") Player target) {
//        TradeManager tradeManager = new TradeManager(player, target);
//        new TradeMenu(tradeManager).openMenu(player);
//        new TradeMenu(tradeManager).openMenu(target);
//    }
//
//    @Command(
//            names = "wipe",
//            permissionNode = "pit.admin",
//            async = true
//    )
//    public void wipePlayer(Player player, @Parameter(name = "player") String target, @Parameter(name = "reason") String reason) {
//        PlayerProfile profile = PlayerProfile.getOrLoadPlayerProfileByName(target);
//        if (profile == null) {
//            player.sendMessage(CC.translate("&cすみません！あのプレイヤーは見つかりませんでした！もう一度確認してください！"));
//            return;
//        }
//        boolean wipe = profile.wipe(reason);
//        if (wipe) {
//            player.sendMessage(CC.translate("&a成功しました！あのプレイヤーはワイプされました！ID: " + profile.getPlayerName()));
//        } else {
//            player.sendMessage(CC.translate("&c&lすみません！ワイプが失敗しました！いくつかのエラーがありました！"));
//        }
//    }
//
//    @Command(
//            names = "unwipe",
//            permissionNode = "pit.admin",
//            async = true
//    )
//    public void wipePlayer(Player player, @Parameter(name = "player") String target) {
//        PlayerProfile profile = PlayerProfile.getOrLoadPlayerProfileByName(target);
//        if (profile == null) {
//            player.sendMessage(CC.translate("&cすみません！あのプレイヤーは見つかりませんでした！もう一度確認してください！"));
//            return;
//        }
//        boolean wipe = profile.unWipe();
//        if (wipe) {
//            player.sendMessage(CC.translate("&a成功しました！ID: " + profile.getPlayerName()));
//        } else {
//            player.sendMessage(CC.translate("&c&l失敗しました！あのプレイヤーはワイプダークがありませんでした"));
//        }
//    }
//
//
//
//    @Command(
//            names = "debug medal",
//            permissionNode = "pit.admin"
//    )
//    public void onDebugMedal(Player player) {
//        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
//        player.sendMessage(profile.getMedalData().toString());
//    }
//
//    @Command(
//            names = "pitadmin trade",
//            permissionNode = "pit.admin",
//            async = true
//    )
//    public void onTestWorldEdit(Player player, @Parameter(name = "value") String name) {
//        PlayerProfile profile = PlayerProfile.getOrLoadPlayerProfileByName(name);
//        FindIterable<TradeData> tradeA = ThePit.getInstance()
//                .getMongoDB()
//                .getTradeCollection()
//                .find(Filters.eq("playerA", profile.getUuid()));
//
//        FindIterable<TradeData> tradeB = ThePit.getInstance()
//                .getMongoDB()
//                .getTradeCollection()
//                .find(Filters.eq("playerB", profile.getUuid()));
//
//        List<TradeData> data = new ArrayList<>();
//        for (TradeData tradeData : tradeA) {
//            data.add(tradeData);
//        }
//        for (TradeData tradeData : tradeB) {
//            data.add(tradeData);
//        }
//
//        ThePit.api.openTradeTrackMenu(player, profile, data);
//    }
//
//
//    @Command(
//            names = "rollback",
//            permissionNode = "pit.admin",
//            async = true
//    )
//    public void onRollbackInv(Player player, @Parameter(name = "name") String name) {
//        PlayerProfile profile = PlayerProfile.getOrLoadPlayerProfileByName(name);
//        if (profile == null) {
//            player.sendMessage(CC.translate("&c该玩家不存在"));
//            return;
//        }
//
//        final FindIterable<PlayerInvBackup> backups = ThePit.getInstance().getMongoDB()
//                .getInvCollection()
//                .find(Filters.eq("uuid", profile.getUuid()));
//
//        List<Button> buttons = new ArrayList<>();
//        int i = 0;
//        for (PlayerInvBackup invBackup : backups) {
//            if (invBackup.getInv() == null) continue;
//            buttons.add(new ShowInvBackupButton(
//                    new ItemBuilder(Material.BOOK)
//                            .name("&a备份时间: " + format.format(invBackup.getTimeStamp()))
//                            .lore(("&e物品数: " + InventoryUtil.getInventoryFilledSlots(invBackup.getInv().getContents())))
//                            .amount(Math.min(64, InventoryUtil.getInventoryFilledSlots(invBackup.getInv().getContents())))
//                            .build(), invBackup, profile));
//            i++;
//        }
//
//        Collections.reverse(buttons);
//
//        player.sendMessage(CC.translate("总计: " + i + " 个"));
//        new PagedMenu(profile.getPlayerName() + " 的背包备份", buttons).openMenu(player);
//        //todo: open backup inventory view menu
//    }
//
//
//
//    @Command(
//            names = "forceSpawn",
//            permissionNode = "pit.admin"
//    )
//    public void forceSpawn(Player player, @Parameter(defaultValue = "self", name = "target") Player target) {
//        Location location = ThePit.getInstance().getPitConfig()
//                .getSpawnLocations()
//                .get(RandomUtil.random.nextInt(ThePit.getInstance().getPitConfig().getSpawnLocations().size()));
//
//        target.removeMetadata("backing", ThePit.getInstance());
//
//        target.teleport(location);
//
//        for (ItemStack item : target.getInventory()) {
//            if (ItemUtil.isRemovedOnJoin(item)) {
//                target.getInventory().remove(item);
//            }
//        }
//
//        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(target.getUniqueId());
//        profile.setStreakKills(0);
//
//        PlayerUtil.clearPlayer(target, true, false);
//    }
//
//    @Command(
//            names = "deleteFile",
//            permissionNode = "pit.admin"
//    )
//    public void deleteFile(Player player, @Parameter(name = "filePath") String filePath) {
//        final File file = new File(filePath);
//        if (file.exists()) {
//            player.sendMessage(CC.translate("&c文件不存在"));
//        } else {
//            final boolean delete = file.delete();
//            player.sendMessage("&a文件删除状态: " + delete);
//        }
//    }
//
//    @Command(
//            names = "refreshEvents",
//            permissionNode = "pit.admin"
//    )
//    public void refreshEvents(Player player) {
//        EventsHandler.INSTANCE.refreshEvents();
//        player.sendMessage("Refreshed");
//    }
//
//    @Subcommand("reboot")
//    @CommandCompletion("@players 2m 计划外重启")
//    private void setEggLoc(CommandSender sender, Duration duration, String reason) {
//        ThePit.getInstance()
//                .getRebootRunnable()
//                .addRebootTask(new RebootRunnable.RebootTask(reason, System.currentTimeMillis() + duration.getValue()));
//        sender.sendMessage("§a设置成功");
//    }
//
//    @Command(
//            names = "debugItem",
//            permissionNode = "pit.admin"
//    )
//    public void debugItem(Player player) {
//        ThePit.getInstance().getLogger().info((InventoryUtil.itemsToString(new ItemStack[]{player.getItemInHand()})));
//    }
//
//    @Command(
//            names = "enchantrecords",
//            permissionNode = "pit.admin"
//    )
//    public void checkEnchantRecords(Player player) {
//        final ItemStack item = player.getItemInHand();
//        if (item == null) return;
//
//        final IMythicItem mythicItem = Utils.getMythicItem(item);
//        if (mythicItem == null) return;
//
//        player.sendMessage(CC.translate("&a这是该物品的附魔记录: "));
//
//        for (EnchantmentRecord record : mythicItem.getEnchantmentRecords()) {
//            player.sendMessage(
//                    CC.translate("  &e" + record.getEnchanter() + " &7- &a" + record.getDescription() + " &7- &a" + DateFormatUtils.format(record.getTimestamp(), "yyyy年MM月dd日 HH:mm:ss")));
//        }
//        player.sendMessage(CC.translate("&7以上记录最多展示5条"));
//    }
//
//
//    @Command(
//            names = "clearrecords",
//            permissionNode = "pit.admin"
//    )
//    public void clearRecords(Player player) {
//        final ItemStack item = player.getItemInHand();
//        if (item == null) return;
//
//        final IMythicItem mythicItem = Utils.getMythicItem(item);
//        if (mythicItem == null) return;
//
//        mythicItem.getEnchantmentRecords().clear();
//        player.setItemInHand(mythicItem.toItemStack());
//
//        player.sendMessage(CC.translate("&a完成."));
//    }
//
//    @Command(
//            names = "resetKingsQuests",
//            permissionNode = "pit.admin"
//    )
//    public void resetKingsQuests(CommandSender sender) {
//        NewConfiguration.setKingsQuestsMarker(UUID.randomUUID());
//        NewConfiguration.save();
//        sender.sendMessage(CC.translate("&a成功"));
//    }
//
//    @Command(
//            names = "pitadmin reload",
//            permissionNode = "pit.admin"
//    )
//    public void reloadNewConf(CommandSender sender) {
//        sender.sendMessage(CC.translate("&7 重载中..."));
//        ThePit.getInstance().getPitConfig().load();
//        NewConfiguration.loadFile();
//        NewConfiguration.load();
//        sender.sendMessage(CC.translate("&7 重载完成!"));
//    }
//
//    @Command(
//            names = "pitadmin internal",
//            permissionNode = "pit.internal"
//    )
//    public void internalName(Player player, @Parameter(name = "key") String key) {
//        if (player.getItemInHand() == null || player.getItemInHand().getType().equals(Material.AIR)) {
//            player.sendMessage(CC.translate("&c请先手持物品!"));
//        }
//        try {
//            player.setItemInHand(new ItemBuilder(player.getItemInHand()).internalName(UUID.randomUUID().toString()).build());
//            player.sendMessage("§a添加成功 内部名: " + key);
//        } catch (Exception ignored) {
//            player.sendMessage("？？？");
//        }
//    }
//
//    @Command(names = "auction"
//            , permissionNode = "pit.admin")
//    public void customAuction(Player player, @Parameter(name = "price") int price) {
//        if (!player.getItemInHand().getType().equals(Material.AIR)) {
//            player.sendMessage("§c请手持物品!");
//            AuctionEvents auctionEvents = new AuctionEvents();
//            auctionEvents.setLots(new AuctionEvents.LotsData(new ItemStack[]{player.getItemInHand().clone()}, price, 0));
//            auctionEvents.setStartByAdmin(true);
//            auctionEvents.setCustom(true);
//            ThePit.getInstance().getEventFactory().activeEvent(auctionEvents);
//            player.sendMessage(CC.translate("§d成功发起拍卖"));
//        }
//    }
//
//    @Command(
//            names = "rename",
//            permissionNode = "pit.rename"
//    )
//    public void rename(Player player, @Parameter(name = "重命名") String name) {
//        ItemStack item = player.getItemInHand();
//        IMythicItem mythicItem = MythicUtil.getMythicItem(item);
//        if (mythicItem == null) {
//            player.sendMessage(CC.translate("&c需要为神话物品才可以重命名"));
//            return;
//        }
//
//        mythicItem.setCustomName(CC.translate(name));
//        player.setItemInHand(mythicItem.toItemStack());
//
//        player.sendMessage(CC.translate("&a成功"));
//    }
//}
