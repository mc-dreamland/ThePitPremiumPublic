package cn.charlotte.pit.map.kingsquests.ui.button;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.KingsQuestsData;
import cn.charlotte.pit.item.type.mythic.GoldenHelmet;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.level.LevelUtil;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public final class KingsQuestsButton extends Button {

    public static final KingsQuestsButton INSTANCE = new KingsQuestsButton();
    private static final DecimalFormat DF = new DecimalFormat("#,###.##");

    private KingsQuestsButton() {
    }

    private static double getRewardExp(PlayerProfile p) {
        double needExp = 0.0;
        double boostFactor = LevelUtil.getBoostFactor(p.getPrestige());
        for (int level = 1; level <= 120; level++) {
            needExp += LevelUtil.getLevelExpRequired(boostFactor, level);
        }
        return needExp * 0.30326;
    }

    private static double getRewardCoins(PlayerProfile p) {
        return (2000.0 * p.getPrestige()) + 10000.0;
    }

    private static List<String> getQuestDescription(PlayerProfile p) {
        List<String> lore = new ArrayList<>();
        lore.add("&8罕见的任务");
        lore.add("");
        lore.add("&7任务需求:");
        lore.add(" &7做一个&d 迷你蛋糕");
        lore.add(" &7击杀 &c10000&7 名玩家");
        lore.add(" &7从事件中收集 &e100 &7声望");
        lore.add("");
        lore.add("&7任务奖励:");
        lore.add(" &e+&6国王头盔");
        lore.add(" &b+" + DF.format(getRewardExp(p)) + "经验");
        lore.add(" &6+" + DF.format(getRewardCoins(p)) + "金币");
        lore.add("");
        return lore;
    }

    /* ===================== 计算与文案 ===================== */

    private static List<String> getActiveQuestDescription(Player player, KingsQuestsData data, PlayerProfile p) {
        boolean hasCake = hasMiniCake(player.getInventory());

        String cakeProgress = hasCake ? "&7(&a1&7/1)" : "&7(&c0&7/1)";
        String killProgress = (data.getKilledPlayer() >= 10000)
                ? "&7(&a10000&7/10000)"
                : "&7(&c" + data.getKilledPlayer() + "&7/10000)";
        String renownProgress = (data.getCollectedRenown() >= 100)
                ? "&7(&a100&7/100)"
                : "&7(&c" + data.getCollectedRenown() + "&7/100)";

        List<String> lore = new ArrayList<>();
        lore.add("&8罕见的任务");
        lore.add("");
        lore.add("&7任务需求:");
        lore.add(" &7做一个&d 迷你蛋糕 " + cakeProgress);
        // 原 Kotlin 文案这里为 100，而进度使用 10000；保持一致
        lore.add(" &7击杀 &c10000&7 名玩家 " + killProgress);
        lore.add(" &7从事件中收集 &e100 &7声望 " + renownProgress);
        lore.add("");
        lore.add("&7任务奖励:");
        lore.add(" &e+&6国王头盔");
        lore.add(" &b+" + DF.format(getRewardExp(p)) + "经验");
        lore.add(" &6+" + DF.format(getRewardCoins(p)) + "金币");
        lore.add("");
        return lore;
    }

    private static List<String> appendTail(List<String> src, String tail) {
        List<String> out = new ArrayList<>(src.size() + 1);
        out.addAll(src);
        out.add(tail);
        return out;
    }

    private static boolean hasMiniCake(Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (isMiniCake(item)) return true;
        }
        return false;
    }

    private static void removeOneMiniCake(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isMiniCake(item)) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                    inv.setItem(i, item);
                } else {
                    inv.setItem(i, null);
                }
                return;
            }
        }
    }

    private static boolean isMiniCake(ItemStack it) {
        if (it == null) return false;
        String internal = ItemUtil.getInternalName(it);
        return "mini_cake".equalsIgnoreCase(internal);
    }

    /* ===================== 物品工具：mini_cake 判定与移除 ===================== */

    @Override
    public ItemStack getButtonItem(Player player) {
        PlayerProfile profile = Util.getPitProfile(player);

        KingsQuestsData data = profile.getKingsQuestsData();
        data.checkUpdate();

        if (data.getAccepted()) {
            if (data.getCompleted()) {
                List<String> lore = appendTail(getQuestDescription(profile), "&c你已经完成了这个任务");
                return new ItemBuilder(Material.GOLD_BLOCK)
                        .name("&e国王任务")
                        .lore(lore)
                        .build();
            } else {
                List<String> lore = appendTail(getActiveQuestDescription(player, data, profile), "&a点击提交任务");
                return new ItemBuilder(Material.GOLD_BLOCK)
                        .name("&e国王任务")
                        .lore(lore)
                        .build();
            }
        } else {
            List<String> lore = appendTail(getQuestDescription(profile), "&e点击接受任务");
            return new ItemBuilder(Material.GOLD_BLOCK)
                    .name("&e国王任务")
                    .lore(lore)
                    .build();
        }
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        PlayerProfile profile = Util.getPitProfile(player);

        KingsQuestsData data = profile.getKingsQuestsData();
        data.checkUpdate();

        if (data.getCompleted()) return;

        if (!data.getAccepted()) {
            data.setAccepted(true);
            return;
        }

        // 与原 Kotlin 一致：提交条件（击杀 >= 100 且 声望 >= 10）
        if (data.getKilledPlayer() < 10000 || data.getCollectedRenown() < 100) {
            return;
        }

        // 必须背包里有 "mini_cake"
        if (!hasMiniCake(player.getInventory())) return;

        // 移除 1 个 "mini_cake"
        removeOneMiniCake(player.getInventory());

        data.setCompleted(true);

        // 奖励物品：金头盔
        player.getInventory().addItem(new GoldenHelmet().toItemStack());

        // 经验与金币奖励
        double rewardExp = getRewardExp(profile);
        double rewardCoins = getRewardCoins(profile);

        profile.setExperience(profile.getExperience() + rewardExp);
        profile.setCoins(profile.getCoins() + rewardCoins);

        profile.applyExperienceToPlayer(player);
        profile.grindCoins(rewardCoins);

        // 提示
        player.sendMessage("&a&l任务完成! &a你完成了 &e国王任务&a, 获得奖励: ");
        player.sendMessage(" &e+&6金头盔");
        player.sendMessage(" &b" + DF.format(rewardExp) + "经验");
        player.sendMessage(" &6" + DF.format(rewardCoins) + "金币");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 1.2f);
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }
}
