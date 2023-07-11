//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.xxmicloxx.NoteBlockAPI;

import java.io.*;
import java.util.HashMap;

public class NBSDecoder {
    public NBSDecoder() {
    }

    public static Song parse(File decodeFile) {
        try {
            return parse(new FileInputStream(decodeFile), decodeFile);
        } catch (FileNotFoundException var2) {
            var2.printStackTrace();
            return null;
        }
    }

    public static Song parse(InputStream inputStream) {
        return parse(inputStream, null);
    }

    private static Song parse(InputStream inputStream, File decodeFile) {
        HashMap<Integer, Layer> layerHashMap = new HashMap<>();

        try {
            DataInputStream dis = new DataInputStream(inputStream);
            short length = readShort(dis);
            short songHeight = readShort(dis);
            String title = readString(dis);
            String author = readString(dis);
            readString(dis);
            String description = readString(dis);
            float speed = (float) readShort(dis) / 100.0F;
            dis.readBoolean();
            dis.readByte();
            dis.readByte();
            readInt(dis);
            readInt(dis);
            readInt(dis);
            readInt(dis);
            readInt(dis);
            readString(dis);
            short tick = -1;

            while (true) {
                short i = readShort(dis);
                if (i == 0) {
                    for (i = 0; i < songHeight; ++i) {
                        Layer l = layerHashMap.get((int) i);
                        if (l != null) {
                            l.setName(readString(dis));
                            l.setVolume(dis.readByte());
                        }
                    }

                    return new Song(speed, layerHashMap, songHeight, length, title, author, description, decodeFile);
                }

                tick = (short) (tick + i);
                short layer = -1;

                while (true) {
                    short jumpLayers = readShort(dis);
                    if (jumpLayers == 0) {
                        break;
                    }

                    layer += jumpLayers;
                    setNote(layer, tick, dis.readByte(), dis.readByte(), layerHashMap);
                }
            }
        } catch (IOException var15) {
            var15.printStackTrace();
        }

        return null;
    }

    private static void setNote(int layer, int ticks, byte instrument, byte key, HashMap<Integer, Layer> layerHashMap) {
        Layer l = layerHashMap.get(layer);
        if (l == null) {
            l = new Layer();
            layerHashMap.put(layer, l);
        }

        l.setNote(ticks, new Note(instrument, key));
    }

    private static short readShort(DataInputStream dis) throws IOException {
        int byte1 = dis.readUnsignedByte();
        int byte2 = dis.readUnsignedByte();
        return (short) (byte1 + (byte2 << 8));
    }

    private static int readInt(DataInputStream dis) throws IOException {
        int byte1 = dis.readUnsignedByte();
        int byte2 = dis.readUnsignedByte();
        int byte3 = dis.readUnsignedByte();
        int byte4 = dis.readUnsignedByte();
        return byte1 + (byte2 << 8) + (byte3 << 16) + (byte4 << 24);
    }

    private static String readString(DataInputStream dis) throws IOException {
        int length = readInt(dis);

        StringBuilder sb;
        for (sb = new StringBuilder(length); length > 0; --length) {
            char c = (char) dis.readByte();
            if (c == '\r') {
                c = ' ';
            }

            sb.append(c);
        }

        return sb.toString();
    }
}
