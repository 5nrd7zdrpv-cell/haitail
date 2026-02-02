package com.efd.hytale.farmworld.shared.services;

import com.efd.hytale.farmworld.shared.config.FarmWorldSpawn;

public interface FarmWorldWorldAdapter {
  boolean resetWorld(String worldId, String instanceId);

  boolean loadPrefab(String prefabSpawnId, FarmWorldSpawn spawnPosition);

  boolean savePrefab(String prefabSpawnId, FarmWorldSpawn centerPosition, int radius);
}
