package com.efd.hytale.farmworld.daemon.config;

import java.time.Instant;

public final class FarmWorldConfig {

    public FarmWorldSection farmWorld = new FarmWorldSection();
    public long lastResetEpochSeconds = 0;

    public ProtectionSection protection = new ProtectionSection();
    public CombatSection combat = new CombatSection();

    public DaemonSection daemon = new DaemonSection();

    public static FarmWorldConfig defaultConfig() {
        FarmWorldConfig c = new FarmWorldConfig();

        c.farmWorld.name = "farm";
        c.farmWorld.resetIntervalDays = 7;
        c.farmWorld.resetAt = "SUN 04:00";
        c.farmWorld.seed = "";

        c.farmWorld.spawn.x = 0;
        c.farmWorld.spawn.y = 80;
        c.farmWorld.spawn.z = 0;

        c.farmWorld.spawn.prefabPath = "prefabs/farm_spawn.prefab";
        c.farmWorld.spawn.prefabOffsetX = 0;
        c.farmWorld.spawn.prefabOffsetY = 0;
        c.farmWorld.spawn.prefabOffsetZ = 0;

        c.lastResetEpochSeconds = Instant.now().getEpochSecond();

        // Protection defaults
        c.protection.enabled = true;
        c.protection.radius = 64;
        c.protection.bypassPermission = "efd.farmworld.admin";
        c.protection.actions.place = true;
        c.protection.actions.breakBlock = true;
        c.protection.actions.interact = true;
        c.protection.actions.damage = true;
        c.protection.actions.explosion = true;
        c.protection.actions.fireSpread = true;
        c.protection.actions.liquid = true;

        // Combat defaults
        c.combat.enabled = true;
        c.combat.tagSeconds = 20;
        c.combat.onQuit = "NONE"; // NONE | KILL | PENALTY
        c.combat.penaltySeconds = 30;

        c.daemon.tcp.enabled = false;
        c.daemon.tcp.port = 25575;
        c.daemon.tcp.password = "change-me";
        c.daemon.tcp.maxConnections = 32;
        c.daemon.tcp.maxLineLength = 2048;
        c.daemon.tcp.readTimeoutMillis = 5000;
        c.daemon.tcp.commandRateLimitPerMinute = 120;

        return c;
    }

    public static final class FarmWorldSection {
        public String name = "farm";
        public int resetIntervalDays = 7;
        public String resetAt = "SUN 04:00";
        public String seed = "";
        public SpawnSection spawn = new SpawnSection();
    }

    public static final class SpawnSection {
        public int x = 0;
        public int y = 80;
        public int z = 0;

        public String prefabPath = "prefabs/farm_spawn.prefab";
        public int prefabOffsetX = 0;
        public int prefabOffsetY = 0;
        public int prefabOffsetZ = 0;
    }

    public static final class ProtectionSection {
        public boolean enabled = true;
        public int radius = 64;
        public String bypassPermission = "efd.farmworld.admin";
        public ProtectionActions actions = new ProtectionActions();
    }

    public static final class ProtectionActions {
        public boolean place = true;
        public boolean breakBlock = true;
        public boolean interact = true;
        public boolean damage = true;
        public boolean explosion = true;
        public boolean fireSpread = true;
        public boolean liquid = true;
    }

    public static final class CombatSection {
        public boolean enabled = true;
        public int tagSeconds = 20;
        public String onQuit = "NONE"; // NONE | KILL | PENALTY
        public int penaltySeconds = 30;
    }

    public static final class DaemonSection {
        public TcpSection tcp = new TcpSection();
    }

    public static final class TcpSection {
        public boolean enabled = false;
        public int port = 25575;
        public String password = "change-me";
        public int maxConnections = 32;
        public int maxLineLength = 2048;
        public int readTimeoutMillis = 5000;
        public int commandRateLimitPerMinute = 120;
    }
}
