package cn.charlotte.pit.data.sub;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/20 22:27
 */
@Data
public class QuestCenter {
    private List<String> todayQuest = new ArrayList<>();
    private long lastRefreshTime = 0L;

    public QuestCenter() {
    }

}
