package cn.charlotte.pit.event;

import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.UUID;
@Getter
public class PitEnchBangEvent extends PitEvent {
    public PitEnchBangEvent(List<UUID> player, Set<UUID> killList) {
        this.player = player;
        this.killList = killList;
    }

    private final List<UUID> player;
    private final Set<UUID> killList;
}
