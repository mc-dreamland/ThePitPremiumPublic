package cn.charlotte.pit.career;

import cn.charlotte.pit.manager.ResourceManager;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractHandleOfCareer {

    public void handleAttackEntity(int level, int description, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
    }

    public void handlePlayerAssist(int level, int description, Player myself, Entity target, double damage, double finalDamage, ResourceManager resourceManager) {
    }

    public void handlePlayerBeKilledByEntity(int level, int description, Player myself, Entity target, ResourceManager resourceManager) {
    }

    public void handlePlayerDamaged(int level, int description, Player myself, Entity attacker, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
    }

    public void handlePlayerKilled(int level, int description, Player myself, Entity target, ResourceManager resourceManager) {
    }

    public void handleShootEntity(int level, int description, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
    }


}
