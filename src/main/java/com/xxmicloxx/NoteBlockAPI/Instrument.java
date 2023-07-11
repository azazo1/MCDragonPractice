//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.xxmicloxx.NoteBlockAPI;

import org.bukkit.Sound;

public class Instrument {
    public Instrument() {
    }

    public static Sound getInstrument(byte instrument) {
        return switch (instrument) {
            case 1 -> Sound.BLOCK_NOTE_BLOCK_BASS;
            case 2 -> Sound.BLOCK_NOTE_BLOCK_BASEDRUM;
            case 3 -> Sound.BLOCK_NOTE_BLOCK_SNARE;
            case 4 -> Sound.BLOCK_NOTE_BLOCK_HAT;
            case 5 -> Sound.BLOCK_NOTE_BLOCK_GUITAR;
            case 6 -> Sound.BLOCK_NOTE_BLOCK_FLUTE;
            case 7 -> Sound.BLOCK_NOTE_BLOCK_BELL;
            case 8 -> Sound.BLOCK_NOTE_BLOCK_CHIME;
            case 9 -> Sound.BLOCK_NOTE_BLOCK_XYLOPHONE;
            default -> Sound.BLOCK_NOTE_BLOCK_HARP; // 0 也是这
        };
    }

    public static org.bukkit.Instrument getBukkitInstrument(byte instrument) {
        return switch (instrument) {
            case 1 -> org.bukkit.Instrument.BASS_GUITAR;
            case 2 -> org.bukkit.Instrument.BASS_DRUM;
            case 3 -> org.bukkit.Instrument.SNARE_DRUM;
            case 4 -> org.bukkit.Instrument.STICKS;
            case 5 -> org.bukkit.Instrument.GUITAR;
            case 6 -> org.bukkit.Instrument.FLUTE;
            case 7 -> org.bukkit.Instrument.BELL;
            case 8 -> org.bukkit.Instrument.CHIME;
            case 9 -> org.bukkit.Instrument.XYLOPHONE;
            default -> org.bukkit.Instrument.PIANO; // 0 也是这
        };
    }
}
