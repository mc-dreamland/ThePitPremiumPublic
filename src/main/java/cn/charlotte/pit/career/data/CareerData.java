package cn.charlotte.pit.career.data;

import cn.charlotte.pit.data.deserializer.MapStringLongDeserializer;
import cn.charlotte.pit.data.serializer.MapStringLongSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class CareerData {

    private Map<String, directionData> dataMap;
    @JsonSerialize(using = MapStringLongSerializer.class)
    @JsonDeserialize(using = MapStringLongDeserializer.class)
    private Map<String, Long> killMap;

    public CareerData() {
        this.dataMap = new HashMap<>(8);
        this.killMap = new HashMap<>(8);
    }

    @Data
    class directionData {
        private int level;
        private int direction;

        public directionData() {
            this.level = 0;
            this.direction = 0;
        }
    }

}
