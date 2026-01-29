package com.efd.hytale.farmworld.daemon;

import com.efd.hytale.farmworld.daemon.commands.CommandManager;
import com.efd.hytale.farmworld.daemon.commands.impl.*;
import com.efd.hytale.farmworld.daemon.config.ConfigManager;
import com.efd.hytale.farmworld.daemon.config.FarmWorldConfig;
import com.efd.hytale.farmworld.daemon.services.*;

import com.efd.hytale.farmworld.daemon.tcp.TcpCommandServer;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Scanner;

public final class Main {

    private static final String CONFIG_FILE = "farmworld.json";

    public static void main(String[] args) {
        Path dataDir = Path.of(".data");
        ConfigManager configManager = new ConfigManager(dataDir.resolve("config"));
        FarmWorldConfig config = configManager.loadOrCreateDefault(CONFIG_FILE, FarmWorldConfig.class, FarmWorldConfig::defaultConfig);

        ZoneId zone = ZoneId.systemDefault();

        FarmWorldService farm = new FarmWorldService(configManager, CONFIG_FILE, dataDir.resolve("worlds"), zone);
        farm.start(config);

        ProtectionService protection = new ProtectionService(() -> config);

        CombatService combat = new CombatService(() -> config);

        CommandManager commands = new CommandManager();
        commands.register(new FarmResetNowCommand(farm));
        commands.register(new FarmResetScheduleCommand(() -> config, farm, zone));
        commands.register(new FarmSetSpawnCommand(() -> config, configManager, CONFIG_FILE));
        commands.register(new FarmStatusCommand(farm));

        commands.register(new ProtectStatusCommand(protection));
        commands.register(new ProtectTestCommand(() -> config, protection));

        // Combat commands
        commands.register(new CombatTagCommand(() -> config, combat));
        commands.register(new CombatStatusCommand(combat));
        commands.register(new CombatCanWarpCommand(combat));
        commands.register(new CombatQuitCommand(() -> config, combat));
        commands.register(new CombatCleanupCommand(combat));

        TcpCommandServer tcp = null;
        if (config.daemon.tcp.enabled) {
            tcp = new TcpCommandServer(config.daemon.tcp, (line) -> {
                String trimmed = line == null ? "" : line.trim();
                if (trimmed.isBlank()) return "";
                String[] parts = trimmed.split("\s+");
                String root = parts[0].toLowerCase();
                String remainder = trimmed.substring(root.length()).trim();
                return commands.dispatchWithOutput(root, remainder);
            });
            tcp.start();
            System.out.println("[FarmWorld] TCP on :" + config.daemon.tcp.port);
        }

        System.out.println("[FarmWorld] ready (Step 4.1). tip: 'exit'");

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String line = sc.nextLine();
            if (line == null) break;
            line = line.trim();
            if (line.equalsIgnoreCase("exit")) break;
            if (line.isBlank()) continue;

            String[] parts = line.split("\s+");
            String root = parts[0].toLowerCase();
            String remainder = line.substring(root.length()).trim();

            String out = commands.dispatchWithOutput(root, remainder);
            if (out != null && !out.isBlank()) System.out.print(out);
        }

        if (tcp != null) tcp.stop();
        farm.stop();
        System.out.println("[FarmWorld] bye");
    }
}
