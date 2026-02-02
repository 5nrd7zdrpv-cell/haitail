package com.efd.hytale.farmworld.shared.commands;

import com.efd.hytale.farmworld.shared.config.CombatConfig;
import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.efd.hytale.farmworld.shared.services.CombatTagService;
import com.efd.hytale.farmworld.shared.config.FarmWorldConfigStore;
import com.efd.hytale.farmworld.shared.services.FarmWorldService;
import com.efd.hytale.farmworld.shared.services.FarmWorldWorldAdapter;
import com.efd.hytale.farmworld.shared.services.ProtectionService;
import com.efd.hytale.farmworld.shared.util.Scheduler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class CommandRegistrySelfTest {
  private CommandRegistrySelfTest() {}

  public static List<String> run() {
    List<String> issues = new ArrayList<>();
    CommandRegistry registry = new CommandRegistry();
    FarmWorldConfig config = new FarmWorldConfig();
    Scheduler scheduler = new Scheduler() {
      @Override
      public void schedule(Duration delay, Runnable task) {}

      @Override
      public void scheduleAtFixedRate(Duration initialDelay, Duration interval, Runnable task) {}

      @Override
      public void shutdown() {}
    };
    FarmWorldConfigStore configStore = ignored -> {};
    FarmWorldWorldAdapter worldAdapter = new FarmWorldWorldAdapter() {
      @Override
      public boolean resetWorld(String worldId, String instanceId) {
        return true;
      }

      @Override
      public boolean loadPrefab(String prefabSpawnId, com.efd.hytale.farmworld.shared.config.FarmWorldSpawn spawnPosition) {
        return true;
      }

      @Override
      public boolean savePrefab(String prefabSpawnId, com.efd.hytale.farmworld.shared.config.FarmWorldSpawn centerPosition, int radius) {
        return true;
      }
    };
    FarmWorldService farmWorldService = new FarmWorldService(
        config,
        scheduler,
        configStore,
        worldAdapter,
        null);
    CombatTagService combatService = new CombatTagService(new CombatConfig());
    ProtectionService protectionService = new ProtectionService(config.protection, null);
    new DefaultCommands().register(registry, farmWorldService, combatService, protectionService, config);

    CommandResult statusResult = registry.execute("tester", "farm", List.of("status"));
    if (!statusResult.success) {
      issues.add("farm status failed: " + statusResult.message);
    }
    CommandResult combatResult = registry.execute("tester", "combat", List.of("status"));
    if (!combatResult.success) {
      issues.add("combat failed: " + combatResult.message);
    }
    CommandResult protectResult = registry.execute("tester", "protect", List.of("status"));
    if (!protectResult.success) {
      issues.add("protect failed: " + protectResult.message);
    }
    return issues;
  }
}
