package cn.charlotte.pit.data.sub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/13 21:53
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerOption {

    boolean Scoreboard_Pres_opt = true;
    boolean Scoreboard_Level_opt = true;
    boolean Scoreboard_Coins_opt = true;
    boolean SewersChest = false;
    boolean FreeCE = true;
    boolean Spawn = false;
    private boolean
            bountyNotify = true,
            streakNotify = true,
            prestigeNotify = true,
            eventNotify = true,
            combatNotify = true,
            chatMsg = true,
            otherMsg = true,
            outfit = true,
            medalNotify = true,

    showGlowing = true,
            bountyHiddenWhenNear = false,


    tradeNotify = true;
    private BarPriority barPriority = BarPriority.DAMAGE_PRIORITY;
    //    private int bartime = 1;
    //admin only
    private boolean debugDamageMessage = false;
    //vip only
    private boolean
            profileVisibility = true,
            inventoryVisibility = true,
            enderChestVisibility = true,
            supporterStarDisplay = true;


    @Getter
    public enum BarPriority {
        DAMAGE_ONLY("仅显示攻击目标状态"),
        ENCHANT_ONLY("仅显示附魔冷却状态"),
        DAMAGE_PRIORITY("优先显示攻击目标状态");

        private final String description;

        BarPriority(String description) {
            this.description = description;
        }

    }
}
