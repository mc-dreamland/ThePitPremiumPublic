package cn.charlotte.pit.event;

import cn.charlotte.pit.data.PlayerProfile;
import lombok.Getter;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 0:54
 */
@Getter
public class PitProfileLoadedEvent extends PitEvent {
    private final PlayerProfile playerProfile;

    public PitProfileLoadedEvent(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }

}
