package com.efd.hytale.farmworld.shared.commands;

import com.efd.hytale.farmworld.shared.config.FarmWorldSpawn;
import com.efd.hytale.farmworld.shared.services.CombatTagService;
import com.efd.hytale.farmworld.shared.services.FarmWorldService;
import com.efd.hytale.farmworld.shared.services.FarmWorldStatus;
import java.util.List;

public class DefaultCommands {
  public void register(
      CommandRegistry registry,
      FarmWorldService farmWorldService,
      CombatTagService combatService) {
    registry.register(
        new CommandDefinition(
            "fwstatus",
            "Show farm world scheduling status.",
            List.of(),
            context -> {
              FarmWorldStatus status = farmWorldService.getStatus();
              String message = "Farm world: " + status.worldId + "/" + status.instanceId +
                  ", reset every " + status.resetIntervalDays +
                  " days, last reset epoch=" + status.lastResetEpochSeconds +
                  ", next reset epoch=" + status.nextResetEpochSeconds +
                  ", last check=" + status.lastCheck + ".";
              return CommandResult.ok(message);
            }));

    registry.register(
        new CommandDefinition(
            "combat",
            "Check combat tag status for yourself.",
            List.of(new CommandArgument("status", false, "Show combat tag status.")),
            context -> {
              if (!context.args.isEmpty() && !"status".equalsIgnoreCase(context.args.get(0))) {
                return CommandResult.error("Usage: /combat status");
              }
              boolean tagged = combatService.isInCombat(context.actorId);
              if (!tagged) {
                return CommandResult.ok("You are not in combat.");
              }
              return CommandResult.ok("You are in combat for " +
                  (combatService.getRemainingMillis(context.actorId) / 1000) + "s.");
            }));

    registry.register(
        new CommandDefinition(
            "fwspawn",
            "Update the farm world spawn position.",
            List.of(
                new CommandArgument("set", true, "Set spawn coordinates."),
                new CommandArgument("x", true, "X coordinate."),
                new CommandArgument("y", true, "Y coordinate."),
                new CommandArgument("z", true, "Z coordinate."),
                new CommandArgument("worldId", false, "World id."),
                new CommandArgument("instanceId", false, "Instance id.")
            ),
            context -> {
              if (context.args.size() < 4 || !"set".equalsIgnoreCase(context.args.get(0))) {
                return CommandResult.error("Usage: /fwspawn set <x> <y> <z> [worldId] [instanceId]");
              }
              double x;
              double y;
              double z;
              try {
                x = Double.parseDouble(context.args.get(1));
                y = Double.parseDouble(context.args.get(2));
                z = Double.parseDouble(context.args.get(3));
              } catch (NumberFormatException ex) {
                return CommandResult.error("Spawn coordinates must be numbers.");
              }
              FarmWorldSpawn currentSpawn = farmWorldService.getSpawn();
              FarmWorldSpawn spawn = new FarmWorldSpawn();
              spawn.x = x;
              spawn.y = y;
              spawn.z = z;
              if (context.args.size() > 4) {
                spawn.worldId = context.args.get(4);
              } else if (currentSpawn != null) {
                spawn.worldId = currentSpawn.worldId;
              }
              if (context.args.size() > 5) {
                spawn.instanceId = context.args.get(5);
              } else if (currentSpawn != null) {
                spawn.instanceId = currentSpawn.instanceId;
              }
              farmWorldService.updateSpawn(spawn);
              return CommandResult.ok("Spawn updated to " + x + ", " + y + ", " + z + ".");
            }));
  }
}
