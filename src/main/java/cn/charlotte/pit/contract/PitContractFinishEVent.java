package cn.charlotte.pit.contract;

import cn.charlotte.pit.event.PitEvent;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
public final class PitContractFinishEVent extends PitEvent {

    private final Player player;
    private final long time;
    private final Abstractcontract contract;
    private final ContractData data;
    private final boolean isNight;

    public PitContractFinishEVent(Player player, Abstractcontract contract, ContractData data, boolean isNight) {
        this.contract = contract;
        this.player = player;
        this.data = data;
        this.time = System.currentTimeMillis();
        this.isNight = isNight;
    }

}
