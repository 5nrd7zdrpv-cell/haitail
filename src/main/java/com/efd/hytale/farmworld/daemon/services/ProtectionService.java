package com.efd.hytale.farmworld.daemon.services;

import com.efd.hytale.farmworld.daemon.config.FarmWorldConfig;

import java.util.Objects;
import java.util.function.Supplier;

public final class ProtectionService {

    // TODO: echte Hytale Events hier drauf verdrahten (BlockPlace/Break/Interact/Damage)


    public enum Action {
        PLACE,
        BREAK,
        INTERACT,
        DAMAGE,
        EXPLOSION,
        FIRE_SPREAD,
        LIQUID
    }

    public record Decision(boolean blocked, String reason) {}

    private final Supplier<FarmWorldConfig> cfg;

    public ProtectionService(Supplier<FarmWorldConfig> cfg) {
        this.cfg = Objects.requireNonNull(cfg, "cfg");
    }

    public Decision isBlocked(Action action, String worldName, int x, int y, int z, boolean hasBypassPermission) {
        FarmWorldConfig c = cfg.get();
        if (c == null || c.protection == null) return new Decision(false, "");
        if (!c.protection.enabled) return new Decision(false, "");
        if (hasBypassPermission) return new Decision(false, "bypass");

        if (worldName == null || !worldName.equalsIgnoreCase(c.farmWorld.name)) {
            return new Decision(false, "");
        }

        if (!isActionBlockedByConfig(c, action)) {
            return new Decision(false, "");
        }

        int sx = c.farmWorld.spawn.x;
        int sz = c.farmWorld.spawn.z;

        int r = Math.max(0, c.protection.radius);

        long dx = (long) x - sx;
        long dz = (long) z - sz;
        long dist2 = dx * dx + dz * dz;

        long r2 = (long) r * r;
        if (dist2 <= r2) {
            return new Decision(true, "spawn-safezone radius=" + r);
        }

        return new Decision(false, "");
    }

    private static boolean isActionBlockedByConfig(FarmWorldConfig c, Action a) {
        FarmWorldConfig.ProtectionActions act = c.protection.actions;
        if (act == null) return true;
        return switch (a) {
            case PLACE -> act.place;
            case BREAK -> act.breakBlock;
            case INTERACT -> act.interact;
            case DAMAGE -> act.damage;
            case EXPLOSION -> act.explosion;
            case FIRE_SPREAD -> act.fireSpread;
            case LIQUID -> act.liquid;
        };
    }
}
