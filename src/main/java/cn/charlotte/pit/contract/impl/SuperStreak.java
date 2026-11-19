package cn.charlotte.pit.contract.impl;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.contract.Abstractcontract;
import cn.charlotte.pit.contract.ContractData;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.mail.Mail;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        list.add(String.format(" &f+ &5暗聚块 * %s", level));
        if (InventoryUtil.isInvFull(player)) {
            player.getInventory().addItem(PitItems.CHUNK_OF_VILE.toManyItem(level));
        } else {
            Mail mail = new Mail();
            mail.setExpireTime(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L);
            mail.setSendTime(System.currentTimeMillis());
            PlayerInv contents = new PlayerInv();
            contents.setContents(new ItemStack[]{PitItems.CHUNK_OF_VILE.toManyItem(level)});
            mail.setTitle("&e【合同处】 暗聚块物品");
            mail.setContent("&f由于您的背包满了,店员已将暗聚块打包并邮寄至您的邮箱! (运费由合同处出)");
            mail.setItem(contents);
            Util.sendMail(player.getUniqueId(), mail);
            list.add("&f由于您的背包满了,店员已将暗聚块打包并邮寄至您的邮箱! (运费由合同处出)");
        }
        CC.send(MessageType.MISC, player, list);
    }
}