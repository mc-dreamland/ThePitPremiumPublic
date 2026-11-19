package cn.charlotte.pit.data;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.sub.PlayerInv;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.Data;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/27 13:03
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeData {
    private static final ExecutorService executor = new ScheduledThreadPoolExecutor(3);

    private String tradeUuid;
    private String playerA;
    private String playerB;
    private String playerAName;
    private String playerBName;

    private long completeTime;

    private PlayerInv aPaidItem;
    private PlayerInv bPaidItem;

    private long aPaidCoin;
    private long bPaidCoin;

    public TradeData(String playerA, String playerB, String playerAName, String playerBName) {
        this.tradeUuid = UUID.randomUUID().toString();

        this.playerA = playerA;
        this.playerB = playerB;
        this.playerAName = playerAName;
        this.playerBName = playerBName;
    }

    public TradeData() {

    }

    public static ExecutorService getExecutor() {
        return TradeData.executor;
    }

    public void save() {
        executor.execute(() -> {
            ThePit.getInstance()
                    .getMongoDB()
                    .getTradeCollection()
                    .replaceOne(Filters.eq("tradeUuid", tradeUuid), this, new ReplaceOptions().upsert(true));
        });
    }
}
