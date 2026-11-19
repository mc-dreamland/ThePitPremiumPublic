package cn.charlotte.pit.manager;

/** Shared ordinary-streak currency formula; never changes streaks or triggers mega-streaks. */
public final class KillStreakRewards {
    private KillStreakRewards() { }

    public static void apply(ResourceManager resources, double streak, boolean streaker, boolean highlander) {
        if (streak <= 3) {
            resources.addExp(4, "小连杀补偿");
            resources.addGold(4, "小连杀补偿");
        }
        if (streak >= 3) {
            double xp;
            int gold;
            if (streak <= 4) {
                xp = 3;
                gold = 1;
            } else if (streak <= 19) {
                xp = 5;
                gold = 3;
            } else {
                xp = Math.min(3 * (streak / 10), streaker ? 60 : 30);
                gold = (int) streak / 10;
            }
            if (streaker) xp *= 3;
            resources.addExp(xp, "连杀" + (streaker ? "(嗜血者加成)" : ""));
            resources.addGold(gold * (highlander && streak >= 50 ? 1.5 : 1), "连杀");
        }
        if (streaker && streak > 20) resources.addBaseExp((int) (streak / 10), "嗜血者基础连杀经验");
        if (streak != 0 && streak % 10 == 0) {
            resources.addExp((streak / 10) * 3, "连杀奖励");
            resources.addGold((streak / 10) * 2, "连杀奖励");
        }
    }
}
