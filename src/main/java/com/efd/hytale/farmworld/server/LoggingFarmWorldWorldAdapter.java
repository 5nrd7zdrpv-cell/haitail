package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.config.FarmWorldSpawn;
import com.efd.hytale.farmworld.shared.services.FarmWorldWorldAdapter;
import java.util.logging.Logger;

public class LoggingFarmWorldWorldAdapter implements FarmWorldWorldAdapter {
  private final Logger logger;

  public LoggingFarmWorldWorldAdapter(Logger logger) {
    this.logger = logger;
  }

  @Override
  public boolean resetWorld(String worldId, String instanceId) {
    logger.info("Requesting reset for world " + worldId + "/" + instanceId + ".");
    return true;
  }

  @Override
  public boolean loadPrefab(String prefabSpawnId, FarmWorldSpawn spawnPosition) {
    logger.info("Loading prefab " + prefabSpawnId + " at " +
        spawnPosition.worldId + "/" + spawnPosition.instanceId +
        " (" + spawnPosition.x + ", " + spawnPosition.y + ", " + spawnPosition.z + ").");
    return true;
  }
}
