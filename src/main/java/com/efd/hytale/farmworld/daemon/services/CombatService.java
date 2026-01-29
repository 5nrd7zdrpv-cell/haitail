package com.efd.hytale.farmworld.daemon.services;

import com.efd.hytale.farmworld.daemon.config.FarmWorldConfig;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

// Combat tracking (Anti-Combat-Log)
// TODO: später aus Damage/Quit Events füttern
public final class CombatService {

    public record CombatState(long combatUntilEpochSeconds, long penaltyUntilEpochSeconds, String lastReason) {}

    private final Supplier<FarmWorldConfig> cfg;
    private final Map<String, CombatState> states = new ConcurrentHashMap<>();

    public CombatService(Supplier<FarmWorldConfig> cfg) {
        this.cfg = Objects.requireNonNull(cfg, "cfg");
    }

    /** Tag player in combat for configured seconds or overrideSeconds if > 0. */
    public void tag(String playerId, String reason, int overrideSeconds) {
        FarmWorldConfig c = cfg.get();
        if (c == null || c.combat == null || !c.combat.enabled) return;

        int sec = overrideSeconds > 0 ? overrideSeconds : Math.max(0, c.combat.tagSeconds);
        long now = Instant.now().getEpochSecond();
        long combatUntil = now + sec;

        CombatState prev = states.get(playerId);
        long penaltyUntil = prev == null ? 0 : prev.penaltyUntilEpochSeconds();

        // Extend combat time (max)
        long newCombatUntil = prev == null ? combatUntil : Math.max(prev.combatUntilEpochSeconds(), combatUntil);

        states.put(playerId, new CombatState(newCombatUntil, penaltyUntil, reason == null ? "" : reason));
    }

    public void tag(String playerId, String reason) {
        tag(playerId, reason, 0);
    }

    public boolean isInCombat(String playerId) {
        cleanupIfExpired(playerId);
        CombatState s = states.get(playerId);
        if (s == null) return false;
        long now = Instant.now().getEpochSecond();
        return s.combatUntilEpochSeconds() > now;
    }

    public int getRemainingSeconds(String playerId) {
        cleanupIfExpired(playerId);
        CombatState s = states.get(playerId);
        if (s == null) return 0;
        long now = Instant.now().getEpochSecond();
        long rem = s.combatUntilEpochSeconds() - now;
        return (int) Math.max(0, rem);
    }

    public boolean hasPenalty(String playerId) {
        cleanupIfExpired(playerId);
        CombatState s = states.get(playerId);
        if (s == null) return false;
        long now = Instant.now().getEpochSecond();
        return s.penaltyUntilEpochSeconds() > now;
    }

    public int getPenaltyRemainingSeconds(String playerId) {
        cleanupIfExpired(playerId);
        CombatState s = states.get(playerId);
        if (s == null) return 0;
        long now = Instant.now().getEpochSecond();
        long rem = s.penaltyUntilEpochSeconds() - now;
        return (int) Math.max(0, rem);
    }

    /** Warp check: deny if in combat or penalty. */
    public boolean canWarp(String playerId) {
        return !isInCombat(playerId) && !hasPenalty(playerId);
    }

    /**
     * Called on quit. Applies policy:
     * - NONE: nothing
     * - KILL: marks player as "killed" (engine emits info string; real server would apply death)
     * - PENALTY: sets penalty timer that warp-system can enforce
     */
    public String onPlayerQuit(String playerId) {
        FarmWorldConfig c = cfg.get();
        if (c == null || c.combat == null || !c.combat.enabled) return "combat disabled";
        if (!isInCombat(playerId)) return "not in combat";

        String policy = c.combat.onQuit == null ? "NONE" : c.combat.onQuit.toUpperCase();
        long now = Instant.now().getEpochSecond();

        CombatState prev = states.get(playerId);
        long combatUntil = prev == null ? now : prev.combatUntilEpochSeconds();
        long penaltyUntil = prev == null ? 0 : prev.penaltyUntilEpochSeconds();
        String reason = prev == null ? "" : prev.lastReason();

        switch (policy) {
            case "KILL" -> {
                // engine note
                states.put(playerId, new CombatState(combatUntil, penaltyUntil, reason));
                return "QUIT_IN_COMBAT -> KILL (apply death in real server)";
            }
            case "PENALTY" -> {
                int pSec = Math.max(0, c.combat.penaltySeconds);
                long newPenaltyUntil = Math.max(penaltyUntil, now + pSec);
                // also clear combat (optional) - we keep combat state but it will expire naturally
                states.put(playerId, new CombatState(combatUntil, newPenaltyUntil, reason));
                return "QUIT_IN_COMBAT -> PENALTY " + pSec + "s";
            }
            default -> {
                return "QUIT_IN_COMBAT -> NONE";
            }
        }
    }

    public void cleanupExpired() {
        long now = Instant.now().getEpochSecond();
        states.entrySet().removeIf(e -> {
            CombatState s = e.getValue();
            return s.combatUntilEpochSeconds() <= now && s.penaltyUntilEpochSeconds() <= now;
        });
    }

    private void cleanupIfExpired(String playerId) {
        CombatState s = states.get(playerId);
        if (s == null) return;
        long now = Instant.now().getEpochSecond();
        if (s.combatUntilEpochSeconds() <= now && s.penaltyUntilEpochSeconds() <= now) {
            states.remove(playerId);
        }
    }

    public CombatState getState(String playerId) {
        cleanupIfExpired(playerId);
        return states.get(playerId);
    }
}
