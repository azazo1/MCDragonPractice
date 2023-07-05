package com.azazo1.dragonpractice.progress;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * 进程状态机: 开始前，进行时，结束后
 */
public abstract class Progress {
    protected JavaPlugin plugin;

    public Progress(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 做一些定期要处理的任务
     */
    public abstract void update();

    /**
     * 切换到下一个状态<br>
     * 子类要判断下一个状态是否可被切换到<br>
     * 此方法用于进行状态间的相互信息传递，状态开始前和状态开始后的相应处理
     */
    public abstract void toggleTo(Progress nextProgress);

    /**
     * 此状态作为将被切换到的状态时，该方法被上一个状态的 {@link #toggleTo} 调用<br>
     * 若此方法为游戏开始时第一个状态，则该方法由 {@link ProgressManager#toggleTo(Progress)} 调用
     */
    protected abstract void beforeToggle();

    /**
     * 此状态作为将被切换走的状态时，该方法被该状态的{@link #toggleTo}调用
     */
    protected abstract void afterToggle();
}
