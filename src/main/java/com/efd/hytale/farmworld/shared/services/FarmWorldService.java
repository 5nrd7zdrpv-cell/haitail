package com.efd.hytale.farmworld.shared.services;

import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.efd.hytale.farmworld.shared.util.Scheduler;
import java.time.Duration;
import java.time.Instant;

public class FarmWorldService {
  private final FarmWorldConfig config;
  private final Scheduler scheduler;
  private Instant lastCheck = Instant.EPOCH;

  public FarmWorldService(FarmWorldConfig config, Scheduler scheduler) {
    this.config = config;
    this.scheduler = scheduler;
  }

  public void start() {
    scheduler.scheduleAtFixedRate(Duration.ZERO, Duration.ofHours(1), this::tick);
  }

  public void stop() {
    scheduler.shutdown();
  }

  private void tick() {
    lastCheck = Instant.now();
    // TODO: integrate with Hytale world reset scheduling once API is known.
  }

  public FarmWorldStatus getStatus() {
    return new FarmWorldStatus(
        config.farmWorld.name,
        config.farmWorld.resetIntervalDays,
        config.farmWorld.resetAt,
        config.lastResetEpochSeconds,
        lastCheck);
  }
}
