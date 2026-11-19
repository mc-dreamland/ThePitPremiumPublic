package cn.charlotte.pit.activity.newyear;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.deserializer.IntegerStringMapDeserializer;
import cn.charlotte.pit.data.serializer.IntegerStringMapSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class NewYearData{
    
    private Long coins;
    private Long kills;
    private Long points;
    
    @JsonSerialize(using=IntegerStringMapSerializer.class)
    @JsonDeserialize(using=IntegerStringMapDeserializer.class)
    private Map<Integer, String> buyMap;
    
    private Map<String, Integer> perkMap;
    
    public NewYearData(){
        this.coins = 0L;
        this.kills = 0L;
        this.points = 0L;
        this.perkMap = new HashMap<>(4);
    }
    
    public void addKill(){
        this.kills++;
    }
    
    public void addPotions(){
        this.points++;
    }
    
    public Long addCoins(int level){
        long amount;
        if(level == 1) amount = 1;
        else amount = Util.random.nextLong(Math.max(level, 1)) + 1;
        this.coins += amount;
        return amount;
    }
    
    public void removeCoins(int amount){
        if(amount == 0) return;
        if(this.coins < amount) this.coins = 0L;
        else this.coins -= amount;
    }
    
    public void removePoint(int amount){
        if(amount == 0) return;
        if(this.points < amount) this.points = 0L;
        else this.points -= amount;
    }
    
    public void clearBuyMap(){
        if(this.buyMap == null) this.buyMap = new HashMap<>(3);
        else this.buyMap.clear();
    }
}
