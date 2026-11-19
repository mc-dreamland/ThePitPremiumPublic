package cn.charlotte.pit.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContractData {
    private String interestName;
    private long time;
    private int level;
    private String playerUnid;
    private int goal;
    private boolean isNight;
    private int record;

    public ContractData() {
    }

    public ContractData(int goal, String interestName, int level, long time, String playeruuid, boolean isNight) {
        this.goal = goal;
        this.interestName = interestName;
        this.time = time;
        this.level = level;
        this.playerUnid = playeruuid;
        this.isNight = isNight;
        this.record = 0;
    }
}