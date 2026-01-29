package com.efd.hytale.farmworld.shared.util;

import java.time.Duration;

public interface Scheduler {
  void schedule(Duration delay, Runnable task);

  void scheduleAtFixedRate(Duration initialDelay, Duration interval, Runnable task);

  void shutdown();
}
