package cn.charlotte.pit.data.sub;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.util.cooldown.Cooldown;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * @Author: EmptyIrony
 * @Date: 2021/2/2 22:45
 */
@Getter
public class KillRecap {
    public static Map<UUID, KillRecap> recapMap = new HashMap<>();

    @Setter
    private long completeTime;
    //killer
    @Setter
    private UUID killer;
    @Setter
    private ResourceManager resourceManager;
    @Setter
    private List<String> perk = new ArrayList<>();

    @Setter
    private double totalExp;

    private double baseExp;
    //连杀小于等于3的exp
    private double notStreakExp;
    //连杀为5的倍数的exp
    private double streakExp;
    //等级差距exp
    private double levelDisparityExp;
    private double otherExp;

    @Setter
    private double totalCoin;
    private double notStreakCoin;
    private double streakCoin;
    private double levelDisparityCoin;
    private double baseCoin;
    private double otherCoin;


    //assist
    @Setter
    private List<AssistData> assistData = new ArrayList<>();

    //damage log
    @Setter
    private List<DamageData> damageLogs = new ArrayList<>();

    public KillRecap() {
    }

    public void setBaseExp(double baseExp) {
        this.totalExp = this.totalExp - this.baseExp + baseExp;
        this.baseExp = baseExp;
    }

    public void setNotStreakExp(double notStreakExp) {
        this.totalExp = this.totalExp - this.notStreakExp + notStreakExp;
        this.notStreakExp = notStreakExp;
    }

    public void setStreakExp(double streakExp) {
        this.totalExp = this.totalExp - this.streakExp + streakExp;
        this.streakExp = streakExp;
    }

    public void setLevelDisparityExp(double levelDisparityExp) {
        this.totalExp = this.totalExp - this.levelDisparityExp + levelDisparityExp;
        this.levelDisparityExp = levelDisparityExp;
    }

    public void setOtherExp(double otherExp) {
        this.totalExp = this.totalExp - this.otherExp + otherExp;
        this.otherExp = otherExp;
    }

    public void setNotStreakCoin(double notStreakCoin) {
        this.totalCoin = this.totalCoin - this.notStreakCoin + notStreakCoin;
        this.notStreakCoin = notStreakCoin;
    }

    public void setStreakCoin(double streakCoin) {
        this.totalCoin = this.totalCoin - this.streakCoin + streakCoin;
        this.streakCoin = streakCoin;
    }

    public void setLevelDisparityCoin(double levelDisparityCoin) {
        this.totalCoin = this.totalCoin - this.levelDisparityCoin + levelDisparityCoin;
        this.levelDisparityCoin = levelDisparityCoin;
    }

    public void setBaseCoin(double baseCoin) {
        this.totalCoin = this.totalCoin - this.baseCoin + baseCoin;
        this.baseCoin = baseCoin;

    }

    public void setOtherCoin(double otherCoin) {
        this.totalCoin = this.totalCoin - this.otherCoin + otherCoin;
        this.otherCoin = otherCoin;
    }

    public void completeLog(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        this.completeTime = System.currentTimeMillis();
        profile.setKillRecap(new KillRecap());

        recapMap.put(player.getUniqueId(), this);
    }

    @Data
    public static class DamageData {
        private boolean attack;
        private boolean melee;
        private boolean lava;
        private double damage;
        private double boostDamage;
        private String displayName;
        private double afterHealth;
        private ItemStack usedItem;

        private Cooldown timer;

        public DamageData() {
        }

    }

    @Setter
    public static class AssistData {
        @Getter
        private String displayName;
        @Getter
        private double percentage;

        private double baseExp;
        //连杀为5的倍数的exp
        private double streakExp;
        //等级差距exp
        private double levelDisparityExp;

        private double streakCoin;
        private double levelDisparityCoin;
        private double baseCoin;

        private double totalCoin;
        private double totalExp;

        public double getBaseExp() {
            return baseExp * percentage;
        }

        public double getStreakExp() {
            return streakExp * percentage;
        }

        public double getLevelDisparityExp() {
            return levelDisparityExp * percentage;
        }

        public double getStreakCoin() {
            return streakCoin * percentage;
        }

        public double getLevelDisparityCoin() {
            return levelDisparityCoin * percentage;
        }

        public double getBaseCoin() {
            return baseCoin * percentage;
        }

        public double getTotalCoin() {
            return totalCoin * percentage;
        }

        public double getTotalExp() {
            return totalExp * percentage;
        }

    }
}
