package com.efd.hytale.farmworld.shared.commands;

import com.efd.hytale.farmworld.shared.config.CombatConfig;
import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.efd.hytale.farmworld.shared.services.CombatService;
import com.efd.hytale.farmworld.shared.services.FarmWorldService;
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
    FarmWorldService farmWorldService = new FarmWorldService(config, scheduler);
    CombatService combatService = new CombatService(new CombatConfig());
    new DefaultCommands().register(registry, farmWorldService, combatService);

    CommandResult statusResult = registry.execute("tester", "fwstatus", List.of());
    if (!statusResult.success) {
      issues.add("fwstatus failed: " + statusResult.message);
    }
    CommandResult combatResult = registry.execute("tester", "fwcombat", List.of());
    if (!combatResult.success) {
      issues.add("fwcombat failed: " + combatResult.message);
    }
    return issues;
  }
}
