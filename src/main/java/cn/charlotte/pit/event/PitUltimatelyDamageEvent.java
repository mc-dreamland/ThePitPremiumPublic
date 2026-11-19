package cn.charlotte.pit.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

@Setter
@Getter
public class PitUltimatelyDamageEvent extends PitEvent {
    private Player attacker;
    private Entity target;
    private double ultimatelyDamage;

    public PitUltimatelyDamageEvent(Player attacker, Entity target, double ultimatelyDamage) {
        this.attacker = attacker;
        this.target = target;
        this.ultimatelyDamage = ultimatelyDamage;
    }
}
