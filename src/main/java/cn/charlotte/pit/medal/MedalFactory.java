package cn.charlotte.pit.medal;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.activity.newyear.medal.NewYear;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.MedalData;
import cn.charlotte.pit.event.PitProfileLoadedEvent;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.ClassUtil;
import lombok.Data;
import lombok.SneakyThrows;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Creator Misoryan
 * @Date 2021/6/8 20:13
 */
@Data
@AutoRegister
public class MedalFactory implements Listener {

    private final List<AbstractMedal> medals;
    private Collection<Class<?>> outsideMedals;

    public MedalFactory() {
        this.medals = new ArrayList<>();

        this.outsideMedals = new ArrayList<>(List.of(
                NewYear.class
        ));
    }

    @SneakyThrows
    public void init() {
        Collection<Class<?>> classes = ClassUtil.getClassesInPackage(ThePit.getInstance(), "cn.charlotte.pit.medal.impl");
        for (Class<?> clazz : classes) {
            if (AbstractMedal.class.isAssignableFrom(clazz)) {
                Object instance = clazz.newInstance();
                medals.add((AbstractMedal) instance);
            }
        }
        outsideMedals.stream().filter(AbstractMedal.class::isAssignableFrom)
                .forEach(aClass -> {
                    try {
                        medals.add((AbstractMedal) aClass.newInstance());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    public int getMedalAmount() {
        int amount = 0;
        for (AbstractMedal medal : medals) {
            amount += medal.getMaxLevel();
        }
        return amount;
    }

    @EventHandler
    public void onProfileLoaded(PitProfileLoadedEvent event) {
        try {
            PlayerProfile profile = event.getPlayerProfile();
            MedalData medalData = profile.getMedalData();
            if (medalData == null) {
                medalData = new MedalData();
                profile.setMedalData(medalData);
            }

            // 迁移：清历史中误写入的默认项
            medalData.compact();

            for (AbstractMedal medal : medals) {
                try {
                    medal.handleProfileLoaded(profile);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
