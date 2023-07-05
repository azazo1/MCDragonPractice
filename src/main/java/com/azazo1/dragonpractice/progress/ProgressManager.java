package com.azazo1.dragonpractice.progress;

import org.bukkit.plugin.java.JavaPlugin;

public class ProgressManager {
    protected Progress progress;

    public ProgressManager() {

    }

    public void toggleTo(Progress progress) {
        if (this.progress == null) {
            this.progress = progress;
            progress.beforeToggle();
        } else {
            this.progress.toggleTo(progress);
        }
    }
}
