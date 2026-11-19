package cn.charlotte.pit.contract;

import cn.charlotte.pit.Util;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.entity.Player;

import java.util.List;

@Slf4j
public abstract class Abstractcontract {
    public abstract String getName();

    public abstract String getInternalName();

    public abstract int getMaxLevel();

    public abstract int getMinLevel();

    public abstract List<String> getLore(int level, boolean isNight);

    public abstract ContractType getType();

    public abstract long getTime(int level);

    public abstract int getGoal(int level, boolean isNight);

    public abstract void toComplete(Player player, ContractData data, boolean isNight);

    public void complete(Player player, boolean isNight) {
        ContractData data = Util.getPitProfile(player).getContractData();
        for (PitContract con : PitContract.values()) {
            if (con.getInstance().getInternalName().equals(data.getInterestName())) {
                new PitContractFinishEVent(player, con.getInstance(), data, isNight).callEvent();
                this.toComplete(player, data, isNight);
                Util.getPitProfile(player).setContractData(null);
                return;
            }
        }
        log.warn("未知合同? 触发玩家: {}", player.getName());
    }

    @Getter
    public enum ContractType {
        STREAK("在一次性获得目标连杀");

        private final String description;

        ContractType(String description) {
            this.description = description;
        }
    }
}