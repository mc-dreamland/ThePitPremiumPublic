package cn.charlotte.pit.data.sub;

import cn.charlotte.pit.quest.AbstractQuest;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/19 16:18
 */
@Data
public class QuestData {
    private String internalName;
    private int level;
    private long startTime;
    private long endTime;
    private int current;
    private int total;
    private Set<String> killed;
    private AbstractQuest.QuestType questType;
    private boolean night;

    /**
     * 需保留无参构筑器
     * 否则Jackson无法正确从MongoDB中序列化该对象
     */
    public QuestData() {
        this.killed = new HashSet<>();
        this.night = false;
    }

    public QuestData(boolean isNight) {
        this.killed = new HashSet<>();
        this.night = isNight;
    }

}
