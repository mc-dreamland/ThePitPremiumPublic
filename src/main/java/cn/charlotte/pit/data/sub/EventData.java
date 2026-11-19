package cn.charlotte.pit.data.sub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class EventData {
    private int eventrenown = 0;
    private Map<String, Integer> historicalevents = new HashMap<>();

    public void addEventrenown(int i) {
        this.eventrenown += i;
    }

    public void callHistoricalEvents(String eventName, int count) {
        historicalevents.merge(eventName, count, Integer::sum);
    }
}
