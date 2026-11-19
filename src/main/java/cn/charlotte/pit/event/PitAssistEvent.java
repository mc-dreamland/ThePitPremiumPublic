package cn.charlotte.pit.event;

import cn.charlotte.pit.manager.ResourceManager;
import org.bukkit.entity.Player;

/**
 * @Author: EmptyIrony
 * @Date: 2021/2/4 13:03
 */

public class PitAssistEvent extends PitEvent {
    private final Player assist;
    private final Player target;

    private ResourceManager resourceManager;

    public PitAssistEvent(Player assist, Player target, ResourceManager resourceManager) {
        this.assist = assist;
        this.target = target;
        this.resourceManager = resourceManager;
    }

    public Player getAssist() {
        return assist;
    }

    public Player getTarget() {
        return target;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }
}
