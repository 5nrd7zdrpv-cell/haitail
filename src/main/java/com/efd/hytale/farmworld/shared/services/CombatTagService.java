package com.efd.hytale.farmworld.shared.services;

import com.efd.hytale.farmworld.shared.config.CombatConfig;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatTagService {
  private final CombatConfig config;
  private final Map<UUID, Instant> tags = new ConcurrentHashMap<>();
  private final Map<UUID, String> names = new ConcurrentHashMap<>();
  private final Map<String, UUID> idsByName = new ConcurrentHashMap<>();

  public CombatTagService(CombatConfig config) {
    this.config = config;
  }

  public void tag(UUID playerId) {
    tag(playerId, 0L);
  }

  public void tag(UUID playerId, long durationSeconds) {
    if (!config.enabled) {
      return;
    }
    long duration = durationSeconds > 0 ? durationSeconds : config.tagSeconds;
    tags.put(playerId, Instant.now().plusSeconds(duration));
  }

  public void recordPlayer(UUID playerId, String name) {
    if (playerId == null || name == null || name.isBlank()) {
      return;
    }
    names.put(playerId, name);
    idsByName.put(name.toLowerCase(Locale.ROOT), playerId);
  }

  public UUID resolvePlayerId(String rawId) {
    if (rawId == null || rawId.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(rawId);
    } catch (IllegalArgumentException ex) {
      return idsByName.get(rawId.toLowerCase(Locale.ROOT));
    }
  }

  public String getPlayerName(UUID playerId) {
    if (playerId == null) {
      return "Unbekannt";
    }
    return names.getOrDefault(playerId, "Unbekannt");
  }

  public String describePlayer(String rawId) {
    UUID resolved = resolvePlayerId(rawId);
    if (resolved != null) {
      return getPlayerName(resolved);
    }
    if (rawId == null || rawId.isBlank()) {
      return "Unbekannt";
    }
    try {
      UUID.fromString(rawId);
      return "Unbekannt";
    } catch (IllegalArgumentException ex) {
      return rawId;
    }
  }

  public boolean isInCombat(UUID playerId) {
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

  public long getRemainingSeconds(UUID playerId) {
    Instant expiresAt = tags.get(playerId);
    if (expiresAt == null) {
      return 0L;
    }
    long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
    if (remaining <= 0L) {
      tags.remove(playerId);
      return 0L;
    }
    return remaining;
  }

  public void clear(UUID playerId) {
    tags.remove(playerId);
  }

  public void clearAll() {
    tags.clear();
  }
}
