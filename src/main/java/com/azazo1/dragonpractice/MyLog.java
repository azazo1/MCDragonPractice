package com.azazo1.dragonpractice;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class MyLog {
    public static void i(String message) {
        Bukkit.getLogger().info(message);
        Bukkit.broadcast(Component.text(message));
    }

    public static void e(String message) {
        Bukkit.getLogger().info(message);
        Bukkit.broadcast(Component.text(message).color(TextColor.color(255, 0, 0)));
    }

    /**
     * 用特定字符包裹内容达到如下效果
     * "aaaaaa" -> "-----aaaaaa-----"
     */
    public static @NotNull String warpWith(String content, @NotNull String wrapChar, int totalLength) {
        if (wrapChar.length() != 1) {
            throw new IllegalArgumentException("wrapChar length must be 1");
        }
        StringBuilder sb = new StringBuilder(content);
        for (int w = 0; getDisplayLength(sb.toString()) < totalLength; w++) {
            sb.insert(w % 2 == 0 ? 0 : sb.length(), wrapChar);
        }
        return sb.toString();
    }

    /**
     * 用特定字符在内容两边放置，达到如下效果
     * "aaaaaa" -> "-    aaaaaa    -"
     */
    public static @NotNull String placeBorder(String content, @NotNull String wrapChar, int totalLength) {
        if (wrapChar.length() != 1) {
            throw new IllegalArgumentException("wrapChar length must be 1");
        }
        String center = warpWith(content, " ", totalLength);
        if (center.startsWith(" ")) {
            center = wrapChar + center.substring(1);
        }
        if (center.endsWith(" ")) {
            center = center.substring(0, center.length() - 1) + wrapChar;
        }
        return center;
    }

    /**
     * 考虑中文字体占两格宽度
     */
    @Contract(pure = true)
    public static int getDisplayLength(@NotNull String content) {
        int rst = 0;
        for (char c : content.toCharArray()) {
            rst += (c >= 0x0391 && c <= 0xFFE5) ? 2 : 1;  //中文字符
        }
        return rst;
    }
}
