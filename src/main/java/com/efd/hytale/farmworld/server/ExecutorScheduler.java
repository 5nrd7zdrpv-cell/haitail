package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.util.Scheduler;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorScheduler implements Scheduler {
  private final ScheduledExecutorService executorService;

  public ExecutorScheduler(ScheduledExecutorService executorService) {
    this.executorService = executorService;
  }

  @Override
  public void schedule(Duration delay, Runnable task) {
    executorService.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
  }

  @Override
  public void scheduleAtFixedRate(Duration initialDelay, Duration interval, Runnable task) {
    executorService.scheduleAtFixedRate(
        task,
        initialDelay.toMillis(),
        interval.toMillis(),
        TimeUnit.MILLISECONDS);
  }

  @Override
  public void shutdown() {
    executorService.shutdown();
  }
}
