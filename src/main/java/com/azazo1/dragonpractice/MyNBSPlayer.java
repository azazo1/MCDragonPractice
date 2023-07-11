package com.azazo1.dragonpractice;

import com.xxmicloxx.NoteBlockAPI.NBSDecoder;
import com.xxmicloxx.NoteBlockAPI.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.Song;
import com.xxmicloxx.NoteBlockAPI.SongPlayer;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyNBSPlayer {
    public static SongPlayer songPlayer;
    public static AtomicBoolean loop = new AtomicBoolean(false);
    public static File lastPlayed;

    /**
     * 播放音乐，
     * 使用者需要将jar包内的nbs文件解压到该插件data目录下才能播放
     */
    public static void playMusic(File filePath, boolean loop) {
        stopMusic();
        MyLog.i("play music");
        lastPlayed = filePath;
        Song song;
        try {
            song = NBSDecoder.parse(Files.newInputStream(filePath.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        songPlayer = new RadioSongPlayer(song);
        songPlayer.setVolume((byte) 10);
        songPlayer.setAutoDestroy(true);
        Bukkit.getOnlinePlayers().forEach(songPlayer::addPlayer);
        songPlayer.setPlaying(true);
        MyNBSPlayer.loop.set(loop);
    }

    public static void onMusicStop() {
        if (loop.get()) {
            Bukkit.getScheduler().runTask(DragonPractice.plugin, () -> playMusic(lastPlayed, true));
        }
    }

    public static void stopMusic() {
        if (songPlayer != null) {
            songPlayer.setPlaying(false);
            songPlayer.destroy();
        }
    }
}
