package com.azazo1.dragonpractice;

import com.azazo1.dragonpractice.progress.BeforeStart;
import com.azazo1.dragonpractice.progress.ProgressManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class DragonPractice extends JavaPlugin {
    public ProgressManager progressManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        progressManager = new ProgressManager();
        try {
            progressManager.toggleTo(new BeforeStart(this));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
