package cn.charlotte.pit.util.music;

import org.bukkit.Sound;

public class Instrument {
    public static Sound getInstrument(final byte b) {
        return switch (b) {
            case 1 -> EnumSoundEffect.BLOCK_NOTE_BLOCK_BASS.getSound();
            case 2 -> EnumSoundEffect.BLOCK_NOTE_BLOCK_BASEDRUM.getSound();
            case 3 -> EnumSoundEffect.BLOCK_NOTE_BLOCK_SNARE.getSound();
            case 4 -> EnumSoundEffect.BLOCK_NOTE_BLOCK_HAT.getSound();
            case 5 -> EnumSoundEffect.BLOCK_NOTE_BLOCK_GUITAR.getSound();
            case 6 -> EnumSoundEffect.BLOCK_NOTE_BLOCK_FLUTE.getSound();
            case 7 -> EnumSoundEffect.BLOCK_NOTE_BLOCK_BELL.getSound();
            case 8 -> EnumSoundEffect.BLOCK_NOTE_BLOCK_CHIME.getSound();
            case 9 -> EnumSoundEffect.BLOCK_NOTE_BLOCK_XYLOPHONE.getSound();
            case 10 -> EnumSoundEffect.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE.getSound();
            case 11 -> EnumSoundEffect.BLOCK_NOTE_BLOCK_COW_BELL.getSound();
            case 12 -> EnumSoundEffect.BLOCK_NOTE_BLOCK_DIDGERIDOO.getSound();
            case 13 -> EnumSoundEffect.BLOCK_NOTE_BLOCK_BIT.getSound();
            case 14 -> EnumSoundEffect.BLOCK_NOTE_BLOCK_BANJO.getSound();
            case 15 -> EnumSoundEffect.BLOCK_NOTE_BLOCK_PLING.getSound();
            default -> EnumSoundEffect.BLOCK_NOTE_BLOCK_HARP.getSound();
        };
    }

    public static org.bukkit.Instrument getBukkitInstrument(final byte b) {
        return switch (b) {
            case 1 -> EnumInstrument.BASS_GUITAR.toInstrument();
            case 2 -> EnumInstrument.BASS_DRUM.toInstrument();
            case 3 -> EnumInstrument.SNARE_DRUM.toInstrument();
            case 4 -> EnumInstrument.STICKS.toInstrument();
            case 5 -> EnumInstrument.GUITAR.toInstrument();
            case 6 -> EnumInstrument.FLUTE.toInstrument();
            case 7 -> EnumInstrument.BELL.toInstrument();
            case 8 -> EnumInstrument.CHIME.toInstrument();
            case 9 -> EnumInstrument.XYLOPHONE.toInstrument();
            case 10 -> EnumInstrument.IRON_XYLOPHONE.toInstrument();
            case 11 -> EnumInstrument.COW_BELL.toInstrument();
            case 12 -> EnumInstrument.DIDGERIDOO.toInstrument();
            case 13 -> EnumInstrument.BIT.toInstrument();
            case 14 -> EnumInstrument.BANJO.toInstrument();
            case 15 -> EnumInstrument.PLING.toInstrument();
            default -> EnumInstrument.PIANO.toInstrument();
        };
    }

    private enum EnumInstrument {
        BANJO("BANJO", 0),
        BASS_DRUM("BASS_DRUM", 1),
        BASS_GUITAR("BASS_GUITAR", 2),
        BELL("BELL", 3),
        BIT("BIT", 4),
        CHIME("CHIME", 5),
        COW_BELL("COW_BELL", 6),
        DIDGERIDOO("DIDGERIDOO", 7),
        FLUTE("FLUTE", 8),
        GUITAR("GUITAR", 9),
        IRON_XYLOPHONE("IRON_XYLOPHONE", 10),
        PIANO("PIANO", 11),
        PLING("PLING", 12),
        SNARE_DRUM("SNARE_DRUM", 13),
        STICKS("STICKS", 14),
        XYLOPHONE("XYLOPHONE", 15);

        EnumInstrument(final String s, final int n) {
        }

        public org.bukkit.Instrument toInstrument() {
            try {
                return org.bukkit.Instrument.valueOf(this.name());
            } catch (IllegalArgumentException ex) {
                return org.bukkit.Instrument.PIANO;
            }
        }
    }

    public enum EnumSoundEffect {
        BLOCK_NOTE_BLOCK_BANJO("BLOCK_NOTE_BLOCK_BANJO", 0, "", "", "BLOCK_NOTE_BLOCK_BANJO"),
        BLOCK_NOTE_BLOCK_BASEDRUM("BLOCK_NOTE_BLOCK_BASEDRUM", 1, "BLOCK_NOTE_BASEDRUM", "NOTE_BASS_DRUM", "BLOCK_NOTE_BLOCK_BASEDRUM"),
        BLOCK_NOTE_BLOCK_BASS("BLOCK_NOTE_BLOCK_BASS", 2, "BLOCK_NOTE_BASS", "NOTE_BASS", "BLOCK_NOTE_BLOCK_BASS"),
        BLOCK_NOTE_BLOCK_BELL("BLOCK_NOTE_BLOCK_BELL", 3, "BLOCK_NOTE_BELL", "", "BLOCK_NOTE_BLOCK_BELL"),
        BLOCK_NOTE_BLOCK_BIT("BLOCK_NOTE_BLOCK_BIT", 4, "", "", "BLOCK_NOTE_BLOCK_BIT"),
        BLOCK_NOTE_BLOCK_CHIME("BLOCK_NOTE_BLOCK_CHIME", 5, "BLOCK_NOTE_CHIME", "", "BLOCK_NOTE_BLOCK_CHIME"),
        BLOCK_NOTE_BLOCK_COW_BELL("BLOCK_NOTE_BLOCK_COW_BELL", 6, "", "", "BLOCK_NOTE_BLOCK_COW_BELL"),
        BLOCK_NOTE_BLOCK_DIDGERIDOO("BLOCK_NOTE_BLOCK_DIDGERIDOO", 7, "", "", "BLOCK_NOTE_BLOCK_DIDGERIDOO"),
        BLOCK_NOTE_BLOCK_FLUTE("BLOCK_NOTE_BLOCK_FLUTE", 8, "BLOCK_NOTE_FLUTE", "", "BLOCK_NOTE_BLOCK_FLUTE"),
        BLOCK_NOTE_BLOCK_GUITAR("BLOCK_NOTE_BLOCK_GUITAR", 9, "BLOCK_NOTE_GUITAR", "", "BLOCK_NOTE_BLOCK_GUITAR"),
        BLOCK_NOTE_BLOCK_HARP("BLOCK_NOTE_BLOCK_HARP", 10, "BLOCK_NOTE_HARP", "NOTE_PIANO", "BLOCK_NOTE_BLOCK_HARP"),
        BLOCK_NOTE_BLOCK_HAT("BLOCK_NOTE_BLOCK_HAT", 11, "BLOCK_NOTE_HAT", "NOTE_STICKS", "BLOCK_NOTE_BLOCK_HAT"),
        BLOCK_NOTE_BLOCK_IRON_XYLOPHONE("BLOCK_NOTE_BLOCK_IRON_XYLOPHONE", 12, "", "", "BLOCK_NOTE_BLOCK_IRON_XYLOPHONE"),
        BLOCK_NOTE_BLOCK_PLING("BLOCK_NOTE_BLOCK_PLING", 13, "BLOCK_NOTE_PLING", "NOTE_PLING", "BLOCK_NOTE_BLOCK_PLING"),
        BLOCK_NOTE_BLOCK_SNARE("BLOCK_NOTE_BLOCK_SNARE", 14, "BLOCK_NOTE_SNARE", "NOTE_SNARE_DRUM", "BLOCK_NOTE_BLOCK_SNARE"),
        BLOCK_NOTE_BLOCK_XYLOPHONE("BLOCK_NOTE_BLOCK_XYLOPHONE", 15, "BLOCK_NOTE_XYLOPHONE", "", "BLOCK_NOTE_BLOCK_XYLOPHONE");

        private final String newName;
        private final String legacyName;
        private final String latestName;

        EnumSoundEffect(final String s, final int n, final String newName, final String legacyName, final String latestName) {
            this.newName = newName;
            this.legacyName = legacyName;
            this.latestName = latestName;
        }

        public Sound getSound() {
            try {
                if (this.legacyName == "") {
                    return Sound.valueOf("NOTE_PIANO");
                }
                return Sound.valueOf(this.legacyName);
            } catch (IllegalArgumentException ex3) {
                return Sound.valueOf("BLOCK_NOTE_BLOCK_HARP");
            }
        }
    }
}
