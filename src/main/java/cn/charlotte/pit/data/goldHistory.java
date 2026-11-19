package cn.charlotte.pit.data;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class goldHistory {
    private long dailyGoldTotal;
    private Map<String, Long> goldSource;
}
