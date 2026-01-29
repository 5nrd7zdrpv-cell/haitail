package com.efd.hytale.farmworld.daemon.config;

import java.util.Locale;

public final class ConfigValidator {
    private ConfigValidator() {}

    public static boolean sanitize(FarmWorldConfig cfg) {
        if (cfg == null) {
            return false;
        }

        boolean changed = false;

        if (cfg.farmWorld == null) {
            cfg.farmWorld = new FarmWorldConfig.FarmWorldSection();
            changed = true;
        }
        if (cfg.farmWorld.spawn == null) {
            cfg.farmWorld.spawn = new FarmWorldConfig.SpawnSection();
            changed = true;
        }

        if (cfg.farmWorld.name == null || cfg.farmWorld.name.isBlank()) {
            cfg.farmWorld.name = "farm";
            changed = true;
        }
        if (cfg.farmWorld.resetIntervalDays < 1) {
            cfg.farmWorld.resetIntervalDays = 7;
            changed = true;
        } else if (cfg.farmWorld.resetIntervalDays > 365) {
            cfg.farmWorld.resetIntervalDays = 365;
            changed = true;
        }

        if (cfg.protection == null) {
            cfg.protection = new FarmWorldConfig.ProtectionSection();
            changed = true;
        }
        if (cfg.protection.actions == null) {
            cfg.protection.actions = new FarmWorldConfig.ProtectionActions();
            changed = true;
        }
        if (cfg.protection.radius < 0) {
            cfg.protection.radius = 0;
            changed = true;
        } else if (cfg.protection.radius > 4096) {
            cfg.protection.radius = 4096;
            changed = true;
        }
        if (cfg.protection.bypassPermission == null || cfg.protection.bypassPermission.isBlank()) {
            cfg.protection.bypassPermission = "efd.farmworld.admin";
            changed = true;
        }

        if (cfg.combat == null) {
            cfg.combat = new FarmWorldConfig.CombatSection();
            changed = true;
        }
        if (cfg.combat.tagSeconds < 0) {
            cfg.combat.tagSeconds = 0;
            changed = true;
        }
        if (cfg.combat.penaltySeconds < 0) {
            cfg.combat.penaltySeconds = 0;
            changed = true;
        }
        if (cfg.combat.onQuit == null || cfg.combat.onQuit.isBlank()) {
            cfg.combat.onQuit = "NONE";
            changed = true;
        } else {
            String normalized = cfg.combat.onQuit.toUpperCase(Locale.ROOT);
            if (!normalized.equals("NONE") && !normalized.equals("KILL") && !normalized.equals("PENALTY")) {
                cfg.combat.onQuit = "NONE";
                changed = true;
            } else if (!cfg.combat.onQuit.equals(normalized)) {
                cfg.combat.onQuit = normalized;
                changed = true;
            }
        }

        if (cfg.daemon == null) {
            cfg.daemon = new FarmWorldConfig.DaemonSection();
            changed = true;
        }
        if (cfg.daemon.tcp == null) {
            cfg.daemon.tcp = new FarmWorldConfig.TcpSection();
            changed = true;
        }
        if (cfg.daemon.tcp.port < 1 || cfg.daemon.tcp.port > 65535) {
            cfg.daemon.tcp.port = 25575;
            changed = true;
        }
        if (cfg.daemon.tcp.password == null || cfg.daemon.tcp.password.isBlank()) {
            cfg.daemon.tcp.password = "change-me";
            changed = true;
        }
        if (cfg.daemon.tcp.maxConnections < 1) {
            cfg.daemon.tcp.maxConnections = 32;
            changed = true;
        }
        if (cfg.daemon.tcp.maxLineLength < 64 || cfg.daemon.tcp.maxLineLength > 8192) {
            cfg.daemon.tcp.maxLineLength = 2048;
            changed = true;
        }
        if (cfg.daemon.tcp.readTimeoutMillis < 0) {
            cfg.daemon.tcp.readTimeoutMillis = 5000;
            changed = true;
        }
        if (cfg.daemon.tcp.commandRateLimitPerMinute < 1) {
            cfg.daemon.tcp.commandRateLimitPerMinute = 120;
            changed = true;
        }

        return changed;
    }
}
