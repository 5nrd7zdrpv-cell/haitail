package com.efd.hytale.farmworld.daemon.commands.impl;

import com.efd.hytale.farmworld.daemon.commands.ICommand;
import com.efd.hytale.farmworld.daemon.config.ConfigManager;
import com.efd.hytale.farmworld.daemon.config.FarmWorldConfig;

import java.util.List;
import java.util.function.Supplier;

public final class FarmSetSpawnCommand implements ICommand {
    private final Supplier<FarmWorldConfig> cfg;
    private final ConfigManager configManager;
    private final String configFile;

    public FarmSetSpawnCommand(Supplier<FarmWorldConfig> cfg, ConfigManager configManager, String configFile) {
        this.cfg = cfg; this.configManager = configManager; this.configFile = configFile;
    }

    @Override public String root() { return "farm"; }
    @Override public List<String> path() { return List.of("setspawn"); }
    @Override public String help() { return "Sets spawn: farm setspawn <x> <y> <z>"; }

    @Override public String execute(String[] args) {
        if (args.length < 3) return "Usage: farm setspawn <x> <y> <z>\n";
        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);

            FarmWorldConfig c = cfg.get();
            c.farmWorld.spawn.x = x;
            c.farmWorld.spawn.y = y;
            c.farmWorld.spawn.z = z;
            configManager.save(configFile, c);

            return "[FarmWorld] Spawn saved -> (" + x + "," + y + "," + z + ")\n";
        } catch (NumberFormatException nfe) {
            return "Invalid coordinates. Expected integers.\n";
        }
    }
}
