package cn.charlotte.pit.util.nametag;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

/**
 * 负责维护单个玩家视角下的记分板（名牌 + 血条）。
 * <p>已加入“脏值检查”缓存，只在 prefix / suffix / invis 发生变化时才真正调用 Bukkit API。<p>
 */
@Getter
public class NametagBoard {

    /**
     * 该记分板所属玩家 UUID
     */
    private final UUID uuid;

    /**
     * 注入的 NametagHandler
     */
    private final NametagHandler handler;

    /**
     * 上一次 tick 中出现过的 Team 名称
     */
    private final Set<String> bufferedTeams = new HashSet<>();

    /**
     * 记录每个 Team 上一次下发到客户端的状态，
     * 用于“脏值检查”——只有不同才 setPrefix / setSuffix / setCanSeeFriendlyInvisibles。
     */
    private final Map<String, CachedTag> cache = new HashMap<>();
    // 记录本 tick 出现过的 Team，用于事后清理
    private final Set<String> currentTickTeams = new HashSet<>();


    // ─────────────────────────────────────────────────────────────────────
    // 构造 & 初始化
    // ─────────────────────────────────────────────────────────────────────
    public NametagBoard(Player player, NametagHandler handler) {
        this.uuid = player.getUniqueId();
        this.handler = handler;
        this.setup(player);
    }

    private void setup(Player player) {
        player.setScoreboard(getScoreboard());
    }

    /**
     * 每次 NametagHandler#tick() 时调用
     */
    public void update() {
        Scoreboard scoreboard = this.getScoreboard();
        Player player = Bukkit.getPlayer(getUuid());

        if (player == null) {
            return; // 玩家已离线
        }

        this.updateHealthBelow(player, scoreboard);
        this.updateNametags(player, scoreboard);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 对外 API
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 清空缓存（如玩家离线时调用）
     */
    public void cleanup() {
        cache.clear();
        bufferedTeams.clear();
    }

    // ─────────────────────────────────────────────────────────────────────
    // 内部逻辑
    // ─────────────────────────────────────────────────────────────────────
    private Scoreboard getScoreboard() {
        Player player = Bukkit.getPlayer(getUuid());
        if (player == null) {
            // 理论上不会走到这里；防御性返回主记分板
            return Bukkit.getScoreboardManager().getMainScoreboard();
        }

        if (handler.isHook() || player.getScoreboard() != Bukkit.getScoreboardManager().getMainScoreboard()) {
            return player.getScoreboard();
        }
        return Bukkit.getScoreboardManager().getNewScoreboard();
    }

    /**
     * 更新血条（BELOW_NAME）
     */
    private void updateHealthBelow(Player player, Scoreboard scoreboard) {
        if (!handler.getAdapter().showHealthBelowName(player)) {
            Objective old = scoreboard.getObjective(DisplaySlot.BELOW_NAME);
            if (old != null) old.unregister();
            return;
        }

        Objective objective = scoreboard.getObjective(DisplaySlot.BELOW_NAME);
        if (objective == null) {
            objective = scoreboard.registerNewObjective(
                    "showhealth",
                    "health",
                    ChatColor.RED + "❤"
            );
            objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }
    }

    /**
     * 核心：只在需要时才真正调用 Bukkit API 更新 Team。
     */
    private void updateNametags(Player player, Scoreboard scoreboard) {
        List<BufferedNametag> nametags = this.handler.getAdapter().getPlate(player);
        if (nametags == null) return;

        // 1. 依次应用每个 BufferedNametag
        for (BufferedNametag buffered : nametags) {
//            Team team = this.getOrRegisterTeam(scoreboard, buffered.getGroupName());
// Seems groupName is not stable so leads extra team player operators
            Team team = this.getOrRegisterTeam(scoreboard, buffered.getGroupName());

            currentTickTeams.add(team.getName());
            // 从旧 buffer 里删除，剩下的就是需要 unregister 的
            bufferedTeams.remove(team.getName());

            String prefix = buffered.getPrefix() != null ? buffered.getPrefix() : ChatColor.RESET.toString();
            String suffix = buffered.getSuffix() != null ? buffered.getSuffix() : ChatColor.RESET.toString();
            boolean invis = buffered.isFriendlyInvis();

            // 只在内容变化时才下发数据包
            applyTag(team, prefix, suffix, invis);

            // 玩家条目
            if (buffered.getPlayer() != null) {
                if (!team.hasEntry(buffered.getPlayer())) {
                    team.addEntry(buffered.getPlayer());
                }
            }
        }

        // 2. 清理上一次 tick 中多余的 Team
        for (String oldName : bufferedTeams) {
            Team team = scoreboard.getTeam(oldName);
            if (team == null) continue;

            for (String entry : team.getEntries()) {
                team.removeEntry(entry);
            }
            team.unregister();
            cache.remove(oldName); // 同步移除缓存
        }

        // 3. 更新缓存
        bufferedTeams.clear();
        bufferedTeams.addAll(currentTickTeams);
        currentTickTeams.clear();
    }

    /**
     * 取得或创建 Team；保证名称 ≤ 16 字符
     */
    private Team getOrRegisterTeam(Scoreboard scoreboard, String name) {
        String teamName = name.length() > 16 ? name.substring(0, 16) : name;
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        return team;
    }

    /**
     * “脏值检查”具体实现：只在 prefix / suffix / invis 实际变化时调用 Bukkit API。
     */
    private void applyTag(Team team, String prefix, String suffix, boolean invis) {
        CachedTag last = cache.computeIfAbsent(team.getName(), k -> new CachedTag());

        if (!Objects.equals(last.prefix, prefix)) {
            team.setPrefix(prefix);
            last.prefix = prefix;
        }
        if (!Objects.equals(last.suffix, suffix)) {
            team.setSuffix(suffix);
            last.suffix = suffix;
        }
        if (last.invis != invis) {
            team.setCanSeeFriendlyInvisibles(invis);
            last.invis = invis;
        }
    }

    /**
     * prefix / suffix / invisibility 的快照
     */
    private static final class CachedTag {
        String prefix;
        String suffix;
        boolean invis;
    }
}
