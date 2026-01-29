package com.efd.hytale.farmworld.shared.services;

import com.efd.hytale.farmworld.shared.config.CombatConfig;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CombatTagService {
  private final CombatConfig config;
  private final Map<String, CombatTag> tags = new ConcurrentHashMap<>();

  public CombatTagService(CombatConfig config) {
    this.config = config;
  }

  public void tag(String playerId, long durationSeconds, String reason) {
    if (!config.enabled) {
      return;
    }
    long duration = durationSeconds > 0 ? durationSeconds : config.tagSeconds;
    tags.put(playerId, new CombatTag(Instant.now().plusSeconds(duration), reason));
  }

  public boolean isInCombat(String playerId) {
    if (!config.enabled) {
      return false;
    }
    CombatTag tag = tags.get(playerId);
    if (tag == null) {
      return false;
    }
    if (tag.expiresAt.isBefore(Instant.now())) {
      tags.remove(playerId);
      return false;
    }
    return true;
  }

  public long getRemainingMillis(String playerId) {
    CombatTag tag = tags.get(playerId);
    if (tag == null) {
      return 0L;
    }
    long remaining = tag.expiresAt.toEpochMilli() - Instant.now().toEpochMilli();
    if (remaining <= 0) {
      tags.remove(playerId);
      return 0L;
    }
    return remaining;
  }

  public void clear(String playerId) {
    tags.remove(playerId);
  }

  public void clearAll() {
    tags.clear();
  }

  private static final class CombatTag {
    private final Instant expiresAt;
    private final String reason;

    private CombatTag(Instant expiresAt, String reason) {
      this.expiresAt = expiresAt;
      this.reason = reason;
    }
  }
}
