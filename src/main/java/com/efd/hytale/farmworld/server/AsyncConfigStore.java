package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.efd.hytale.farmworld.shared.config.FarmWorldConfigStore;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class AsyncConfigStore implements FarmWorldConfigStore {
  private final ConfigManager configManager;
  private final ExecutorService executorService;
  private final Logger logger;

  public AsyncConfigStore(ConfigManager configManager, ExecutorService executorService, Logger logger) {
    this.configManager = configManager;
    this.executorService = executorService;
    this.logger = logger;
  }

  @Override
  public void save(FarmWorldConfig config) {
    executorService.submit(() -> {
      try {
        configManager.save(config);
      } catch (RuntimeException ex) {
        logger.warning("Failed to persist farmworld config: " + ex.getMessage());
      }
    });
  }
}
