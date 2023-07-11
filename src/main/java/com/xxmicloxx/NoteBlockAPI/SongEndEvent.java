//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.xxmicloxx.NoteBlockAPI;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class SongEndEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private SongPlayer song;

    public SongEndEvent(SongPlayer song) {
        this.song = song;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public SongPlayer getSongPlayer() {
        return this.song;
    }

    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
