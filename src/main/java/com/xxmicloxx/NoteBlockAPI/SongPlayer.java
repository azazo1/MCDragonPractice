//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.xxmicloxx.NoteBlockAPI;

import com.azazo1.dragonpractice.DragonPractice;
import com.azazo1.dragonpractice.MyNBSPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SongPlayer {
    protected Song song;
    protected boolean playing = false;
    protected short tick = -1;
    protected ArrayList<String> playerList = new ArrayList<>();
    protected boolean autoDestroy = false;
    protected boolean destroyed = false;
    protected Thread playerThread;
    protected byte fadeTarget = 100;
    protected byte volume = 100;
    protected byte fadeStart;
    protected int fadeDuration;
    protected int fadeDone;
    protected FadeType fadeType;

    public SongPlayer(Song song) {
        this.fadeStart = this.volume;
        this.fadeDuration = 60;
        this.fadeDone = 0;
        this.fadeType = FadeType.FADE_LINEAR;
        this.song = song;
        this.createThread();
    }

    public FadeType getFadeType() {
        return this.fadeType;
    }

    public void setFadeType(FadeType fadeType) {
        this.fadeType = fadeType;
    }

    public byte getFadeTarget() {
        return this.fadeTarget;
    }

    public void setFadeTarget(byte fadeTarget) {
        this.fadeTarget = fadeTarget;
    }

    public byte getFadeStart() {
        return this.fadeStart;
    }

    public void setFadeStart(byte fadeStart) {
        this.fadeStart = fadeStart;
    }

    public int getFadeDuration() {
        return this.fadeDuration;
    }

    public void setFadeDuration(int fadeDuration) {
        this.fadeDuration = fadeDuration;
    }

    public int getFadeDone() {
        return this.fadeDone;
    }

    public void setFadeDone(int fadeDone) {
        this.fadeDone = fadeDone;
    }

    protected void calculateFade() {
        if (this.fadeDone != this.fadeDuration) {
            double targetVolume = Interpolator.interpLinear(new double[]{0.0, (double) this.fadeStart, (double) this.fadeDuration, (double) this.fadeTarget}, (double) this.fadeDone);
            this.setVolume((byte) ((int) targetVolume));
            ++this.fadeDone;
        }
    }

    protected void createThread() {
        this.playerThread = new Thread(() -> {
            while (!SongPlayer.this.destroyed) {
                long startTime = System.currentTimeMillis();
                synchronized (SongPlayer.this) {
                    if (SongPlayer.this.playing) {
                        SongPlayer.this.calculateFade();
                        ++SongPlayer.this.tick;
                        if (SongPlayer.this.tick > SongPlayer.this.song.getLength()) {
                            SongPlayer.this.playing = false;
                            SongPlayer.this.tick = -1;
                            SongEndEvent event = new SongEndEvent(SongPlayer.this);
                            MyNBSPlayer.onMusicStop();
                            if (SongPlayer.this.autoDestroy) {
                                SongPlayer.this.destroy();
                                return;
                            }
                        }

                        for (String s : SongPlayer.this.playerList) {
                            Player p = Bukkit.getPlayerExact(s);
                            if (p != null) {
                                SongPlayer.this.playTick(p, SongPlayer.this.tick);
                            }
                        }
                    }
                }

                long duration = System.currentTimeMillis() - startTime;
                float delayMillis = SongPlayer.this.song.getDelay() * 50.0F;
                if ((float) duration < delayMillis) {
                    try {
                        Thread.sleep((long) (delayMillis - (float) duration));
                    } catch (InterruptedException ignored) {
                    }
                }
            }

        });
        this.playerThread.setPriority(10);
        this.playerThread.start();
    }

    public List<String> getPlayerList() {
        return Collections.unmodifiableList(this.playerList);
    }

    public void addPlayer(Player p) {
        synchronized (this) {
            if (!this.playerList.contains(p.getName())) {
                this.playerList.add(p.getName());
                ArrayList<SongPlayer> songs = DragonPractice.NoteBlockPlayerMain.plugin.playingSongs.get(p.getName());
                if (songs == null) {
                    songs = new ArrayList<>();
                }

                songs.add(this);
                DragonPractice.NoteBlockPlayerMain.plugin.playingSongs.put(p.getName(), songs);
            }

        }
    }

    public boolean getAutoDestroy() {
        synchronized (this) {
            return this.autoDestroy;
        }
    }

    public void setAutoDestroy(boolean value) {
        synchronized (this) {
            this.autoDestroy = value;
        }
    }

    public abstract void playTick(Player var1, int var2);

    public void destroy() {
        synchronized (this) {
            SongDestroyingEvent event = new SongDestroyingEvent(this);
//            MyNBSPlayer.onMusicStop();
            if (!event.isCancelled()) {
                this.destroyed = true;
                this.playing = false;
                this.setTick((short) -1);
            }
        }
    }

    public boolean isPlaying() {
        return this.playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
        if (!playing) {
            SongStoppedEvent event = new SongStoppedEvent(this);
//            MyNBSPlayer.onMusicStop();
        }

    }

    public short getTick() {
        return this.tick;
    }

    public void setTick(short tick) {
        this.tick = tick;
    }

    public void removePlayer(Player p) {
        synchronized (this) {
            this.playerList.remove(p.getName());
            if (DragonPractice.NoteBlockPlayerMain.plugin.playingSongs.get(p.getName()) != null) {
                ArrayList<SongPlayer> songs = new ArrayList<>(DragonPractice.NoteBlockPlayerMain.plugin.playingSongs.get(p.getName()));
                songs.remove(this);
                DragonPractice.NoteBlockPlayerMain.plugin.playingSongs.put(p.getName(), songs);
                if (this.playerList.isEmpty() && this.autoDestroy) {
                    SongEndEvent event = new SongEndEvent(this);
//                    MyNBSPlayer.onMusicStop();
                    this.destroy();
                }

            }
        }
    }

    public byte getVolume() {
        return this.volume;
    }

    public void setVolume(byte volume) {
        this.volume = volume;
    }

    public Song getSong() {
        return this.song;
    }
}
