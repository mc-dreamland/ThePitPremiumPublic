package cn.charlotte.pit.data.sub;

import lombok.Data;

/**
 * @Author: Misoryan
 * @Created_In: 2021/4/7 19:04
 */
@Data
public class QuestLimit {
    private long lastRefresh;
    private int times;

    public QuestLimit() {
    }

    public void addTimes(int times) {
        this.times += times;
    }

}
