package com.efd.hytale.farmworld.shared.services;

import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.efd.hytale.farmworld.shared.config.FarmWorldConfigStore;
import com.efd.hytale.farmworld.shared.config.FarmWorldSpawn;
import com.efd.hytale.farmworld.shared.util.Scheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

public class FarmWorldService {
  private final FarmWorldConfig config;
  private final Scheduler scheduler;
  private final FarmWorldConfigStore configStore;
  private final FarmWorldWorldAdapter worldAdapter;
  private final Logger logger;
  private Instant lastCheck = Instant.EPOCH;

  public FarmWorldService(
      FarmWorldConfig config,
      Scheduler scheduler,
      FarmWorldConfigStore configStore,
      FarmWorldWorldAdapter worldAdapter,
      Logger logger) {
    this.config = config;
    this.scheduler = scheduler;
    this.configStore = configStore;
    this.worldAdapter = worldAdapter;
    this.logger = logger;
  }

  public void start() {
    ensureNextResetScheduled();
    logNextReset();
    scheduler.scheduleAtFixedRate(Duration.ZERO, Duration.ofHours(1), this::tick);
  }

  public void stop() {
    scheduler.shutdown();
  }

  private void tick() {
    lastCheck = Instant.now();
    Instant now = Instant.now();
    if (config.nextResetEpochSeconds <= 0L) {
      ensureNextResetScheduled();
      return;
    }
    Instant nextReset = Instant.ofEpochSecond(config.nextResetEpochSeconds);
    if (now.isBefore(nextReset)) {
      return;
    }
    resetWorld(now);
  }

  public FarmWorldStatus getStatus() {
    return new FarmWorldStatus(
        config.farmWorld.worldId,
        config.farmWorld.instanceId,
        config.farmWorld.resetIntervalDays,
        config.lastResetEpochSeconds,
        config.nextResetEpochSeconds,
        lastCheck);
  }

  public boolean resetNow() {
    return resetWorld(Instant.now());
  }

  public Instant scheduleNextReset() {
    Instant nextReset = Instant.now().plus(Duration.ofDays(config.farmWorld.resetIntervalDays));
    config.nextResetEpochSeconds = nextReset.getEpochSecond();
    configStore.save(config);
    return nextReset;
  }

  public void updateSpawn(FarmWorldSpawn spawn) {
    config.farmWorld.spawn = spawn;
    configStore.save(config);
  }

  public void updateProtection(java.util.function.Consumer<com.efd.hytale.farmworld.shared.config.ProtectionConfig> updater) {
    updater.accept(config.protection);
    configStore.save(config);
  }

  public FarmWorldConfig getConfig() {
    return config;
  }

  public FarmWorldSpawn getSpawn() {
    return config.farmWorld.spawn;
  }

  public FarmWorldSpawn resolveSpawn() {
    return resolveSpawn(config.farmWorld.spawn);
  }

  private void ensureNextResetScheduled() {
    if (config.nextResetEpochSeconds > 0L) {
      return;
    }
    Instant nextReset = Instant.now().plus(Duration.ofDays(config.farmWorld.resetIntervalDays));
    config.nextResetEpochSeconds = nextReset.getEpochSecond();
    configStore.save(config);
  }

  private void logNextReset() {
    if (logger == null) {
      return;
    }
    Instant nextReset = Instant.ofEpochSecond(config.nextResetEpochSeconds);
    logger.info("[FarmWorld] Nächster Reset geplant: " + nextReset + " (Intervall=" +
        config.farmWorld.resetIntervalDays + " Tage).");
  }

  private boolean resetWorld(Instant now) {
    if (logger != null) {
      logger.info("[FarmWorld] Farmwelt wird zurückgesetzt...");
    }
    boolean resetOk = worldAdapter.resetWorld(config.farmWorld.worldId, config.farmWorld.instanceId);
    if (resetOk && logger != null) {
      logger.info("[FarmWorld] Farmwelt wurde entladen/zurückgesetzt.");
    }
    if (!resetOk) {
      if (logger != null) {
        logger.warning("[FarmWorld] Reset fehlgeschlagen.");
      }
      return false;
    }
    if (resetOk) {
      if (config.farmWorld.prefabSpawnId == null || config.farmWorld.prefabSpawnId.isBlank()) {
        if (logger != null) {
          logger.severe("[FarmWorld] prefabSpawnId ist leer; Prefab-Laden wird übersprungen.");
        }
      } else {
        FarmWorldSpawn spawn = resolveSpawn(config.farmWorld.spawn);
        boolean prefabLoaded = worldAdapter.loadPrefab(config.farmWorld.prefabSpawnId, spawn);
        if (prefabLoaded && logger != null) {
          logger.info("[FarmWorld] Prefab geladen");
        }
      }
    }
    config.lastResetEpochSeconds = now.getEpochSecond();
    Instant nextReset = now.plus(Duration.ofDays(config.farmWorld.resetIntervalDays));
    config.nextResetEpochSeconds = nextReset.getEpochSecond();
    configStore.save(config);
    logNextReset();
    if (logger != null) {
      logger.info("[FarmWorld] Reset abgeschlossen");
    }
    return true;
  }

  private FarmWorldSpawn resolveSpawn(FarmWorldSpawn spawn) {
    if (spawn == null) {
      spawn = new FarmWorldSpawn();
    }
    FarmWorldSpawn resolved = new FarmWorldSpawn();
    resolved.x = spawn.x;
    resolved.y = spawn.y;
    resolved.z = spawn.z;
    resolved.worldId = spawn.worldId == null || spawn.worldId.isBlank()
        ? config.farmWorld.worldId
        : spawn.worldId;
    resolved.instanceId = spawn.instanceId == null || spawn.instanceId.isBlank()
        ? config.farmWorld.instanceId
        : spawn.instanceId;
    return resolved;
  }
}
