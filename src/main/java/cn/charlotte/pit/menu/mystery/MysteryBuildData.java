package cn.charlotte.pit.menu.mystery;

import lombok.Data;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

@Data
public class MysteryBuildData {

    private boolean canceled;
    private String cancelMessage;
    private List<String> returnMessage;
    private int delCount;
    private ItemStack returnItemStack;
    private boolean handled;
    private String operationId;
    private String delivery;
    private Runnable postCommitAction;

    public MysteryBuildData() {
        this.canceled = false;
        this.cancelMessage = null;
        this.returnMessage = null;
        this.delCount = 0;
        this.returnItemStack = null;
        this.handled = false;
        this.operationId = UUID.randomUUID().toString();
        this.delivery = "NONE";
        this.postCommitAction = null;
    }

    public void runPostCommitAction() {
        if (postCommitAction != null) {
            postCommitAction.run();
        }
    }
}
