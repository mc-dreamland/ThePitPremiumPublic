package cn.charlotte.pit.menu.trade.button;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.trade.TradeManager;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.exception.SignGUIVersionException;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class CoinButton extends Button {
    private final TradeManager tradeManager;

    public CoinButton(TradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile.isSupporter()) {
            return new ItemBuilder(Material.GOLD_INGOT).name("&6硬币交易").lore(" ", "&7每日交易限制: " + (profile.getTradeLimit().getAmount() >= 500000 ? "&c" : "&a") + profile.getTradeLimit().getAmount() + "g&7/&6500000g &7(" + profile.getTradeLimit().getTimes() + "/50次)", " ", "&e点击设置转账金额!").build();
        } else {
            return new ItemBuilder(Material.GOLD_INGOT).name("&6硬币交易").lore(" ", "&7每日交易限制: " + (profile.getTradeLimit().getAmount() >= 50000 ? "&c" : "&a") + profile.getTradeLimit().getAmount() + "g&7/&650000g &7(" + profile.getTradeLimit().getTimes() + "/25次)", " ", "&e点击设置转账金额!").build();
        }
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        if (tradeManager.getPlayerA().equals(player)) {
            tradeManager.setAEditingCoins(true);
        } else {
            tradeManager.setBEditingCoins(true);
        }

        try {
            SignGUI signGUI = SignGUI.builder()
                    .setType(Material.BAMBOO_SIGN)
                    .setColor(DyeColor.CYAN)
                    .setLines("", "~~~~~~~~~~~~~~", "在上方", "输入交易金额")
                    .setHandler((player1, result) -> {
                        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                            player1.closeInventory();

                            if (tradeManager.getPlayerA().equals(player)) {
                                tradeManager.setAEditingCoins(false);
                            } else {
                                tradeManager.setBEditingCoins(false);
                            }

                            if (TradeManager.trades.get(player.getUniqueId()) == null) {
                                player.sendMessage(CC.translate("&c当前交易已被取消/结算!"));
                                return;
                            }


                            String[] lines = result.getLines();
                            if (lines.length == 0 || lines[0].isEmpty()) {
                                tradeManager.openMenu(player1);
                                return;
                            }

                            long coins;
                            try {
                                coins = Long.parseLong(lines[0].replaceAll("\"", ""));
                            } catch (Exception ignore) {
                                player.sendMessage(CC.translate("&c请输入一个整数金额!"));
                                tradeManager.openMenu(player1);
                                return;
                            }

                            if (coins < 0) {
                                player.sendMessage(CC.translate("&c请输入一个正数金额!"));
                                tradeManager.openMenu(player1);
                                return;
                            }

                            PlayerProfile profileA = PlayerProfile.getPlayerProfileByUuid(tradeManager.getPlayerA().getUniqueId());
                            PlayerProfile profileB = PlayerProfile.getPlayerProfileByUuid(tradeManager.getPlayerB().getUniqueId());
                            long aLimit = profileA.isSupporter() ? 500000 : 50000;
                            long bLimit = profileB.isSupporter() ? 500000 : 50000;

                            if (tradeManager.getPlayerA().equals(player)) {
                                if (tradeManager.getAItems().size() >= 8) {
                                    player.sendMessage(CC.translate("&c你无法再放入更多物品了!"));
                                    tradeManager.openMenu(player1);
                                    return;
                                }
                                if (coins > profileA.getCoins()) {
                                    player.sendMessage(CC.translate("&c你的硬币不足!"));
                                    tradeManager.openMenu(player1);
                                    return;
                                }
                                if (profileA.getTradeLimit().getAmount() + coins > aLimit) {
                                    player.sendMessage(CC.translate("&c你的今日交易硬币量已达上限!"));
                                    tradeManager.openMenu(player1);
                                    return;
                                }
                                if (profileB.getTradeLimit().getAmount() + tradeManager.getBCoins() > bLimit) {
                                    player.sendMessage(CC.translate("&c对方今日交易硬币量已达上限，无法放置!"));
                                    tradeManager.openMenu(player1);
                                    return;
                                }

                                profileA.setCoins(profileA.getCoins() + tradeManager.getACoins() - coins);
                                tradeManager.setACoins(coins);
                            } else {
                                if (tradeManager.getBItems().size() >= 8) {
                                    player.sendMessage(CC.translate("&c你无法再放入更多物品了!"));
                                    tradeManager.openMenu(player1);
                                    return;
                                }
                                if (coins > profileB.getCoins()) {
                                    player.sendMessage(CC.translate("&c你的硬币不足!"));
                                    tradeManager.openMenu(player1);
                                    return;
                                }
                                if (profileB.getTradeLimit().getAmount() + coins > bLimit) {
                                    player.sendMessage(CC.translate("&c你的今日交易硬币量已达上限!"));
                                    tradeManager.openMenu(player1);
                                    return;
                                }
                                if (profileA.getTradeLimit().getAmount() + tradeManager.getACoins() > aLimit) {
                                    player.sendMessage(CC.translate("&c对方今日交易硬币量已达上限，无法放置!"));
                                    tradeManager.openMenu(player1);
                                    return;
                                }

                                profileB.setCoins(profileB.getCoins() + tradeManager.getBCoins() - coins);
                                tradeManager.setBCoins(coins);
                            }

                            tradeManager.getPlayerA().playSound(tradeManager.getPlayerA().getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1, 1);
                            tradeManager.getPlayerB().playSound(tradeManager.getPlayerB().getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1, 1);
                            tradeManager.setBConfirm(false);
                            tradeManager.setAConfirm(false);
                            tradeManager.setAPutCooldown(new Cooldown(5, TimeUnit.SECONDS));
                            tradeManager.setBPutCooldown(new Cooldown(5, TimeUnit.SECONDS));
                            tradeManager.openMenu(player1);
                        });

                        return Collections.emptyList();
                    }).build();

            signGUI.open(player);
        } catch (SignGUIVersionException e) {
            e.printStackTrace();
        }
    }
}