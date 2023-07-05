package com.azazo1.dragonpractice.progress;

import com.azazo1.dragonpractice.ItemLoader;
import com.azazo1.dragonpractice.MyLog;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class BeforeStart extends Progress implements Listener {
    HashMap<Player, Boolean> players = new HashMap<>(); // 用以标记玩家是否准备
    Block switchBlock; // 用于标记准备按钮（拉杆）
    World overworld; // 主世界
    World endWorld; // 临时创建的末地
    Location spawnLocation; // 重生点
    protected final AtomicLong countdownTime = new AtomicLong(System.currentTimeMillis()); // 倒计时开始时间
    protected final AtomicBoolean countdown = new AtomicBoolean(false); // 是否正在倒计;
    protected static final int countdownDuration = 10000; // 倒计时时长

    protected final AtomicBoolean alive = new AtomicBoolean(true);
    protected final ItemLoader itemLoader = new ItemLoader("items.txt");


    public BeforeStart(JavaPlugin plugin) throws IOException {
        super(plugin);
    }

    /**
     * 当玩家都准备好时，
     * 恢复玩家的血量饥饿值和药水效果, 设置游戏模式为生存，
     * 清空所有人背包，并给予他们必需装备。10秒后开始战斗
     */
    @Override
    public void update() {
        if (!alive.get()) {
            return;
        }

        boolean allPrepared = players.size() != 0;
        for (Player player : players.keySet()) {
            if (!players.get(player)) {
                allPrepared = false;
                break;
            }
        }
        if (allPrepared && !countdown.get()) { // 所有人准备了，开始倒计时
            MyLog.i("开始倒计时");
            startCountDown();
            recoverPlayers();
            initPlayersBag();
        } else if (countdown.get() && !allPrepared) { // 正在倒计时，但是有人取消准备
            MyLog.i("有人取消准备，倒计时结束");
            stopCountDown();
        }
        long deltaTime = System.currentTimeMillis() - countdownTime.get();
        if (countdown.get() && deltaTime < countdownDuration) { // 倒计时中
            MyLog.i("倒计时：%ds".formatted(10 - (deltaTime) / 1000));
        } else if (countdown.get() && deltaTime >= countdownDuration) { // 倒计时结束
            MyLog.i("倒计时结束");
            stopCountDown();
            toggleTo(new Fighting(plugin));
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::update, 20); // 循环
    }

    /**
     * 停止倒计时
     */
    protected void stopCountDown() {
        countdown.set(false);
    }

    /**
     * 开始倒计时
     */
    protected void startCountDown() {
        countdownTime.set(System.currentTimeMillis());
        countdown.set(true);
    }

    @Override
    public void toggleTo(Progress nextProgress) {
        if (!alive.get()) {
            MyLog.e("该 BeforeStart 状态已关闭！");
            return;
        }
        if (!(nextProgress instanceof Fighting fightingProgress)) {
            throw new IllegalArgumentException("BeforeStart can only be toggled to Fighting");
        }
        afterToggle();
        fightingProgress.init(this);
        fightingProgress.beforeToggle();
    }

    /**
     * 将所有玩家设置为冒险模式<br>
     * 设置死亡不掉落<br>
     * 将世界出生点设置为 x=0 z=0<br>
     * 放置世界位置 x=0 z=0 处的拉杆<br>
     * 让玩家通过点击世界位置 x=0 z=0 处的拉杆进行准备<br>
     * 让玩家通过再次点击世界位置 x=0 z=0 处的拉杆取消准备<br>
     */
    @Override
    protected void beforeToggle() {
        if (!alive.get()) {
            MyLog.e("该 BeforeStart 状态已关闭！");
            return;
        }

        players.clear();

        overworld = Bukkit.getWorld("world");
        if (overworld == null) {
            MyLog.e("找不到主世界");
            return;
        } else {
            MyLog.i("找到主世界");
        }
        overworld.setGameRule(GameRule.KEEP_INVENTORY, true);

        spawnLocation = new Location(overworld, 0, overworld.getHighestBlockYAt(0, 0) + 1, 0);
        if (spawnLocation.getBlock().getType() != Material.LEVER) {
            spawnLocation = new Location(spawnLocation.getWorld(), spawnLocation.getX(), spawnLocation.getY() + 1, spawnLocation.getZ());
        }
        if (!overworld.setSpawnLocation(spawnLocation)) {
            MyLog.e("无法设置世界出生点！");
            return;
        } else {
            MyLog.i("出生点设置完成");
        }
        Bukkit.setDefaultGameMode(GameMode.ADVENTURE);

        switchBlock = overworld.getBlockAt(spawnLocation);
        switchBlock.setType(Material.LEVER);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        update();// 启动循环
    }

    /**
     * 重置末地世界<br>
     * 将所有玩家传送到末地，并设置重生点<br>
     */
    @Override
    protected void afterToggle() {
        if (!alive.get()) {
            MyLog.e("该 BeforeStart 状态已关闭！");
            return;
        }
        resetEndWorld();
        teleportPlayer();
        alive.set(false);
    }

    /**
     * 将所有玩家传送到末地，并设置重生点
     */
    protected void teleportPlayer() {
        players.keySet().forEach(player -> {
            Location endSpawnLocation = new Location(
                    endWorld.getSpawnLocation().getWorld(),
                    endWorld.getSpawnLocation().getX() + 1, endWorld.getSpawnLocation().getY(), endWorld.getSpawnLocation().getZ() + 1
            );
            player.teleport(endSpawnLocation);
            player.setBedSpawnLocation(endSpawnLocation, true);
            MyLog.i("传送玩家 %s 至末地, 并设置重生点".formatted(player.getName()));
        });
    }

    /**
     * 恢复玩家的血量饥饿值和药水效果, 设置游戏模式为生存
     */
    protected void recoverPlayers() {
        players.keySet().forEach(player -> {
            player.setGameMode(GameMode.SURVIVAL);
            player.setFoodLevel(20); // 饱食度
            player.setSaturation(20); //饥饿度;
            player.setHealth(20);
            player.setExhaustion(0);
            player.clearActivePotionEffects();
            MyLog.i("恢复了玩家 %s 的血量饥饿值，饱食度，疲劳度和药水效果, 设置游戏模式为生存".formatted(player.getName()));
        });
    }

    /**
     * 清空玩家背包并给予装备
     */
    protected void initPlayersBag() {
        LinkedList<ItemStack> items = itemLoader.load();
        players.keySet().forEach(player -> {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.getInventory().setContents(new ItemStack[36]);
            player.getInventory().setArmorContents(new ItemStack[4]);
            MyLog.i("清空了玩家 %s 的物品".formatted(player.getName()));
            for (ItemStack item : items) {
                player.getInventory().addItem(item);
            }
            MyLog.i("给予了玩家 %s 物品".formatted(player.getName()));
        });
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!alive.get()) {
            return;
        }
        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        if (clicked != null && clicked.getType() == switchBlock.getType()) {
            StringBuilder sb = new StringBuilder();
            if (!players.getOrDefault(player, false)) { // 未准备
                players.put(player, true);
                sb.append(player.getName()).append(" 准备完毕, 未准备者: ");
            } else { // 已准备
                players.put(player, false);
                sb.append(player.getName()).append(" 取消准备, 未准备者: ");
            }
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (!players.getOrDefault(p, false)) {
                    sb.append(p.getName()).append("\n");
                }
            });
            MyLog.i(sb.toString());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!alive.get()) {
            return;
        }
        if (!players.containsKey(event.getPlayer())) {
            players.put(event.getPlayer(), false);
        }

        Player player = event.getPlayer();
        player.setBedSpawnLocation(spawnLocation, true);
        player.teleport(spawnLocation);
        player.sendMessage(Component.text("已重设你的出生点至 x=0 z=0"));
        player.setGameMode(GameMode.ADVENTURE);
    }

    /**
     * 重置末地
     */
    public void resetEndWorld() {
        Bukkit.unloadWorld("temp_end", false);
        Path tempEndDir = Bukkit.getWorldContainer().toPath().resolve("temp_end");
        try { // 删除temp_end文件夹
            Files.walkFileTree(tempEndDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    file.toFile().delete();
                    plugin.getLogger().info("删除文件: " + file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) { // 删除空文件夹
                    dir.toFile().delete();
                    plugin.getLogger().info("删除文件夹: " + dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        WorldCreator worldCreator = WorldCreator.name("temp_end");
        worldCreator.environment(World.Environment.THE_END);
        endWorld = Bukkit.createWorld(worldCreator);
        MyLog.i("末地已重置");
        endWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
        MyLog.i("末地死亡不掉落已设置");
    }
}
