package com.efd.hytale.farmworld.shared.services;

import com.efd.hytale.farmworld.shared.config.CombatConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CombatService {
  private final CombatConfig config;
  private final Map<String, Instant> tags = new ConcurrentHashMap<>();

  public CombatService(CombatConfig config) {
    this.config = config;
  }

  public void tagPlayer(String playerId) {
    if (!config.enabled) {
      return;
    }
    tags.put(playerId, Instant.now().plusSeconds(config.tagSeconds));
  }

  public boolean isTagged(String playerId) {
    if (!config.enabled) {
      return false;
    }
    Instant expiresAt = tags.get(playerId);
    if (expiresAt == null) {
      return false;
    }
    if (expiresAt.isBefore(Instant.now())) {
      tags.remove(playerId);
      return false;
    }
    return true;
  }

  public Duration remainingTag(String playerId) {
    Instant expiresAt = tags.get(playerId);
    if (expiresAt == null) {
      return Duration.ZERO;
    }
    Duration remaining = Duration.between(Instant.now(), expiresAt);
    if (remaining.isNegative()) {
      tags.remove(playerId);
      return Duration.ZERO;
    }
    return remaining;
  }

  public CombatQuitResult handleQuit(String playerId) {
    if (!config.enabled || !isTagged(playerId)) {
      return CombatQuitResult.noPenalty();
    }
    return CombatQuitResult.fromPolicy(config.onQuit, config.penaltySeconds);
  }

  public void clearAll() {
    tags.clear();
  }
}
