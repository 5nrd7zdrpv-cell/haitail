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
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultCommands {
  private static final String PREFIX = "[FarmWorld] ";

  public void register(
      CommandRegistry registry,
      FarmWorldService farmWorldService,
      CombatTagService combatService,
      ProtectionService protectionService,
      FarmWorldConfig config) {
    registry.register(
        new CommandDefinition(
            "farm",
            "Farmwelt-Status und Reset-Befehle.",
            List.of(),
            context -> {
              if (context.args.isEmpty()) {
                return CommandResult.error(withPrefix("Nutzung: /farm status|reset now|reset schedule|setspawn <x> <y> <z> [worldId] [instanceId]"));
              }
              String action = context.args.get(0).toLowerCase(Locale.ROOT);
              return switch (action) {
                case "status" -> CommandResult.ok(formatFarmStatus(farmWorldService));
                case "reset" -> handleFarmReset(context.args, farmWorldService);
                case "setspawn" -> handleFarmSetSpawn(context.args, farmWorldService);
                default -> CommandResult.error(withPrefix("Unbekannter Farm-Befehl: " + action));
              };
            }));

    registry.register(
        new CommandDefinition(
            "combat",
            "Kampfstatus und Admin-Werkzeuge.",
            List.of(),
            context -> {
              if (context.args.isEmpty()) {
                return CommandResult.error(withPrefix("Nutzung: /combat status|canwarp|tag|quit|cleanup"));
              }
              String action = context.args.get(0).toLowerCase(Locale.ROOT);
              return switch (action) {
                case "status" -> handleCombatStatus(context.args, context.actorId, combatService);
                case "canwarp" -> handleCombatCanWarp(context.args, context.actorId, combatService);
                case "tag" -> handleCombatTag(context.args, combatService);
                case "quit" -> handleCombatQuit(context.args, combatService);
                case "cleanup" -> {
                  combatService.clearAll();
                  yield CommandResult.ok(withPrefix("Alle Kampftags wurden entfernt."));
                }
                default -> CommandResult.error(withPrefix("Unbekannter Kampf-Befehl: " + action));
              };
            }));

    registry.register(
        new CommandDefinition(
            "protect",
            "Schutzzone-Status und Test.",
            List.of(),
            context -> {
              if (context.args.isEmpty()) {
                return CommandResult.error(withPrefix("Nutzung: /protect status|test <AKTION> <x> <y> <z> [perm=true|false]"));
              }
              String action = context.args.get(0).toLowerCase(Locale.ROOT);
              return switch (action) {
                case "status" -> CommandResult.ok(formatProtectionStatus(config));
                case "test" -> handleProtectionTest(context.args, context.actorId, protectionService, config);
                default -> CommandResult.error(withPrefix("Unbekannter Schutz-Befehl: " + action));
              };
            }));
  }

  private static CommandResult handleFarmReset(List<String> args, FarmWorldService farmWorldService) {
    if (args.size() < 2) {
      return CommandResult.error(withPrefix("Nutzung: /farm reset now|schedule"));
    }
    String resetMode = args.get(1).toLowerCase(Locale.ROOT);
    switch (resetMode) {
      case "now" -> {
        farmWorldService.resetNow();
        return CommandResult.ok(withPrefix("Farmwelt-Reset wurde gestartet."));
      }
      case "schedule" -> {
        Instant nextReset = farmWorldService.scheduleNextReset();
        return CommandResult.ok(withPrefix("Nächster Farmwelt-Reset geplant: " + nextReset + "."));
      }
      default -> {
        return CommandResult.error(withPrefix("Nutzung: /farm reset now|schedule"));
      }
    }
  }

  private static CommandResult handleFarmSetSpawn(List<String> args, FarmWorldService farmWorldService) {
    if (args.size() < 4) {
      return CommandResult.error(withPrefix("Nutzung: /farm setspawn <x> <y> <z> [worldId] [instanceId]"));
    }
    double x;
    double y;
    double z;
    try {
      x = Double.parseDouble(args.get(1));
      y = Double.parseDouble(args.get(2));
      z = Double.parseDouble(args.get(3));
    } catch (NumberFormatException ex) {
      return CommandResult.error(withPrefix("Spawn-Koordinaten müssen Zahlen sein."));
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
    return CommandResult.ok(withPrefix("Spawn gespeichert: " + formatCoordinate(x) + " " +
        formatCoordinate(y) + " " + formatCoordinate(z) + "."));
  }

  private static CommandResult handleCombatStatus(
      List<String> args,
      String actorId,
      CombatTagService combatService) {
    String rawTarget = args.size() > 1 ? args.get(1) : actorId;
    UUID targetId = combatService.resolvePlayerId(rawTarget);
    String displayName = combatService.describePlayer(rawTarget);
    if (targetId == null || !combatService.isInCombat(targetId)) {
      return CommandResult.ok(withPrefix(displayName + " ist nicht im Kampf."));
    }
    return CommandResult.ok(withPrefix(displayName + " ist im Kampf für " +
        combatService.getRemainingSeconds(targetId) + " Sekunden."));
  }

  private static CommandResult handleCombatCanWarp(
      List<String> args,
      String actorId,
      CombatTagService combatService) {
    String rawTarget = args.size() > 1 ? args.get(1) : actorId;
    UUID targetId = combatService.resolvePlayerId(rawTarget);
    boolean canWarp = targetId == null || !combatService.isInCombat(targetId);
    String displayName = combatService.describePlayer(rawTarget);
    return CommandResult.ok(withPrefix("Warp möglich für " + displayName + ": " +
        (canWarp ? "ja" : "nein") + "."));
  }

  private static CommandResult handleCombatTag(List<String> args, CombatTagService combatService) {
    if (args.size() < 2) {
      return CommandResult.error(withPrefix("Nutzung: /combat tag <player> [sekunden] [grund...]"));
    }
    String target = args.get(1);
    UUID targetId = combatService.resolvePlayerId(target);
    if (targetId == null) {
      return CommandResult.error(withPrefix("Spieler nicht gefunden: " + target + "."));
    }
    long durationSeconds = 0L;
    if (args.size() > 2) {
      try {
        durationSeconds = Long.parseLong(args.get(2));
      } catch (NumberFormatException ex) {
        durationSeconds = 0L;
      }
    }
    combatService.tag(targetId, durationSeconds);
    String displayName = combatService.getPlayerName(targetId);
    String durationLabel = durationSeconds > 0 ? durationSeconds + "s" : "Standard";
    return CommandResult.ok(withPrefix("Kampftag gesetzt für " + displayName + " (" + durationLabel + ")."));
  }

  private static CommandResult handleCombatQuit(List<String> args, CombatTagService combatService) {
    if (args.size() < 2) {
      return CommandResult.error(withPrefix("Nutzung: /combat quit <player>"));
    }
    String target = args.get(1);
    UUID targetId = combatService.resolvePlayerId(target);
    if (targetId == null) {
      return CommandResult.error(withPrefix("Spieler nicht gefunden: " + target + "."));
    }
    boolean wasTagged = combatService.isInCombat(targetId);
    combatService.clear(targetId);
    String displayName = combatService.getPlayerName(targetId);
    if (!wasTagged) {
      return CommandResult.ok(withPrefix(displayName + " war nicht im Kampf."));
    }
    return CommandResult.ok(withPrefix("Kampftag entfernt für " + displayName + "."));
  }

  private static CommandResult handleProtectionTest(
      List<String> args,
      String actorId,
      ProtectionService protectionService,
      FarmWorldConfig config) {
    if (args.size() < 5) {
      return CommandResult.error(withPrefix("Nutzung: /protect test <AKTION> <x> <y> <z> [perm=true|false]"));
    }
    ProtectionAction action;
    try {
      action = ProtectionAction.valueOf(args.get(1).toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return CommandResult.error(withPrefix("Unbekannte Aktion: " + args.get(1)));
    }
    double x;
    double y;
    double z;
    try {
      x = Double.parseDouble(args.get(2));
      y = Double.parseDouble(args.get(3));
      z = Double.parseDouble(args.get(4));
    } catch (NumberFormatException ex) {
      return CommandResult.error(withPrefix("Koordinaten müssen Zahlen sein."));
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
    return CommandResult.ok(withPrefix("Schutz-Test für " + action + ": " +
        (allowed ? "erlaubt" : "blockiert") + " (Entfernung=" + Math.round(distance) +
        ", Bypass=" + (hasBypass ? "ja" : "nein") + ")."));
  }

  private static String formatFarmStatus(FarmWorldService farmWorldService) {
    FarmWorldStatus status = farmWorldService.getStatus();
    return withPrefixLines(List.of(
        "Farmwelt: " + status.worldId + "/" + status.instanceId,
        "Reset-Intervall: " + status.resetIntervalDays + " Tage",
        "Letzter Reset: " + status.lastResetEpochSeconds,
        "Nächster Reset: " + status.nextResetEpochSeconds,
        "Letzter Check: " + status.lastCheck));
  }

  private static String formatProtectionStatus(FarmWorldConfig config) {
    FarmWorldSpawn center = config.protection.center != null ? config.protection.center : config.farmWorld.spawn;
    ProtectionActions actions = config.protection.actions;
    return withPrefixLines(List.of(
        "Schutz: " + (config.protection.enabled ? "aktiv" : "deaktiviert"),
        "Radius: " + config.protection.radius,
        "Zentrum: " + formatCoordinate(center.x) + " " + formatCoordinate(center.y) + " " + formatCoordinate(center.z),
        "Welt: " + config.farmWorld.worldId + "/" + config.farmWorld.instanceId,
        "Aktionen: place=" + actions.place +
            ", break=" + actions.breakBlock +
            ", interact=" + actions.interact +
            ", damage=" + actions.damage +
            ", explosion=" + actions.explosion +
            ", fire=" + actions.fireSpread +
            ", liquid=" + actions.liquid));
  }

  private static double distance(double ax, double ay, double az, double bx, double by, double bz) {
    double dx = ax - bx;
    double dy = ay - by;
    double dz = az - bz;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  private static String withPrefix(String message) {
    return PREFIX + message;
  }

  private static String withPrefixLines(List<String> lines) {
    return lines.stream().map(DefaultCommands::withPrefix).collect(Collectors.joining("\n"));
  }

  private static String formatCoordinate(double value) {
    long rounded = Math.round(value);
    if (Math.abs(value - rounded) < 0.0001) {
      return String.valueOf(rounded);
    }
    return String.format(Locale.ROOT, "%.2f", value);
  }
}
