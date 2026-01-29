package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.commands.CommandRegistry;
import com.efd.hytale.farmworld.shared.commands.CommandRegistrySelfTest;
import com.efd.hytale.farmworld.shared.commands.DefaultCommands;
import com.efd.hytale.farmworld.shared.config.ConfigValidator;
import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.efd.hytale.farmworld.shared.services.CombatTagService;
import com.efd.hytale.farmworld.shared.services.FarmWorldService;
import com.efd.hytale.farmworld.shared.services.ProtectionService;
import com.efd.hytale.farmworld.shared.services.FarmWorldWorldAdapter;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FarmWorldPlugin extends JavaPlugin {
  private final Logger logger = Logger.getLogger(FarmWorldPlugin.class.getName());

  private ScheduledExecutorService executorService;
  private FarmWorldService farmWorldService;
  private CombatTagService combatService;
  private ProtectionService protectionService;
  private CommandBridge commandBridge;
  private CombatBridge combatBridge;
  private ProtectionBridge protectionBridge;

  public FarmWorldPlugin(JavaPluginInit init) {
    super(init);
  }

  @Override
  public void setup() {
    logger.info("Setting up FarmWorld plugin...");
    ConfigManager configManager = new ConfigManager(logger);
    FarmWorldConfig config = configManager.load();
    for (String issue : ConfigValidator.validateSevere(config)) {
      logger.severe(issue);
    }

    executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "FarmWorldScheduler");
      thread.setDaemon(true);
      return thread;
    });

    ExecutorScheduler scheduler = new ExecutorScheduler(executorService);
    AsyncConfigStore configStore = new AsyncConfigStore(configManager, executorService, logger);
    FarmWorldWorldAdapter worldAdapter = new LoggingFarmWorldWorldAdapter(logger);
    farmWorldService = new FarmWorldService(config, scheduler, configStore, worldAdapter, logger);
    combatService = new CombatTagService(config.combat);
    protectionService = new ProtectionService(config.protection, logger);
    combatBridge = new CombatBridge(combatService, logger);
    protectionBridge = new ProtectionBridge(config, protectionService);

    CommandRegistry registry = new CommandRegistry();
    new DefaultCommands().register(registry, farmWorldService, combatService);
    commandBridge = new CommandBridge(registry);

    logger.info("FarmWorld plugin setup complete.");
  }

  @Override
  public void start() {
    logger.info("Starting FarmWorld plugin...");
    if (farmWorldService != null) {
      farmWorldService.start();
    }

    runSelfTest();
    logger.info("FarmWorld plugin started.");
  }

  @Override
  public void shutdown() {
    logger.info("Shutting down FarmWorld plugin...");
    if (farmWorldService != null) {
      farmWorldService.stop();
    }
    if (executorService != null) {
      executorService.shutdown();
    }
    if (combatService != null) {
      combatService.clearAll();
    }
    logger.info("FarmWorld plugin shutdown complete.");
  }

  public CommandBridge getCommandBridge() {
    return commandBridge;
  }

  public CombatTagService getCombatService() {
    return combatService;
  }

  public ProtectionService getProtectionService() {
    return protectionService;
  }

  public CombatBridge getCombatBridge() {
    return combatBridge;
  }

  public ProtectionBridge getProtectionBridge() {
    return protectionBridge;
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
