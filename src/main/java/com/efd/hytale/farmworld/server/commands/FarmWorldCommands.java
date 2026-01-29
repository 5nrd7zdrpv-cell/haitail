package com.efd.hytale.farmworld.server.commands;

import com.efd.hytale.farmworld.server.CommandBridge;
import com.efd.hytale.farmworld.shared.commands.CommandRegistry;
import com.efd.hytale.farmworld.shared.commands.CommandMessages;
import com.efd.hytale.farmworld.shared.commands.CommandResult;
import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.efd.hytale.farmworld.shared.services.CombatTagService;
import com.efd.hytale.farmworld.shared.services.FarmWorldService;
import com.efd.hytale.farmworld.shared.services.ProtectionService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.math.vector.Vector3d;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

public final class FarmWorldCommands {
  private FarmWorldCommands() {}

  public static AbstractCommand createFarmCommand(
      CommandRegistry registry,
      String adminPermission,
      FarmWorldService farmWorldService,
      FarmWorldConfig config) {
    return new FarmWorldRootCommand(
        "farm",
        "Farmworld-Status und Reset-Befehle.",
        registry,
        adminPermission,
        farmWorldService,
        config,
        null,
        null);
  }

  public static AbstractCommand createProtectCommand(
      CommandRegistry registry,
      String adminPermission,
      FarmWorldConfig config,
      FarmWorldService farmWorldService,
      ProtectionService protectionService) {
    return new FarmWorldRootCommand(
        "protect",
        "Schutzzone-Status und Test.",
        registry,
        adminPermission,
        farmWorldService,
        config,
        protectionService,
        null);
  }

  public static AbstractCommand createCombatCommand(
      CommandRegistry registry,
      String adminPermission,
      CombatTagService combatService) {
    return new FarmWorldRootCommand(
        "combat",
        "Kampfstatus und Admin-Werkzeuge.",
        registry,
        adminPermission,
        null,
        null,
        null,
        combatService);
  }

  private static final class FarmWorldRootCommand extends AbstractCommand {
    private final CommandBridge bridge;
    private final String commandName;
    private final String adminPermission;
    private final FarmWorldService farmWorldService;
    private final FarmWorldConfig config;
    private final ProtectionService protectionService;
    private final CombatTagService combatService;

    private FarmWorldRootCommand(
        String name,
        String description,
        CommandRegistry registry,
        String adminPermission,
        FarmWorldService farmWorldService,
        FarmWorldConfig config,
        ProtectionService protectionService,
        CombatTagService combatService) {
      super(name, description);
      this.commandName = name.toLowerCase(Locale.ROOT);
      this.bridge = new CommandBridge(registry);
      this.adminPermission = adminPermission;
      this.farmWorldService = farmWorldService;
      this.config = config;
      this.protectionService = protectionService;
      this.combatService = combatService;
      setAllowsExtraArguments(true);
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext context) {
      String input = context.getInputString();
      String raw = buildRawCommand(input);
      CommandSender sender = context.sender();
      String actorId = resolveActorId(sender);
      recordPlayer(sender);
      CommandResult permissionFailure = checkPermission(sender, raw);
      if (permissionFailure != null) {
        context.sendMessage(Message.raw(permissionFailure.message));
        return CompletableFuture.completedFuture(null);
      }
      CommandResult special = handleSpecialCommands(context, raw, actorId);
      CommandResult result = special != null ? special : bridge.handleCommand(actorId, raw);
      context.sendMessage(Message.raw(result.message));
      return CompletableFuture.completedFuture(null);
    }

    private String buildRawCommand(String input) {
      if (input == null || input.isBlank()) {
        return commandName;
      }
      String trimmed = input.trim();
      if (trimmed.toLowerCase(Locale.ROOT).startsWith(commandName + " ")) {
        return trimmed;
      }
      if (trimmed.equalsIgnoreCase(commandName)) {
        return commandName;
      }
      return commandName + " " + trimmed;
    }

    private String resolveActorId(CommandSender sender) {
      if (sender == null) {
        return "unknown";
      }
      if (sender.getUuid() != null) {
        return sender.getUuid().toString();
      }
      String displayName = sender.getDisplayName();
      return displayName == null || displayName.isBlank() ? "unknown" : displayName;
    }

    private void recordPlayer(CommandSender sender) {
      if (combatService == null || sender == null) {
        return;
      }
      if (sender instanceof Player player && player.getPlayerRef() != null) {
        String username = player.getPlayerRef().getUsername();
        if (player.getPlayerRef().getUuid() != null && username != null && !username.isBlank()) {
          combatService.recordPlayer(player.getPlayerRef().getUuid(), username);
          return;
        }
      }
      if (sender.getUuid() == null || sender.getDisplayName() == null || sender.getDisplayName().isBlank()) {
        return;
      }
      combatService.recordPlayer(sender.getUuid(), sender.getDisplayName());
    }

    private CommandResult handleSpecialCommands(CommandContext context, String raw, String actorId) {
      String[] parts = raw.trim().split("\\s+");
      if (parts.length < 2) {
        return null;
      }
      if ("farm".equals(commandName)) {
        return handleFarmSetSpawnSelf(context, parts, actorId);
      }
      if ("protect".equals(commandName)) {
        CommandResult result = handleProtectAddSelf(context, parts, actorId);
        if (result != null) {
          return result;
        }
        return handleProtectStatusSelf(context, parts, actorId);
      }
      return null;
    }

    private CommandResult checkPermission(CommandSender sender, String raw) {
      if (adminPermission == null || adminPermission.isBlank()) {
        return null;
      }
      String[] parts = raw.trim().split("\\s+");
      if (parts.length < 2) {
        return null;
      }
      String action = parts[1].toLowerCase(Locale.ROOT);
      if (!requiresAdmin(action)) {
        return null;
      }
      if (sender == null || sender.hasPermission(adminPermission)) {
        return null;
      }
      return CommandResult.error(CommandMessages.error("Keine Berechtigung für diesen Befehl."));
    }

    private boolean requiresAdmin(String action) {
      return switch (commandName) {
        case "farm" -> switch (action) {
          case "status" -> false;
          case "reset", "setspawn" -> true;
          default -> false;
        };
        case "protect" -> switch (action) {
          case "status" -> false;
          case "add", "remove", "list", "test" -> true;
          default -> false;
        };
        case "combat" -> switch (action) {
          case "status", "canwarp" -> false;
          case "tag", "quit", "cleanup" -> true;
          default -> false;
        };
        default -> false;
      };
    }

    private CommandResult handleFarmSetSpawnSelf(CommandContext context, String[] parts, String actorId) {
      if (farmWorldService == null || config == null) {
        return null;
      }
      if (parts.length != 3 || !"setspawn".equalsIgnoreCase(parts[1]) || !"self".equalsIgnoreCase(parts[2])) {
        return null;
      }
      Player player = resolvePlayer(context);
      if (player == null) {
        return CommandResult.error(CommandMessages.error("Nur Spieler können den Spawn auf sich setzen."));
      }
      Vector3d position = resolvePosition(player);
      if (position == null || player.getWorld() == null) {
        return CommandResult.error(CommandMessages.error("Position konnte nicht ermittelt werden."));
      }
      String rawCommand = buildCommand("farm", "setspawn",
          String.valueOf(position.x),
          String.valueOf(position.y),
          String.valueOf(position.z),
          player.getWorld().getName(),
          config.farmWorld.instanceId);
      return bridge.handleCommand(actorId, rawCommand);
    }

    private CommandResult handleProtectAddSelf(CommandContext context, String[] parts, String actorId) {
      if (protectionService == null || config == null) {
        return null;
      }
      if (parts.length < 4 || !"add".equalsIgnoreCase(parts[1]) || !"self".equalsIgnoreCase(parts[2])) {
        return null;
      }
      Player player = resolvePlayer(context);
      if (player == null) {
        return CommandResult.error(CommandMessages.error("Nur Spieler können Schutzpunkte setzen."));
      }
      Vector3d position = resolvePosition(player);
      if (position == null) {
        return CommandResult.error(CommandMessages.error("Position konnte nicht ermittelt werden."));
      }
      String radius = parts[3];
      String name = parts.length > 4 ? String.join(" ", java.util.Arrays.copyOfRange(parts, 4, parts.length)) : "";
      String rawCommand = buildCommand("protect", "add",
          String.valueOf(position.x),
          String.valueOf(position.y),
          String.valueOf(position.z),
          radius,
          name);
      return bridge.handleCommand(actorId, rawCommand);
    }

    private CommandResult handleProtectStatusSelf(CommandContext context, String[] parts, String actorId) {
      if (protectionService == null || config == null) {
        return null;
      }
      if (!"status".equalsIgnoreCase(parts[1])) {
        return null;
      }
      if (parts.length != 2) {
        return null;
      }
      Player player = resolvePlayer(context);
      if (player == null) {
        return CommandResult.error(CommandMessages.error(
            "Nutzung: /protect status <x> <y> <z> [worldId] [instanceId]"));
      }
      Vector3d position = resolvePosition(player);
      if (position == null || player.getWorld() == null) {
        return CommandResult.error(CommandMessages.error("Position konnte nicht ermittelt werden."));
      }
      String rawCommand = buildCommand("protect", "status",
          String.valueOf(position.x),
          String.valueOf(position.y),
          String.valueOf(position.z),
          player.getWorld().getName(),
          config.farmWorld.instanceId);
      return bridge.handleCommand(actorId, rawCommand);
    }

    private Player resolvePlayer(CommandContext context) {
      if (context == null || !context.isPlayer()) {
        return null;
      }
      return context.senderAs(Player.class);
    }

    private Vector3d resolvePosition(Player player) {
      if (player == null || player.getTransformComponent() == null) {
        return null;
      }
      return player.getTransformComponent().getPosition();
    }

    private String buildCommand(String... parts) {
      StringJoiner joiner = new StringJoiner(" ");
      for (String part : parts) {
        if (part == null) {
          continue;
        }
        if (!part.isBlank()) {
          joiner.add(part);
        }
      }
      return joiner.toString();
    }
  }
}
