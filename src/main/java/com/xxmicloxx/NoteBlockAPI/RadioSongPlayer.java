//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.xxmicloxx.NoteBlockAPI;

import com.azazo1.dragonpractice.DragonPractice;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class RadioSongPlayer extends SongPlayer {
    public RadioSongPlayer(Song song) {
        super(song);
    }

    public void playTick(Player p, int tick) {
        byte playerVolume = DragonPractice.NoteBlockPlayerMain.getPlayerVolume(p);

        for (Layer l : this.song.getLayerHashMap().values()) {
            Note note = l.getNote(tick);
            if (note != null) {
                p.playSound(p.getEyeLocation(), Instrument.getInstrument(note.getInstrument()), SoundCategory.BLOCKS, (float) (l.getVolume() * this.volume * playerVolume) / 1000000.0F, NotePitch.getPitch(note.getKey() - 33));
            }
        }

    }
}
