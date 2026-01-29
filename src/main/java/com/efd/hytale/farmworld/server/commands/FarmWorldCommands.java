package com.efd.hytale.farmworld.server.commands;

import com.efd.hytale.farmworld.shared.commands.CommandRegistry;
import com.efd.hytale.farmworld.shared.commands.CommandResult;
import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.efd.hytale.farmworld.shared.config.FarmWorldSpawn;
import com.efd.hytale.farmworld.shared.config.ProtectionPoint;
import com.efd.hytale.farmworld.shared.services.CombatTagService;
import com.efd.hytale.farmworld.shared.services.FarmWorldService;
import com.efd.hytale.farmworld.shared.services.ProtectionCheckRequest;
import com.efd.hytale.farmworld.shared.services.ProtectionService;
import com.efd.hytale.farmworld.shared.services.ProtectionZoneResult;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.math.vector.Vector3d;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class FarmWorldCommands {
  private FarmWorldCommands() {}

  private static final String USE_PERMISSION = "";
  private static final String PREFIX = "[FarmWorld] ";

  public static AbstractCommandCollection createFarmCommand(
      CommandRegistry registry,
      String adminPermission,
      FarmWorldService farmWorldService,
      FarmWorldConfig config) {
    return new FarmCommand(registry, adminPermission, farmWorldService, config);
  }

  public static AbstractCommandCollection createProtectCommand(
      CommandRegistry registry,
      String adminPermission,
      FarmWorldConfig config,
      FarmWorldService farmWorldService,
      ProtectionService protectionService) {
    return new ProtectCommand(registry, adminPermission, config, farmWorldService, protectionService);
  }

  public static AbstractCommandCollection createCombatCommand(
      CommandRegistry registry,
      String adminPermission,
      CombatTagService combatService) {
    return new CombatCommand(registry, adminPermission, combatService);
  }

  private static String actorId(CommandContext context) {
    UUID uuid = context.sender().getUuid();
    return uuid != null ? uuid.toString() : context.sender().getDisplayName();
  }

  private static void sendResult(CommandContext context, CommandResult result) {
    context.sendMessage(Message.raw(result.message));
  }

  private static boolean ensurePermission(CommandContext context, String permission) {
    if (permission == null || permission.isBlank()) {
      return true;
    }
    if (context.sender().hasPermission(permission)) {
      return true;
    }
    context.sendMessage(Message.raw(error("Fehlende Berechtigung: " + permission + ".")));
    return false;
  }

  private static final class FarmCommand extends AbstractCommandCollection {
    FarmCommand(
        CommandRegistry registry,
        String adminPermission,
        FarmWorldService farmWorldService,
        FarmWorldConfig config) {
      super("farm", "Farmwelt-Status und Reset-Befehle.");
      addSubCommand(new FarmStatusCommand(registry));
      addSubCommand(new FarmResetCommand(registry, adminPermission));
      addSubCommand(new FarmSetSpawnCommand(adminPermission, farmWorldService, config));
    }
  }

  private static final class FarmStatusCommand extends CommandBase {
    private final CommandRegistry registry;
    private final String usePermission;

    FarmStatusCommand(CommandRegistry registry) {
      super("status", "Zeigt den Farmwelt-Status.");
      this.registry = registry;
      this.usePermission = USE_PERMISSION;
    }

    @Override
    protected void executeSync(CommandContext context) {
      if (!ensurePermission(context, usePermission)) {
        return;
      }
      CommandResult result = registry.execute(actorId(context), "farm", List.of("status"));
      sendResult(context, result);
    }
  }

  private static final class FarmResetCommand extends AbstractCommandCollection {
    FarmResetCommand(CommandRegistry registry, String adminPermission) {
      super("reset", "Setzt die Farmwelt zurück.");
      addSubCommand(new FarmResetNowCommand(registry, adminPermission));
      addSubCommand(new FarmResetScheduleCommand(registry, adminPermission));
    }
  }

  private static final class FarmResetNowCommand extends CommandBase {
    private final CommandRegistry registry;

    FarmResetNowCommand(CommandRegistry registry, String adminPermission) {
      super("now", "Setzt die Farmwelt sofort zurück.");
      this.registry = registry;
      this.adminPermission = adminPermission;
    }

    private final String adminPermission;

    @Override
    protected void executeSync(CommandContext context) {
      if (!ensurePermission(context, adminPermission)) {
        return;
      }
      CommandResult result = registry.execute(actorId(context), "farm", List.of("reset", "now"));
      sendResult(context, result);
    }
  }

  private static final class FarmResetScheduleCommand extends CommandBase {
    private final CommandRegistry registry;

    FarmResetScheduleCommand(CommandRegistry registry, String adminPermission) {
      super("schedule", "Plant den nächsten Farmwelt-Reset.");
      this.registry = registry;
      this.adminPermission = adminPermission;
    }

    private final String adminPermission;

    @Override
    protected void executeSync(CommandContext context) {
      if (!ensurePermission(context, adminPermission)) {
        return;
      }
      CommandResult result = registry.execute(actorId(context), "farm", List.of("reset", "schedule"));
      sendResult(context, result);
    }
  }

  private static final class FarmSetSpawnCommand extends CommandBase {
    private final FarmWorldService farmWorldService;
    private final FarmWorldConfig config;
    private final RequiredArg<String> selfOrXArg;
    private final OptionalArg<Double> yArg;
    private final OptionalArg<Double> zArg;
    private final OptionalArg<String> worldIdArg;
    private final OptionalArg<String> instanceIdArg;

    FarmSetSpawnCommand(
        String adminPermission,
        FarmWorldService farmWorldService,
        FarmWorldConfig config) {
      super("setspawn", "Setzt den Farmwelt-Spawn.");
      this.farmWorldService = farmWorldService;
      this.config = config;
      requirePermission(adminPermission);
      selfOrXArg = withRequiredArg("self|x", "self oder X", ArgTypes.STRING);
      yArg = withOptionalArg("y", "Spawn Y", ArgTypes.DOUBLE);
      zArg = withOptionalArg("z", "Spawn Z", ArgTypes.DOUBLE);
      worldIdArg = withOptionalArg("worldId", "Welt-Id", ArgTypes.STRING);
      instanceIdArg = withOptionalArg("instanceId", "Instanz-Id", ArgTypes.STRING);
    }

    private final String adminPermission;

    @Override
    protected void executeSync(CommandContext context) {
      String firstArg = context.get(selfOrXArg);
      if ("self".equalsIgnoreCase(firstArg)) {
        if (!context.isPlayer()) {
          context.sendMessage(Message.raw(error("Nur Spieler können /farm setspawn self verwenden.")));
          return;
        }
        Player player = context.senderAs(Player.class);
        TransformComponent transform = player.getTransformComponent();
        if (transform == null || transform.getPosition() == null) {
          context.sendMessage(Message.raw(error("Position konnte nicht ermittelt werden.")));
          return;
        }
        Vector3d position = transform.getPosition();
        FarmWorldSpawn spawn = new FarmWorldSpawn();
        spawn.x = position.x;
        spawn.y = position.y;
        spawn.z = position.z;
        spawn.worldId = player.getWorld() != null ? player.getWorld().getName() : config.farmWorld.worldId;
        spawn.instanceId = config.farmWorld.instanceId;
        farmWorldService.updateSpawn(spawn);
        context.sendMessage(Message.raw(success("Spawn gespeichert: " +
            formatCoordinate(spawn.x) + " " + formatCoordinate(spawn.y) + " " + formatCoordinate(spawn.z) + ".")));
        return;
      }
      if (!context.provided(yArg) || !context.provided(zArg)) {
        context.sendMessage(Message.raw(error("Nutzung: /farm setspawn <x> <y> <z> [worldId] [instanceId]")));
        return;
      }
      double x;
      try {
        x = Double.parseDouble(firstArg);
      } catch (NumberFormatException ex) {
        context.sendMessage(Message.raw(error("X muss eine Zahl sein oder 'self'.")));
        return;
      }
      double y = context.get(yArg);
      double z = context.get(zArg);
      FarmWorldSpawn currentSpawn = farmWorldService.getSpawn();
      FarmWorldSpawn spawn = new FarmWorldSpawn();
      spawn.x = x;
      spawn.y = y;
      spawn.z = z;
      if (context.provided(worldIdArg)) {
        spawn.worldId = context.get(worldIdArg);
      } else if (currentSpawn != null) {
        spawn.worldId = currentSpawn.worldId;
      }
      if (context.provided(instanceIdArg)) {
        spawn.instanceId = context.get(instanceIdArg);
      } else if (currentSpawn != null) {
        spawn.instanceId = currentSpawn.instanceId;
      }
      farmWorldService.updateSpawn(spawn);
      context.sendMessage(Message.raw(success("Spawn gespeichert: " +
          formatCoordinate(spawn.x) + " " + formatCoordinate(spawn.y) + " " + formatCoordinate(spawn.z) + ".")));
    }
  }

  private static final class ProtectCommand extends AbstractCommandCollection {
    ProtectCommand(
        CommandRegistry registry,
        String adminPermission,
        FarmWorldConfig config,
        FarmWorldService farmWorldService,
        ProtectionService protectionService) {
      super("protect", "Schutzzone-Status und Test.");
      addSubCommand(new ProtectStatusCommand(config, protectionService));
      addSubCommand(new ProtectAddCommand(adminPermission, config, farmWorldService));
      addSubCommand(new ProtectRemoveCommand(adminPermission, config, farmWorldService));
      addSubCommand(new ProtectListCommand(config));
      addSubCommand(new ProtectTestCommand(registry, adminPermission));
    }
  }

  private static final class ProtectStatusCommand extends CommandBase {
    private final FarmWorldConfig config;
    private final ProtectionService protectionService;
    private final OptionalArg<String> targetArg;

    ProtectStatusCommand(FarmWorldConfig config, ProtectionService protectionService) {
      super("status", "Zeigt den Schutzstatus.");
      this.config = config;
      this.protectionService = protectionService;
      targetArg = withOptionalArg("self|name", "self oder Schutzpunkt", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(CommandContext context) {
      String target = context.provided(targetArg) ? context.get(targetArg) : "self";
      if ("self".equalsIgnoreCase(target)) {
        if (!context.isPlayer()) {
          context.sendMessage(Message.raw(error("Nur Spieler können den Schutzstatus abfragen.")));
          return;
        }
        Player player = context.senderAs(Player.class);
        TransformComponent transform = player.getTransformComponent();
        Vector3d position = transform != null ? transform.getPosition() : null;
        if (position == null) {
          context.sendMessage(Message.raw(error("Position konnte nicht ermittelt werden.")));
          return;
        }
        FarmWorldSpawn center = config.protection.center != null ? config.protection.center : config.farmWorld.spawn;
        ProtectionCheckRequest request = new ProtectionCheckRequest(
            "status",
            com.efd.hytale.farmworld.shared.services.ProtectionAction.INTERACT,
            position.x,
            position.y,
            position.z,
            center.x,
            center.y,
            center.z,
            false,
            config.farmWorld.worldId,
            config.farmWorld.instanceId);
        ProtectionZoneResult zone = protectionService.resolveZone(request);
        List<String> lines = new ArrayList<>();
        lines.add("Schutz: " + (config.protection.enabled ? "aktiv" : "deaktiviert"));
        if (config.protection.points != null && !config.protection.points.isEmpty()) {
          String points = config.protection.points.stream()
              .map(point -> (point.name == null || point.name.isBlank() ? "unbenannt" : point.name) +
                  " (r=" + point.radius + ")")
              .sorted()
              .collect(java.util.stream.Collectors.joining(", "));
          lines.add("Schutzpunkte: " + points);
        } else {
          lines.add("Radius: " + config.protection.radius);
          lines.add("Zentrum: " + formatCoordinate(center.x) + " " +
              formatCoordinate(center.y) + " " + formatCoordinate(center.z));
        }
        lines.add("Zone: " + (zone.name == null || zone.name.isBlank() ? "default" : zone.name));
        lines.add("Entfernung: " + Math.round(zone.distance) + " Blöcke (" +
            (zone.inside ? "INNERHALB" : "AUSSERHALB") + ")");
        context.sendMessage(Message.raw(prefixLines(lines, "ℹ️ ")));
        return;
      }
      ProtectionPoint point = findProtectionPoint(config, target);
      if (point == null) {
        context.sendMessage(Message.raw(error("Schutzpunkt nicht gefunden: " + target + ".")));
        return;
      }
      List<String> lines = List.of(
          "Schutzpunkt: " + point.name,
          "Radius: " + point.radius,
          "Position: " + formatCoordinate(point.x) + " " +
              formatCoordinate(point.y) + " " + formatCoordinate(point.z));
      context.sendMessage(Message.raw(prefixLines(lines, "ℹ️ ")));
    }
  }

  private static final class ProtectAddCommand extends CommandBase {
    private final FarmWorldConfig config;
    private final FarmWorldService farmWorldService;
    private final RequiredArg<String> nameArg;
    private final RequiredArg<String> targetArg;
    private final OptionalArg<Integer> radiusArg;

    ProtectAddCommand(String adminPermission, FarmWorldConfig config, FarmWorldService farmWorldService) {
      super("add", "Fügt einen Schutzpunkt hinzu.");
      this.config = config;
      this.farmWorldService = farmWorldService;
      requirePermission(adminPermission);
      nameArg = withRequiredArg("name", "Name", ArgTypes.STRING);
      targetArg = withRequiredArg("self", "self", ArgTypes.STRING);
      radiusArg = withOptionalArg("radius", "Radius", ArgTypes.INTEGER);
    }

    @Override
    protected void executeSync(CommandContext context) {
      if (!context.isPlayer()) {
        context.sendMessage(Message.raw(error("Nur Spieler können Schutzpunkte setzen.")));
        return;
      }
      String target = context.get(targetArg);
      if (!"self".equalsIgnoreCase(target)) {
        context.sendMessage(Message.raw(error("Nutzung: /protect add <name> self [radius]")));
        return;
      }
      String name = context.get(nameArg);
      if (name == null || name.isBlank()) {
        context.sendMessage(Message.raw(error("Name darf nicht leer sein.")));
        return;
      }
      if (findProtectionPoint(config, name) != null) {
        context.sendMessage(Message.raw(error("Schutzpunkt existiert bereits: " + name + ".")));
        return;
      }
      Player player = context.senderAs(Player.class);
      TransformComponent transform = player.getTransformComponent();
      Vector3d position = transform != null ? transform.getPosition() : null;
      if (position == null) {
        context.sendMessage(Message.raw(error("Position konnte nicht ermittelt werden.")));
        return;
      }
      int radius = config.protection.radius;
      if (context.provided(radiusArg)) {
        radius = context.get(radiusArg);
      }
      if (radius <= 0) {
        context.sendMessage(Message.raw(error("Radius muss > 0 sein.")));
        return;
      }
      ProtectionPoint point = new ProtectionPoint();
      point.name = name;
      point.x = position.x;
      point.y = position.y;
      point.z = position.z;
      point.radius = radius;
      farmWorldService.updateProtection(protection -> {
        if (protection.points == null) {
          protection.points = new java.util.ArrayList<>();
        }
        protection.points.add(point);
      });
      context.sendMessage(Message.raw(success("Schutzpunkt gespeichert: " + name + " (" +
          formatCoordinate(point.x) + " " + formatCoordinate(point.y) + " " + formatCoordinate(point.z) +
          ", r=" + radius + ").")));
    }
  }

  private static final class ProtectRemoveCommand extends CommandBase {
    private final FarmWorldConfig config;
    private final FarmWorldService farmWorldService;
    private final RequiredArg<String> nameArg;

    ProtectRemoveCommand(String adminPermission, FarmWorldConfig config, FarmWorldService farmWorldService) {
      super("remove", "Entfernt einen Schutzpunkt.");
      this.config = config;
      this.farmWorldService = farmWorldService;
      requirePermission(adminPermission);
      nameArg = withRequiredArg("name", "Name", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(CommandContext context) {
      String name = context.get(nameArg);
      ProtectionPoint point = findProtectionPoint(config, name);
      if (point == null) {
        context.sendMessage(Message.raw(error("Schutzpunkt nicht gefunden: " + name + ".")));
        return;
      }
      farmWorldService.updateProtection(protection -> {
        protection.points.removeIf(existing -> existing.name != null
            && existing.name.equalsIgnoreCase(point.name));
      });
      context.sendMessage(Message.raw(success("Schutzpunkt entfernt: " + point.name + ".")));
    }
  }

  private static final class ProtectListCommand extends CommandBase {
    private final FarmWorldConfig config;

    ProtectListCommand(FarmWorldConfig config) {
      super("list", "Listet Schutzpunkte auf.");
      this.config = config;
    }

    @Override
    protected void executeSync(CommandContext context) {
      if (config.protection.points == null || config.protection.points.isEmpty()) {
        context.sendMessage(Message.raw(info("Keine Schutzpunkte gesetzt.")));
        return;
      }
      List<ProtectionPoint> points = new ArrayList<>(config.protection.points);
      points.sort(Comparator.comparing(point -> point.name == null ? "" : point.name.toLowerCase(Locale.ROOT)));
      List<String> lines = new ArrayList<>();
      lines.add("Schutzpunkte: " + points.size());
      for (ProtectionPoint point : points) {
        String name = point.name == null ? "unbenannt" : point.name;
        lines.add("- " + name + " (r=" + point.radius + ") @ " +
            formatCoordinate(point.x) + " " + formatCoordinate(point.y) + " " +
            formatCoordinate(point.z));
      }
      context.sendMessage(Message.raw(prefixLines(lines, "ℹ️ ")));
    }
  }

  private static final class ProtectTestCommand extends CommandBase {
    private final CommandRegistry registry;
    private final RequiredArg<String> actionArg;
    private final RequiredArg<Double> xArg;
    private final RequiredArg<Double> yArg;
    private final RequiredArg<Double> zArg;
    private final OptionalArg<Boolean> bypassArg;

    ProtectTestCommand(CommandRegistry registry, String adminPermission) {
      super("test", "Testet eine Schutzaktion.");
      this.registry = registry;
      requirePermission(adminPermission);
      actionArg = withRequiredArg("action", "Schutzaktion", ArgTypes.STRING);
      xArg = withRequiredArg("x", "X-Koordinate", ArgTypes.DOUBLE);
      yArg = withRequiredArg("y", "Y-Koordinate", ArgTypes.DOUBLE);
      zArg = withRequiredArg("z", "Z-Koordinate", ArgTypes.DOUBLE);
      bypassArg = withOptionalArg("bypass", "Bypass", ArgTypes.BOOLEAN);
    }

    private final String adminPermission;

    @Override
    protected void executeSync(CommandContext context) {
      if (!ensurePermission(context, adminPermission)) {
        return;
      }
      List<String> args = new ArrayList<>();
      args.add("test");
      args.add(context.get(actionArg));
      args.add(String.valueOf(context.get(xArg)));
      args.add(String.valueOf(context.get(yArg)));
      args.add(String.valueOf(context.get(zArg)));
      if (context.provided(bypassArg)) {
        args.add(String.valueOf(context.get(bypassArg)));
      }
      CommandResult result = registry.execute(actorId(context), "protect", args);
      sendResult(context, result);
    }
  }

  private static final class CombatCommand extends AbstractCommandCollection {
    CombatCommand(CommandRegistry registry, String adminPermission, CombatTagService combatService) {
      super("combat", "Kampfstatus und Admin-Werkzeuge.");
      addSubCommand(new CombatStatusCommand(registry, combatService));
      addSubCommand(new CombatCanWarpCommand(registry, combatService));
      addSubCommand(new CombatTagCommand(registry, adminPermission, combatService));
      addSubCommand(new CombatCleanupCommand(registry, adminPermission));
      addSubCommand(new CombatQuitCommand(registry, adminPermission, combatService));
    }
  }

  private static final class CombatStatusCommand extends CommandBase {
    private final CommandRegistry registry;
    private final CombatTagService combatService;
    private final OptionalArg<PlayerRef> targetArg;
    private final String usePermission;

    CombatStatusCommand(CommandRegistry registry, CombatTagService combatService) {
      super("status", "Prüft den Kampfstatus.");
      this.registry = registry;
      this.combatService = combatService;
      this.usePermission = USE_PERMISSION;
      targetArg = withOptionalArg("spieler", "Spieler", ArgTypes.PLAYER_REF);
    }

    @Override
    protected void executeSync(CommandContext context) {
      if (!ensurePermission(context, usePermission)) {
        return;
      }
      List<String> args = new ArrayList<>();
      args.add("status");
      if (context.provided(targetArg)) {
        PlayerRef target = context.get(targetArg);
        combatService.recordPlayer(target.getUuid(), target.getUsername());
        args.add(target.getUsername());
      }
      CommandResult result = registry.execute(actorId(context), "combat", args);
      sendResult(context, result);
    }
  }

  private static final class CombatCanWarpCommand extends CommandBase {
    private final CommandRegistry registry;
    private final CombatTagService combatService;
    private final OptionalArg<PlayerRef> targetArg;
    private final String usePermission;

    CombatCanWarpCommand(CommandRegistry registry, CombatTagService combatService) {
      super("canwarp", "Prüft, ob ein Spieler warpen darf.");
      this.registry = registry;
      this.combatService = combatService;
      this.usePermission = USE_PERMISSION;
      targetArg = withOptionalArg("spieler", "Spieler", ArgTypes.PLAYER_REF);
    }

    @Override
    protected void executeSync(CommandContext context) {
      if (!ensurePermission(context, usePermission)) {
        return;
      }
      List<String> args = new ArrayList<>();
      args.add("canwarp");
      if (context.provided(targetArg)) {
        PlayerRef target = context.get(targetArg);
        combatService.recordPlayer(target.getUuid(), target.getUsername());
        args.add(target.getUsername());
      }
      CommandResult result = registry.execute(actorId(context), "combat", args);
      sendResult(context, result);
    }
  }

  private static final class CombatTagCommand extends CommandBase {
    private final CommandRegistry registry;
    private final CombatTagService combatService;
    private final RequiredArg<PlayerRef> targetArg;
    private final OptionalArg<Integer> secondsArg;
    private final OptionalArg<List<String>> reasonArg;

    CombatTagCommand(CommandRegistry registry, String adminPermission, CombatTagService combatService) {
      super("tag", "Setzt ein Kampftag.");
      this.registry = registry;
      this.combatService = combatService;
      requirePermission(adminPermission);
      targetArg = withRequiredArg("spieler", "Spieler", ArgTypes.PLAYER_REF);
      secondsArg = withOptionalArg("seconds", "Sekunden", ArgTypes.INTEGER);
      reasonArg = withListOptionalArg("reason", "Grund", ArgTypes.STRING);
    }

    private final String adminPermission;

    @Override
    protected void executeSync(CommandContext context) {
      if (!ensurePermission(context, adminPermission)) {
        return;
      }
      List<String> args = new ArrayList<>();
      args.add("tag");
      PlayerRef target = context.get(targetArg);
      combatService.recordPlayer(target.getUuid(), target.getUsername());
      args.add(target.getUuid().toString());
      if (context.provided(secondsArg)) {
        args.add(String.valueOf(context.get(secondsArg)));
      }
      if (context.provided(reasonArg)) {
        args.addAll(context.get(reasonArg));
      }
      CommandResult result = registry.execute(actorId(context), "combat", args);
      sendResult(context, result);
    }
  }

  private static final class CombatCleanupCommand extends CommandBase {
    private final CommandRegistry registry;

    CombatCleanupCommand(CommandRegistry registry, String adminPermission) {
      super("cleanup", "Entfernt alle Kampftags.");
      this.registry = registry;
      this.adminPermission = adminPermission;
    }

    private final String adminPermission;

    @Override
    protected void executeSync(CommandContext context) {
      if (!ensurePermission(context, adminPermission)) {
        return;
      }
      CommandResult result = registry.execute(actorId(context), "combat", List.of("cleanup"));
      sendResult(context, result);
    }
  }

  private static final class CombatQuitCommand extends CommandBase {
    private final CommandRegistry registry;
    private final CombatTagService combatService;
    private final RequiredArg<PlayerRef> targetArg;

    CombatQuitCommand(CommandRegistry registry, String adminPermission, CombatTagService combatService) {
      super("quit", "Entfernt ein Kampftag von einem Spieler.");
      this.registry = registry;
      this.combatService = combatService;
      requirePermission(adminPermission);
      targetArg = withRequiredArg("spieler", "Spieler", ArgTypes.PLAYER_REF);
    }

    private final String adminPermission;

    @Override
    protected void executeSync(CommandContext context) {
      if (!ensurePermission(context, adminPermission)) {
        return;
      }
      List<String> args = new ArrayList<>();
      args.add("quit");
      PlayerRef target = context.get(targetArg);
      combatService.recordPlayer(target.getUuid(), target.getUsername());
      args.add(target.getUuid().toString());
      CommandResult result = registry.execute(actorId(context), "combat", args);
      sendResult(context, result);
    }
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
    return lines.stream().map(line -> PREFIX + emoji + line).collect(java.util.stream.Collectors.joining("\n"));
  }

  private static String formatCoordinate(double value) {
    long rounded = Math.round(value);
    if (Math.abs(value - rounded) < 0.0001) {
      return String.valueOf(rounded);
    }
    return String.format(Locale.ROOT, "%.2f", value);
  }

  private static ProtectionPoint findProtectionPoint(FarmWorldConfig config, String name) {
    if (config == null || config.protection == null || config.protection.points == null) {
      return null;
    }
    String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
    for (ProtectionPoint point : config.protection.points) {
      if (point != null && point.name != null && point.name.toLowerCase(Locale.ROOT).equals(normalized)) {
        return point;
      }
    }
    return null;
  }
}
