//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.xxmicloxx.NoteBlockAPI;

public class Note {
    private byte instrument;
    private byte key;

    public Note(byte instrument, byte key) {
        this.instrument = instrument;
        this.key = key;
    }

    public byte getInstrument() {
        return this.instrument;
    }

    public void setInstrument(byte instrument) {
        this.instrument = instrument;
    }

    public byte getKey() {
        return this.key;
    }

    public void setKey(byte key) {
        this.key = key;
    }
}
