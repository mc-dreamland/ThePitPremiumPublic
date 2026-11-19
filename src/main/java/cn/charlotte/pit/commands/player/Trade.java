package cn.charlotte.pit.commands.player;


import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.OfferData;
import cn.charlotte.pit.data.temp.TradeRequest;
import cn.charlotte.pit.events.EventFactory;
import cn.charlotte.pit.game.runnable.TradeMonitorRunnable;
import cn.charlotte.pit.menu.offer.OfferMenu;
import cn.charlotte.pit.menu.trade.TradeManager;
import cn.charlotte.pit.menu.trade.TradeMenu;
import cn.charlotte.pit.menu.trade.npc.tradeMenuOfNpc;
import cn.charlotte.pit.npc.type.TradeNPC;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.ChatComponentBuilder;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.level.LevelUtil;
import cn.charlotte.pit.util.rank.RankUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Conditions;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Calendar;

@CommandAlias("trade")
public class Trade extends BaseCommand {
    public Trade(PaperCommandManager manager) {
        manager.getCommandConditions().addCondition("tradeEnabled", context -> {
            if (TradeManager.isTradingEnabled()) {
                return;
            }
            if (context.hasConfig("returnOffer") && context.getIssuer().getIssuer() instanceof Player player) {
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                if (profile.isLoaded() && profile.getOfferData().getBuyer() != null) {
                    returnOffer(player, profile);
                }
            }
            throw new ConditionFailedException(TradeManager.TRADE_DISABLED_MESSAGE);
        });
    }

    // 获取玩家的交易限额配置
    private int[] getTradeLimits(PlayerProfile profile) {
        if (profile.isSupporter()) {
            return new int[]{50, 200000}; // [交易次数, 交易金额]
        }
        return new int[]{25, 50000};
    }

    // 检查并执行每日重置
    private void checkDailyReset(PlayerProfile profile) {
        long now = System.currentTimeMillis();
        String nowDay = PitCommands.dateFormat.format(now);
        String lastRefreshDay = PitCommands.dateFormat.format(profile.getTradeLimit().getLastRefresh());

        if (!lastRefreshDay.equals(nowDay) && Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 4) {
            profile.getTradeLimit().setLastRefresh(now);
            profile.getTradeLimit().setAmount(0);
            profile.getTradeLimit().setTimes(0);
        }
    }

    @Subcommand("limit|limits")
    @CommandAlias("limit")
    public void sendTradeLimits(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        checkDailyReset(profile);

        int[] limits = getTradeLimits(profile);
        int maxTimes = limits[0];
        int maxAmount = limits[1];

        player.sendMessage(CC.translate("&6&l每日交易限制! &7(每日4:00 AM重置)"));
        player.sendMessage(CC.translate("&7每日硬币交易上限: " +
                (profile.getTradeLimit().getAmount() >= maxAmount ? "&c" : "&a") +
                profile.getTradeLimit().getAmount() + "&7/&6" + maxAmount + "g"));
        player.sendMessage(CC.translate("&7每日交易次数上限: &e" +
                profile.getTradeLimit().getTimes() + "/" + maxTimes));
    }

    @Subcommand("viewOffer|viewoffer")
    @CommandAlias("@players")
    @Conditions("tradeEnabled")
    public void viewOffer(Player player, OnlinePlayer target) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        int[] limits = getTradeLimits(profile);
        int maxTimes = limits[0];
        int maxAmount = limits[1];

        PlayerProfile targetProfile = PlayerProfile.getOrLoadPlayerProfileByUuid(target.player.getUniqueId());
        if (!TradeManager.ensurePlayersInTradeArea(player, target.player)) {
            return;
        }
        if (targetProfile.getOfferData().getBuyer() == null || !targetProfile.getOfferData().getBuyer().equals(player.getUniqueId())) {
            player.sendMessage(CC.translate("&c你没有来自此玩家的交易报价!"));
            return;
        }

        if (targetProfile.getOfferData().hasUnclaimedOffer()) {
            player.sendMessage(CC.translate("&c此交易报价已过期!"));
            return;
        }

        if (!profile.getCombatTimer().hasExpired() || PlayerUtil.isSupperStreak(profile,player)) {
            player.sendMessage(CC.translate("&c你无法在战斗中使用此功能!"));
            return;
        }

        checkDailyReset(profile);

        if (profile.getTradeLimit().getTimes() >= maxTimes) {
            player.sendMessage(CC.translate("&c你今天的交易次数已经达到上限! (" + maxTimes + "/" + maxTimes + ")"));
            player.sendMessage(CC.translate("&c使用 &e/tradeLimits &c查看你的今日交易上限情况."));
            return;
        }

        if (profile.getTradeLimit().getAmount() + targetProfile.getOfferData().getPrice() > maxAmount) {
            player.sendMessage(CC.translate("&c对方的开价加上今日已交易量已超过交易上限 (" + maxAmount + "g)!"));
            player.sendMessage(CC.translate("&c使用 &e/tradeLimits &c查看你的今日交易上限情况."));
            return;
        }

        if (!player.getName().equalsIgnoreCase(player.getName())) {
            player.sendMessage(CC.translate("&c你无法在匿名模式下使用交易功能!"));
            return;
        }

        if (profile.getLevel() < 60) {
            player.sendMessage(CC.translate("&c&l等级不足! &7此指令在 " + LevelUtil.getLevelTag(profile.getPrestige(), 60) + " &7时解锁."));
            return;
        }

        if (!TradeManager.ensureCanEnterTrade(player)) {
            return;
        }

        new OfferMenu(target.player).openMenu(player);
    }

    @Subcommand("offer")
    @CommandCompletion("* [价格]|500|1000")
    @Conditions("tradeEnabled:returnOffer")
    public void offerItem(Player player, Player target, String price) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        int[] limits = getTradeLimits(profile);
        int maxTimes = limits[0];
        int maxAmount = limits[1];

        if (profile.getOfferData().hasActiveOffer()) {
            player.sendMessage(CC.translate("&c你当前有一个正在进行中的交易报价!"));
            return;
        } else if (profile.getOfferData().hasUnclaimedOffer()) {
            returnOffer(player, profile);
            return;
        }

        try {
            int offerPrice = Integer.parseInt(price);
            if (offerPrice <= 0) {
                throw new NumberFormatException("价格必须为正数");
            }
        } catch (Exception e) {
            player.sendMessage(CC.translate("&c你输入的价格有误! 请输入正整数。"));
            return;
        }

        if (target == null || !target.isOnline() || PlayerUtil.isVanished(target)) {
            player.sendMessage(CC.translate("&c你选择的玩家不在线!"));
            return;
        }

        PlayerProfile targetProfile = PlayerProfile.getOrLoadPlayerProfileByUuid(target.getUniqueId());

        if (!TradeManager.ensurePlayersInTradeArea(player, target)) {
            return;
        }

        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(CC.translate("&c你无法选择此玩家进行交易!"));
            return;
        }

        if (player.getWalkSpeed() == 0f) {
            player.sendMessage(CC.translate("&c你无法选择此玩家进行交易!"));
            return;
        }
        if (!profile.getCombatTimer().hasExpired() || PlayerUtil.isSupperStreak(profile,player)) {
            player.sendMessage(CC.translate("&c你无法在战斗中使用此功能!"));
            return;
        }

        checkDailyReset(profile);

        if (profile.getTradeLimit().getTimes() >= maxTimes) {
            player.sendMessage(CC.translate("&c你今天的交易次数已经达到上限! (" + maxTimes + "/" + maxTimes + ")"));
            player.sendMessage(CC.translate("&c使用 &e/tradeLimits &c查看你的今日交易上限情况."));
            return;
        }

        int offerPrice = Integer.parseInt(price);
        if (profile.getTradeLimit().getAmount() + offerPrice > maxAmount) {
            player.sendMessage(CC.translate("&c你的开价加上今日已交易量已超过交易上限 (" + maxAmount + "g)!"));
            player.sendMessage(CC.translate("&c使用 &e/tradeLimits &c查看你的今日交易上限情况."));
            return;
        }

        if (!player.getName().equalsIgnoreCase(player.getDisplayName())) {
            player.sendMessage(CC.translate("&c你无法在匿名模式下使用交易功能!"));
            return;
        }

        if (profile.getLevel() < 60) {
            player.sendMessage(CC.translate("&c&l等级不足! &7此指令在 " + LevelUtil.getLevelTag(profile.getPrestige(), 60) + " &7时解锁."));
            return;
        }

        if (!TradeManager.ensureCanEnterTrade(player)) {
            return;
        }

        if (player.getItemInHand() == null || player.getItemInHand().getType() == Material.AIR) {
            player.sendMessage(CC.translate("&c请手持你要出售的物品再设置出售对象与价格!"));
            return;
        }

        if (!ItemUtil.canTrade(player.getItemInHand())) {
            player.sendMessage(CC.translate("&c此物品无法用于交易!"));
            return;
        }

        // 创建交易报价
        profile.getOfferData().createOffer(target.getUniqueId(), player.getItemInHand(), offerPrice);
        player.setItemInHand(new ItemStack(Material.AIR));

        if (!targetProfile.getPlayerOption().isTradeNotify() && !player.hasPermission(PlayerUtil.getStaffPermission())) {
            player.sendMessage(CC.translate("&c对方在游戏选项中设置了不接受交易请求,因此无法查看你的请求提示."));
            player.sendMessage(CC.translate("&c但对方仍可以通过使用 &e/viewOffer " + player.getName() + " &c以同意你的请求."));
        } else {
            player.sendMessage(CC.translate("&e&l交易报价发送! &7成功向 " + LevelUtil.getLevelTag(targetProfile.getPrestige(), targetProfile.getLevel()) + " " + RankUtil.getPlayerColoredName(target.getUniqueId()) + " &7发送了交易报价!"));
            target.sendMessage(
                    new ChatComponentBuilder(
                            CC.translate("&e&l交易报价! " + LevelUtil.getLevelTag(profile.getPrestige(), profile.getLevel()) + " " + RankUtil.getPlayerColoredName(player.getUniqueId()) + " &7向你发送了交易报价,请")
                    ).append(
                                    new ChatComponentBuilder(CC.translate(" &e点击这里"))
                                            .setCurrentHoverEvent(HoverEvent.showText(new ChatComponentBuilder(CC.translate("&6点击以接受")).build()))
                                            .setCurrentClickEvent(ClickEvent.runCommand("/trade viewoffer " + player.getName())).build())
                            .append(CC.translate(" &r&7以查看此交易报价."))
                            .build());
        }
    }

    private static void returnOffer(Player player, PlayerProfile profile) {
        if (InventoryUtil.isInvFull(player.getInventory())) {
            player.sendMessage(CC.translate("&c你有一个未结算的交易报价,请将背包腾出空间后重试!"));
            return;
        }
        InventoryUtil.addInvReverse(player.getInventory(), profile.getOfferData().getItemStack());
        profile.setOfferData(new OfferData());
        player.sendMessage(CC.translate("&e未结算的交易报价物品已退还到你的背包。"));
    }

    @Subcommand("gui|AuctionGui|auction")
    public void openAuctionGui(Player player) {
        EventFactory eventFactory = ThePit.getInstance().getEventFactory();
        if (eventFactory.getActiveNormalEvent() == null || !"auction".equals(eventFactory.getActiveNormalEventName())) {
            player.sendMessage(CC.translate("&c此指令当前无法使用!"));
            return;
        }
        ThePit.getApi().openAuctionMenu(player);
    }

    @CatchUnknown
    @CommandCompletion("@players")
    @Conditions("tradeEnabled")
    public void onRequestTrade(Player player, OnlinePlayer target) {
        Player targetPlayer = target.player;
        if (player.getUniqueId().equals(targetPlayer.getUniqueId()) || Bukkit.getPlayer(targetPlayer.getUniqueId()) == null) {
            player.sendMessage(CC.translate("&c你无法选择此玩家进行交易!"));
            return;
        }

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        int[] limits = getTradeLimits(profile);
        int maxTimes = limits[0];
        int maxAmount = limits[1];
        if (player.getWalkSpeed() == 0f) {
            player.sendMessage(CC.translate("&c你无法选择此玩家进行交易!"));
            return;
        }
        PlayerProfile targetProfile = PlayerProfile.getPlayerProfileByUuid(targetPlayer.getUniqueId());
        if (!TradeManager.ensurePlayersInTradeArea(player, targetPlayer)) {
            return;
        }
        if (!profile.getCombatTimer().hasExpired() || PlayerUtil.isSupperStreak(profile,player)) {
            player.sendMessage(CC.translate("&c你无法在战斗中使用此功能!"));
            return;
        }

        checkDailyReset(profile);

        if (profile.getTradeLimit().getTimes() >= maxTimes) {
            player.sendMessage(CC.translate("&c你今天的交易次数已经达到上限! (" + profile.getTradeLimit().getTimes() + "/" + maxTimes + ")"));
            player.sendMessage(CC.translate("&c使用 &e/tradeLimits &c查看你的今日交易上限情况."));
            return;
        }

        if (!player.getName().equalsIgnoreCase(player.getDisplayName())) {
            player.sendMessage(CC.translate("&c你无法在匿名模式下使用交易功能!"));
            return;
        }

        if (profile.getLevel() < 60) {
            player.sendMessage(CC.translate("&c&l等级不足! &7此指令在 " + LevelUtil.getLevelTag(profile.getPrestige(), 60) + " &7时解锁."));
            return;
        }

        if (!TradeManager.ensureCanEnterTrade(player)) {
            return;
        }

        TradeRequest incomingRequest = TradeMonitorRunnable.getRequest(targetPlayer.getUniqueId(), player.getUniqueId());
        if (incomingRequest != null) {
            if (validateTradeConditions(player)) {
                TradeMonitorRunnable.removeRequest(targetPlayer.getUniqueId(), player.getUniqueId());
                return;
            }
            if (validateTradeConditions(target.getPlayer())) {
                player.sendMessage(CC.translate("&c对方不再满足交易条件，交易请求已取消!"));
                TradeMonitorRunnable.removeRequest(targetPlayer.getUniqueId(), player.getUniqueId());
                return;
            }

            TradeManager tradeManager = new TradeManager(player, targetPlayer);
            new TradeMenu(tradeManager).openMenu(player);
            new TradeMenu(tradeManager).openMenu(targetPlayer);
            TradeMonitorRunnable.removeRequest(targetPlayer.getUniqueId(), player.getUniqueId());
            return;
        }

        if (TradeMonitorRunnable.getRequest(player.getUniqueId(), targetPlayer.getUniqueId()) != null) {
            player.sendMessage(CC.translate("&c你已经发送过请求了,请等待对方接受!"));
            return;
        }

        TradeMonitorRunnable.addRequest(new TradeRequest(player.getUniqueId(), targetPlayer.getUniqueId()));

        if (!targetProfile.getPlayerOption().isTradeNotify() && !player.hasPermission(PlayerUtil.getStaffPermission())) {
            player.sendMessage(CC.translate("&c对方在游戏选项中设置了不接受交易请求,因此无法查看你的请求提示."));
            player.sendMessage(CC.translate("&c但对方仍可以通过使用 &e/trade " + player.getName() + " &c以同意你的请求."));
            return;
        } else {
            player.sendMessage(CC.translate("&a&l交易请求发送! &7成功向 " + LevelUtil.getLevelTag(targetProfile.getPrestige(), targetProfile.getLevel()) + " " + RankUtil.getPlayerColoredName(targetPlayer.getUniqueId()) + " &7发送了交易请求!"));
        }

        targetPlayer.sendMessage(
                new ChatComponentBuilder(
                        CC.translate("&6&l交易请求! " + LevelUtil.getLevelTag(profile.getPrestige(), profile.getLevel()) + " " + RankUtil.getPlayerColoredName(player.getUniqueId()) + " &7向你发送了交易请求,请")
                ).append(
                                new ChatComponentBuilder(CC.translate(" &6&o点击这里"))
                                        .setCurrentHoverEvent(HoverEvent.showText(new ChatComponentBuilder(CC.translate("&6点击以接受")).build()))
                                        .setCurrentClickEvent(ClickEvent.runCommand("/trade " + player.getName())).build())
                        .append(CC.translate(" &r&7以接受交易请求."))
                        .build());
    }

    private boolean validateTradeConditions(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        if (!profile.getCombatTimer().hasExpired() || PlayerUtil.isSupperStreak(profile,player)) {
            player.sendMessage(CC.translate("&c你无法在战斗中使用此功能!"));
            return true;
        }
        if (profile.getLevel() < 60) {
            player.sendMessage(CC.translate("&c&l等级不足! &7此指令在 " + LevelUtil.getLevelTag(profile.getPrestige(), 60) + " &7时解锁."));
            return true;
        }
        if (!player.getName().equalsIgnoreCase(player.getDisplayName())) {
            player.sendMessage(CC.translate("&c你无法在匿名模式下使用交易功能!"));
            return true;
        }
        checkDailyReset(profile);
        int[] limits = getTradeLimits(profile);
        if (profile.getTradeLimit().getTimes() >= limits[0]) {
            player.sendMessage(CC.translate("&c你今天的交易次数已经达到上限! (" + profile.getTradeLimit().getTimes() + "/" + limits[0] + ")"));
            player.sendMessage(CC.translate("&c使用 &e/tradeLimits &c查看你的今日交易上限情况."));
            return true;
        }
        if (!TradeManager.ensureCanEnterTrade(player)) {
            return true;
        }

        return false;
    }
    @Subcommand("tradeGui")
    @Conditions("tradeEnabled")
    public void openTradeGui(Player player) {
        PlayerProfile profile = Util.getPitProfile(player);
        if (!TradeManager.ensureCanEnterTrade(player)) {
            return;
        }
        if (!profile.getCombatTimer().hasExpired() || PlayerUtil.isSupperStreak(profile,player)) {
            player.sendMessage(CC.translate("&c你无法在战斗中使用此功能!"));
            return;
        }
        if (profile.getLevel() < 60) {
            player.sendMessage(CC.translate(String.format("&c&l等级不足! &7此指令在 %s &7时解锁.", LevelUtil.getLevelTag(profile.getPrestige(), 60))));
            return;
        }
        new tradeMenuOfNpc(TradeNPC.getPendingRequests(player)).openMenu(player);
    }
}
