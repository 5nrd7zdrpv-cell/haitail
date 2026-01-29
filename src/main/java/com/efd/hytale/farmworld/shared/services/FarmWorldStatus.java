package com.efd.hytale.farmworld.shared.services;

import java.time.Instant;

public class FarmWorldStatus {
  public final String worldId;
  public final String instanceId;
  public final int resetIntervalDays;
  public final long lastResetEpochSeconds;
  public final long nextResetEpochSeconds;
  public final Instant lastCheck;

  public FarmWorldStatus(
      String worldId,
      String instanceId,
      int resetIntervalDays,
      long lastResetEpochSeconds,
      long nextResetEpochSeconds,
      Instant lastCheck) {
    this.worldId = worldId;
    this.instanceId = instanceId;
    this.resetIntervalDays = resetIntervalDays;
    this.lastResetEpochSeconds = lastResetEpochSeconds;
    this.nextResetEpochSeconds = nextResetEpochSeconds;
    this.lastCheck = lastCheck;
  }
}
