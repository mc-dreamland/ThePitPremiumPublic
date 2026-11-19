package cn.charlotte.pit.data;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.sub.PlayerInv;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: EmptyIrony
 * @Date: 2021/3/25 13:26
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CDKData {
    private static volatile Map<String, CDKData> cachedCDK = new HashMap<>();
    private static boolean loading;

    private String cdk;
    private long timeout;
    private int limitLevel;
    private String limitPermission;
    private String pitUUID;
    private double exp;
    private double coins;
    private int renown;
    private PlayerInv item = new PlayerInv();
    private long sendTime;
    private long expireTime;
    private int limitPrestige = -1;
    private int limitClaimed;
    private List<String> claimedPlayers = new ArrayList<>();

    public CDKData() {
    }

    public static void loadAllCDKFromData() {
        loading = true;
        cachedCDK.clear();
        for (CDKData data : ThePit.getInstance()
                .getMongoDB()
                .getCdkCollection()
                .find()) {
            cachedCDK.put(data.cdk, data);
        }
        loading = false;
    }

    public static Map<String, CDKData> getCachedCDK() {
        return CDKData.cachedCDK;
    }

    public static boolean isLoading() {
        return CDKData.loading;
    }

    public void active() {
        ThePit.getInstance()
                .getMongoDB()
                .getCdkCollection()
                .replaceOne(Filters.eq("cdk", cdk), this, new ReplaceOptions().upsert(true));
        cachedCDK.put(cdk, this);
    }

}