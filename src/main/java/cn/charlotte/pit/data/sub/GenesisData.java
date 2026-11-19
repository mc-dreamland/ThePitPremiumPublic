package cn.charlotte.pit.data.sub;

import cn.charlotte.pit.events.genesis.team.GenesisTeam;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/29 22:05
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GenesisData {
    public int tier = 0;
    public GenesisTeam team = GenesisTeam.NONE;
    public Map<String, Integer> buyList = new HashMap<>();
    private int points = 0;
    private int boostTier = 0;
}
