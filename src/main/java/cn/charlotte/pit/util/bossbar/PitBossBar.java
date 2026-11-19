package cn.charlotte.pit.util.bossbar;

import cn.charlotte.pit.ThePit;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PitBossBar {

    private final Set<UUID> players = new HashSet<>();
    private final String title;
    private BossBar bossBar = null;

    public PitBossBar(String title) {
        this.title = title;
    }

    public void addPlayer(Player player) {
        if (!Bukkit.isPrimaryThread()) {
            runSync(() -> addPlayer(player));
            return;
        }
        if (players.contains(player.getUniqueId())) {
            return;
        }
        if (bossBar != null) {
            player.showBossBar(bossBar);
            players.add(player.getUniqueId());
        }
    }

    public void removePlayer(Player player) {
        if (!Bukkit.isPrimaryThread()) {
            runSync(() -> removePlayer(player));
            return;
        }
        if (players.remove(player.getUniqueId())) {
            if (bossBar != null) {
                player.hideBossBar(bossBar);
            }
        }
    }

    public void setProgress(double progress) {
        if (!Bukkit.isPrimaryThread()) {
            runSync(() -> setProgress(progress));
            return;
        }
        if (bossBar != null) {
            bossBar.progress(Math.max(0f, Math.min((float) progress, 1.0f)));
        }
    }

    public String getTitle() {
        if (bossBar != null) {
            return bossBar.name().toString(); // 需要解析 Component（可用 Adventure 的组件转字符串工具）
        }
        return "";
    }

    public void setTitle(String title) {
        if (!Bukkit.isPrimaryThread()) {
            runSync(() -> setTitle(title));
            return;
        }
        if (title != null && !title.isBlank()) {
            if (this.bossBar != null) {
                bossBar.name(Component.text(title));
            } else {
                this.bossBar = BossBar.bossBar(Component.text(title), 1.0f, Color.PURPLE, Overlay.PROGRESS);
            }

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.showBossBar(this.bossBar);
            }
        } else {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (this.bossBar != null) {
                    onlinePlayer.hideBossBar(this.bossBar);
                }
            }
        }
    }

    public void updateTitle(String title) {
        if (!Bukkit.isPrimaryThread()) {
            runSync(() -> updateTitle(title));
            return;
        }
        if (bossBar == null) {
            return;
        }
        Component newTitle = Component.text(title);
        if (!bossBar.name().equals(newTitle)) {
            bossBar.name(newTitle);
        }
    }

    public void clear() {
        if (!Bukkit.isPrimaryThread()) {
            runSync(this::clear);
            return;
        }
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.hideBossBar(bossBar);
            }
        }
        players.clear();
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public BossBar getBossBar() {
        return bossBar;
    }

    public void setColor(Color color) {
        if (!Bukkit.isPrimaryThread()) {
            runSync(() -> setColor(color));
            return;
        }
        if (bossBar != null) {
            bossBar.color(color);
        }
    }

    public void setOverlay(Overlay overlay) {
        if (!Bukkit.isPrimaryThread()) {
            runSync(() -> setOverlay(overlay));
            return;
        }
        if (bossBar != null) {
            bossBar.overlay(overlay);
        }
    }

    private void runSync(Runnable action) {
        Bukkit.getScheduler().runTask(ThePit.getInstance(), action);
    }
}
