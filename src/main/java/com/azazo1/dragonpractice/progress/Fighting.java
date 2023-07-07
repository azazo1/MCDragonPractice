package com.azazo1.dragonpractice.progress;

import com.azazo1.dragonpractice.MyLog;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Fighting extends Progress implements Listener, CommandExecutor {
    protected BeforeStart lastProgress;
    protected final AtomicLong sessionStartTime = new AtomicLong(System.currentTimeMillis());
    protected final LeftTimeBossBar leftTimeBossBar = new LeftTimeBossBar();
    protected static final long fightDuration = 1000 * 60 * 15; // 本局战斗最长的时长
    protected static final int healthIncreasePerDeath = 50; // 玩家每次死亡末影龙增加的血量
    protected static final int maxPlayerLife = 5; // 玩家最大生命数量
    protected static final int sucWaitingSeconds = 30; // 玩家对局成功后在末地等待的时间
    protected static final int irritateEnderManHealth = 10; // 末影龙濒危血量阈值，末影龙在低于该血量时会号召所有末影人让他们朝玩家攻击
    protected static final int irritateEnderManAmountPerPlayer = 5; // 对于每个玩家，末影龙号召末影人的数量
    protected static final int fireBallIntervalSeconds = 5; // 末影龙在濒危血量下每发火球发射间隔时间
    protected static final int fireBallVelocity = 5; // 末影龙在濒危血量下发射的火球的飞行速度 m/s
    protected HashMap<Player, Integer> playerLife = new HashMap<>(); // 玩家剩余生命数量
    protected HashMap<Player, Double> playerDamageToEnderDragon = new HashMap<>(); // 玩家对末影龙造成的积累伤害
    protected HashMap<Player, Double> playerGetDamage = new HashMap<>(); // 玩家受伤统计
    protected final AtomicBoolean alive = new AtomicBoolean(true);
    protected final AtomicBoolean fightOver = new AtomicBoolean(false); // 对战是否结束（即胜负是否已分）
    protected final AtomicBoolean enderManIrritated = new AtomicBoolean(false); // 是否惹怒了末影人（一场对局末影龙发动一次）
    protected World endWorld; // 临时创建的末地
    protected EnderDragon enderDragon; //末影龙
    protected Location spawnLocationInOverWorld; // 主世界的重生点
    protected double enderDragonMaxHealth; // 末影龙最大血量
    protected FightResult result;
    protected HashSet<Enderman> freeOfDragonBreath = new HashSet<>(); // 免受龙息伤害的末影人（末影龙号召的末影人不会受龙息伤害）

    public Fighting(JavaPlugin plugin) {
        super(plugin);
    }

    public void init(BeforeStart lastProgress) {
        if (!alive.get()) {
            MyLog.e("本 Fighting 状态已结束");
            return;
        }
        this.lastProgress = lastProgress;
        this.endWorld = lastProgress.endWorld;
        this.spawnLocationInOverWorld = lastProgress.spawnLocation;
        sessionStartTime.set(System.currentTimeMillis());
        lastProgress.players.keySet().forEach(player -> {
            playerLife.put(player, maxPlayerLife); // 初始化玩家生命数
            playerDamageToEnderDragon.put(player, 0.0); // 初始化玩家对末影龙造成的伤害
            playerGetDamage.put(player, 0.0); //  初始化玩家收到的伤害
        });
    }

    /**
     * 观察计时，达到15分钟而龙未死则对局失败，设置所有玩家出生点至主世界，杀死所有玩家
     * 观察末影龙状态，当末影龙死亡/15分钟已到/玩家生命耗尽时，结束对局，保存战绩
     * 记录玩家对末影龙造成的伤害
     * 记录是否胜利
     * 记录谁杀的龙
     * 记录玩家积累扣血数
     * 记录玩家死亡次数
     * 记录挑战时间
     */
    @Override
    public void update() {
        if (!alive.get()) {
            MyLog.e("本 Fighting 状态已结束");
            return;
        }
        leftTimeBossBar.update();
        if (System.currentTimeMillis() - sessionStartTime.get() > fightDuration) {
            MyLog.i("时间耗尽，对战结束");
            onFightEnd(false);
        }
        if (enderDragon != null) { // 原有的末影龙引用还在，但有可能真实的消失了
            EnderDragon dragon = endWorld.getEntitiesByClass(EnderDragon.class).stream().findFirst().orElse(null);  // 搜寻世界上的末影龙
            if (dragon == null) {
                // 找不到末影龙，说明末影龙消失了，即死亡了
                MyLog.i("末影龙被神秘力量杀死！");
                onFightEnd(true);
            } else {
                if (dragon.getHealth() < irritateEnderManHealth && !enderManIrritated.get()) {
                    irritateEnderMan();

                    Location location = enderDragon.getLocation();
                    location.setY(location.getY() + 50);
                    enderDragon.teleport(location);
                    MyLog.i("末影龙迅速飞升");
                    MyLog.i("末影龙发射火球频率增加，每 %d 秒发射一次".formatted(fireBallIntervalSeconds));
                    fireBallFrequently();
                }
            }
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::update, 20);
    }

    /**
     * 末影龙在极低生命值时会增加发射火球的频率
     */
    protected void fireBallFrequently() {
        if (!alive.get()) {
            MyLog.e("本 Fighting 状态已结束");
            return;
        }
        // 随机选择一个玩家
        Player randomChosen = (Player) playerLife.keySet().toArray()[new Random().nextInt(0, playerLife.size())];
        Location endLocation = randomChosen.getLocation();
        Location startLocation = enderDragon.getEyeLocation();
        // 取两个location的中点 防止火球被末影龙自己干掉
        Location middleLocation = new Location(endLocation.getWorld(), (endLocation.getX() + startLocation.getX()) / 2, (endLocation.getY() + startLocation.getY()) / 2, (endLocation.getZ() + startLocation.getZ()) / 2);
        Vector fireVector = new Vector(endLocation.getX() - startLocation.getX(), endLocation.getY() - startLocation.getY(), endLocation.getZ() - startLocation.getZ()).normalize();
        middleLocation.setDirection(fireVector);
        // 发射
        DragonFireball ball = (DragonFireball) endWorld.spawnEntity(middleLocation, EntityType.DRAGON_FIREBALL);
        ball.setVelocity(fireVector.multiply(fireBallVelocity));
        ball.setShooter(enderDragon);
        ball.setIsIncendiary(false);
        ball.setFireTicks(0);
        enderDragon.playSound(Sound.sound(org.bukkit.Sound.ENTITY_ENDER_DRAGON_AMBIENT, Sound.Source.AMBIENT, 640, 1));
        MyLog.i("末影龙发射火球！");
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::fireBallFrequently, 20 * fireBallIntervalSeconds);
    }

    /**
     * 末影龙进入超残血状态：
     * 1. 号召一定数量的末影人
     * 2. 取消栖息
     * 3. 增大喷火球的频率
     */
    protected void irritateEnderMan() {
        enderManIrritated.set(true);
        Collection<Enderman> enderMen = endWorld.getEntitiesByClass(Enderman.class);
        int expectedToCallAmount = playerLife.size() * irritateEnderManAmountPerPlayer;
        int calledAmount = 0;
        for (Enderman enderman : enderMen) {
            if (calledAmount >= expectedToCallAmount) {
                break;
            }
            Player randomChosen = (Player) playerLife.keySet().toArray()[new Random().nextInt(0, playerLife.size())];
            enderman.setScreaming(true);
            enderman.setHasBeenStaredAt(true);
            enderman.setTarget(randomChosen);
            freeOfDragonBreath.add(enderman); // 不受龙息伤害的末影人
            calledAmount++;
        }
        MyLog.i("末影龙号召 %d 个末影人攻击玩家！".formatted(calledAmount));
    }

    @Override
    public void toggleTo(Progress nextProgress) {
        if (!alive.get()) {
            MyLog.e("本 Fighting 状态已结束");
            return;
        }
        if (!(nextProgress instanceof BeforeStart beforeStart)) {
            throw new IllegalArgumentException("Fighting can only be toggled to BeforeStart");
        }
        afterToggle();
        beforeStart.beforeToggle();
    }

    /**
     * 创建 boss 条显示剩余时间<br>
     * 监听玩家死亡事件，当玩家死亡时，末影龙血量增加100，复活的玩家再次进入末地<br>
     * 记录玩家死亡次数，当玩家耗尽5次生命后，进入旁观模式并传送到末地<br>
     * 查找末影龙uuid并检测其状态<br>
     * 开始计时
     */
    @Override
    protected void beforeToggle() {
        if (!alive.get()) {
            MyLog.e("本 Fighting 状态已结束");
            return;
        }
        MyLog.i("你们已进入一级战备状态。加油，特种兵！");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        //noinspection DataFlowIssue
        Bukkit.getPluginCommand("stopfight").setExecutor(this);
        findEnderDragon();
        update();
    }

    /**
     * 关闭剩余时间Boss条<br>
     * 广播战绩<br>
     * 将所有玩家设置为冒险模式<br>
     */
    @Override
    protected void afterToggle() {
        if (!alive.get()) {
            MyLog.e("本 Fighting 状态已结束");
            return;
        }
        // 处理结算内容
        // 玩家对末影龙造成的伤害排行
        LinkedList<Player> playerListSortByDamageToDragon = new LinkedList<>(playerLife.keySet());
        playerListSortByDamageToDragon.sort((o1, o2) -> Double.compare(result.playerData.get(o1).playerDamageToEnderDragon, result.playerData.get(o2).playerDamageToEnderDragon));
        // 玩家收到伤害的排行
        LinkedList<Player> playerListSortByGetDamage = new LinkedList<>(playerLife.keySet());
        playerListSortByGetDamage.sort((o1, o2) -> Double.compare(result.playerData.get(o1).playerGetDamage, result.playerData.get(o2).playerGetDamage));
        // 玩家死亡数排行
        LinkedList<Player> playerListSortByDeathTimes = new LinkedList<>(playerLife.keySet());
        playerListSortByDeathTimes.sort((o1, o2) -> Integer.compare(result.playerData.get(o1).playerDeathTimes, result.playerData.get(o2).playerDeathTimes));

        int totalLength = 50;
        double fightTimeInSeconds = result.fightingTime * 1.0 / 1000;
        MyLog.i(MyLog.warpWith("对战结算: " + (result.suc ? "成功" : "失败"), "-", totalLength));
        MyLog.i(MyLog.warpWith("挑战时间: %.2f秒, 剩余挑战时间: %.2f秒".formatted(fightTimeInSeconds, fightDuration * 1.0 / 1000 - fightTimeInSeconds), "-", totalLength));

        if (result.whoBeat != null) {
            MyLog.i(MyLog.placeBorder("击杀末影龙玩家: %s".formatted(result.whoBeat.getName()), "|", totalLength));
        } else {
            MyLog.i(MyLog.placeBorder("击杀末影龙玩家: " + (result.suc ? "/神秘力量击杀/" : "[未击杀]"), "|", totalLength));
        }

        MyLog.i(MyLog.placeBorder("***累计对末影龙造成的伤害排行***", "|", totalLength));
        for (Player player : playerListSortByDamageToDragon) {
            MyLog.i(MyLog.placeBorder("%s : %.2f点".formatted(player.getName(), result.playerData.get(player).playerDamageToEnderDragon), "|", totalLength));
        }

        MyLog.i(MyLog.placeBorder("***累计受伤血量排行***", "|", totalLength));
        for (Player player : playerListSortByGetDamage) {
            MyLog.i(MyLog.placeBorder("%s : %.2f点".formatted(player.getName(), result.playerData.get(player).playerGetDamage), "|", totalLength));
        }

        MyLog.i(MyLog.placeBorder("***玩家死亡次数排名***", "|", totalLength));
        for (Player player : playerListSortByDeathTimes) {
            MyLog.i(MyLog.placeBorder("%s : %d次".formatted(player.getName(), result.playerData.get(player).playerDeathTimes), "|", totalLength));
        }
        MyLog.i(MyLog.warpWith("-", "-", totalLength));

        // 冒险模式在onFightEnd已设置

        // 关闭剩余时间boss条
        leftTimeBossBar.close();
        alive.set(false);
    }

    protected void findEnderDragon() {
        enderDragon = endWorld.getEntitiesByClass(EnderDragon.class).stream().findFirst().orElse(null);
        if (enderDragon != null) {
            MyLog.i("找到末影龙");
        } else {
            MyLog.e("无法找到末影龙");
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::findEnderDragon, 20);
            return;
        }
        enderDragonMaxHealth = enderDragon.getHealth();
        MyLog.i("末影龙最大血量: " + enderDragonMaxHealth);
    }

    /**
     * (对战胜利时)一段时间后将所有玩家传送回主世界
     */
    protected void postTeleportPlayers(int waitSeconds) {
        MyLog.i(waitSeconds + "秒后将所有人传送到主世界");
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
                        Bukkit.getOnlinePlayers().forEach(player ->
                                player.teleport(spawnLocationInOverWorld)),
                20L * waitSeconds);
    }

    /**
     * 让玩家变成冒险模式，重生点设置到主世界，击杀所有玩家
     */
    protected void killAllPlayers() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.setGameMode(GameMode.ADVENTURE);
            player.setBedSpawnLocation(spawnLocationInOverWorld, true);
            player.damage(player.getHealth());
        });
    }

    /**
     * 清空玩家背包
     */
    protected void clearPlayersBag() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.getInventory().setContents(new ItemStack[36]);
            player.getInventory().setArmorContents(new ItemStack[4]);
            MyLog.i("清空了玩家 %s 的物品".formatted(player.getName()));
        });
    }

    /**
     * 恢复玩家的血量，饥饿值，饱食度，疲劳度和药水效果, 设置游戏模式为冒险
     */
    protected void recoverPlayers() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.setGameMode(GameMode.ADVENTURE);
            player.setFoodLevel(20); // 饱食度
            player.setSaturation(20); // 饥饿度;
            player.setHealth(20);
            player.setExhaustion(0);
            player.clearActivePotionEffects();
            MyLog.i("恢复了玩家 %s 的血量，饥饿值，饱食度，疲劳度和药水效果, 设置游戏模式为冒险".formatted(player.getName()));
        });
    }

    /**
     * 在对局中加入的玩家进入观战模式
     */
    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent e) {
        if (!alive.get()) {
            return;
        }
        Player player = e.getPlayer();
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(endWorld.getSpawnLocation());
        MyLog.i("玩家 %s 进入观战".formatted(player.getName()));
    }

    /**
     * 记录玩家死亡次数, 增加末影龙血量
     */
    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent e) {
        if (!alive.get() || fightOver.get()) {
            return;
        }
        Player player = e.getPlayer();
        if (!playerLife.containsKey(player)) {
            return;
        }
        double health = enderDragon.getHealth();
        enderDragon.setHealth(Math.min(health + healthIncreasePerDeath, enderDragonMaxHealth)); // 增加末影龙生命
        MyLog.i("玩家 %s 死了，让末影龙增加血量 %d, 末影龙剩余血量: %d".formatted(player.getName(), healthIncreasePerDeath, (int) enderDragon.getHealth()));
        int life = playerLife.get(player);
        playerLife.replace(player, life - 1); // 计数
        MyLog.i("玩家 %s 剩余生命数: %d".formatted(player.getName(), playerLife.get(player)));
        if (playerLife.get(player) == 0) { // 耗尽生命，进入观战模式
            player.setGameMode(GameMode.SPECTATOR);
            MyLog.i("玩家 %s 进入观战模式".formatted(player.getName()));
            boolean allOver = true; // 是否所有玩家都耗尽生命
            for (Player p : playerLife.keySet()) {
                if (playerLife.get(p) > 0) {
                    allOver = false;
                    break;
                }
            }
            if (allOver) {
                MyLog.i("所有玩家生命都耗尽了，对战结束......");
                onFightEnd(false);
            }
        }
    }

    /**
     * 统计玩家收到的伤害<br>
     * 消除被号召末影人收到龙息的伤害
     */
    @EventHandler
    public void onEntityDamageEvent(@NotNull EntityDamageEvent e) {
        if (!alive.get()) {
            return;
        }
        double damage = e.getDamage();
        if (e.getEntity() instanceof Player player && playerGetDamage.containsKey(player)) {
            playerGetDamage.replace(player, playerGetDamage.get(player) + damage);
            double health = player.getHealth() - damage; // 这里似乎血量时延迟改变的这一帧仍然是扣血前的血量
            MyLog.i("玩家 %s 收到 %d 点伤害, 剩余血量: %d".formatted(player.getName(), (int) damage, (int) health));
        } else if (e.getEntity() instanceof Enderman enderman && freeOfDragonBreath.contains(enderman)) {
            e.setCancelled(true);
        }
    }

    /**
     * 统计玩家对末影龙造成的伤害
     */
    @EventHandler
    public void onEntityDamageByEntityEvent(@NotNull EntityDamageByEntityEvent e) {
        if (!alive.get()) {
            return;
        }
        double damage = e.getDamage();
        if (e.getDamager() instanceof Player player && playerDamageToEnderDragon.containsKey(player) && e.getEntity().getEntityId() == enderDragon.getEntityId()) {
            double health = enderDragon.getHealth() - damage; // 这里似乎血量时延迟改变的这一帧仍然是扣血前的血量
            MyLog.i("玩家 %s 对末影龙造成 %d 点伤害, 末影龙剩余血量: %d".formatted(player.getName(), (int) damage, (int) health));
            playerDamageToEnderDragon.replace(player, playerDamageToEnderDragon.get(player) + damage);
        } else if (e.getEntity().getEntityId() == enderDragon.getEntityId()) {
            double health = enderDragon.getHealth() - damage; // 这里似乎血量时延迟改变的这一帧仍然是扣血前的血量
            if (e.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player player) {
                MyLog.i("玩家 %s 用弓箭对末影龙造成 %d 点伤害, 末影龙剩余血量: %d".formatted(player.getName(), (int) damage, (int) health));
                playerDamageToEnderDragon.replace(player, playerDamageToEnderDragon.get(player) + damage);
            } else {
                MyLog.i("神秘力量对末影龙造成 %d 点伤害, 末影龙剩余血量: %d".formatted((int) damage, (int) health));
            }
        }
    }

    /**
     * 玩家退出直接标记为死亡
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if (playerLife.containsKey(player)) {
            playerLife.replace(player, 1); // 调用 onPlayerDeath 时生命数会将为0
            MyLog.e("玩家: %s 退出, 生命数降为0.");
            onPlayerDeath(new PlayerDeathEvent(player, new LinkedList<>(), 0, Component.text("玩家退出")));
        }
    }

    /**
     * 将重生的玩家传送到末地, 防止因方块阻挡而使复活点变成主世界
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        if (!alive.get() || fightOver.get()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> e.getPlayer().teleport(endWorld.getSpawnLocation()));
        MyLog.i("重生的 %s 被传送到末地".formatted(e.getPlayer().getName()));
    }

    /**
     * 检测末影龙死亡事件
     */
    @EventHandler
    public void onEntityDeath(@NotNull EntityDeathEvent e) {
        if (!alive.get()) {
            return;
        }
        if (e.getEntity().getEntityId() == enderDragon.getEntityId()) {
            Player killer = enderDragon.getKiller();
            if (killer == null) {
                MyLog.i("神秘玩家击杀末影龙, 末影龙死亡！！！");
            } else {
                MyLog.i("玩家 %s 击杀末影龙, 末影龙死亡！！！".formatted(killer.getName()));

            }
            onFightEnd(true);
        }
    }

    /**
     * 标志结束对战，保存战绩
     * 恢复玩家状态/击杀所有玩家
     * 将所有玩家传送回主世界
     *
     * @param suc 是否胜利
     */
    public void onFightEnd(boolean suc) {
        fightOver.set(true);
        if (suc) {
            MyLog.i("对战胜利");
            recoverPlayers();
            postTeleportPlayers(sucWaitingSeconds);
        } else {
            MyLog.i("对战失败");
            killAllPlayers();
            clearPlayersBag();
            postTeleportPlayers(0);
        }
        result = new FightResult(suc);
        MyLog.i("对局结算生成成功");
        try {
            toggleTo(new BeforeStart(plugin));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!alive.get()) {
            MyLog.e("该 Fighting 状态已关闭！");
            return true;
        }
        if (label.equals("stopfight")) {
            MyLog.i("%s 通过指令结束对战".formatted(sender.getName()));
            onFightEnd(false);
        }
        return false;
    }

    public class LeftTimeBossBar {

        BossBar bossBar;

        public LeftTimeBossBar() {
            bossBar = Bukkit.createBossBar("剩余时间", BarColor.RED, BarStyle.SOLID);
            Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
            bossBar.setVisible(true);
        }

        /**
         * 更新进度条
         */
        public void update() {
            if (!alive.get()) {
                MyLog.e("本 Fighting 状态已结束");
                return;
            }
            long deltaTime = System.currentTimeMillis() - sessionStartTime.get();
            try {
                bossBar.setProgress(1 - deltaTime * 1.0 / fightDuration);
            } catch (IllegalArgumentException ignore) {
            }
        }

        public void close() {
            if (!alive.get()) {
                MyLog.e("本 Fighting 状态已结束");
                return;
            }
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
    }

    public class FightResult {
        public final Player whoBeat = enderDragon.getKiller();
        public final boolean suc;
        public final HashMap<Player, ResultData> playerData = new HashMap<>();
        public final long fightingTime = System.currentTimeMillis() - sessionStartTime.get();

        public FightResult(boolean suc) {
            this.suc = suc;
            for (Player player : playerLife.keySet()) {
                playerData.put(player, new ResultData(
                        playerDamageToEnderDragon.get(player),
                        playerGetDamage.get(player),
                        playerLife.get(player)
                ));
            }
        }


        @Override
        public String toString() {
            return "FightResult{" +
                    "whoBeat=" + whoBeat +
                    ", suc=" + suc +
                    ", playerData=" + playerData +
                    ", fightingTime=" + fightingTime +
                    '}';
        }

        private class ResultData {
            public final double playerDamageToEnderDragon;
            public final double playerGetDamage;
            public final int playerDeathTimes;

            public ResultData(double playerDamageToEnderDragon, double playerGetDamage, int playerRestLife) {
                this.playerDamageToEnderDragon = playerDamageToEnderDragon;
                this.playerGetDamage = playerGetDamage;
                this.playerDeathTimes = maxPlayerLife - playerRestLife;
            }

            @Override
            public String toString() {
                return "ResultData{" +
                        "playerDamageToEnderDragon=" + playerDamageToEnderDragon +
                        ", playerGetDamage=" + playerGetDamage +
                        ", playerDeathTimes=" + playerDeathTimes +
                        '}';
            }
        }
    }
}
