package cn.charlotte.pit.sound.impl;

import cn.charlotte.pit.util.sound.AbstractPitSound;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class DoubleStreakSound extends AbstractPitSound {

    public static final DoubleStreakSound INSTANCE = new DoubleStreakSound();

    private DoubleStreakSound() {
    }

    @Override
    public String getMusicInternalName() {
        return "double_streak";
    }

    @Override
    public void onSoundTick(Player player, int tick) {
        if (tick == 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.7f);
        } else if (tick == 2) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.8f);
        } else if (tick > 2) {
            end(player);
        }
    }
}
