package com.efd.hytale.farmworld.shared.services;

import java.time.Instant;

public class FarmWorldStatus {
  public final String worldName;
  public final int resetIntervalDays;
  public final String resetAt;
  public final long lastResetEpochSeconds;
  public final Instant lastCheck;

  public FarmWorldStatus(
      String worldName,
      int resetIntervalDays,
      String resetAt,
      long lastResetEpochSeconds,
      Instant lastCheck) {
    this.worldName = worldName;
    this.resetIntervalDays = resetIntervalDays;
    this.resetAt = resetAt;
    this.lastResetEpochSeconds = lastResetEpochSeconds;
    this.lastCheck = lastCheck;
  }
}
