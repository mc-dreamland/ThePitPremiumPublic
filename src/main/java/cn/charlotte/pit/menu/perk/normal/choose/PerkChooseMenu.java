package cn.charlotte.pit.menu.perk.normal.choose;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.perk.normal.buy.button.BoostButton;
import cn.charlotte.pit.menu.perk.normal.choose.button.KillStreakMainButton;
import cn.charlotte.pit.menu.perk.normal.choose.button.PerkButton;
import cn.charlotte.pit.perk.PerksPassivesEnum;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 22:46
 */
public class PerkChooseMenu extends Menu {
    @Override
    public String getTitle(Player player) {
        return "天赋商店";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        Map<Integer, Button> button = new HashMap<>();
        int perkLevel = profile.getLevel(PerksPrestigeEnum.EXTRA_PERK_SLOT);
        if (perkLevel > 0) {
            for (int i = 0; i < 4; i++) {
                button.put(11 + i, new PerkButton(i + 1));
            }
        }
        for (int i = 0; i < 3; i++) {
            button.putIfAbsent(12 + i, new PerkButton(i + 1));
        }
        button.put(15, new KillStreakMainButton());

        button.put(28, new BoostButton(PerksPassivesEnum.XP_BOOST.getPerk()));
        button.put(29, new BoostButton(PerksPassivesEnum.GOLD_BOOST.getPerk()));
        button.put(30, new BoostButton(PerksPassivesEnum.MELEE_DAMAGE.getPerk()));
        button.put(31, new BoostButton(PerksPassivesEnum.BOW_DAMAGE.getPerk()));
        button.put(32, new BoostButton(PerksPassivesEnum.DAMAGE_REDUCTION.getPerk()));
        button.put(33, new BoostButton(PerksPassivesEnum.BUILD_BATTLER.getPerk()));
        button.put(34, new BoostButton(PerksPassivesEnum.EL_GATO.getPerk()));

        return button;
    }

    @Override
    public int getSize() {
        return 5 * 9;
    }

    @Override
    public boolean isAutoUpdate() {
        return true;
    }
}
