package com.azazo1.dragonpractice;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 用于游戏进行时从外部配置物品
 */
public class ItemLoader {
    protected final File loadFile;

    /**
     * @throws IOException 物品配置文件不存在时无法创建物品配置文件
     */
    public ItemLoader(String loadFile) throws IOException {
        this.loadFile = new File(loadFile);
        if (!this.loadFile.exists()) {
            boolean created = this.loadFile.createNewFile();
            if (created) {
                MyLog.i("新建了物品配置文件: " + this.loadFile.toPath());
            } else {
                throw new IOException("Failed to create item config file");
            }
        }
    }

    public LinkedList<ItemStack> load() {
        LinkedList<ItemStack> items = new LinkedList<>();
        try (FileInputStream fis = new FileInputStream(loadFile)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            Stream<String> lines = br.lines();
            boolean keyLine = true; // 是否是表示物品id的行，否：为物品数量行
            String key = null; // 物品 id
            int count; // 物品数量
            for (String line : lines.toList()) {
                if (keyLine) {
                    key = line;
                } else {
                    if (key == null) { // 上一行没读到物品id
                        throw new IllegalArgumentException("Item id is null");
                    }
                    count = Integer.parseInt(line);
                    items.add(new ItemStack(Objects.requireNonNull(Material.matchMaterial(key)), count));
                    key = null;
                }
                keyLine = !keyLine;
            }
        } catch (FileNotFoundException ignore) {
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return items;
    }
}
