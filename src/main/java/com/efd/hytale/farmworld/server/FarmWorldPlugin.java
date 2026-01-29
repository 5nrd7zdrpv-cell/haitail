package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.commands.CommandRegistry;
import com.efd.hytale.farmworld.shared.commands.CommandRegistrySelfTest;
import com.efd.hytale.farmworld.shared.commands.DefaultCommands;
import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.efd.hytale.farmworld.shared.services.CombatService;
import com.efd.hytale.farmworld.shared.services.FarmWorldService;
import com.efd.hytale.farmworld.shared.services.ProtectionService;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FarmWorldPlugin implements PluginLifecycle {
  private final Logger logger = Logger.getLogger(FarmWorldPlugin.class.getName());

  private ScheduledExecutorService executorService;
  private FarmWorldService farmWorldService;
  private CombatService combatService;
  private ProtectionService protectionService;
  private CommandBridge commandBridge;

  @Override
  public void enable() {
    logger.info("Enabling FarmWorld plugin...");
    ConfigManager configManager = new ConfigManager(logger);
    FarmWorldConfig config = configManager.load();

    executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "FarmWorldScheduler");
      thread.setDaemon(true);
      return thread;
    });

    ExecutorScheduler scheduler = new ExecutorScheduler(executorService);
    farmWorldService = new FarmWorldService(config, scheduler);
    combatService = new CombatService(config.combat);
    protectionService = new ProtectionService(config.protection);

    farmWorldService.start();

    CommandRegistry registry = new CommandRegistry();
    new DefaultCommands().register(registry, farmWorldService, combatService);
    commandBridge = new CommandBridge(registry);

    runSelfTest();
    logger.info("FarmWorld plugin enabled.");
  }

  @Override
  public void disable() {
    logger.info("Disabling FarmWorld plugin...");
    if (farmWorldService != null) {
      farmWorldService.stop();
    }
    if (executorService != null) {
      executorService.shutdown();
    }
    if (combatService != null) {
      combatService.clearAll();
    }
    logger.info("FarmWorld plugin disabled.");
  }

  public CommandBridge getCommandBridge() {
    return commandBridge;
  }

  public CombatService getCombatService() {
    return combatService;
  }

  public ProtectionService getProtectionService() {
    return protectionService;
  }

  private void runSelfTest() {
    List<String> issues = CommandRegistrySelfTest.run();
    if (issues.isEmpty()) {
      logger.info("Command registry self-test passed.");
      return;
    }
    for (String issue : issues) {
      logger.warning("Command registry self-test issue: " + issue);
    }
    logger.log(Level.INFO, "Command registry self-test completed with {0} issue(s).", issues.size());
  }
}
