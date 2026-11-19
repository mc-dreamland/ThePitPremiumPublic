package cn.charlotte.pit.contract.impl;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.contract.Abstractcontract;
import cn.charlotte.pit.contract.ContractData;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SuperStreak extends Abstractcontract {

    @Override
    public String getName() {
        return "超级连杀";
    }

    @Override
    public String getInternalName() {
        return "super_streak";
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public List<String> getLore(int level, boolean isNight) {
        List<String> list = new ArrayList<>(3);
        list.add(String.format(" &f+ &e金币 * %s", getRewards(level)));
        list.add(String.format(" &f+ &b经验 * %s", getRewards(level)));
        if (isNight) list.add(String.format(" &f+ &5暗聚块 * %s", level));
        return list;
    }

    private int getRewards(int level) {
        return level == 3 ? level * 1500 : level * 1000;
    }

    @Override
    public ContractType getType() {
        return ContractType.STREAK;
    }

    @Override
    public long getTime(int level) {
        return 1000L * 60 * level * 10;
    }

    @Override
    public int getGoal(int level, boolean isNight) {
        return isNight ? level == 3 ? 100 + level * 100 : level * 100 : level * 50;
    }

    @Override
    public void toComplete(Player player, ContractData data, boolean isNight) {
        List<String> list = new ArrayList<>();
        int level = data.getLevel();
        list.add("&e合同处 &a合同完成! 获得奖励");
        list.addAll(this.getLore(level, isNight));
        PlayerProfile profile = Util.getPitProfile(player);
        profile.setCoins(profile.getCoins() + getRewards(level));
        profile.setExperience(profile.getExperience() + getRewards(level));
        if (!isNight) {
            CC.send(MessageType.MISC, player, list);
            return;
        }
        var remaining = player.getInventory().addItem(PitItems.CHUNK_OF_VILE.toItemStacks(level));
        if (!remaining.isEmpty()) {
            String mailContent = "&f由于您的背包空间不足，店员已将放不下的暗聚块邮寄至您的邮箱！";
            Util.sendItemMail(player, List.copyOf(remaining.values()), "&e【合同处】 暗聚块物品", mailContent);
            list.add(mailContent);
        }
        CC.send(MessageType.MISC, player, list);
    }
}
