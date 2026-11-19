package cn.charlotte.pit.sound.impl;

import cn.charlotte.pit.util.sound.AbstractPitSound;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SuccessfullySound extends AbstractPitSound {

    public static final SuccessfullySound INSTANCE = new SuccessfullySound();

    private SuccessfullySound() {
    }

    @Override
    public String getMusicInternalName() {
        return "successfully";
    }

    @Override
    public void onSoundTick(Player player, int tick) {
        if (tick == 0 || tick == 2) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1f);
        } else if (tick == 4 || tick == 6) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.1f);
        } else if (tick == 8 || tick == 10) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.2f);
        } else if (tick > 10) {
            end(player);
        }
    }
}
