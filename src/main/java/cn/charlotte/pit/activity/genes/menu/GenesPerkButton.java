package cn.charlotte.pit.activity.genes.menu;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.genes.data.GenesPlayerData;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.genesis.team.GenesisTeam;
import cn.charlotte.pit.item.type.mythic.AngelChestplate;
import cn.charlotte.pit.item.type.mythic.ArmageddonBoots;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GenesPerkButton extends Button {

//    private final GenesPlayerData data;
//
//    public GenesPerkButton(GenesPlayerData data) {
//        this.data = data;
//    }

    @Override
    public ItemStack getButtonItem(Player player) {
        GenesPlayerData data = Util.getPitProfile(player).getActivityPlayerData().getGenesisData();
        return new ItemBuilder(data.getTeam() == GenesisTeam.ANGEL ? Material.END_STONE :
                data.getTeam() == GenesisTeam.NONE ? Material.BARRIER : Material.NETHERRACK)
                .name("&e升级")
                .lore(getLines(player))
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        GenesPlayerData data = Util.getPitProfile(player).getActivityPlayerData().getGenesisData();
        if (data.getTeam() == GenesisTeam.NONE || data.getTier() >= 15) return;
        if (!data.takePotions(getPrice(data))) {
            CC.send(player, "&c你的活动点数不足以支付此升级");
            return;
        }
        CC.send(player, "&a&l升级");
        data.setTier(data.getTier() + 1);
        if (data.getTier() == 7) {
            if (data.getTeam() == GenesisTeam.ANGEL) Util.addItemOfMail(new AngelChestplate().toItemStack(),player,null);
            else Util.addItemOfMail(new ArmageddonBoots().toItemStack(),player,null);
        }
        new GenesMenu().openMenu(player);
    }

    private List<String> getLines(Player player) {
        GenesPlayerData data = Util.getPitProfile(player).getActivityPlayerData().getGenesisData();
        if (data.getTeam() == GenesisTeam.NONE) {
            return List.of(
                    "",
                    "&c请先加入一个阵营!",
                    ""
            );
        }
        List<String> list = new ArrayList<>();
        list.add("");
        list.add("&7通过点数升级自己!");
        list.add("");
        list.add("&7可用升级:");
        list.add(String.format(" %s &7对不同阵营的目标造成的基础伤害 &c+1❤", data.getTier() >= 1 ? "&a▶" : "&c×"));
        list.add(String.format(" %s &7商店内的 &b钻石类物品 &7的售价 &6-50%% &7.", data.getTier() >= 2 ? "&a▶" : "&c×"));
        list.add(String.format(" %s &7在 &d神话之井 &7进行附魔需要的硬币 &6-20%% &7.", data.getTier() >= 3 ? "&a▶" : "&c×"));
        list.add(String.format(" %s &d神话装备&7掉落倍率 &6+30%% &7.", data.getTier() >= 4 ? "&a▶" : "&c×"));
        if (data.getTeam() == GenesisTeam.DEMON)
            list.add(String.format(" %s &7攻击穿着 &6皮革装备 &7的玩家造成的基础伤害 &c+0.5❤ &7.", data.getTier() >= 5 ? "&a▶" : "&c×"));
        else
            list.add(String.format(" %s &7攻击穿着 &b钻石装备 &7的玩家造成的基础伤害 &c+0.5❤ &7.", data.getTier() >= 5 ? "&a▶" : "&c×"));
        list.add(String.format(" %s &7击杀赏金的赏金获得量 &6+50%% &7,大型事件中获得声望时获得量 &e+1 &7.", data.getTier() >= 6 ? "&a▶" : "&c×"));
        if (data.getTeam() == GenesisTeam.ANGEL)
            list.add(String.format(" %s &7获得活动限定物品: &f天使之甲", data.getTier() >= 7 ? "&a▶" : "&c×"));
        else list.add(String.format(" %s &7获得活动限定物品: &c终末之靴", data.getTier() >= 7 ? "&a▶" : "&c×"));
        list.add(String.format(" %s &7获得活动限时加成(助攻/击杀生效,每级提升8%%最高64%%): &f+%s%%%s", data.getTier() >= 8 ? "&a▶" : "&c×", Math.max((data.getTier() - 7) * 8, 0), data.getTeam() == GenesisTeam.ANGEL ? "&b经验" : "&e金币"));
        list.add("");
        list.add(data.getTier() < 15 ? String.format("&7升级下一级所需点数: &e&l%s", getPrice(data)) : "&e已达到最大等级了");
        list.add("");
        return list;
    }

    private int getPrice(GenesPlayerData data) {
        switch (data.getTier()) {
            case 0 -> {
                return 100;
            }
            case 1 -> {
                return 300;
            }
            case 2 -> {
                return 800;
            }
            case 3 -> {
                return 1500;
            }
            case 4 -> {
                return 3000;
            }
            case 5 -> {
                return 5000;
            }
            case 6 -> {
                return 20000;
            }
            case 7, 8, 9, 10, 11, 12, 13, 14 -> {
                return 300 * (data.getTier() - 6);
            }
            default -> {
                return -1;
            }
        }
    }
}
