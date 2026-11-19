package cn.charlotte.pit.buff.impl;

import cn.charlotte.pit.buff.AbstractPitBuff;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitDamageEvent;
import cn.charlotte.pit.parm.AutoRegister;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.List;

@AutoRegister
public class BeeVenomDeBuff extends AbstractPitBuff implements Listener {


    @Override
    public String getInternalBuffName() {
        return "beeVenom";
    }

    @Override
    public String getDisplayName() {
        return "蜂毒";
    }

    @Override
    public int getMaxTier() {
        return 4;
    }

    @Override
    public List<String> getDescription() {
        return Collections.singletonList("&7攻击造成的伤害 &c-0.5♥ &7,可叠加");
    }

    @EventHandler
    public void onDamage(PitDamageEvent event) {
        if (!(event.getAttacker() instanceof Player player)) {
            return;
        }

        int tier = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).getBuffData().getBuff(getInternalBuffName()).getTier();

        if (tier > 0) {
            event.setDamage(Math.max(0, event.getDamage() - 0.5 * Math.min(tier, 4)));
        }
    }
}