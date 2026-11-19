package cn.charlotte.pit.manager;

import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.util.time.TimeUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResourceManager {

    @Getter
    private final Map<String, Double> expAdditions = new LinkedHashMap<>();
    @Getter
    private final Map<String, Double> expModifiers = new LinkedHashMap<>();
    @Getter
    private final Map<String, Double> goldAdditions = new LinkedHashMap<>();
    @Getter
    private final Map<String, Double> goldModifiers = new LinkedHashMap<>();
    @Getter
    private final Map<String, Double> additionalExperience = new LinkedHashMap<>();
    @Getter
    private final Map<String, Double> additionalGold = new LinkedHashMap<>();
    protected double percentage = 1;
    @Setter
    @Getter
    private int maxExp = 400;
    @Setter
    @Getter
    private int maxGold = 600;
    @Setter
    @Getter
    private boolean hasCelebrityPerk = false;

    public ResourceManager() {
        if (TimeUtil.IS_HOLIDAY) {
            addExpModifier((1 + NewConfiguration.getFestivalBoosts()), TimeUtil.HOLIDAY_NAME);
            addGoldModifier((1 + NewConfiguration.getFestivalBoosts()), TimeUtil.HOLIDAY_NAME);
        }
    }

    public double calculateEXP() {
        return calculateEXP(1);
    }

    /** Variant multipliers apply to the whole reward before rounding and the normal per-kill cap. */
    public double calculateEXP(double rewardMultiplier) {
        double baseExp = 0;
        double extraExp = 0;
        for (Map.Entry<String, Double> entry : additionalExperience.entrySet()) {
            extraExp += entry.getValue();
        }
        for (Map.Entry<String, Double> entry : expAdditions.entrySet()) {
            baseExp += entry.getValue();
        }

        double bonus = 0.0;
        double penalty = 1.0;

        for (Double value : expModifiers.values()) {
            if (value > 1.0) {
                bonus += (value - 1.0);
            } else if (value < 1.0) {
                penalty *= value;
            }
        }

        double finalModifier = (1.0 + bonus);
        double exp = (baseExp * finalModifier + extraExp) * penalty * validMultiplier(rewardMultiplier);

        return Math.min(Math.ceil(exp), maxExp);
    }

    public double calculateGold() {
        return calculateGold(1);
    }

    public double calculateGold(double rewardMultiplier) {
        double baseGold = 0.0;
        double bounty = 0.0;
        double extraGold = 0.0;

        for (Map.Entry<String, Double> entry : additionalGold.entrySet()) {
            extraGold += entry.getValue();
        }
        for (Map.Entry<String, Double> entry : goldAdditions.entrySet()) {
            if (entry.getKey().contains("赏金")) {
                bounty += entry.getValue();
            } else {
                baseGold += entry.getValue();
            }
        }

        double bonus = 0.0;
        double penalty = 1.0;
        double bountyBonus = 1.0;
        for (Map.Entry<String, Double> entry : goldModifiers.entrySet()) {
            if (entry.getValue() > 1.0) {
                if (entry.getKey().contains("赏金溶剂") || entry.getKey().contains("金手铐") || entry.getKey().contains("阵营赏金加成") || entry.getKey().contains("悬赏令")) {
                    bountyBonus += entry.getValue() - 1.0;
                } else {
                    bonus += entry.getValue() - 1.0;
                }
            } else if (entry.getValue() < 1.0) {
                penalty *= entry.getValue();
            }
        }

        double finalModifier = (1.0 + bonus);
        double multiplier = validMultiplier(rewardMultiplier);
        double gold = (baseGold * finalModifier + extraGold) * (hasCelebrityPerk ? 2 : 1) * penalty * multiplier;

        if (gold > maxGold) {
            gold = maxGold;
        }

        return gold + bounty * bountyBonus * multiplier;
    }

    private static double validMultiplier(double value) {
        return Double.isFinite(value) ? Math.max(0, value) : 0;
    }

    public void addExp(double exp, String reason) {
        additionalExperience.put(reason, exp);
    }

    public void addExpModifier(double modifier, String reason) {
        expModifiers.put(reason, modifier);
    }

    public void addGold(double gold, String reason) {
        additionalGold.put(reason, gold);
    }

    public void addGoldModifier(double modifier, String reason) {
        goldModifiers.put(reason, modifier);
    }

    public void addBaseGold(double gold, String reason) {
        goldAdditions.put(reason, gold);
    }

    public void addBaseExp(double exp, String reason) {
        expAdditions.put(reason, exp);
    }
}
