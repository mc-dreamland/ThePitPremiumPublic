package cn.charlotte.pit.hologram.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.LeaderBoardEntry;
import cn.charlotte.pit.hologram.AbstractHologram;
import cn.charlotte.pit.util.chat.StringUtil;
import cn.charlotte.pit.util.level.LevelUtil;
import cn.charlotte.pit.util.rank.RankUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.*;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/4 13:04
 */
public class LeaderBoardHologram extends AbstractHologram {
    @Override
    public String getInternalName() {
        return "leader_board_hologram";
    }

    @Override
    public List<String> getText(Player player) {
        if (player == null) {
            return new ArrayList<>();
        }
        List<String> hologramText = new ArrayList<>();
        hologramText.add("&b&l顶级活跃玩家");
        hologramText.add("&7天坑乱斗等级排名");
        hologramText.add("");


        Map<UUID, LeaderBoardEntry> leaderBoardEntries = LeaderBoardEntry.getLeaderBoardEntries();
        Iterator<LeaderBoardEntry> allEntries = leaderBoardEntries.values().iterator();
        for (int b = 0; b < 10; b++) {
            if (b + 1 > leaderBoardEntries.size()) {
                hologramText.add("&e" + (b + 1) + "&7. &7暂无");
            } else {
                LeaderBoardEntry entry = allEntries.next();

                int prestige = entry.prestige();
                double experience = entry.experience();
                for (int i = 0; i < prestige; i++) {
                    experience = experience + LevelUtil.getLevelTotalExperience(i, 120);
                }
                String levelTag = LevelUtil.getLevelTagWithRoman(entry.prestige(), entry.experience());
                String formattedExp = StringUtil.getFormatLong((long) experience);

                hologramText.add("&e" + entry.rank() + "&7." + " " + levelTag + " " + RankUtil.getPlayerRealColoredName(entry.name()) + " &7- &b" + formattedExp + " 经验值");
            }
        }

//        Optional<LeaderBoardEntry> first = LeaderBoardEntry.getLeaderBoardEntries()
//                .stream()
//                .filter(leaderBoardEntry -> leaderBoardEntry.getUuid().equals(player.getUniqueId()))
//                .findFirst();
        Optional<LeaderBoardEntry> first = Optional.ofNullable(leaderBoardEntries
                .get(player.getUniqueId()));

        if (first.isPresent()) {
            LeaderBoardEntry entry = first.get();
            double top = 100D * entry.rank() / leaderBoardEntries.size();
            int prestige = entry.prestige();
            double experience = entry.experience();
            for (int i = 0; i < prestige; i++) {
                experience = experience + LevelUtil.getLevelTotalExperience(i, 120);
            }
            String formattedExp = StringUtil.getFormatLong((long) experience);

            hologramText.add("");
            hologramText.add("&7你的经验值: &b" + formattedExp + " 经验值");
            hologramText.add("&7排名: " + "&e#" + entry.rank() + " &7(前&e" + new DecimalFormat("0.0").format(top) + "%&7)");
            hologramText.add("&7本排名仅显示 &f7天 &7内登录过的玩家 (&f" + leaderBoardEntries.size() + "&7名)");
        } else {
            hologramText.add("");
            hologramText.add("&7&o还没有你的排行数据,请等会再来...");
        }

        return hologramText;
    }

    @Override
    public boolean shouldLoop() {
        return true;
    }

    @Override
    public int loopTicks() {
        return 20;
    }

    @Override
    public double getHologramHighInterval() {
        return 0.3;
    }

    @Override
    public Location getLocation() {
        return ThePit.getInstance().getPitConfig().getLeaderBoardHologram();
    }
}
