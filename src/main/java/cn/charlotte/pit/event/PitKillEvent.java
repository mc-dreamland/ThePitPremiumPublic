package cn.charlotte.pit.event;

import cn.charlotte.pit.manager.ResourceManager;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/30 1:07
 */
@Getter
public class PitKillEvent extends PitEvent {
    private final Player killer;
    private final LivingEntity target;
    private final ResourceManager resourceManager;
    private final LinkedList<String> msg;

    public PitKillEvent(Player killer, LivingEntity target, ResourceManager resourceManager, LinkedList<String> msg) {
        this.killer = killer;
        this.target = target;
        this.resourceManager = resourceManager;
        this.msg = msg;
    }

    public Player getKiller() {
        return killer;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

}
