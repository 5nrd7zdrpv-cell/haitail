package com.efd.hytale.farmworld.shared.config;

public class FarmWorldSettings {
  public String worldId = "farm";
  public String instanceId = "default";
  public int resetIntervalDays = 7;
  public String prefabSpawnId = "prefabs/farm_spawn.prefab";
  public FarmWorldSpawn spawn = new FarmWorldSpawn();
}
