package cn.charlotte.pit.data.temp;

import cn.charlotte.pit.util.cooldown.Cooldown;
import lombok.Data;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/28 13:24
 */
@Data
public class TradeRequest {
    private UUID player;
    private UUID target;
    private Cooldown cooldown;

    public TradeRequest(UUID player, UUID target) {
        this.player = player;
        this.target = target;
        this.cooldown = new Cooldown(15, TimeUnit.SECONDS);
    }
}
