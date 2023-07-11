package com.azazo1.dragonpractice;

import com.azazo1.dragonpractice.progress.BeforeStart;
import com.azazo1.dragonpractice.progress.ProgressManager;
import com.xxmicloxx.NoteBlockAPI.SongPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public final class DragonPractice extends JavaPlugin {
    public static DragonPractice plugin;
    public ProgressManager progressManager;
    public static NoteBlockPlayerMain noteBlockPlayerMain;

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        progressManager = new ProgressManager();
        try {
            progressManager.toggleTo(new BeforeStart(this));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        noteBlockPlayerMain = new NoteBlockPlayerMain();
        noteBlockPlayerMain.onEnable();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        noteBlockPlayerMain.onDisable();
    }

    public static class NoteBlockPlayerMain {
        public static NoteBlockPlayerMain plugin;
        public HashMap<String, ArrayList<SongPlayer>> playingSongs = new HashMap<>();
        public HashMap<String, Byte> playerVolume = new HashMap<>();

        public NoteBlockPlayerMain() {
        }

        public static boolean isReceivingSong(Player p) {
            return plugin.playingSongs.get(p.getName()) != null && !plugin.playingSongs.get(p.getName()).isEmpty();
        }

        public static void stopPlaying(Player p) {
            if (plugin.playingSongs.get(p.getName()) != null) {

                for (SongPlayer o : plugin.playingSongs.get(p.getName())) {
                    o.removePlayer(p);
                }

            }
        }

        public static void setPlayerVolume(Player p, byte volume) {
            plugin.playerVolume.put(p.getName(), volume);
        }

        public static byte getPlayerVolume(Player p) {
            Byte b = plugin.playerVolume.computeIfAbsent(p.getName(), k -> (byte) 100);

            return b;
        }

        public void onEnable() {
            plugin = this;
        }

        public void onDisable() {
            Bukkit.getScheduler().cancelTasks(DragonPractice.plugin);
        }
    }

    public static String removeQuotes(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        if (str.charAt(0) == '\"' && str.charAt(str.length() - 1) == '\"') {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

}
