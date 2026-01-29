package com.efd.hytale.farmworld.shared.commands;

import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.efd.hytale.farmworld.shared.config.FarmWorldSpawn;
import com.efd.hytale.farmworld.shared.config.ProtectionActions;
import com.efd.hytale.farmworld.shared.services.CombatTagService;
import com.efd.hytale.farmworld.shared.services.FarmWorldService;
import com.efd.hytale.farmworld.shared.services.FarmWorldStatus;
import com.efd.hytale.farmworld.shared.services.ProtectionAction;
import com.efd.hytale.farmworld.shared.services.ProtectionCheckRequest;
import com.efd.hytale.farmworld.shared.services.ProtectionService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public class DefaultCommands {
  public void register(
      CommandRegistry registry,
      FarmWorldService farmWorldService,
      CombatTagService combatService,
      ProtectionService protectionService,
      FarmWorldConfig config) {
    registry.register(
        new CommandDefinition(
            "farm",
            "Farm world status and reset commands.",
            List.of(),
            context -> {
              if (context.args.isEmpty()) {
                return CommandResult.error("Usage: /farm status|reset now|reset schedule|setspawn <x> <y> <z> [worldId] [instanceId]");
              }
              String action = context.args.get(0).toLowerCase(Locale.ROOT);
              return switch (action) {
                case "status" -> CommandResult.ok(formatFarmStatus(farmWorldService));
                case "reset" -> handleFarmReset(context.args, farmWorldService);
                case "setspawn" -> handleFarmSetSpawn(context.args, farmWorldService);
                default -> CommandResult.error("Unknown farm command: " + action);
              };
            }));

    registry.register(
        new CommandDefinition(
            "combat",
            "Combat tag diagnostics and admin helpers.",
            List.of(),
            context -> {
              if (context.args.isEmpty()) {
                return CommandResult.error("Usage: /combat status|canwarp|tag|quit|cleanup");
              }
              String action = context.args.get(0).toLowerCase(Locale.ROOT);
              return switch (action) {
                case "status" -> handleCombatStatus(context.args, context.actorId, combatService);
                case "canwarp" -> handleCombatCanWarp(context.args, context.actorId, combatService);
                case "tag" -> handleCombatTag(context.args, combatService);
                case "quit" -> handleCombatQuit(context.args, combatService);
                case "cleanup" -> {
                  combatService.clearAll();
                  yield CommandResult.ok("Combat tags cleared.");
                }
                default -> CommandResult.error("Unknown combat command: " + action);
              };
            }));

    registry.register(
        new CommandDefinition(
            "protect",
            "Protection zone status and test.",
            List.of(),
            context -> {
              if (context.args.isEmpty()) {
                return CommandResult.error("Usage: /protect status|test <ACTION> <x> <y> <z> [perm=true|false]");
              }
              String action = context.args.get(0).toLowerCase(Locale.ROOT);
              return switch (action) {
                case "status" -> CommandResult.ok(formatProtectionStatus(config));
                case "test" -> handleProtectionTest(context.args, context.actorId, protectionService, config);
                default -> CommandResult.error("Unknown protect command: " + action);
              };
            }));
  }

  private static CommandResult handleFarmReset(List<String> args, FarmWorldService farmWorldService) {
    if (args.size() < 2) {
      return CommandResult.error("Usage: /farm reset now|schedule");
    }
    String resetMode = args.get(1).toLowerCase(Locale.ROOT);
    switch (resetMode) {
      case "now" -> {
        farmWorldService.resetNow();
        return CommandResult.ok("Farm world reset triggered.");
      }
      case "schedule" -> {
        Instant nextReset = farmWorldService.scheduleNextReset();
        return CommandResult.ok("Next farm reset scheduled at " + nextReset + ".");
      }
      default -> {
        return CommandResult.error("Usage: /farm reset now|schedule");
      }
    }
  }

  private static CommandResult handleFarmSetSpawn(List<String> args, FarmWorldService farmWorldService) {
    if (args.size() < 4) {
      return CommandResult.error("Usage: /farm setspawn <x> <y> <z> [worldId] [instanceId]");
    }
    double x;
    double y;
    double z;
    try {
      x = Double.parseDouble(args.get(1));
      y = Double.parseDouble(args.get(2));
      z = Double.parseDouble(args.get(3));
    } catch (NumberFormatException ex) {
      return CommandResult.error("Spawn coordinates must be numbers.");
    }
    FarmWorldSpawn currentSpawn = farmWorldService.getSpawn();
    FarmWorldSpawn spawn = new FarmWorldSpawn();
    spawn.x = x;
    spawn.y = y;
    spawn.z = z;
    if (args.size() > 4) {
      spawn.worldId = args.get(4);
    } else if (currentSpawn != null) {
      spawn.worldId = currentSpawn.worldId;
    }
    if (args.size() > 5) {
      spawn.instanceId = args.get(5);
    } else if (currentSpawn != null) {
      spawn.instanceId = currentSpawn.instanceId;
    }
    farmWorldService.updateSpawn(spawn);
    return CommandResult.ok("Spawn updated to " + x + ", " + y + ", " + z + ".");
  }

  private static CommandResult handleCombatStatus(
      List<String> args,
      String actorId,
      CombatTagService combatService) {
    String target = args.size() > 1 ? args.get(1) : actorId;
    boolean tagged = combatService.isInCombat(target);
    if (!tagged) {
      return CommandResult.ok(target + " is not in combat.");
    }
    return CommandResult.ok(target + " is in combat for " +
        (combatService.getRemainingMillis(target) / 1000) + "s.");
  }

  private static CommandResult handleCombatCanWarp(
      List<String> args,
      String actorId,
      CombatTagService combatService) {
    String target = args.size() > 1 ? args.get(1) : actorId;
    boolean canWarp = !combatService.isInCombat(target);
    return CommandResult.ok("Can warp (" + target + "): " + canWarp + ".");
  }

  private static CommandResult handleCombatTag(List<String> args, CombatTagService combatService) {
    if (args.size() < 2) {
      return CommandResult.error("Usage: /combat tag <playerId> [seconds] [reason...]");
    }
    String target = args.get(1);
    long durationSeconds = 0L;
    String reason = "manual";
    int reasonStartIndex = 2;
    if (args.size() > 2) {
      try {
        durationSeconds = Long.parseLong(args.get(2));
        reasonStartIndex = 3;
      } catch (NumberFormatException ex) {
        durationSeconds = 0L;
        reasonStartIndex = 2;
      }
    }
    if (args.size() > reasonStartIndex) {
      reason = String.join(" ", args.subList(reasonStartIndex, args.size()));
    }
    combatService.tag(target, durationSeconds, reason);
    return CommandResult.ok("Tagged " + target + " (" + (durationSeconds > 0 ? durationSeconds + "s" : "default") + ").");
  }

  private static CommandResult handleCombatQuit(List<String> args, CombatTagService combatService) {
    if (args.size() < 2) {
      return CommandResult.error("Usage: /combat quit <playerId>");
    }
    String target = args.get(1);
    boolean wasTagged = combatService.isInCombat(target);
    combatService.clear(target);
    if (!wasTagged) {
      return CommandResult.ok(target + " was not in combat.");
    }
    return CommandResult.ok("Combat tag cleared for " + target + ".");
  }

  private static CommandResult handleProtectionTest(
      List<String> args,
      String actorId,
      ProtectionService protectionService,
      FarmWorldConfig config) {
    if (args.size() < 5) {
      return CommandResult.error("Usage: /protect test <ACTION> <x> <y> <z> [perm=true|false]");
    }
    ProtectionAction action;
    try {
      action = ProtectionAction.valueOf(args.get(1).toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return CommandResult.error("Unknown action: " + args.get(1));
    }
    double x;
    double y;
    double z;
    try {
      x = Double.parseDouble(args.get(2));
      y = Double.parseDouble(args.get(3));
      z = Double.parseDouble(args.get(4));
    } catch (NumberFormatException ex) {
      return CommandResult.error("Coordinates must be numbers.");
    }
    boolean hasBypass = args.size() > 5 && "true".equalsIgnoreCase(args.get(5));
    FarmWorldSpawn center = config.protection.center != null ? config.protection.center : config.farmWorld.spawn;
    double distance = distance(center.x, center.y, center.z, x, y, z);
    ProtectionCheckRequest request = new ProtectionCheckRequest(
        actorId,
        action,
        distance,
        hasBypass,
        config.farmWorld.worldId,
        config.farmWorld.instanceId);
    boolean allowed = protectionService.isActionAllowed(request);
    return CommandResult.ok("Protection test for " + action + ": " + (allowed ? "allowed" : "blocked") +
        " (distance=" + Math.round(distance) + ", bypass=" + hasBypass + ").");
  }

  private static String formatFarmStatus(FarmWorldService farmWorldService) {
    FarmWorldStatus status = farmWorldService.getStatus();
    return "Farm world: " + status.worldId + "/" + status.instanceId +
        ", reset every " + status.resetIntervalDays +
        " days, last reset epoch=" + status.lastResetEpochSeconds +
        ", next reset epoch=" + status.nextResetEpochSeconds +
        ", last check=" + status.lastCheck + ".";
  }

  private static String formatProtectionStatus(FarmWorldConfig config) {
    FarmWorldSpawn center = config.protection.center != null ? config.protection.center : config.farmWorld.spawn;
    ProtectionActions actions = config.protection.actions;
    return "Protection: enabled=" + config.protection.enabled +
        ", radius=" + config.protection.radius +
        ", center=" + center.x + "," + center.y + "," + center.z +
        ", world=" + config.farmWorld.worldId + "/" + config.farmWorld.instanceId +
        ", actions(place=" + actions.place +
        ", break=" + actions.breakBlock +
        ", interact=" + actions.interact +
        ", damage=" + actions.damage +
        ", explosion=" + actions.explosion +
        ", fire=" + actions.fireSpread +
        ", liquid=" + actions.liquid + ").";
  }

  private static double distance(double ax, double ay, double az, double bx, double by, double bz) {
    double dx = ax - bx;
    double dy = ay - by;
    double dz = az - bz;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }
}
