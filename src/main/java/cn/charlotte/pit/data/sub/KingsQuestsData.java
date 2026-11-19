package cn.charlotte.pit.data.sub;

import cn.charlotte.pit.ThePit;

import java.util.Objects;
import java.util.UUID;

public class KingsQuestsData {

    private UUID currentKingQuestsUuid = null;

    private boolean accepted = false;
    private boolean completed = false;

    private int killedPlayer = 0;
    private int collectedRenown = 0;

    public void checkUpdate() {
        UUID running = ThePit.getApi().getRunningKingsQuestsUuid();
        if (currentKingQuestsUuid == null) {
            currentKingQuestsUuid = running;
            return;
        }
        if (!Objects.equals(currentKingQuestsUuid, running)) {
            currentKingQuestsUuid = running;
            accepted = false;
            completed = false;
            killedPlayer = 0;
            collectedRenown = 0;
        }
    }

    // ===== getters / setters =====

    public UUID getCurrentKingQuestsUuid() {
        return currentKingQuestsUuid;
    }

    public void setCurrentKingQuestsUuid(UUID currentKingQuestsUuid) {
        this.currentKingQuestsUuid = currentKingQuestsUuid;
    }

    public boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean getCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getKilledPlayer() {
        return killedPlayer;
    }

    public void setKilledPlayer(int killedPlayer) {
        this.killedPlayer = killedPlayer;
    }

    public int getCollectedRenown() {
        return collectedRenown;
    }

    public void setCollectedRenown(int collectedRenown) {
        this.collectedRenown = collectedRenown;
    }
}
