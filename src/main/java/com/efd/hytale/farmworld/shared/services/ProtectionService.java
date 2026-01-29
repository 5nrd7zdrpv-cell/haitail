package com.efd.hytale.farmworld.shared.services;

import com.efd.hytale.farmworld.shared.config.ProtectionActions;
import com.efd.hytale.farmworld.shared.config.ProtectionConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ProtectionService {
  private final ProtectionConfig config;
  private final Logger logger;
  private final Map<String, Instant> lastDeniedLog = new ConcurrentHashMap<>();
  private final Duration logCooldown = Duration.ofSeconds(5);

  public ProtectionService(ProtectionConfig config, Logger logger) {
    this.config = config;
    this.logger = logger;
  }

  public boolean isActionAllowed(ProtectionCheckRequest request) {
    if (!config.enabled) {
      return true;
    }
    if (request.hasBypassPermission) {
      return true;
    }
    if (request.distanceFromSpawn > config.radius) {
      return true;
    }
    ProtectionActions actions = config.actions;
    boolean allowed = switch (request.action) {
      case PLACE -> !actions.place;
      case BREAK_BLOCK -> !actions.breakBlock;
      case INTERACT -> !actions.interact;
      case DAMAGE -> !actions.damage;
      case EXPLOSION -> !actions.explosion;
      case FIRE_SPREAD -> !actions.fireSpread;
      case LIQUID -> !actions.liquid;
    };
    if (!allowed) {
      logDenied(request);
    }
    return allowed;
  }

  private void logDenied(ProtectionCheckRequest request) {
    if (logger == null) {
      return;
    }
    String key = request.actorId + ":" + request.action;
    Instant now = Instant.now();
    Instant last = lastDeniedLog.get(key);
    if (last != null && Duration.between(last, now).compareTo(logCooldown) < 0) {
      return;
    }
    lastDeniedLog.put(key, now);
    logger.info("[FarmWorld] Schutz verweigert: " + request.action +
        " durch " + request.actorId +
        " bei " + request.worldId + "/" + request.instanceId +
        " (Entfernung=" + Math.round(request.distanceFromSpawn) + ").");
  }
}
