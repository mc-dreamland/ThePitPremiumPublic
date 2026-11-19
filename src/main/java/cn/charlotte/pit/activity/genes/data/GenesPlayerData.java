package cn.charlotte.pit.activity.genes.data;

import cn.charlotte.pit.data.deserializer.IntegerStringMapDeserializer;
import cn.charlotte.pit.data.serializer.IntegerStringMapSerializer;
import cn.charlotte.pit.events.genesis.team.GenesisTeam;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class GenesPlayerData {

    private GenesisTeam team = GenesisTeam.NONE;
    private int tier;
    private Long points;
    @JsonDeserialize(using = IntegerStringMapDeserializer.class)
    @JsonSerialize(using = IntegerStringMapSerializer.class)
    private Map<Integer, String> BuyMap;
    private boolean better;

    public void addPoints(int count) {
        this.points += count;
    }

    public GenesPlayerData() {
        this.points = 0L;
        this.better = false;
        this.tier = 0;
    }

    public boolean takePotions(int count) {
        if (count == 0) return true;
        if (count > this.points) return false;
        this.points -= count;
        return true;
    }
}
