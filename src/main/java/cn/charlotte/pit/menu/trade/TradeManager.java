package cn.charlotte.pit.menu.trade;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.TradeData;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.medal.impl.challenge.FirstTradeMedal;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.ChatComponentBuilder;
import cn.charlotte.pit.util.chat.StringUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.MythicOwnershipUtil;
import cn.charlotte.pit.util.item.MythicTradeCooldown;
import cn.charlotte.pit.util.level.LevelUtil;
import cn.charlotte.pit.util.log.ItemOperationJson;
import cn.charlotte.pit.util.rank.RankUtil;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/19 20:57
 */
@Slf4j
public class TradeManager {
    public static final String TRADE_DISABLED_MESSAGE = "当前服务器已关闭交易功能。";
    public static final Map<UUID, TradeManager> trades = new HashMap<>();
    private static final int MIN_EMPTY_SLOTS_TO_START = 2;
    private static final int MIN_EMPTY_SLOTS_AFTER_TRADE = 1;

    private final Player playerA;
    private final Player playerB;
    private final List<ItemStack> aItems;
    private final List<ItemStack> bItems;
    private double aCoins;
    private double bCoins;

    private boolean aConfirm;
    private boolean bConfirm;

    private boolean aEditingCoins;
    private boolean bEditingCoins;

    private Cooldown aPutCooldown;
    private Cooldown bPutCooldown;

    private boolean end;

    public TradeManager(Player playerA, Player playerB) {
        this.playerA = playerA;
        this.playerB = playerB;
        this.aItems = new ArrayList<>();
        this.bItems = new ArrayList<>();
        this.aPutCooldown = new Cooldown(5, TimeUnit.SECONDS);
        this.bPutCooldown = new Cooldown(5, TimeUnit.SECONDS);

        trades.put(playerA.getUniqueId(), this);
        trades.put(playerB.getUniqueId(), this);
    }

    public void openMenu(Player player) {
        if (end) {
            return;
        }
        new TradeMenu(this).openMenu(player);
    }


    public void onPlayerCancel() {
        if (end) {
            return;
        }
        end = true;
        playerA.closeInventory();
        playerB.closeInventory();
        playerA.sendMessage(CC.translate("&c交易被取消."));
        playerB.sendMessage(CC.translate("&c交易被取消."));
        this.cancelTrade();
    }

    public void onPlayerCancel(String msg) {
        if (end) {
            return;
        }
        end = true;
        playerA.closeInventory();
        playerB.closeInventory();
        msg = String.format("&c交易被取消, 原因: %s", msg);
        playerA.sendMessage(CC.translate(msg));
        playerB.sendMessage(CC.translate(msg));
        this.cancelTrade();
    }

    private void doTrade() {
        if (end) {
            return;
        }

        if (!isTradingEnabled()) {
            onPlayerCancel(TRADE_DISABLED_MESSAGE);
            return;
        }

        if (!arePlayersInTradeArea(playerA, playerB)) {
            onPlayerCancel("交易双方必须位于天坑作战区域内");
            return;
        }

        Optional<MythicTradeCooldown.CheckResult> tradeRestriction = MythicTradeCooldown.findRestriction(aItems);
        if (tradeRestriction.isEmpty()) {
            tradeRestriction = MythicTradeCooldown.findRestriction(bItems);
        }
        if (tradeRestriction.isPresent()) {
            onPlayerCancel(tradeRestriction.get().deniedMessage());
            return;
        }
        if (aItems.stream().anyMatch(MythicOwnershipUtil::isFullEquipmentBundle)
                || bItems.stream().anyMatch(MythicOwnershipUtil::isFullEquipmentBundle)) {
            onPlayerCancel("装有装备的收纳箱不能参与交易");
            return;
        }

        long tradeTime = System.currentTimeMillis();
        List<ItemStack> aTransferredItems = MythicOwnershipUtil.transferCopies(aItems, playerA, playerB, tradeTime);
        List<ItemStack> bTransferredItems = MythicOwnershipUtil.transferCopies(bItems, playerB, playerA, tradeTime);

        PlayerInv aInv = new PlayerInv();
        aInv.setContents(aTransferredItems.toArray(new ItemStack[36]));


        PlayerInv bInv = new PlayerInv();
        bInv.setContents(bTransferredItems.toArray(new ItemStack[36]));

        if (!canReceiveItemsWithReserve(this.playerA, bItems.size())) {
            playerA.sendMessage(CC.translate("&c你的背包空间不足, 交易完成后至少需要保留 1 格空位!"));
            playerB.sendMessage(CC.translate("&c对方背包空间不足, 交易已取消!"));
            onPlayerCancel(playerA.getName() + " 的背包空间不足");
            return;
        }
        if (!canReceiveItemsWithReserve(this.playerB, aItems.size())) {
            playerB.sendMessage(CC.translate("&c你的背包空间不足, 交易完成后至少需要保留 1 格空位!"));
            playerA.sendMessage(CC.translate("&c对方背包空间不足, 交易已取消!"));
            onPlayerCancel(playerB.getName() + " 的背包空间不足");
            return;
        }

        TradeData tradeData = new TradeData(this.playerA.getUniqueId().toString(), this.playerB.getUniqueId().toString(), this.playerA.getName(), this.getPlayerB().getName());
        tradeData.setAPaidCoin((long) this.aCoins);
        tradeData.setBPaidCoin((long) this.bCoins);

        tradeData.setAPaidItem(aInv);
        tradeData.setBPaidItem(bInv);

        tradeData.setCompleteTime(tradeTime);

        tradeData.save();

        end = true;

        for (ItemStack item : aTransferredItems) {
            playerB.getInventory().addItem(item);
        }
        for (ItemStack item : bTransferredItems) {
            playerA.getInventory().addItem(item);
        }
        PlayerProfile profileA = PlayerProfile.getPlayerProfileByUuid(playerA.getUniqueId());
        profileA.setCoins(profileA.getCoins() + bCoins);

        PlayerProfile profileB = PlayerProfile.getPlayerProfileByUuid(playerB.getUniqueId());
        profileB.setCoins(profileB.getCoins() + aCoins);

        profileA.getTradeLimit().setAmount(profileA.getTradeLimit().getAmount() + aCoins + bCoins);
        profileB.getTradeLimit().setAmount(profileB.getTradeLimit().getAmount() + aCoins + bCoins);
        profileA.getTradeLimit().setTimes(profileA.getTradeLimit().getTimes() + 1);
        profileB.getTradeLimit().setTimes(profileB.getTradeLimit().getTimes() + 1);

        sendTradeCompleteMsg(profileB, playerB, playerA, bTransferredItems, bCoins, aTransferredItems, aCoins);
        sendTradeCompleteMsg(profileA, playerA, playerB, aTransferredItems, aCoins, bTransferredItems, bCoins);

        FirstTradeMedal medal = new FirstTradeMedal();
        medal.addProgress(profileA, 1);
        medal.addProgress(profileB, 1);

        JsonObject operationLog = ItemOperationJson.operation("TRADE", playerA);
        operationLog.addProperty("targetUuid", playerB.getUniqueId().toString());
        operationLog.addProperty("targetName", playerB.getName());
        operationLog.addProperty("givenCoins", aCoins);
        operationLog.addProperty("receivedCoins", bCoins);
        operationLog.add("givenItems", ItemOperationJson.items(aTransferredItems));
        operationLog.add("receivedItems", ItemOperationJson.items(bTransferredItems));
        log.info("{}", ItemOperationJson.toJson(operationLog));

        aItems.clear();
        bItems.clear();
        aCoins = 0;
        bCoins = 0;


        ThePit.getInstance().getSoundFactory()
                .playSound("successfully", playerA);
        ThePit.getInstance().getSoundFactory()
                .playSound("successfully", playerB);

        trades.remove(playerA.getUniqueId());
        trades.remove(playerB.getUniqueId());

        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
            playerA.closeInventory();
            playerB.closeInventory();
        }, 1L);
    }

    private void sendTradeCompleteMsg(PlayerProfile profileB, Player playerB, Player playerA, List<ItemStack> bItems, double bCoins, List<ItemStack> aItems, double aCoins) {
        String nameB = RankUtil.getPlayerColoredName(playerB.getUniqueId());
        playerA.sendMessage(CC.translate("&6与 " + LevelUtil.getLevelTag(profileB.getPrestige(), profileB.getLevel()) + " " + nameB + " &6交易成功!"));


        if (!bItems.isEmpty()) {
            for (ItemStack item : bItems) {
                String message = "&a&l+ &7" + item.getAmount() + "x &f" + (item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null ? item.getItemMeta().getDisplayName() : item.getType().toString().toUpperCase());
                Component cp = new ChatComponentBuilder(CC.translate(message)).build().hoverEvent(item.asHoverEvent());
                playerA.sendMessage(cp);
            }
        }
        if (bCoins > 0) {
            playerA.sendMessage(CC.translate("&a&l+ &6" + StringUtil.getFormatLong((long) bCoins) + "硬币"));
        }

        if (!aItems.isEmpty()) {
            for (ItemStack item : aItems) {
                String message = "&c&l- &7" + item.getAmount() + "x &f" + (item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null ? item.getItemMeta().getDisplayName() : item.getType().toString().toUpperCase());
                Component cp = new ChatComponentBuilder(CC.translate(message)).build().hoverEvent(item.asHoverEvent());
                playerA.sendMessage(cp);
            }
        }
        if (aCoins > 0) {
            playerA.sendMessage(CC.translate("&c&l- &6" + StringUtil.getFormatLong((long) aCoins) + "硬币"));
        }
    }

    private void cancelTrade() {
        //fixme 无法关闭背包或者取消交易
        for (ItemStack item : aItems) {
            playerA.getInventory().addItem(item);
        }
        for (ItemStack item : bItems) {
            playerB.getInventory().addItem(item);
        }
        PlayerProfile profileA = PlayerProfile.getPlayerProfileByUuid(playerA.getUniqueId());
        profileA.setCoins(profileA.getCoins() + aCoins);

        PlayerProfile profileB = PlayerProfile.getPlayerProfileByUuid(playerB.getUniqueId());
        profileB.setCoins(profileB.getCoins() + bCoins);

        trades.remove(playerA.getUniqueId());
        trades.remove(playerB.getUniqueId());

    }

    public Player getPlayerA() {
        return this.playerA;
    }

    public Player getPlayerB() {
        return this.playerB;
    }

    public List<ItemStack> getAItems() {
        return this.aItems;
    }

    public List<ItemStack> getBItems() {
        return this.bItems;
    }

    public double getACoins() {
        return this.aCoins;
    }

    public void setACoins(double aCoins) {
        this.aCoins = aCoins;
    }

    public double getBCoins() {
        return this.bCoins;
    }

    public void setBCoins(double bCoins) {
        this.bCoins = bCoins;
    }

    public boolean isAConfirm() {
        return this.aConfirm;
    }

    public void setAConfirm(boolean aConfirm) {
        this.aConfirm = aConfirm;
        if (aConfirm && bConfirm) {
            doTrade();
        }
    }

    public boolean isBConfirm() {
        return this.bConfirm;
    }

    public void setBConfirm(boolean bConfirm) {
        this.bConfirm = bConfirm;
        if (aConfirm && bConfirm) {
            doTrade();
        }
    }

    public boolean isAEditingCoins() {
        return this.aEditingCoins;
    }

    public void setAEditingCoins(boolean aEditingCoins) {
        this.aEditingCoins = aEditingCoins;
    }

    public boolean isBEditingCoins() {
        return this.bEditingCoins;
    }

    public void setBEditingCoins(boolean bEditingCoins) {
        this.bEditingCoins = bEditingCoins;
    }

    public Cooldown getAPutCooldown() {
        return this.aPutCooldown;
    }

    public void setAPutCooldown(Cooldown aPutCooldown) {
        this.aPutCooldown = aPutCooldown;
    }

    public Cooldown getBPutCooldown() {
        return this.bPutCooldown;
    }

    public void setBPutCooldown(Cooldown bPutCooldown) {
        this.bPutCooldown = bPutCooldown;
    }

    public boolean isEnd() {
        return this.end;
    }

    public void setEnd(boolean end) {
        this.end = end;
    }

    public static boolean isTradingEnabled() {
        return ThePit.getInstance().getPitConfig().isTradeEnabled();
    }

    public static boolean ensureTradingEnabled(Player player) {
        if (isTradingEnabled()) {
            return true;
        }
        player.sendMessage(CC.translate("&c" + TRADE_DISABLED_MESSAGE));
        return false;
    }

    public static boolean ensureCanEnterTrade(Player player) {
        if (!isTradeAreaAllowed(player)) {
            player.sendMessage(CC.translate("&c只能在天坑作战区域内进行交易!"));
            return false;
        }
        if (InventoryUtil.getInventoryEmptySlots(player.getInventory()) < MIN_EMPTY_SLOTS_TO_START) {
            player.sendMessage(CC.translate("&c发起或同意交易前, 背包至少需要空出 2 格!"));
            return false;
        }
        if (!isOffHandEmpty(player)) {
            player.sendMessage(CC.translate("&c发起或同意交易前, 请先清空副手物品!"));
            return false;
        }
        return true;
    }

    public static boolean ensurePlayersInTradeArea(Player first, Player second) {
        if (arePlayersInTradeArea(first, second)) {
            return true;
        }

        first.sendMessage(CC.translate("&c交易双方必须位于天坑作战区域内!"));
        if (!first.equals(second)) {
            second.sendMessage(CC.translate("&c交易双方必须位于天坑作战区域内!"));
        }
        return false;
    }

    private static boolean arePlayersInTradeArea(Player first, Player second) {
        return isTradeAreaAllowed(first) && isTradeAreaAllowed(second);
    }

    private static boolean isTradeAreaAllowed(Player player) {
        if (player.hasPermission("pit.trade.outside")
                || player.hasPermission("pit.admin")
                || player.hasPermission("thepit.admin")) {
            return true;
        }

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        return profile.isLoaded() && profile.isInArena();
    }

    public static boolean ensureCanReceiveItemsWithReserve(Player player, int incomingItemCount) {
        if (!canReceiveItemsWithReserve(player, incomingItemCount)) {
            player.sendMessage(CC.translate("&c背包空间不足, 交易完成后至少需要保留 1 格空位!"));
            return false;
        }
        return true;
    }

    public boolean ensurePlayerCanCompleteTrade(Player player) {
        if (this.playerA.equals(player)) {
            return ensureCanReceiveItemsWithReserve(player, this.bItems.size());
        }
        if (this.playerB.equals(player)) {
            return ensureCanReceiveItemsWithReserve(player, this.aItems.size());
        }
        return false;
    }

    public static boolean canReceiveItemsWithReserve(Player player, int incomingItemCount) {
        return InventoryUtil.getInventoryEmptySlots(player.getInventory()) >= incomingItemCount + MIN_EMPTY_SLOTS_AFTER_TRADE;
    }

    private static boolean isOffHandEmpty(Player player) {
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        return offHandItem == null || offHandItem.getType() == Material.AIR;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof TradeManager other)) return false;
        if (!other.canEqual(this)) return false;
        final Object this$playerA = this.getPlayerA();
        final Object other$playerA = other.getPlayerA();
        if (!Objects.equals(this$playerA, other$playerA)) return false;
        final Object this$playerB = this.getPlayerB();
        final Object other$playerB = other.getPlayerB();
        if (!Objects.equals(this$playerB, other$playerB)) return false;
        final Object this$aItems = this.getAItems();
        final Object other$aItems = other.getAItems();
        if (!Objects.equals(this$aItems, other$aItems)) return false;
        final Object this$bItems = this.getBItems();
        final Object other$bItems = other.getBItems();
        if (!Objects.equals(this$bItems, other$bItems)) return false;
        if (Double.compare(this.getACoins(), other.getACoins()) != 0) return false;
        if (Double.compare(this.getBCoins(), other.getBCoins()) != 0) return false;
        if (this.isAConfirm() != other.isAConfirm()) return false;
        if (this.isBConfirm() != other.isBConfirm()) return false;
        if (this.isAEditingCoins() != other.isAEditingCoins()) return false;
        if (this.isBEditingCoins() != other.isBEditingCoins()) return false;
        final Object this$aPutCooldown = this.getAPutCooldown();
        final Object other$aPutCooldown = other.getAPutCooldown();
        if (!Objects.equals(this$aPutCooldown, other$aPutCooldown))
            return false;
        final Object this$bPutCooldown = this.getBPutCooldown();
        final Object other$bPutCooldown = other.getBPutCooldown();
        if (!Objects.equals(this$bPutCooldown, other$bPutCooldown))
            return false;
        return this.isEnd() == other.isEnd();
    }

    protected boolean canEqual(final Object other) {
        return other instanceof TradeManager;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $playerA = this.getPlayerA();
        result = result * PRIME + ($playerA == null ? 43 : $playerA.hashCode());
        final Object $playerB = this.getPlayerB();
        result = result * PRIME + ($playerB == null ? 43 : $playerB.hashCode());
        final Object $aItems = this.getAItems();
        result = result * PRIME + ($aItems == null ? 43 : $aItems.hashCode());
        final Object $bItems = this.getBItems();
        result = result * PRIME + ($bItems == null ? 43 : $bItems.hashCode());
        final long $aCoins = Double.doubleToLongBits(this.getACoins());
        result = result * PRIME + (int) ($aCoins >>> 32 ^ $aCoins);
        final long $bCoins = Double.doubleToLongBits(this.getBCoins());
        result = result * PRIME + (int) ($bCoins >>> 32 ^ $bCoins);
        result = result * PRIME + (this.isAConfirm() ? 79 : 97);
        result = result * PRIME + (this.isBConfirm() ? 79 : 97);
        result = result * PRIME + (this.isAEditingCoins() ? 79 : 97);
        result = result * PRIME + (this.isBEditingCoins() ? 79 : 97);
        final Object $aPutCooldown = this.getAPutCooldown();
        result = result * PRIME + ($aPutCooldown == null ? 43 : $aPutCooldown.hashCode());
        final Object $bPutCooldown = this.getBPutCooldown();
        result = result * PRIME + ($bPutCooldown == null ? 43 : $bPutCooldown.hashCode());
        result = result * PRIME + (this.isEnd() ? 79 : 97);
        return result;
    }

    public String toString() {
        return "TradeManager(playerA=" + this.getPlayerA() + ", playerB=" + this.getPlayerB() + ", aItems=" + this.getAItems() + ", bItems=" + this.getBItems() + ", aCoins=" + this.getACoins() + ", bCoins=" + this.getBCoins() + ", aConfirm=" + this.isAConfirm() + ", bConfirm=" + this.isBConfirm() + ", aEditingCoins=" + this.isAEditingCoins() + ", bEditingCoins=" + this.isBEditingCoins() + ", aPutCooldown=" + this.getAPutCooldown() + ", bPutCooldown=" + this.getBPutCooldown() + ", end=" + this.isEnd() + ")";
    }
}
