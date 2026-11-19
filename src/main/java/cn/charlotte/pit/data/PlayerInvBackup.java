package cn.charlotte.pit.data;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.mongojack.Id;
import org.mongojack.ObjectId;

/**
 * @Author: EmptyIrony
 * @Date: 2021/3/17 21:51
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerInvBackup {
    @Id
    @ObjectId
    private String id;

    private String uuid;
    private long timeStamp;
    private String backupUuid;

    private PlayerInv inv;
    private PlayerEnderChest[] chest;

    public PlayerInvBackup() {
    }

    @SneakyThrows
    public void save() {
        ThePit.getInstance()
                .getMongoDB()
                .getInvCollection()
                .replaceOne(Filters.eq("backupUuid", backupUuid), this, new ReplaceOptions().upsert(true));
    }

}
