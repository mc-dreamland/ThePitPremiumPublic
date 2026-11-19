package cn.charlotte.pit.util.bossbar;

import cn.charlotte.pit.parm.AutoRegister;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/11 23:50
 */
@AutoRegister
public class BossBarHandler implements Listener {
    private final PitBossBar pitBossBar;

    public BossBarHandler() {
        this.pitBossBar = new PitBossBar("");
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        this.pitBossBar.addPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.pitBossBar.removePlayer(event.getPlayer());
    }


    public PitBossBar getBossBar() {
        return this.pitBossBar;
    }
}
