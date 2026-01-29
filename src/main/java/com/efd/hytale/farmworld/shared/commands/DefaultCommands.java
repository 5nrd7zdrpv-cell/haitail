package com.efd.hytale.farmworld.shared.commands;

import com.efd.hytale.farmworld.shared.services.CombatService;
import com.efd.hytale.farmworld.shared.services.FarmWorldService;
import com.efd.hytale.farmworld.shared.services.FarmWorldStatus;
import java.util.List;

public class DefaultCommands {
  public void register(CommandRegistry registry, FarmWorldService farmWorldService, CombatService combatService) {
    registry.register(
        new CommandDefinition(
            "fwstatus",
            "Show farm world scheduling status.",
            List.of(),
            context -> {
              FarmWorldStatus status = farmWorldService.getStatus();
              String message = "Farm world: " + status.worldName +
                  ", reset every " + status.resetIntervalDays +
                  " days at " + status.resetAt +
                  ", last reset epoch=" + status.lastResetEpochSeconds +
                  ", last check=" + status.lastCheck + ".";
              return CommandResult.ok(message);
            }));

    registry.register(
        new CommandDefinition(
            "fwcombat",
            "Check combat tag status for yourself.",
            List.of(),
            context -> {
              boolean tagged = combatService.isTagged(context.actorId);
              if (!tagged) {
                return CommandResult.ok("You are not in combat.");
              }
              return CommandResult.ok("You are in combat for " +
                  combatService.remainingTag(context.actorId).getSeconds() + "s.");
            }));
  }
}
