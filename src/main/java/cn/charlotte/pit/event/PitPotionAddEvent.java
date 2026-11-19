package cn.charlotte.pit.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.potion.PotionEffect;

public class PitPotionAddEvent extends PitEvent implements Cancellable {
    private final Player player;
    private final PotionEffect effect;
    private boolean cancel;

    public PitPotionAddEvent(Player player, PotionEffect effect) {
        this.player = player;
        this.effect = effect;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean b) {
        cancel = b;
    }

    public Player getPlayer() {
        return player;
    }

    public PotionEffect getEffect() {
        return effect;
    }
}
