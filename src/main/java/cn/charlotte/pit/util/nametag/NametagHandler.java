package cn.charlotte.pit.util.nametag;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class NametagHandler {

    public long ticks = 50;
    private JavaPlugin plugin;
    private NametagAdapter adapter;
    private NametagListeners listeners;
    private Map<UUID, NametagBoard> boards;
    private boolean hook = false;
    private int taskId = -1;

    /**
     * Nametag Handler.
     *
     * @param plugin  instance.
     * @param adapter to display nametags.
     */
    public NametagHandler(JavaPlugin plugin, NametagAdapter adapter) {
        if (plugin == null) {
            throw new RuntimeException("Nametag Handler can not be instantiated without a plugin instance!");
        }

        this.plugin = plugin;
        this.adapter = adapter;
        this.boards = new ConcurrentHashMap<>();

        this.setup();
    }

    /**
     * Setup Library.
     */
    public void setup() {
        // Register Events.
        this.listeners = new NametagListeners(this);
        this.plugin.getServer().getPluginManager().registerEvents(this.listeners, this.plugin);

        for (Player player : Bukkit.getOnlinePlayers()) {
            getBoards().putIfAbsent(player.getUniqueId(), new NametagBoard(player, this));
        }

//        this.thread = new NametagThread(this);

        long period = ticks; // 以 server tick 为单位
        this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin,
                this::tick,   // 引用下面的新 tick()
                0L,                  // 初始延迟
                period               // 重复间隔
        );
    }

    private void tick() {
        if (adapter == null) {
            return;
        }
        adapter.refreshSnapshot();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            NametagBoard board = boards.get(player.getUniqueId());
            if (board != null) {
                board.update();
            }
        }
    }

    /**
     * Cleanup Library.
     */
    public void cleanup() {
        // Cancel Task.
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }

        // Unregister Listeners.
        if (this.listeners != null) {
            HandlerList.unregisterAll(this.listeners);
            this.listeners = null;
        }

        // Destroy boards.
        for (UUID uuid : getBoards().keySet()) {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                continue;
            }

            getBoards().remove(uuid);
            if (!isHook()) {
                player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
        }
    }

}
