//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.xxmicloxx.NoteBlockAPI;

import java.io.File;
import java.util.HashMap;

public class Song {
    private HashMap<Integer, Layer> layerHashMap;
    private short songHeight;
    private short length;
    private String title;
    private File path;
    private String author;
    private String description;
    private float speed;
    private float delay;

    public Song(Song other) {
        this.speed = other.getSpeed();
        this.delay = 20.0F / this.speed;
        this.layerHashMap = other.getLayerHashMap();
        this.songHeight = other.getSongHeight();
        this.length = other.getLength();
        this.title = other.getTitle();
        this.author = other.getAuthor();
        this.description = other.getDescription();
        this.path = other.getPath();
    }

    public Song(float speed, HashMap<Integer, Layer> layerHashMap, short songHeight, short length, String title, String author, String description, File path) {
        this.speed = speed;
        this.delay = 20.0F / speed;
        this.layerHashMap = layerHashMap;
        this.songHeight = songHeight;
        this.length = length;
        this.title = title;
        this.author = author;
        this.description = description;
        this.path = path;
    }

    public HashMap<Integer, Layer> getLayerHashMap() {
        return this.layerHashMap;
    }

    public short getSongHeight() {
        return this.songHeight;
    }

    public short getLength() {
        return this.length;
    }

    public String getTitle() {
        return this.title;
    }

    public String getAuthor() {
        return this.author;
    }

    public File getPath() {
        return this.path;
    }

    public String getDescription() {
        return this.description;
    }

    public float getSpeed() {
        return this.speed;
    }

    public float getDelay() {
        return this.delay;
    }
}
