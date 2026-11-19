package cn.charlotte.pit.data;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.item.type.CustomNamePaper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
import lombok.Data;
import org.bukkit.inventory.ItemStack;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomPaperData {

    public static final int ALLOWED_QUEUED = 0;
    public static final int ALLOWED_PROMISED = 1;
    public static final int ALLOWED_REJECTED = 2;

    private String uuid;
    private String playerName;
    private Date time;
    private String customName;
    private int allowed;

    public static CompletableFuture<CustomPaperData> load(String uuid) {
        return CompletableFuture.supplyAsync(() -> ThePit.getInstance().getMongoDB()
                .getCustomPaperCollection()
                .find(Filters.eq("uuid", uuid))
                .first());
    }

    public CompletableFuture<UpdateResult> save() {
        return CompletableFuture.supplyAsync(() -> ThePit.getInstance().getMongoDB()
                .getCustomPaperCollection()
                .replaceOne(Filters.eq("uuid", this.uuid),
                        this,
                        new ReplaceOptions().upsert(true)));
    }

    public ItemStack toItem() {
        CustomNamePaper customNamePaper = new CustomNamePaper();
        customNamePaper.setUuid(UUID.fromString(uuid));
        switch (allowed) {
            case ALLOWED_QUEUED -> customNamePaper.setCustomName(customName);
            case ALLOWED_PROMISED -> {
                customNamePaper.setCustomName(customName);
                customNamePaper.setAllowed(true);
            }
        }
        customNamePaper.setMillis(System.currentTimeMillis() + 1000L * 60 * 5);// 5m
        return customNamePaper.toItemStack();
    }
}
