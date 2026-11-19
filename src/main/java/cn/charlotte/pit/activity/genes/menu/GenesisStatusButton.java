package cn.charlotte.pit.activity.genes.menu;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.ActivityType;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.genesis.team.GenesisTeam;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.menus.ConfirmMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/2/4 11:40
 */

public class GenesisStatusButton extends Button {

    private final GenesisTeam displayTeam;
    private final PlayerProfile profile;

    public GenesisStatusButton(GenesisTeam displayTeam, PlayerProfile profile) {
        this.displayTeam = displayTeam;
        this.profile = profile;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        List<String> lines = new ArrayList<>();
        String teamColor = "&f";
        Material material = Material.STONE;
        if (displayTeam == GenesisTeam.ANGEL) {
            teamColor = "&b";
            material = Material.END_STONE;
        }
        if (displayTeam == GenesisTeam.DEMON) {
            teamColor = "&c";
            material = Material.NETHERRACK;
        }
        lines.add("&8阵营");
        lines.add(" ");
        if (profile.getActivityPlayerData().getGenesisData().getTeam() == GenesisTeam.NONE) {
            lines.add("&7向此阵营宣誓你的忠诚.");
            lines.add("&7参与阵营活动,解锁");
            lines.add("&7专属活动奖励!");
            lines.add(" ");
        } else if (profile.getActivityPlayerData().getGenesisData().getTeam() == displayTeam) {
            lines.add("&7你已经向此阵营宣誓了你的忠诚.");
            lines.add(" ");
            lines.add("&7你的阵营点数: " + teamColor + profile.getActivityPlayerData().getGenesisData().getPoints() + " 活动点数");
            lines.add(" ");
            lines.add(teamColor + "1 对立阵营玩家击杀 = 2 活动点数");
            lines.add(teamColor + "1 相同阵营玩家击杀 = 1 活动点数");
            lines.add(" ");
            lines.add(String.format("&f活动结束时间:%s", Util.tyyyy_MM_dd_h_min_s.format(ActivityType.GENES.getEndTime())));
        } else if (profile.getActivityPlayerData().getGenesisData().getPoints() < 500) {
            lines.add("&7你已经向敌方阵营宣誓了你的忠诚,");
            lines.add("&7但你的活动点数高于500前,");
            lines.add("&7你仍有叛变的机会.");
            lines.add(" ");
            lines.add(teamColor + "叛变会重置你的点数");
            lines.add(teamColor + "以及奖励进度.");
            lines.add(" ");
            lines.add("&e点击叛变至当前阵营!");
        } else {
            lines.add("&7你已经向敌方阵营宣誓了你的忠诚.");
            lines.add("&7这里不属于你.");
        }
        return new ItemBuilder(material).name(teamColor + "宣誓效忠").lore(lines).build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        if (ThePit.getInstance().getActivityFactory().getFactoryType() != ActivityType.GENES) {
            player.sendMessage(CC.translate("&c&l失败 &7请你先等到活动开始在进行."));
            return;
        }
        new ConfirmMenu(profile.getActivityPlayerData().getGenesisData().getTeam() == GenesisTeam.NONE ? "确认加入此阵营" : "确认切换阵营?", confirm -> {
            if (confirm) {
                if (profile.getActivityPlayerData().getGenesisData().getTeam() != GenesisTeam.NONE && profile.getActivityPlayerData().getGenesisData().getTeam() != displayTeam && profile.getActivityPlayerData().getGenesisData().getPoints() <= 500) {
                    profile.getActivityPlayerData().getGenesisData().setTeam(displayTeam);
                    profile.getActivityPlayerData().getGenesisData().setPoints(0L);
                    profile.getActivityPlayerData().getGenesisData().setTier(0);
                    profile.getActivityPlayerData().getGenesisData().setBuyMap(null);
                    player.sendMessage(CC.translate("&a&l切换阵营! &7成功切换阵营."));
                }
                if (profile.getActivityPlayerData().getGenesisData().getTeam() == GenesisTeam.NONE) {
                    profile.getActivityPlayerData().getGenesisData().setTeam(displayTeam);
                    ThePit.getInstance().getActivityFactory().getActivityData().getUuidList().add(player.getUniqueId());
                    player.sendMessage(CC.translate("&a&l加入阵营! &7成功加入阵营."));
                }
            }
            new GenesMenu().openMenu(player);

        }, true, 3, null).openMenu(player);
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }
}
