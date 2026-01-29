package com.efd.hytale.farmworld.server.commands;

import com.efd.hytale.farmworld.shared.commands.CommandRegistry;
import com.efd.hytale.farmworld.shared.commands.CommandResult;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FarmWorldCommands {
  private FarmWorldCommands() {}

  public static AbstractCommandCollection createFarmCommand(
      CommandRegistry registry,
      String usePermission,
      String adminPermission) {
    return new FarmCommand(registry, usePermission, adminPermission);
  }

  public static AbstractCommandCollection createProtectCommand(
      CommandRegistry registry,
      String usePermission,
      String adminPermission) {
    return new ProtectCommand(registry, usePermission, adminPermission);
  }

  public static AbstractCommandCollection createCombatCommand(
      CommandRegistry registry,
      String usePermission,
      String adminPermission) {
    return new CombatCommand(registry, usePermission, adminPermission);
  }

  private static String actorId(CommandContext context) {
    UUID uuid = context.sender().getUuid();
    return uuid != null ? uuid.toString() : context.sender().getDisplayName();
  }

  private static void sendResult(CommandContext context, CommandResult result) {
    context.sendMessage(Message.raw(result.message));
  }

  private static boolean ensurePermission(CommandContext context, String permission) {
    if (context.sender().hasPermission(permission)) {
      return true;
    }
    context.sendMessage(Message.raw("Missing permission: " + permission + "."));
    return false;
  }

  private static final class FarmCommand extends AbstractCommandCollection {
    FarmCommand(CommandRegistry registry, String usePermission, String adminPermission) {
      super("farm", "Farm world status and reset commands.");
      addSubCommand(new FarmStatusCommand(registry, usePermission));
      addSubCommand(new FarmResetCommand(registry, adminPermission));
      addSubCommand(new FarmSetSpawnCommand(registry, adminPermission));
    }
  }

  private static final class FarmStatusCommand extends CommandBase {
    private final CommandRegistry registry;
    private final String usePermission;

    FarmStatusCommand(CommandRegistry registry, String usePermission) {
      super("status", "Show farm world status.");
      this.registry = registry;
      this.usePermission = usePermission;
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
      super("reset", "Reset the farm world.");
      addSubCommand(new FarmResetNowCommand(registry, adminPermission));
      addSubCommand(new FarmResetScheduleCommand(registry, adminPermission));
    }
  }

  private static final class FarmResetNowCommand extends CommandBase {
    private final CommandRegistry registry;

    FarmResetNowCommand(CommandRegistry registry, String adminPermission) {
      super("now", "Reset the farm world immediately.");
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
      super("schedule", "Schedule the next farm world reset.");
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
    private final CommandRegistry registry;
    private final RequiredArg<Double> xArg;
    private final RequiredArg<Double> yArg;
    private final RequiredArg<Double> zArg;
    private final OptionalArg<String> worldIdArg;
    private final OptionalArg<String> instanceIdArg;

    FarmSetSpawnCommand(CommandRegistry registry, String adminPermission) {
      super("setspawn", "Set the farm world spawn.");
      this.registry = registry;
      this.adminPermission = adminPermission;
      xArg = withRequiredArg("x", "Spawn X", ArgTypes.DOUBLE);
      yArg = withRequiredArg("y", "Spawn Y", ArgTypes.DOUBLE);
      zArg = withRequiredArg("z", "Spawn Z", ArgTypes.DOUBLE);
      worldIdArg = withOptionalArg("worldId", "World Id", ArgTypes.STRING);
      instanceIdArg = withOptionalArg("instanceId", "Instance Id", ArgTypes.STRING);
    }

    private final String adminPermission;

    @Override
    protected void executeSync(CommandContext context) {
      if (!ensurePermission(context, adminPermission)) {
        return;
      }
      List<String> args = new ArrayList<>();
      args.add("setspawn");
      args.add(String.valueOf(context.get(xArg)));
      args.add(String.valueOf(context.get(yArg)));
      args.add(String.valueOf(context.get(zArg)));
      if (context.provided(worldIdArg)) {
        args.add(context.get(worldIdArg));
      }
      if (context.provided(instanceIdArg)) {
        args.add(context.get(instanceIdArg));
      }
      CommandResult result = registry.execute(actorId(context), "farm", args);
      sendResult(context, result);
    }
  }

  private static final class ProtectCommand extends AbstractCommandCollection {
    ProtectCommand(CommandRegistry registry, String usePermission, String adminPermission) {
      super("protect", "Protection zone status and test.");
      addSubCommand(new ProtectStatusCommand(registry, usePermission));
      addSubCommand(new ProtectTestCommand(registry, adminPermission));
    }
  }

  private static final class ProtectStatusCommand extends CommandBase {
    private final CommandRegistry registry;
    private final String usePermission;

    ProtectStatusCommand(CommandRegistry registry, String usePermission) {
      super("status", "Show protection status.");
      this.registry = registry;
      this.usePermission = usePermission;
    }

    @Override
    protected void executeSync(CommandContext context) {
      if (!ensurePermission(context, usePermission)) {
        return;
      }
      CommandResult result = registry.execute(actorId(context), "protect", List.of("status"));
      sendResult(context, result);
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
      super("test", "Test a protection action.");
      this.registry = registry;
      this.adminPermission = adminPermission;
      actionArg = withRequiredArg("action", "Protection action", ArgTypes.STRING);
      xArg = withRequiredArg("x", "X coordinate", ArgTypes.DOUBLE);
      yArg = withRequiredArg("y", "Y coordinate", ArgTypes.DOUBLE);
      zArg = withRequiredArg("z", "Z coordinate", ArgTypes.DOUBLE);
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
    CombatCommand(CommandRegistry registry, String usePermission, String adminPermission) {
      super("combat", "Combat tag diagnostics and admin helpers.");
      addSubCommand(new CombatStatusCommand(registry, usePermission));
      addSubCommand(new CombatCanWarpCommand(registry, usePermission));
      addSubCommand(new CombatTagCommand(registry, adminPermission));
      addSubCommand(new CombatCleanupCommand(registry, adminPermission));
      addSubCommand(new CombatQuitCommand(registry, adminPermission));
    }
  }

  private static final class CombatStatusCommand extends CommandBase {
    private final CommandRegistry registry;
    private final OptionalArg<String> targetArg;
    private final String usePermission;

    CombatStatusCommand(CommandRegistry registry, String usePermission) {
      super("status", "Check combat tag status.");
      this.registry = registry;
      this.usePermission = usePermission;
      targetArg = withOptionalArg("playerId", "Player Id", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(CommandContext context) {
      if (!ensurePermission(context, usePermission)) {
        return;
      }
      List<String> args = new ArrayList<>();
      args.add("status");
      if (context.provided(targetArg)) {
        args.add(context.get(targetArg));
      }
      CommandResult result = registry.execute(actorId(context), "combat", args);
      sendResult(context, result);
    }
  }

  private static final class CombatCanWarpCommand extends CommandBase {
    private final CommandRegistry registry;
    private final OptionalArg<String> targetArg;
    private final String usePermission;

    CombatCanWarpCommand(CommandRegistry registry, String usePermission) {
      super("canwarp", "Check if a player can warp.");
      this.registry = registry;
      this.usePermission = usePermission;
      targetArg = withOptionalArg("playerId", "Player Id", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(CommandContext context) {
      if (!ensurePermission(context, usePermission)) {
        return;
      }
      List<String> args = new ArrayList<>();
      args.add("canwarp");
      if (context.provided(targetArg)) {
        args.add(context.get(targetArg));
      }
      CommandResult result = registry.execute(actorId(context), "combat", args);
      sendResult(context, result);
    }
  }

  private static final class CombatTagCommand extends CommandBase {
    private final CommandRegistry registry;
    private final RequiredArg<String> targetArg;
    private final OptionalArg<Integer> secondsArg;
    private final OptionalArg<List<String>> reasonArg;

    CombatTagCommand(CommandRegistry registry, String adminPermission) {
      super("tag", "Apply a combat tag.");
      this.registry = registry;
      this.adminPermission = adminPermission;
      targetArg = withRequiredArg("playerId", "Player Id", ArgTypes.STRING);
      secondsArg = withOptionalArg("seconds", "Seconds", ArgTypes.INTEGER);
      reasonArg = withListOptionalArg("reason", "Reason", ArgTypes.STRING);
    }

    private final String adminPermission;

    @Override
    protected void executeSync(CommandContext context) {
      if (!ensurePermission(context, adminPermission)) {
        return;
      }
      List<String> args = new ArrayList<>();
      args.add("tag");
      args.add(context.get(targetArg));
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
      super("cleanup", "Clear all combat tags.");
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
    private final RequiredArg<String> targetArg;

    CombatQuitCommand(CommandRegistry registry, String adminPermission) {
      super("quit", "Clear a combat tag for a player.");
      this.registry = registry;
      this.adminPermission = adminPermission;
      targetArg = withRequiredArg("playerId", "Player Id", ArgTypes.STRING);
    }

    private final String adminPermission;

    @Override
    protected void executeSync(CommandContext context) {
      if (!ensurePermission(context, adminPermission)) {
        return;
      }
      List<String> args = new ArrayList<>();
      args.add("quit");
      args.add(context.get(targetArg));
      CommandResult result = registry.execute(actorId(context), "combat", args);
      sendResult(context, result);
    }
  }
}
