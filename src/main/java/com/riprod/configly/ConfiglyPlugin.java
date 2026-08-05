package com.riprod.configly;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public final class ConfiglyPlugin extends JavaPlugin {
    public ConfiglyPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        Configly.install();
    }
}
