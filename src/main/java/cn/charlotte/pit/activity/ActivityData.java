package cn.charlotte.pit.activity;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.deserializer.LongStringMapDeserializer;
import cn.charlotte.pit.data.deserializer.StringStringMapDeserializer;
import cn.charlotte.pit.data.deserializer.UUIDListDeserializer;
import cn.charlotte.pit.data.serializer.LongStringMapSerializer;
import cn.charlotte.pit.data.serializer.StringBooleanMapSerializer;
import cn.charlotte.pit.data.serializer.StringStringMapSerializer;
import cn.charlotte.pit.data.serializer.UUIDListSerializer;
import cn.charlotte.pit.menu.Help.button.UpdateMenu;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.Data;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ActivityData {

    private Double ver;
    private List<String> list;
    @JsonSerialize(using = UUIDListSerializer.class)
    @JsonDeserialize(using = UUIDListDeserializer.class)
    private List<UUID> uuidList;
    @JsonDeserialize(using = StringStringMapDeserializer.class)
    @JsonSerialize(using = StringStringMapSerializer.class)
    private Map<String,String> map;

    public ActivityData() {
        this.list = new ArrayList<>();
        this.map = new HashMap<>();
        this.ver = UpdateMenu.getVer();
        this.uuidList = new ArrayList<>();
    }

    public static ActivityData load(Double ver) {
        if (ver == null || ver == 0.0) return null;

        ActivityData data = ThePit.getInstance()
                .getMongoDB()
                .getActivityDataCollection()
                .find(Filters.eq("ver", ver))
                .first();
        if (data == null) {
            data = new ActivityData();
            data.setVer(ver);
        }
        return data;
    }

    public void save() {
        ThePit.getInstance()
                .getMongoDB()
                .getActivityDataCollection()
                .replaceOne(Filters.eq("ver", this.ver), this, new ReplaceOptions().upsert(true));
    }
    public void clear() {
        ThePit.getInstance()
                .getMongoDB()
                .getActivityDataCollection()
                .deleteOne(Filters.eq("ver", this.ver));
        this.list.clear();
        this.uuidList.clear();
        this.map.clear();
    }
}