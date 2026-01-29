package com.efd.hytale.farmworld.shared.services;

import com.efd.hytale.farmworld.shared.config.ProtectionActions;
import com.efd.hytale.farmworld.shared.config.ProtectionConfig;
import com.efd.hytale.farmworld.shared.config.ProtectionPoint;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ProtectionService {
  private final ProtectionConfig config;
  private final Logger logger;
  private final Map<String, Instant> lastDeniedLog = new ConcurrentHashMap<>();
  private final Map<String, Instant> lastDeniedMessage = new ConcurrentHashMap<>();
  private final Duration logCooldown = Duration.ofSeconds(5);
  private final Duration messageCooldown = Duration.ofSeconds(3);

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
    ProtectionZoneResult zone = resolveZone(request);
    if (!zone.inside) {
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
      logDenied(request, zone);
    }
    return allowed;
  }

  public ProtectionZoneResult resolveZone(ProtectionCheckRequest request) {
    List<ProtectionPoint> points = config.points == null
        ? List.of()
        : config.points.stream().filter(Objects::nonNull).collect(Collectors.toList());
    if (!points.isEmpty()) {
      // Entscheidung: Wenn mehrere Zonen zutreffen, gewinnt die naechstgelegene Zone.
      ProtectionPoint closestInside = points.stream()
          .filter(point -> distance(point.x, point.y, point.z, request.x, request.y, request.z) <= point.radius)
          .min(Comparator.comparingDouble(point -> distance(point.x, point.y, point.z, request.x, request.y, request.z)))
          .orElse(null);
      ProtectionPoint closestAny = points.stream()
          .min(Comparator.comparingDouble(point -> distance(point.x, point.y, point.z, request.x, request.y, request.z)))
          .orElse(null);
      ProtectionPoint candidate = closestInside != null ? closestInside : closestAny;
      if (candidate != null) {
        double distance = distance(candidate.x, candidate.y, candidate.z, request.x, request.y, request.z);
        boolean inside = distance <= candidate.radius;
        return new ProtectionZoneResult(
            candidate.name,
            candidate.x,
            candidate.y,
            candidate.z,
            candidate.radius,
            distance,
            inside,
            true);
      }
    }
    double centerX = config.center != null ? config.center.x : request.fallbackCenterX;
    double centerY = config.center != null ? config.center.y : request.fallbackCenterY;
    double centerZ = config.center != null ? config.center.z : request.fallbackCenterZ;
    double distance = distance(centerX, centerY, centerZ, request.x, request.y, request.z);
    return new ProtectionZoneResult(
        null,
        centerX,
        centerY,
        centerZ,
        config.radius,
        distance,
        distance <= config.radius,
        false);
  }

  public boolean shouldNotify(String actorId) {
    if (actorId == null || actorId.isBlank()) {
      return false;
    }
    Instant now = Instant.now();
    Instant last = lastDeniedMessage.get(actorId);
    if (last != null && Duration.between(last, now).compareTo(messageCooldown) < 0) {
      return false;
    }
    lastDeniedMessage.put(actorId, now);
    return true;
  }

  private void logDenied(ProtectionCheckRequest request, ProtectionZoneResult zone) {
    if (logger == null || !config.debugLog) {
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
        " (Entfernung=" + Math.round(zone.distance) +
        ", Zone=" + (zone.name == null || zone.name.isBlank() ? "default" : zone.name) + ").");
  }

  private double distance(double ax, double ay, double az, double bx, double by, double bz) {
    double dx = ax - bx;
    double dy = ay - by;
    double dz = az - bz;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }
}
