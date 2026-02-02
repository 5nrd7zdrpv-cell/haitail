package com.efd.hytale.farmworld.shared.commands;

import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.efd.hytale.farmworld.shared.config.FarmWorldSpawn;
import com.efd.hytale.farmworld.shared.config.ProtectionActions;
import com.efd.hytale.farmworld.shared.config.ProtectionPoint;
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
import java.util.concurrent.atomic.AtomicInteger;

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
            "Farmwelt-Status und Reset-Befehle.",
            List.of(),
            context -> {
              if (isHelpRequest(context.args)) {
                return CommandResult.ok(CommandMessages.prefixLines(farmHelpLines(), "[INFO] "));
              }
              String action = context.args.get(0).toLowerCase(Locale.ROOT);
              return switch (action) {
                case "status" -> CommandResult.ok(formatFarmStatus(farmWorldService));
                case "reset" -> handleFarmReset(context.args, farmWorldService);
                case "setspawn" -> handleFarmSetSpawn(context.args, farmWorldService);
                case "save" -> handleFarmSave(context.args, farmWorldService);
                default -> CommandResult.error(CommandMessages.error("Unbekannter Farm-Befehl: " + action));
              };
            }));

    registry.register(
        new CommandDefinition(
            "combat",
            "Kampfstatus und Admin-Werkzeuge.",
            List.of(),
            context -> {
              if (isHelpRequest(context.args)) {
                return CommandResult.ok(CommandMessages.prefixLines(combatHelpLines(), "[INFO] "));
              }
              String action = context.args.get(0).toLowerCase(Locale.ROOT);
              return switch (action) {
                case "status" -> handleCombatStatus(context.args, context.actorId, combatService);
                case "canwarp" -> handleCombatCanWarp(context.args, context.actorId, combatService);
                case "tag" -> handleCombatTag(context.args, combatService);
                case "quit" -> handleCombatQuit(context.args, combatService);
                case "cleanup" -> {
                  combatService.clearAll();
                  yield CommandResult.ok(CommandMessages.success("Alle Kampftags wurden entfernt."));
                }
                default -> CommandResult.error(CommandMessages.error("Unbekannter Kampf-Befehl: " + action));
              };
            }));

    registry.register(
        new CommandDefinition(
            "protect",
            "Schutzzone-Status und Test.",
            List.of(),
            context -> {
              if (isHelpRequest(context.args)) {
                return CommandResult.ok(CommandMessages.prefixLines(protectHelpLines(), "[INFO] "));
              }
              String action = context.args.get(0).toLowerCase(Locale.ROOT);
              return switch (action) {
                case "status" -> handleProtectionStatus(context.args, context.actorId, protectionService, config);
                case "add" -> handleProtectionAdd(context.args, farmWorldService);
                case "remove" -> handleProtectionRemove(context.args, farmWorldService);
                case "list" -> handleProtectionList(context.args, config);
                case "test" -> handleProtectionTest(context.args, context.actorId, protectionService, config);
                default -> CommandResult.error(CommandMessages.error("Unbekannter Schutz-Befehl: " + action));
              };
            }));
  }

  private static boolean isHelpRequest(List<String> args) {
    if (args == null || args.isEmpty()) {
      return true;
    }
    String value = args.get(0).toLowerCase(Locale.ROOT);
    return value.equals("--help") || value.equals("help") || value.equals("-h");
  }

  private static List<String> farmHelpLines() {
    return List.of(
        "Verfügbare /farm Befehle:",
        "  /farm status",
        "  /farm reset now",
        "  /farm reset schedule",
        "  /farm setspawn <x> <y> <z> [worldId] [instanceId]",
        "  /farm save [prefabId] [radius]",
        "  /farm setspawn self");
  }

  private static List<String> protectHelpLines() {
    return List.of(
        "Verfügbare /protect Befehle:",
        "  /protect status",
        "  /protect status <x> <y> <z> [worldId] [instanceId]",
        "  /protect add <x> <y> <z> <radius> [name]",
        "  /protect add self <radius> [name]",
        "  /protect remove <name|id>",
        "  /protect list",
        "  /protect test <AKTION> <x> <y> <z> [perm=true|false]");
  }

  private static List<String> combatHelpLines() {
    return List.of(
        "Verfügbare /combat Befehle:",
        "  /combat status [player]",
        "  /combat canwarp [player]",
        "  /combat tag <player> [sekunden] [grund...]",
        "  /combat quit <player>",
        "  /combat cleanup");
  }

  private static CommandResult handleFarmReset(List<String> args, FarmWorldService farmWorldService) {
    if (args.size() < 2) {
      return CommandResult.error(CommandMessages.error("Nutzung: /farm reset now|schedule"));
    }
    String resetMode = args.get(1).toLowerCase(Locale.ROOT);
    switch (resetMode) {
      case "now" -> {
        boolean resetOk = farmWorldService.resetNow();
        if (resetOk) {
          return CommandResult.ok(CommandMessages.success("Farmwelt-Reset wurde gestartet."));
        }
        return CommandResult.error(CommandMessages.error("Farmwelt-Reset fehlgeschlagen. Bitte Logs prüfen."));
      }
      case "schedule" -> {
        Instant nextReset = farmWorldService.scheduleNextReset();
        return CommandResult.ok(CommandMessages.success("Nächster Farmwelt-Reset geplant: " + nextReset + "."));
      }
      default -> {
        return CommandResult.error(CommandMessages.error("Nutzung: /farm reset now|schedule"));
      }
    }
  }

  private static CommandResult handleFarmSetSpawn(List<String> args, FarmWorldService farmWorldService) {
    if (args.size() < 4) {
      return CommandResult.error(CommandMessages.error(
          "Nutzung: /farm setspawn <x> <y> <z> [worldId] [instanceId]"));
    }
    double x;
    double y;
    double z;
    try {
      x = Double.parseDouble(args.get(1));
      y = Double.parseDouble(args.get(2));
      z = Double.parseDouble(args.get(3));
    } catch (NumberFormatException ex) {
      return CommandResult.error(CommandMessages.error("Spawn-Koordinaten müssen Zahlen sein."));
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
    return CommandResult.ok(CommandMessages.success("Spawn gespeichert: " +
        CommandMessages.formatCoordinate(x) + " " +
        CommandMessages.formatCoordinate(y) + " " +
        CommandMessages.formatCoordinate(z) + "."));
  }

  private static CommandResult handleFarmSave(List<String> args, FarmWorldService farmWorldService) {
    String prefabId = args.size() > 1 ? args.get(1) : null;
    Integer radius = null;
    if (args.size() > 2) {
      try {
        radius = Integer.parseInt(args.get(2));
      } catch (NumberFormatException ex) {
        return CommandResult.error(CommandMessages.error("Radius muss eine Zahl sein."));
      }
    }
    boolean saved = farmWorldService.savePrefab(prefabId, radius);
    if (!saved) {
      return CommandResult.error(CommandMessages.error("Prefab konnte nicht gespeichert werden. Bitte Logs prüfen."));
    }
    String savedName = prefabId != null && !prefabId.isBlank()
        ? prefabId
        : farmWorldService.getConfig().farmWorld.prefabSpawnId;
    return CommandResult.ok(CommandMessages.success("Prefab gespeichert: " + savedName + "."));
  }

  private static CommandResult handleCombatStatus(
      List<String> args,
      String actorId,
      CombatTagService combatService) {
    String rawTarget = args.size() > 1 ? args.get(1) : actorId;
    UUID targetId = combatService.resolvePlayerId(rawTarget);
    String displayName = combatService.describePlayer(rawTarget);
    if (targetId == null) {
      if (rawTarget == null || rawTarget.isBlank()) {
        return CommandResult.error(CommandMessages.error("Spieler nicht gefunden."));
      }
      return CommandResult.error(CommandMessages.error("Spieler nicht gefunden: " + rawTarget + "."));
    }
    if (targetId == null || !combatService.isInCombat(targetId)) {
      return CommandResult.ok(CommandMessages.info(displayName + " ist nicht im Kampf."));
    }
    return CommandResult.ok(CommandMessages.info(displayName + " ist im Kampf (" +
        combatService.getRemainingSeconds(targetId) + "s verbleibend)."));
  }

  private static CommandResult handleCombatCanWarp(
      List<String> args,
      String actorId,
      CombatTagService combatService) {
    String rawTarget = args.size() > 1 ? args.get(1) : actorId;
    UUID targetId = combatService.resolvePlayerId(rawTarget);
    if (targetId == null) {
      if (rawTarget == null || rawTarget.isBlank()) {
        return CommandResult.error(CommandMessages.error("Spieler nicht gefunden."));
      }
      return CommandResult.error(CommandMessages.error("Spieler nicht gefunden: " + rawTarget + "."));
    }
    boolean canWarp = targetId == null || !combatService.isInCombat(targetId);
    String displayName = combatService.describePlayer(rawTarget);
    return CommandResult.ok(CommandMessages.info("Warp für " + displayName + ": " +
        (canWarp ? "erlaubt" : "gesperrt") + "."));
  }

  private static CommandResult handleCombatTag(List<String> args, CombatTagService combatService) {
    if (args.size() < 2) {
      return CommandResult.error(CommandMessages.error("Nutzung: /combat tag <player> [sekunden] [grund...]"));
    }
    String target = args.get(1);
    UUID targetId = combatService.resolvePlayerId(target);
    if (targetId == null) {
      return CommandResult.error(CommandMessages.error("Spieler nicht gefunden: " + target + "."));
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
    return CommandResult.ok(CommandMessages.success("Kampftag gesetzt für " + displayName + " (" + durationLabel + ")."));
  }

  private static CommandResult handleCombatQuit(List<String> args, CombatTagService combatService) {
    if (args.size() < 2) {
      return CommandResult.error(CommandMessages.error("Nutzung: /combat quit <player>"));
    }
    String target = args.get(1);
    UUID targetId = combatService.resolvePlayerId(target);
    if (targetId == null) {
      return CommandResult.error(CommandMessages.error("Spieler nicht gefunden: " + target + "."));
    }
    boolean wasTagged = combatService.isInCombat(targetId);
    combatService.clear(targetId);
    String displayName = combatService.getPlayerName(targetId);
    if (!wasTagged) {
      return CommandResult.ok(CommandMessages.info(displayName + " war nicht im Kampf."));
    }
    return CommandResult.ok(CommandMessages.success("Kampftag entfernt für " + displayName + "."));
  }

  private static CommandResult handleProtectionTest(
      List<String> args,
      String actorId,
      ProtectionService protectionService,
      FarmWorldConfig config) {
    if (args.size() < 5) {
      return CommandResult.error(CommandMessages.error(
          "Nutzung: /protect test <AKTION> <x> <y> <z> [perm=true|false]"));
    }
    ProtectionAction action;
    try {
      action = ProtectionAction.valueOf(args.get(1).toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return CommandResult.error(CommandMessages.error("Unbekannte Aktion: " + args.get(1)));
    }
    double x;
    double y;
    double z;
    try {
      x = Double.parseDouble(args.get(2));
      y = Double.parseDouble(args.get(3));
      z = Double.parseDouble(args.get(4));
    } catch (NumberFormatException ex) {
      return CommandResult.error(CommandMessages.error("Koordinaten müssen Zahlen sein."));
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
    return CommandResult.ok(CommandMessages.info("Schutz-Test für " + action + ": " +
        (allowed ? "erlaubt" : "blockiert") + " (Entfernung=" + Math.round(zone.distance) +
        ", Zone=" + (zone.name == null || zone.name.isBlank() ? "default" : zone.name) +
        ", Bypass=" + (hasBypass ? "ja" : "nein") + ")."));
  }

  private static CommandResult handleProtectionStatus(
      List<String> args,
      String actorId,
      ProtectionService protectionService,
      FarmWorldConfig config) {
    if (args.size() < 4) {
      return CommandResult.error(CommandMessages.error(
          "Nutzung: /protect status <x> <y> <z> [worldId] [instanceId]"));
    }
    double x;
    double y;
    double z;
    try {
      x = Double.parseDouble(args.get(1));
      y = Double.parseDouble(args.get(2));
      z = Double.parseDouble(args.get(3));
    } catch (NumberFormatException ex) {
      return CommandResult.error(CommandMessages.error("Koordinaten müssen Zahlen sein."));
    }
    String worldId = args.size() > 4 ? args.get(4) : config.farmWorld.worldId;
    String instanceId = args.size() > 5 ? args.get(5) : config.farmWorld.instanceId;
    FarmWorldSpawn center = config.protection.center != null ? config.protection.center : config.farmWorld.spawn;
    ProtectionCheckRequest request = new ProtectionCheckRequest(
        actorId,
        ProtectionAction.INTERACT,
        x,
        y,
        z,
        center.x,
        center.y,
        center.z,
        false,
        worldId,
        instanceId);
    ProtectionZoneResult zone = protectionService.resolveZone(request);
    String zoneName = zone.name == null || zone.name.isBlank() ? "default" : zone.name;
    String state = zone.inside ? "IN" : "OUT";
    return CommandResult.ok(CommandMessages.info("Schutzstatus: " + state +
        " (Zone=" + zoneName +
        ", Distanz=" + Math.round(zone.distance) +
        ", Radius=" + Math.round(zone.radius) + ")."));
  }

  private static CommandResult handleProtectionAdd(
      List<String> args,
      FarmWorldService farmWorldService) {
    if (args.size() < 5) {
      return CommandResult.error(CommandMessages.error(
          "Nutzung: /protect add <x> <y> <z> <radius> [name]"));
    }
    double x;
    double y;
    double z;
    double radius;
    try {
      x = Double.parseDouble(args.get(1));
      y = Double.parseDouble(args.get(2));
      z = Double.parseDouble(args.get(3));
      radius = Double.parseDouble(args.get(4));
    } catch (NumberFormatException ex) {
      return CommandResult.error(CommandMessages.error("Koordinaten und Radius müssen Zahlen sein."));
    }
    if (radius <= 0) {
      return CommandResult.error(CommandMessages.error("Radius muss größer als 0 sein."));
    }
    String name = args.size() > 5 ? String.join(" ", args.subList(5, args.size())) : "";
    ProtectionPoint point = new ProtectionPoint();
    point.id = generatePointId(farmWorldService.getConfig().protection.points);
    point.name = name == null ? "" : name;
    point.x = x;
    point.y = y;
    point.z = z;
    point.radius = radius;
    farmWorldService.updateProtection(protection -> protection.points.add(point));
    String label = point.name == null || point.name.isBlank() ? "ohne Namen" : point.name;
    return CommandResult.ok(CommandMessages.success(
        "Schutzpunkt hinzugefügt: " + label + " (ID=" + point.id + ", r=" +
            CommandMessages.formatCoordinate(point.radius) + ")."));
  }

  private static CommandResult handleProtectionRemove(
      List<String> args,
      FarmWorldService farmWorldService) {
    if (args.size() < 2) {
      return CommandResult.error(CommandMessages.error("Nutzung: /protect remove <name|id>"));
    }
    String target = String.join(" ", args.subList(1, args.size())).trim();
    if (target.isBlank()) {
      return CommandResult.error(CommandMessages.error("Name oder ID fehlen."));
    }
    AtomicInteger removed = new AtomicInteger();
    farmWorldService.updateProtection(protection -> {
      protection.points.removeIf(point -> {
        if (point == null) {
          return false;
        }
        if (point.id != null && point.id.equalsIgnoreCase(target)) {
          removed.incrementAndGet();
          return true;
        }
        if (point.name != null && point.name.equalsIgnoreCase(target)) {
          removed.incrementAndGet();
          return true;
        }
        return false;
      });
    });
    if (removed.get() == 0) {
      return CommandResult.error(CommandMessages.error("Kein Schutzpunkt gefunden: " + target + "."));
    }
    return CommandResult.ok(CommandMessages.success("Schutzpunkt entfernt: " + target + "."));
  }

  private static CommandResult handleProtectionList(
      List<String> args,
      FarmWorldConfig config) {
    List<ProtectionPoint> points = config.protection.points == null ? List.of() : config.protection.points;
    if (points.isEmpty()) {
      return CommandResult.ok(CommandMessages.info("Keine Schutzpunkte definiert."));
    }
    List<String> lines = new ArrayList<>();
    for (ProtectionPoint point : points) {
      if (point == null) {
        continue;
      }
      String name = point.name == null || point.name.isBlank() ? "ohne Namen" : point.name;
      String id = point.id == null || point.id.isBlank() ? "ohne-id" : point.id;
      lines.add("ID=" + id +
          ", Name=" + name +
          ", Pos=" + CommandMessages.formatCoordinate(point.x) + " " +
          CommandMessages.formatCoordinate(point.y) + " " +
          CommandMessages.formatCoordinate(point.z) +
          ", r=" + CommandMessages.formatCoordinate(point.radius));
    }
    return CommandResult.ok(CommandMessages.prefixLines(lines, "[INFO] "));
  }

  private static String formatFarmStatus(FarmWorldService farmWorldService) {
    FarmWorldStatus status = farmWorldService.getStatus();
    FarmWorldSpawn spawn = farmWorldService.resolveSpawn();
    ProtectionActions actions = farmWorldService.getConfig().protection.actions;
    return CommandMessages.prefixLines(List.of(
        "Farmwelt: " + status.worldId + "/" + status.instanceId,
        "Spawn: " + CommandMessages.formatCoordinate(spawn.x) + " " +
            CommandMessages.formatCoordinate(spawn.y) + " " +
            CommandMessages.formatCoordinate(spawn.z),
        "Prefab: " + farmWorldService.getConfig().farmWorld.prefabSpawnId,
        "Reset-Intervall: " + status.resetIntervalDays + " Tage",
        "Nächster Reset in: " + CommandMessages.formatSecondsUntil(status.nextResetEpochSeconds),
        "Letzter Reset: " + status.lastResetEpochSeconds,
        "Letzter Check: " + status.lastCheck,
        "Schutz aktiv: " + (farmWorldService.getConfig().protection.enabled ? "ja" : "nein"),
        "Schutz-Aktionen: place=" + actions.place +
            ", break=" + actions.breakBlock +
            ", interact=" + actions.interact +
            ", damage=" + actions.damage +
            ", explosion=" + actions.explosion +
            ", fire=" + actions.fireSpread +
            ", liquid=" + actions.liquid), "[INFO] ");
  }

  private static String generatePointId(List<ProtectionPoint> points) {
    String base = "p" + Long.toString(System.currentTimeMillis(), 36);
    if (points == null) {
      return base;
    }
    String candidate = base;
    int counter = 1;
    while (containsPointId(points, candidate)) {
      candidate = base + counter;
      counter++;
    }
    return candidate;
  }

  private static boolean containsPointId(List<ProtectionPoint> points, String candidate) {
    for (ProtectionPoint point : points) {
      if (point != null && point.id != null && candidate.equalsIgnoreCase(point.id)) {
        return true;
      }
    }
    return false;
  }
}
