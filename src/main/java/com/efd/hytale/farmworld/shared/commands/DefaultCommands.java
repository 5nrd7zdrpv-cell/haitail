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
import com.efd.hytale.farmworld.shared.services.ProtectionZoneResult;
import java.time.Instant;
import java.util.ArrayList;
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
                return CommandResult.error(error("Nutzung: /farm status|reset now|reset schedule|setspawn <x> <y> <z> [worldId] [instanceId]"));
              }
              String action = context.args.get(0).toLowerCase(Locale.ROOT);
              return switch (action) {
                case "status" -> CommandResult.ok(formatFarmStatus(farmWorldService));
                case "reset" -> handleFarmReset(context.args, farmWorldService);
                case "setspawn" -> handleFarmSetSpawn(context.args, farmWorldService);
                default -> CommandResult.error(error("Unbekannter Farm-Befehl: " + action));
              };
            }));

    registry.register(
        new CommandDefinition(
            "combat",
            "Kampfstatus und Admin-Werkzeuge.",
            List.of(),
            context -> {
              if (context.args.isEmpty()) {
                return CommandResult.error(error("Nutzung: /combat status|canwarp|tag|quit|cleanup"));
              }
              String action = context.args.get(0).toLowerCase(Locale.ROOT);
              return switch (action) {
                case "status" -> handleCombatStatus(context.args, context.actorId, combatService);
                case "canwarp" -> handleCombatCanWarp(context.args, context.actorId, combatService);
                case "tag" -> handleCombatTag(context.args, combatService);
                case "quit" -> handleCombatQuit(context.args, combatService);
                case "cleanup" -> {
                  combatService.clearAll();
                  yield CommandResult.ok(success("Alle Kampftags wurden entfernt."));
                }
                default -> CommandResult.error(error("Unbekannter Kampf-Befehl: " + action));
              };
            }));

    registry.register(
        new CommandDefinition(
            "protect",
            "Schutzzone-Status und Test.",
            List.of(),
            context -> {
              if (context.args.isEmpty()) {
                return CommandResult.error(error("Nutzung: /protect status|test <AKTION> <x> <y> <z> [perm=true|false]"));
              }
              String action = context.args.get(0).toLowerCase(Locale.ROOT);
              return switch (action) {
                case "status" -> CommandResult.ok(formatProtectionStatus(config));
                case "test" -> handleProtectionTest(context.args, context.actorId, protectionService, config);
                default -> CommandResult.error(error("Unbekannter Schutz-Befehl: " + action));
              };
            }));
  }

  private static CommandResult handleFarmReset(List<String> args, FarmWorldService farmWorldService) {
    if (args.size() < 2) {
      return CommandResult.error(error("Nutzung: /farm reset now|schedule"));
    }
    String resetMode = args.get(1).toLowerCase(Locale.ROOT);
    switch (resetMode) {
      case "now" -> {
        farmWorldService.resetNow();
        return CommandResult.ok(success("Farmwelt-Reset wurde gestartet."));
      }
      case "schedule" -> {
        Instant nextReset = farmWorldService.scheduleNextReset();
        return CommandResult.ok(success("Nächster Farmwelt-Reset geplant: " + nextReset + "."));
      }
      default -> {
        return CommandResult.error(error("Nutzung: /farm reset now|schedule"));
      }
    }
  }

  private static CommandResult handleFarmSetSpawn(List<String> args, FarmWorldService farmWorldService) {
    if (args.size() < 4) {
      return CommandResult.error(error("Nutzung: /farm setspawn <x> <y> <z> [worldId] [instanceId]"));
    }
    double x;
    double y;
    double z;
    try {
      x = Double.parseDouble(args.get(1));
      y = Double.parseDouble(args.get(2));
      z = Double.parseDouble(args.get(3));
    } catch (NumberFormatException ex) {
      return CommandResult.error(error("Spawn-Koordinaten müssen Zahlen sein."));
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
    return CommandResult.ok(success("Spawn gespeichert: " + formatCoordinate(x) + " " +
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
      return CommandResult.ok(info(displayName + " ist nicht im Kampf."));
    }
    return CommandResult.ok(info(displayName + " ist im Kampf (" +
        combatService.getRemainingSeconds(targetId) + "s verbleibend)."));
  }

  private static CommandResult handleCombatCanWarp(
      List<String> args,
      String actorId,
      CombatTagService combatService) {
    String rawTarget = args.size() > 1 ? args.get(1) : actorId;
    UUID targetId = combatService.resolvePlayerId(rawTarget);
    boolean canWarp = targetId == null || !combatService.isInCombat(targetId);
    String displayName = combatService.describePlayer(rawTarget);
    return CommandResult.ok(info("Warp für " + displayName + ": " + (canWarp ? "erlaubt" : "gesperrt") + "."));
  }

  private static CommandResult handleCombatTag(List<String> args, CombatTagService combatService) {
    if (args.size() < 2) {
      return CommandResult.error(error("Nutzung: /combat tag <player> [sekunden] [grund...]"));
    }
    String target = args.get(1);
    UUID targetId = combatService.resolvePlayerId(target);
    if (targetId == null) {
      return CommandResult.error(error("Spieler nicht gefunden: " + target + "."));
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
    return CommandResult.ok(success("Kampftag gesetzt für " + displayName + " (" + durationLabel + ")."));
  }

  private static CommandResult handleCombatQuit(List<String> args, CombatTagService combatService) {
    if (args.size() < 2) {
      return CommandResult.error(error("Nutzung: /combat quit <player>"));
    }
    String target = args.get(1);
    UUID targetId = combatService.resolvePlayerId(target);
    if (targetId == null) {
      return CommandResult.error(error("Spieler nicht gefunden: " + target + "."));
    }
    boolean wasTagged = combatService.isInCombat(targetId);
    combatService.clear(targetId);
    String displayName = combatService.getPlayerName(targetId);
    if (!wasTagged) {
      return CommandResult.ok(info(displayName + " war nicht im Kampf."));
    }
    return CommandResult.ok(success("Kampftag entfernt für " + displayName + "."));
  }

  private static CommandResult handleProtectionTest(
      List<String> args,
      String actorId,
      ProtectionService protectionService,
      FarmWorldConfig config) {
    if (args.size() < 5) {
      return CommandResult.error(error("Nutzung: /protect test <AKTION> <x> <y> <z> [perm=true|false]"));
    }
    ProtectionAction action;
    try {
      action = ProtectionAction.valueOf(args.get(1).toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return CommandResult.error(error("Unbekannte Aktion: " + args.get(1)));
    }
    double x;
    double y;
    double z;
    try {
      x = Double.parseDouble(args.get(2));
      y = Double.parseDouble(args.get(3));
      z = Double.parseDouble(args.get(4));
    } catch (NumberFormatException ex) {
      return CommandResult.error(error("Koordinaten müssen Zahlen sein."));
    }
    boolean hasBypass = args.size() > 5 && "true".equalsIgnoreCase(args.get(5));
    FarmWorldSpawn center = config.protection.center != null ? config.protection.center : config.farmWorld.spawn;
    ProtectionCheckRequest request = new ProtectionCheckRequest(
        actorId,
        action,
        x,
        y,
        z,
        center.x,
        center.y,
        center.z,
        hasBypass,
        config.farmWorld.worldId,
        config.farmWorld.instanceId);
    ProtectionZoneResult zone = protectionService.resolveZone(request);
    boolean allowed = protectionService.isActionAllowed(request);
    return CommandResult.ok(info("Schutz-Test für " + action + ": " +
        (allowed ? "erlaubt" : "blockiert") + " (Entfernung=" + Math.round(zone.distance) +
        ", Zone=" + (zone.name == null || zone.name.isBlank() ? "default" : zone.name) +
        ", Bypass=" + (hasBypass ? "ja" : "nein") + ")."));
  }

  private static String formatFarmStatus(FarmWorldService farmWorldService) {
    FarmWorldStatus status = farmWorldService.getStatus();
    return prefixLines(List.of(
        "Farmwelt: " + status.worldId + "/" + status.instanceId,
        "Reset-Intervall: " + status.resetIntervalDays + " Tage",
        "Letzter Reset: " + status.lastResetEpochSeconds,
        "Nächster Reset: " + status.nextResetEpochSeconds,
        "Letzter Check: " + status.lastCheck), "ℹ️ ");
  }

  private static String formatProtectionStatus(FarmWorldConfig config) {
    ProtectionActions actions = config.protection.actions;
    List<String> lines = new ArrayList<>();
    lines.add("Schutz: " + (config.protection.enabled ? "aktiv" : "deaktiviert"));
    if (config.protection.points != null && !config.protection.points.isEmpty()) {
      String points = config.protection.points.stream()
          .map(point -> (point.name == null || point.name.isBlank() ? "unbenannt" : point.name) +
              " (r=" + point.radius + ")")
          .collect(Collectors.joining(", "));
      lines.add("Schutzpunkte: " + points);
    } else {
      FarmWorldSpawn center = config.protection.center != null ? config.protection.center : config.farmWorld.spawn;
      lines.add("Radius: " + config.protection.radius);
      lines.add("Zentrum: " + formatCoordinate(center.x) + " " +
          formatCoordinate(center.y) + " " + formatCoordinate(center.z));
    }
    lines.add("Welt: " + config.farmWorld.worldId + "/" + config.farmWorld.instanceId);
    lines.add("Aktionen: place=" + actions.place +
        ", break=" + actions.breakBlock +
        ", interact=" + actions.interact +
        ", damage=" + actions.damage +
        ", explosion=" + actions.explosion +
        ", fire=" + actions.fireSpread +
        ", liquid=" + actions.liquid);
    return prefixLines(lines, "ℹ️ ");
  }

  private static String success(String message) {
    return PREFIX + "✅ " + message;
  }

  private static String info(String message) {
    return PREFIX + "ℹ️ " + message;
  }

  private static String error(String message) {
    return PREFIX + "❌ " + message;
  }

  private static String prefixLines(List<String> lines, String emoji) {
    return lines.stream()
        .map(line -> PREFIX + emoji + line)
        .collect(Collectors.joining("\n"));
  }

  private static String formatCoordinate(double value) {
    long rounded = Math.round(value);
    if (Math.abs(value - rounded) < 0.0001) {
      return String.valueOf(rounded);
    }
    return String.format(Locale.ROOT, "%.2f", value);
  }
}
